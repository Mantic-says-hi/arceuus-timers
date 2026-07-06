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
import net.runelite.api.KeyCode;
import net.runelite.api.NPC;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.MenuOpened;
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

import java.awt.Rectangle;
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

	private boolean impishThrall;

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
	private ConfigManager configManager;

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
		applyCooldownIcons();
		applyDeathChargeIcon();
		controllers.get(ArceuusSpell.MARK_COOLDOWN).setDarkenIcon(true);
		overlayManager.add(deathChargeEffectOverlay);
		//Register at the front to intercept the drag before the click reaches the game
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
		//Always track death charge so the active boxes can restore when the config is turned back on
		spellVarbits.add(VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE);
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
			case ArceuusTimersConfig.SHOW_DEATH_CHARGE:
				toggleDeathChargeBoxes();
				return;
			case ArceuusTimersConfig.STACK_DEATH_CHARGE:
				updateDeathChargeBoxes();
				return;
			case ArceuusTimersConfig.DARK_DEATH_CHARGE_COOLDOWN:
				applyCooldownIcons();
				return;
			case ArceuusTimersConfig.DEATH_CHARGE_SMALL_ICON:
				applyDeathChargeIcon();
				return;
			case ArceuusTimersConfig.IMPISH_THRALL_ICONS:
				applyThrallIcon();
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

	private void toggleDeathChargeBoxes() {
		DeathChargeController deathCharge = (DeathChargeController) controllers.get(ArceuusSpell.CHARGE);
		deathCharge.setVisible(config.showDeathChargeActive());
	}

	private void applyCooldownIcons() {
		boolean dark = config.darkDeathChargeCooldown();
		applyCooldownIcon(ArceuusSpell.CHARGE_COOLDOWN, dark, "/death_charge_cooldown_black.png", "/death_charge_cooldown.png");
		applyCooldownIcon(ArceuusSpell.SHADOW_COOLDOWN, dark, "/shadow_veil_cooldown_black.png", "/shadow_veil_cooldown.png");
		applyCooldownIcon(ArceuusSpell.WARD_COOLDOWN, dark, "/ward_cooldown_black.png", "/ward_cooldown.png");
	}

	private void applyCooldownIcon(ArceuusSpell spell, boolean dark, String darkFileName, String fileName) {
		SpellController cooldown = controllers.get(spell);
		cooldown.setFileName(dark ? darkFileName : fileName);
		cooldown.refreshIcon();
	}

	private void applyDeathChargeIcon() {
		SpellController charge = controllers.get(ArceuusSpell.CHARGE);
		charge.setFileName(config.deathChargeSmallIcon() ? "/death_charge_small.png" : "/death_charge.png");
		charge.refreshIcon();
	}

	private void applyThrallIcon() {
		ThrallController thrall = (ThrallController) controllers.get(ArceuusSpell.THRALL);
		String base = thrall.getFileName().replace("_imp.png", ".png");
		thrall.setFileName(impishThrall && config.impishThrallIcons() ? base.replace(".png", "_imp.png") : base);
		thrall.refreshIcon();
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
		if (player.hasSpotAnim(SpotanimID.DEATH_CHARGE_UPGRADE_CAST_SPOTANIM)) {
			markDetectedCaster(name, 2);
		} else if (player.hasSpotAnim(SpotanimID.DEATH_CHARGE_CAST_SPOTANIM)) {
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
		int tick = client.getTickCount();
		if (detected.lastConsumeTick == tick) return;
		detected.lastConsumeTick = tick;
		if (detected.charges > 0) detected.charges--;
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
			impishThrall = message.contains("impish");
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

	@Subscribe
	public void onMenuOpened(MenuOpened event) {
		if (!config.showDeathChargeOnPlayer()) return;
		if (!client.isKeyPressed(KeyCode.KC_SHIFT)) return;
		Rectangle bounds = deathChargeEffectOverlay.getLocalIconBounds();
		if (bounds == null) return;
		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null || !bounds.contains(mouse.getX(), mouse.getY())) return;

		boolean reposition = config.deathChargeReposition();
		String target = "Death Charge icon";

		if (reposition) {
			client.getMenu().createMenuEntry(-1)
				.setOption("Reset position")
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e -> {
					configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.DEATH_CHARGE_OFFSET_X, 0);
					configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.DEATH_CHARGE_OFFSET_Y, 0);
				});
		}

		client.getMenu().createMenuEntry(-1)
			.setOption(reposition ? "Stop repositioning" : "Reposition")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.DEATH_CHARGE_REPOSITION, !reposition));

		client.getMenu().createMenuEntry(-1)
			.setOption("Hide")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> {
				configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.SHOW_DEATH_CHARGE_ON_PLAYER, false);
				configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.SHOW_DEATH_CHARGE_PARTY, false);
				configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.SHOW_DEATH_CHARGE_OTHERS, false);
			});
	}

	private void gameMessageCorrLockRelease() {
		CorruptionController corruption = (CorruptionController) controllers.get(ArceuusSpell.CORRUPTION);
		corruption.setIconLocked(false);
	}

	private void gameMessageMarkOfDarkness() {
		if (config.showMarkTimer()) {
			VariableTimerController mark = (VariableTimerController) controllers.get(ArceuusSpell.MARK);
			mark.nonVarbitChange();
		}
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
		applyThrallIcon();
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
		int lastConsumeTick = -1;

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
