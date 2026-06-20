package com.arceuustimers;

import com.arceuustimers.controllers.CorruptionController;
import com.arceuustimers.controllers.DeathChargeController;
import com.arceuustimers.controllers.SpellController;
import com.arceuustimers.controllers.StandardController;
import com.arceuustimers.controllers.ThrallController;
import com.arceuustimers.controllers.VariableTimerController;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@PluginDescriptor(
	name = "Arceuus Timers",
	description = "Arceuus spellbook timers with an alternate design to the 'Timers' plugin.",
	tags = "timer,arceuus,spellbook,thrall,death charge"
)
public class ArceuusTimersPlugin extends Plugin {
	private static final int VARBIT_UP = 1;
	private static final String RESURRECT_PREFIX = "You resurrect a ";
	private static final String RESURRECT_SUFFIX = " thrall.";

	private final Map<ArceuusSpell, SpellController> controllers = new EnumMap<>(ArceuusSpell.class);
	private final Set<Integer> spellVarbits = new HashSet<>();

	@Getter
	private final Map<Long, Integer> partyDeathCharges = new ConcurrentHashMap<>();

	private final Map<String, DetectedDeathCharge> detectedDeathCharges = new HashMap<>();
	private static final int DEATH_CHARGE_DURATION_TICKS = 102;

	private static final ThrallType[] THRALL_TYPES = {
		new ThrallType("/ghost.png", "Active thrall ( Mage )"),       // VV 0, 3, 6
		new ThrallType("/skeleton.png", "Active thrall ( Ranged )"), // VV 1, 4, 7
		new ThrallType("/zombie.png", "Active thrall ( Melee )")      // VV 2, 5, 8
	};

	private final List<MessageHandler> gameMessageHandlers = Arrays.asList(
		new MessageHandler("You can only cast corruption spells every 30 seconds.", false, this::gameMessageCorrLockRelease),
		new MessageHandler("thrall returns to the grave.", true, this::closeThrallInfoBox),
		new MessageHandler("Your Ward of Arceuus has expired.", false, () -> expiredGameMessage(ArceuusSpell.WARD)),
		new MessageHandler("Your Shadow Veil has faded away.", false, () -> expiredGameMessage(ArceuusSpell.SHADOW)),
		new MessageHandler("You have placed a Mark of Darkness upon yourself.", false, this::gameMessageMarkOfDarkness),
		new MessageHandler("Your Mark of Darkness has faded away.", false, () -> expiredGameMessage(ArceuusSpell.MARK))
	);

	private final Map<String, Runnable> menuOptionHandlers = ImmutableMap.of(
		"Greater Corruption", () -> modifyCorruptionData("/greater.png", "Greater Corruption cooldown"),
		"Lesser Corruption", () -> modifyCorruptionData("/lesser.png", "Lesser Corruption cooldown"),
		"Sinister Offering", () -> modifyOfferingData("/sinister_offering.png", "Sinister Offering cooldown"),
		"Demonic Offering", () -> modifyOfferingData("/demonic_offering.png", "Demonic Offering cooldown")
	);

	@Inject
	private Client client;

	@Inject
	private ArceuusTimersConfig config;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DeathChargeEffectOverlay deathChargeEffectOverlay;

	@Inject
	private DeathChargeIconDrag deathChargeIconDrag;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	@Inject
	private ClientThread clientThread;

	@Provides
	ArceuusTimersConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ArceuusTimersConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		createSpellControllers();
		initialSpellVarbits();
		applyDeathChargeCooldownIcon();
		controllers.get(ArceuusSpell.MARK_COOLDOWN).setDarkenIcon(true);
		overlayManager.add(deathChargeEffectOverlay);
		//Register at the front so we intercept the drag before the click reaches the game
		mouseManager.registerMouseListener(0, deathChargeIconDrag);
		wsClient.registerMessage(DeathChargeUpdate.class);
	}

	@Override
	protected void shutDown() throws Exception {
		wsClient.unregisterMessage(DeathChargeUpdate.class);
		partyDeathCharges.clear();
		mouseManager.unregisterMouseListener(deathChargeIconDrag);
		overlayManager.remove(deathChargeEffectOverlay);
		//Only remove the active InfoBoxes that are created by this plugin
		removeActiveInfoboxes();
		spellVarbits.clear();
		controllers.clear();
	}

	private void createSpellControllers() {
		for (ArceuusSpell spell : ArceuusSpell.values()) {
			SpellController controller = spell.createController(infoBoxManager, this, client);
			controller.setSpellName(spell.name());
			controllers.put(spell, controller);
		}
	}

	private void initialSpellVarbits() {
		for (ArceuusSpell spell : ArceuusSpell.values()) {
			if (spell.hasVarbit() && spell.isEnabled(config)) spellVarbits.add(spell.getVarbitId());
		}
	}

	private void removeActiveInfoboxes() {
		for (SpellController controller : controllers.values()) controller.shutdown();
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		for (SpellController controller : controllers.values()) {
			if (controller.isActive()) controller.updateTime();
		}
		detectedDeathCharges.values().removeIf(detected -> detected.expiryTick <= client.getTickCount());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals(ArceuusTimersConfig.GROUP)) return;

		switch (event.getKey()) {
			case ArceuusTimersConfig.STACK_DEATH_CHARGE:
				updateDeathChargeBoxes();
				return;
			case ArceuusTimersConfig.DARK_DEATH_CHARGE_COOLDOWN:
				applyDeathChargeCooldownIcon();
				return;
			case ArceuusTimersConfig.SPRITE_SIZED_ICONS:
				refreshAllIcons();
				return;
			case ArceuusTimersConfig.SHOW_SHADOW_VEIL:
				if (!config.showShadowVeilCooldown() && !config.showShadowVeil()) {
					updateConfigChange(false, VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
				} else if (!config.showShadowVeilCooldown() && config.showShadowVeil()) {
					//Need to force a true here for the cooldown in case it is not on, otherwise shadow veil
					//won't function properly if it is recast after a cooldown while it is still active
					updateConfigChange(true, VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
					updateConfigChange(config.showShadowVeil(), VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE, ArceuusSpell.SHADOW);
				} else {
					updateConfigChange(config.showShadowVeil(), VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE, ArceuusSpell.SHADOW);
				}
				return;
			case ArceuusTimersConfig.SHOW_SHADOW_VEIL_COOLDOWN:
				if (config.showShadowVeil() && !config.showShadowVeilCooldown()) {
					controllers.get(ArceuusSpell.SHADOW_COOLDOWN).shutdown();
				} else {
					updateConfigChange(config.showShadowVeilCooldown(), VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
				}
				return;
			case ArceuusTimersConfig.SHOW_WARD_OF_ARCEUUS_COOLDOWN:
				if (config.showWardOfArceuus() && !config.showWardOfArceuusCooldown()) {
					controllers.get(ArceuusSpell.WARD_COOLDOWN).shutdown();
				} else {
					updateConfigChange(config.showWardOfArceuusCooldown(), VarbitID.ARCEUUS_WARD_COOLDOWN, ArceuusSpell.WARD_COOLDOWN);
				}
				return;
			default:
				break;
		}

		ArceuusSpell spell = ArceuusSpell.fromConfigKey(event.getKey());
		if (spell == null) return;
		if (spell.hasVarbit()) {
			updateConfigChange(spell.isEnabled(config), spell.getVarbitId(), spell);
		} else if (!spell.isEnabled(config)) {
			controllers.get(spell).shutdown();
		}
	}

	private void updateConfigChange(boolean enabled, int varbitId, ArceuusSpell spell) {
		if (enabled) {
			spellVarbits.add(varbitId);
		} else {
			spellVarbits.remove(varbitId);
			controllers.get(spell).shutdown();
		}
	}

	private void updateDeathChargeBoxes() {
		DeathChargeController deathCharge = (DeathChargeController) controllers.get(ArceuusSpell.CHARGE);
		deathCharge.reCreate();
	}

	private void applyDeathChargeCooldownIcon() {
		SpellController cooldown = controllers.get(ArceuusSpell.CHARGE_COOLDOWN);
		boolean dark = config.darkDeathChargeCooldown();
		cooldown.setDarkenIcon(dark);
		cooldown.setFileName(dark ? "/death_charge.png" : "/death_charge_cooldown.png");
		cooldown.refreshIcon();
	}

	private void refreshAllIcons() {
		for (SpellController controller : controllers.values()) controller.refreshIcon();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		int varbit = event.getVarbitId();
		if (varbit == VarbitID.ARCEUUS_RESURRECTION_COOLDOWN && event.getValue() == VARBIT_UP) closeThrallInfoBox();
		if (varbit == VarbitID.ARCEUUS_RESURRECTION_USED) onThrallUsed(event.getValue());
		if (varbit == VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE && partyService.isInParty()) partyService.send(new DeathChargeUpdate(event.getValue()));
		handleSpellVarbitChanges(varbit, event);
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event) {
		if (!(event.getActor() instanceof Player)) return;
		Player player = (Player) event.getActor();
		if (player == client.getLocalPlayer() || player.getName() == null) return;
		String name = Text.standardize(player.getName());
		if (player.hasSpotAnim(SpotanimID.DEATH_CHARGE_CAST_SPOTANIM)
			|| player.hasSpotAnim(SpotanimID.DEATH_CHARGE_UPGRADE_CAST_SPOTANIM)) {
			markDetectedCaster(name, 1);
		}
		if (player.hasSpotAnim(SpotanimID.DEATH_CHARGE_END_SPOTANIM)
			|| player.hasSpotAnim(SpotanimID.DEATH_CHARGE_END_UPGRADE_SPOTANIM)) {
			consumeDetectedCharge(name);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event) {
		if (!(event.getActor() instanceof NPC) || detectedDeathCharges.isEmpty()) return;
		NPC npc = (NPC) event.getActor();
		for (Player player : client.getTopLevelWorldView().players()) {
			if (player == null || player.getName() == null) continue;
			String name = Text.standardize(player.getName());
			if (!detectedDeathCharges.containsKey(name)) continue;
			if (player.getInteracting() == npc || npc.getInteracting() == player) consumeDetectedCharge(name);
		}
	}

	private void markDetectedCaster(String name, int charges) {
		detectedDeathCharges.put(name, new DetectedDeathCharge(charges, client.getTickCount() + DEATH_CHARGE_DURATION_TICKS));
	}

	private void consumeDetectedCharge(String name) {
		DetectedDeathCharge detected = detectedDeathCharges.get(name);
		if (detected == null) return;
		detected.charges--;
		if (detected.charges <= 0) detectedDeathCharges.remove(name);
	}

	public Map<String, Integer> getDetectedDeathCharges() {
		Map<String, Integer> charges = new HashMap<>();
		for (Map.Entry<String, DetectedDeathCharge> entry : detectedDeathCharges.entrySet()) charges.put(entry.getKey(), entry.getValue().charges);
		return charges;
	}

	@Subscribe
	public void onDeathChargeUpdate(DeathChargeUpdate update) {
		if (update.getCharges() > 0) {
			partyDeathCharges.put(update.getMemberId(), update.getCharges());
		} else {
			partyDeathCharges.remove(update.getMemberId());
		}
		clientThread.invokeLater(() -> {
			PartyMember member = partyService.getMemberById(update.getMemberId());
			if (member != null && member.getDisplayName() != null) detectedDeathCharges.remove(Text.standardize(member.getDisplayName()));
		});
	}

	@Subscribe
	public void onUserSync(UserSync event) {
		clientThread.invokeLater(() -> {
			if (partyService.isInParty()) partyService.send(new DeathChargeUpdate(client.getVarbitValue(VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE)));
		});
	}

	@Subscribe
	public void onUserPart(UserPart event) {
		partyDeathCharges.remove(event.getMemberId());
	}

	@Subscribe
	public void onPartyChanged(PartyChanged event) {
		partyDeathCharges.clear();
	}

	private void onThrallUsed(int varbitValue) {
		if (varbitValue < 0 || varbitValue > 8) return;
		ThrallType thrall = THRALL_TYPES[varbitValue % 3];
		ThrallController controller = (ThrallController) controllers.get(ArceuusSpell.THRALL);
		controller.setFileName(thrall.fileName);
		controller.setTooltip(thrall.tooltip);
	}

	private void handleSpellVarbitChanges(int varbit, VarbitChanged event) {
		if (!spellVarbits.contains(varbit)) return;
		if (handleCooldownConfigs(varbit)) {
			SpellController controller = controllers.get(ArceuusSpell.fromVarbit(varbit));
			controller.varbitChange(client.getVarbitValue(varbit));
		}
		//Following two statements are for when a ward/veil is still active but
		//the cooldown has ended (i.e. boosted magic level is over 83) and is recast
		if (varbit == VarbitID.ARCEUUS_WARD_COOLDOWN && event.getValue() == VARBIT_UP && config.showWardOfArceuus()) {
			VariableTimerController ward = (VariableTimerController) controllers.get(ArceuusSpell.WARD);
			ward.nonVarbitChange();
		}
		if (varbit == VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN && event.getValue() == VARBIT_UP && config.showShadowVeil()) {
			SpellController veil = controllers.get(ArceuusSpell.SHADOW);
			veil.varbitChange(client.getVarbitValue(VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE));
		}
	}

	private boolean handleCooldownConfigs(int varbit) {
		//These spells have cases where the timer relies on the cooldown varbit to re-trigger when already active
		//These boolean checks are to make sure the cooldown infoboxes do not get created if the config for
		//them is turned off but the program is allowed to process actions based off the cooldown varbits.
		boolean blockVeilCooldown = config.showShadowVeil() && !config.showShadowVeilCooldown() && varbit == VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN;
		boolean blockWardCooldown = config.showWardOfArceuus() && !config.showWardOfArceuusCooldown() && varbit == VarbitID.ARCEUUS_WARD_COOLDOWN;
		return !(blockVeilCooldown || blockWardCooldown);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		if (!event.getType().equals(ChatMessageType.GAMEMESSAGE)) return;
		String message = event.getMessage().replaceAll("<col=[a-z0-9]+>", "").replaceAll("</col>", "");

		if (message.startsWith(RESURRECT_PREFIX) && message.endsWith(RESURRECT_SUFFIX)) {
			if (config.showThrall()) {
				createThrall();
				log.debug("ARCEUUS TIMERS - Thrall resurrect message handled: {}", message);
			}
			return;
		}

		for (MessageHandler handler : gameMessageHandlers) {
			if (handler.isSuffix ? message.endsWith(handler.message) : message.equals(handler.message)) {
				handler.action.run();
				log.debug("ARCEUUS TIMERS - Chat message handled: {}", message);
				return;
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked cast) {
		String option = cast.getMenuTarget().replaceAll("<col=[a-z0-9]+>", "").replaceAll("</col>", "");
		Runnable handler = menuOptionHandlers.get(option);
		if (handler != null) handler.run();
	}

	private void gameMessageCorrLockRelease() {
		CorruptionController corruption = (CorruptionController) controllers.get(ArceuusSpell.CORRUPTION);
		corruption.setIconLocked(false);
	}

	private void gameMessageMarkOfDarkness() {
		VariableTimerController mark = (VariableTimerController) controllers.get(ArceuusSpell.MARK);
		mark.nonVarbitChange();
		if (config.showMarkCooldown()) {
			SpellController markCooldown = controllers.get(ArceuusSpell.MARK_COOLDOWN);
			markCooldown.shutdown();
			markCooldown.varbitChange(VARBIT_UP);
		}
	}

	private void expiredGameMessage(ArceuusSpell spell) {
		VariableTimerController responder = (VariableTimerController) controllers.get(spell);
		responder.chatExpiredResponse();
	}

	private void createThrall() {
		ThrallController thrall = (ThrallController) controllers.get(ArceuusSpell.THRALL);
		thrall.createThrall();
	}

	private void closeThrallInfoBox() {
		controllers.get(ArceuusSpell.THRALL).shutdown();
	}

	private void modifyCorruptionData(String fileName, String tooltip) {
		CorruptionController corruption = (CorruptionController) controllers.get(ArceuusSpell.CORRUPTION);
		if (corruption.isIconLocked()) return;
		corruption.setTooltip(tooltip);
		corruption.setFileName(fileName);
		corruption.setIconLocked(true);
	}

	private void modifyOfferingData(String fileName, String tooltip) {
		StandardController offering = (StandardController) controllers.get(ArceuusSpell.OFFERING);
		offering.setTooltip(tooltip);
		offering.setFileName(fileName);
	}

	public ArceuusTimersConfig getConfig() {
		return config;
	}

	private static class ThrallType {
		final String fileName;
		final String tooltip;

		ThrallType(String fileName, String tooltip) {
			this.fileName = fileName;
			this.tooltip = tooltip;
		}
	}

	private static class DetectedDeathCharge {
		int charges;
		final int expiryTick;

		DetectedDeathCharge(int charges, int expiryTick) {
			this.charges = charges;
			this.expiryTick = expiryTick;
		}
	}

	private static class MessageHandler {
		final String message;
		final boolean isSuffix;
		final Runnable action;

		MessageHandler(String message, boolean isSuffix, Runnable action) {
			this.message = message;
			this.isSuffix = isSuffix;
			this.action = action;
		}
	}
}
