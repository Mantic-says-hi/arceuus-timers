package com.arceuustimers;

import com.arceuustimers.controllers.CorruptionController;
import com.arceuustimers.controllers.DeathChargeController;
import com.arceuustimers.controllers.MarkController;
import com.arceuustimers.controllers.SpellController;
import com.arceuustimers.controllers.StandardController;
import com.arceuustimers.controllers.ThrallController;
import com.arceuustimers.controllers.VariableTimerController;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Arceuus Timers",
	description = "Arceuus spellbook timers with an alternate design to the 'Timers' plugin.",
	tags = "timer,arceuus,spellbook,thrall,death charge,arceuus,timers"
)
public class ArceuusTimersPlugin extends Plugin
{
	private final HashMap<ArceuusSpell, SpellController> data = new HashMap<>();
	private final HashSet<Integer> spellVarbits = new HashSet<>();

	//private static final String TIMERS_PLUGIN = "timers";
	//private static final String SHOW_ARCEUUS = "showArceuus";
	//private static final String SHOW_ARCEUUS_COOLDOWN = "showArceuusCooldown";
	//private static final String SHOW_SPELLBOOK_SWAP = "showSpellbookSwap";
    private static final int VARBIT_UP = 1;
	private static final double NO_TEXT = -1.0;
	private static final double CD_LONG = 61.2;
	private static final double CD_MED = 31.2;
	private static final double CD_SHORT = 10.8;
	private static final double CD_TINY = 6.0;
	private static final double SBS_TIME = 120;

	private static final ThrallType[] THRALL_TYPES = {
			new ThrallType("/ghost.png", "Active thrall ( Ghost )"),       // VV 0, 3, 6
			new ThrallType("/skeleton.png", "Active thrall ( Skeleton )"), // VV 1, 4, 7
			new ThrallType("/zombie.png", "Active thrall ( Zombie )")      // VV 2, 5, 8
	};

	private final List<MessageHandler> gameMessageIconLockHandlers = Arrays.asList(
			new MessageHandler("You can only cast corruption spells every 30 seconds.", false, this::gameMessageCorrLockRelease),
			new MessageHandler("You resurrect a lesser ghostly thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a lesser skeletal thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a lesser zombified thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a superior ghostly thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a superior skeletal thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a superior zombified thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a greater ghostly thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a greater skeletal thrall.", false, this::createThrall),
			new MessageHandler("You resurrect a greater zombified thrall.", false, this::createThrall),
			new MessageHandler("thrall returns to the grave.", true, this::closeThrallInfoBox),
			new MessageHandler("Your Ward of Arceuus has expired.", false, () -> expiredGameMessage(ArceuusSpell.WARD)),
			new MessageHandler("Your Shadow Veil has faded away.", false, () -> expiredGameMessage(ArceuusSpell.SHADOW)),
			new MessageHandler("You have placed a Mark of Darkness upon yourself.", false, this::gameMessageMarkOfDarkness),
			new MessageHandler("Your Mark of Darkness has faded away.", false, this::expiredMarkMessage)
	);

	private final Map<String, Runnable> menuOptionHandlers = new HashMap<String, Runnable>() {{
		put("Greater Corruption", () -> modifyCorruptionData("/greater.png", "Greater Corruption cooldown"));
		put("Lesser Corruption", () -> modifyCorruptionData("/lesser.png", "Lesser Corruption cooldown"));
		put("Sinister Offering", () -> modifyOfferingData("/sinister_offering.png", "Sinister Offering cooldown"));
		put("Demonic Offering", () -> modifyOfferingData("/demonic_offering.png", "Demonic Offering cooldown"));
	}};

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ArceuusTimersConfig config;

	@Inject
	private ArceuusTimersOverlay overlay;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private OverlayManager overlayManager;

	@Provides
	ArceuusTimersConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ArceuusTimersConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add( overlay );
		createSpellControllers();
		initialSpellVarbits();
		//Turn off 'Timers' plugin implementation
		//setTimersPlugin(false);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove( overlay );
		//Only remove the active InfoBoxes that are created by this plugin
		removeActiveInfoboxes();
		//Turn on 'Timers' plugin implementation to replace this one
		//setTimersPlugin(true);
	}

	/*private void setTimersPlugin(boolean state)
	{
		configManager.setConfiguration(TIMERS_PLUGIN, SHOW_ARCEUUS, state);
		configManager.setConfiguration(TIMERS_PLUGIN, SHOW_ARCEUUS_COOLDOWN, state);
		configManager.setConfiguration(TIMERS_PLUGIN, SHOW_SPELLBOOK_SWAP, state);
	}*/

	private void removeActiveInfoboxes()
	{
		for (ArceuusSpell identifier : ArceuusSpell.values()) {
			SpellController spellController = data.get( identifier );
			spellController.shutdown();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) { updateActiveSpells(); }

	private void updateActiveSpells() {
		for (ArceuusSpell identifier : ArceuusSpell.values()) {
			SpellController spellController = data.get(identifier);
			if (spellController.getActive()) {
				spellController.updateTime();
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals(ArceuusTimersConfig.GROUP)) {
			return;
		}

		switch (event.getKey()) {
			case ArceuusTimersConfig.SHOW_DEATH_CHARGE:
				updateConfigChange(config.showDeathChargeActive(),  VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE, ArceuusSpell.CHARGE);
				break;
			case ArceuusTimersConfig.SHOW_DEATH_CHARGE_COOLDOWN:
				updateConfigChange(config.showDeathChargeCooldown(),VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN, ArceuusSpell.CHARGE_COOLDOWN);
				break;
			case ArceuusTimersConfig.SHOW_THRALL:
				updateConfigChange(config.showThrall(), ArceuusSpell.THRALL);
				break;
			case ArceuusTimersConfig.SHOW_THRALL_COOLDOWN:
				updateConfigChange(config.showThrallCooldown(), VarbitID.ARCEUUS_RESURRECTION_COOLDOWN, ArceuusSpell.THRALL_COOLDOWN);
				break;
			case ArceuusTimersConfig.SHOW_CORRUPTION_COOLDOWN:
				updateConfigChange(config.showCorruptionCooldown(), VarbitID.ARCEUUS_CORRUPTION_COOLDOWN, ArceuusSpell.CORRUPTION);
				break;
			case ArceuusTimersConfig.SHOW_VILE_VIGOUR_COOLDOWN:
				updateConfigChange(config.showVileVigourCooldown(), VarbitID.ARCEUUS_VILE_VIGOUR_COOLDOWN, ArceuusSpell.VIGOUR);
				break;
			case ArceuusTimersConfig.SHOW_SHADOW_VEIL:
				if(!config.showShadowVeilCooldown() && !config.showShadowVeil())
				{
					updateConfigChange(config.showShadowVeilCooldown(), VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
				}else if((!config.showShadowVeilCooldown() && config.showShadowVeil()))
				{
					//Need to force a true here for the cooldown in case it is not on, otherwise shadow veil
					//won't function properly if it is recast after a cooldown while it is still active
					updateConfigChange(true, VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
					updateConfigChange(config.showShadowVeil(), VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE, ArceuusSpell.SHADOW);
				}else{
					updateConfigChange(config.showShadowVeil(), VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE, ArceuusSpell.SHADOW);
				}
				break;
			case ArceuusTimersConfig.SHOW_SHADOW_VEIL_COOLDOWN:
				if(config.showShadowVeil() && !config.showShadowVeilCooldown()) {
					updateConfigChange(ArceuusSpell.SHADOW_COOLDOWN);
				}else{
					updateConfigChange(config.showShadowVeilCooldown(), VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
				}
				break;
			case ArceuusTimersConfig.SHOW_DARK_LURE_COOLDOWN:
				updateConfigChange(config.showDarkLureCooldown(), VarbitID.ARCEUUS_DARK_LURE_COOLDOWN, ArceuusSpell.LURE);
				break;
			case ArceuusTimersConfig.SHOW_WARD_OF_ARCEUUS_COOLDOWN:
				if(config.showWardOfArceuus() && !config.showWardOfArceuusCooldown()) {
					updateConfigChange(ArceuusSpell.WARD_COOLDOWN);
				}else{
					updateConfigChange(config.showWardOfArceuusCooldown(), VarbitID.ARCEUUS_WARD_COOLDOWN, ArceuusSpell.WARD_COOLDOWN);
				}
				break;
			case ArceuusTimersConfig.SHOW_WARD_OF_ARCEUUS:
				updateConfigChange(config.showWardOfArceuus(), ArceuusSpell.WARD);
				break;
			case ArceuusTimersConfig.SHOW_OFFERINGS_COOLDOWN:
				updateConfigChange(config.showOfferingsCooldown(), VarbitID.ARCEUUS_OFFERING_COOLDOWN, ArceuusSpell.OFFERING);
				break;
			case ArceuusTimersConfig.SHOW_MARK_OF_DARKNESS:
				updateConfigChange(config.showMarkTimer(), ArceuusSpell.MARK);
		}
	}

	private void updateConfigChange(boolean enabled, int bits, ArceuusSpell identifier)
	{
		if(enabled) {
			spellVarbits.add(bits);
		}else {
			spellVarbits.remove(bits);
			data.get(identifier).shutdown();
		}
	}

	private void updateConfigChange(ArceuusSpell identifier)
	{
		data.get(identifier).shutdown();
	}

	private void updateConfigChange(boolean enabled,ArceuusSpell identifier) { if(!enabled) { data.get(identifier).shutdown(); } }

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varbit = event.getVarbitId();
		if (varbit == VarbitID.ARCEUUS_RESURRECTION_COOLDOWN && event.getValue() == VARBIT_UP) closeThrallInfoBox();
		if (varbit == VarbitID.ARCEUUS_RESURRECTION_USED) onThrallUsed(client.getVarbitValue( VarbitID.ARCEUUS_RESURRECTION_USED));
		SpellController spellController = data.get(getSpellIdentifier( varbit ));
		handleSpellVarbitChanges(varbit, spellController, event);
	}

	private void onThrallUsed(int varbitValue) {
		if (varbitValue < 0 || varbitValue > 8) return;
		ThrallType thrall = THRALL_TYPES[varbitValue % 3];
		modifyThrallData(thrall.fileName, thrall.tooltip);
	}

	private void handleSpellVarbitChanges(int varbit, SpellController spellController, VarbitChanged event) {
		if (!spellVarbits.contains(varbit)) { return; }
		if(handleCooldownConfigs(varbit)) { spellController.varbitChange(client.getVarbitValue(varbit)); }
		//Following two statements are for when a ward/veil is still active but
		//the cooldown has ended (i.e. boosted magic level is over 83) and is recast
		if(varbit == VarbitID.ARCEUUS_WARD_COOLDOWN && (event.getValue() == VARBIT_UP) && config.showWardOfArceuus())
		{
			VariableTimerController ward = (VariableTimerController) data.get(ArceuusSpell.WARD);
			ward.nonVarbitChange();
		}
		if(varbit == VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN && (event.getValue() == VARBIT_UP) && config.showShadowVeil())
		{
			SpellController veil = data.get(getSpellIdentifier(VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE));
			veil.varbitChange(client.getVarbitValue(VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE));
		}
	}

	private boolean	handleCooldownConfigs(int varbit) {
		//These spells have cases where the timer relies on the cooldown varbit to re-trigger when already active
		//These boolean checks are to make sure the cooldown infoboxes do not get created if the config for
		//them is turned off but the program is allowed to process actions based off the cooldown varbits.
		boolean blockVeilCooldown = config.showShadowVeil() && !config.showShadowVeilCooldown() && varbit == VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN;
		boolean blockWardCooldown = config.showWardOfArceuus() && !config.showWardOfArceuusCooldown() && varbit == VarbitID.ARCEUUS_WARD_COOLDOWN;
		return !(blockVeilCooldown || blockWardCooldown);
	}

	private void gameMessageCorrLockRelease()
	{
		CorruptionController corruption = (CorruptionController) data.get(ArceuusSpell.CORRUPTION);
		corruption.setIconLock(false);
	}
	private void gameMessageMarkOfDarkness() {
		MarkController mark = (MarkController) data.get(ArceuusSpell.MARK);
		mark.nonVarbitChange();
	}

	private void expiredGameMessage(ArceuusSpell spell)
	{
		VariableTimerController responder = (VariableTimerController) data.get(spell);
		responder.chatExpiredResponse();
	}

	private void expiredMarkMessage()
	{
		MarkController responder = (MarkController) data.get(ArceuusSpell.MARK);
		responder.chatExpiredResponse();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!event.getType().equals(ChatMessageType.GAMEMESSAGE)) return;
		String message = event.getMessage().replaceAll("<col=[a-z0-9]+>", "").replaceAll("</col>", "");
		for (MessageHandler handler : gameMessageIconLockHandlers)
		{
			if (handler.isSuffix ? message.endsWith(handler.message) : message.equals(handler.message))
			{
				if (handler.message.startsWith("You resurrect a ") && !config.showThrall())
				{
					log.debug("Thrall cast message ignored, showThrall=false: {}", message);
					return;
				}
				handler.action.run();
				log.debug("ARCEUUS TIMERS - Chat message handled: {}", message);
				return;
			}
		}
	}

	private void createThrall()
	{
		ThrallController thrall = (ThrallController) data.get(ArceuusSpell.THRALL);
		thrall.createThrall();
	}

	private void modifyThrallData(String fileName, String tooltip) {
		ThrallController changeData = (ThrallController) data.get(ArceuusSpell.THRALL);
		changeData.setFileName(fileName);
		changeData.setTooltip(tooltip);
	}

	private void modifyCorruptionData(String fileName, String tooltip) {
		CorruptionController changeData = (CorruptionController) data.get(ArceuusSpell.CORRUPTION);
		if (changeData.isIconLocked()) return;
		changeData.setTooltip(tooltip);
		changeData.setFileName(fileName);
		changeData.setIconLock(true);
	}

	private void modifyOfferingData(String fileName, String tooltip) {
		StandardController changeData = (StandardController) data.get(ArceuusSpell.OFFERING);
		changeData.setTooltip(tooltip);
		changeData.setFileName(fileName);
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked cast) {
		String option = cast.getMenuTarget().replaceAll("<col=[a-z0-9]+>", "").replaceAll("</col>", "");
		Runnable handler = menuOptionHandlers.get(option);
		if (handler != null) handler.run();
	}

	public ArceuusTimersConfig getConfig()
	{
		return config;
	}

	private ArceuusSpell getSpellIdentifier(int varbit)
	{
		switch(varbit) {
			case VarbitID.ARCEUUS_RESURRECTION_COOLDOWN: return ArceuusSpell.THRALL_COOLDOWN;
			case VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE: return ArceuusSpell.CHARGE;
			case VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN: return ArceuusSpell.CHARGE_COOLDOWN;
			case VarbitID.ARCEUUS_CORRUPTION_COOLDOWN: return ArceuusSpell.CORRUPTION;
			case VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE: return ArceuusSpell.SHADOW;
			case VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN: return ArceuusSpell.SHADOW_COOLDOWN;
			case VarbitID.ARCEUUS_WARD_COOLDOWN: return ArceuusSpell.WARD_COOLDOWN;
			case VarbitID.ARCEUUS_VILE_VIGOUR_COOLDOWN: return ArceuusSpell.VIGOUR;
			case VarbitID.ARCEUUS_DARK_LURE_COOLDOWN: return ArceuusSpell.LURE;
			case VarbitID.ARCEUUS_OFFERING_COOLDOWN: return ArceuusSpell.OFFERING;
			case VarbitID.LUNAR_SPELLBOOK_CHANGE: return  ArceuusSpell.SPELLBOOK_SWAP;
			default: return null;
		}
	}

	private void initialSpellVarbits()
	{
		if (config.showDeathChargeActive()) { spellVarbits.add(VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE); }
		if (config.showDeathChargeCooldown()) { spellVarbits.add(VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN); }
		if (config.showThrallCooldown()) { spellVarbits.add(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN); }
		if (config.showCorruptionCooldown()) { spellVarbits.add(VarbitID.ARCEUUS_CORRUPTION_COOLDOWN); }
		if (config.showShadowVeil() ) { spellVarbits.add(VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE); }
		if (config.showShadowVeilCooldown() || config.showShadowVeil() ){ spellVarbits.add(VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN); }
		if (config.showVileVigourCooldown()) { spellVarbits.add(VarbitID.ARCEUUS_VILE_VIGOUR_COOLDOWN); }
		if (config.showOfferingsCooldown()) { spellVarbits.add(VarbitID.ARCEUUS_OFFERING_COOLDOWN); }
		if (config.showDarkLureCooldown()) { spellVarbits.add(VarbitID.ARCEUUS_DARK_LURE_COOLDOWN); }
		if (config.showWardOfArceuusCooldown() || config.showWardOfArceuus()){ spellVarbits.add(VarbitID.ARCEUUS_WARD_COOLDOWN); }
		if (config.spellbookSwapToggle()){ spellVarbits.add(VarbitID.LUNAR_SPELLBOOK_CHANGE); }
	}

	private void closeThrallInfoBox()
	{
		ThrallController thrall = (ThrallController) data.get(ArceuusSpell.THRALL);
		thrall.shutdown();
	}

	private void createSpellControllers()
	{
		HashMap<ArceuusSpell, InitialSpellData> initData = new HashMap<>();
		initData.put(ArceuusSpell.THRALL, new InitialSpellData("/ghost.png", CD_LONG, "Active thrall ( Ghost )"));
		initData.put(ArceuusSpell.THRALL_COOLDOWN, new InitialSpellData("/thrall_cooldown.png", CD_SHORT, "Thrall cooldown"));
		initData.put(ArceuusSpell.CHARGE, new InitialSpellData("/death_charge.png", CD_LONG, "Death Charge active"));
		initData.put(ArceuusSpell.CHARGE_COOLDOWN, new InitialSpellData("/death_charge_cooldown.png", CD_LONG, "Death Charge cooldown"));
		initData.put(ArceuusSpell.SHADOW, new InitialSpellData("/shadow_veil.png", CD_LONG, "Shadow Veil active"));
		initData.put(ArceuusSpell.SHADOW_COOLDOWN, new InitialSpellData("/shadow_veil_cooldown.png", CD_MED, "Shadow Veil cooldown"));
		initData.put(ArceuusSpell.VIGOUR, new InitialSpellData("/vile_vigour.png", CD_SHORT, "Vile Vigour cooldown"));
		initData.put(ArceuusSpell.CORRUPTION, new InitialSpellData("/greater.png", CD_MED, "Greater Corruption cooldown"));
		initData.put(ArceuusSpell.WARD, new InitialSpellData("/ward.png", CD_LONG, "Ward of Arceuus active"));
		initData.put(ArceuusSpell.WARD_COOLDOWN, new InitialSpellData("/ward_cooldown.png", CD_MED, "Ward of Arceuus cooldown"));
		initData.put(ArceuusSpell.LURE, new InitialSpellData("/lure.png", CD_SHORT, "Dark Lure cooldown"));
		initData.put(ArceuusSpell.OFFERING, new InitialSpellData("/sinister_offering.png", CD_TINY, "Sinister Offering cooldown"));
		initData.put(ArceuusSpell.MARK, new InitialSpellData("/mark.png", NO_TEXT, "Mark of Darkness active"));
		initData.put(ArceuusSpell.SPELLBOOK_SWAP, new InitialSpellData("/sbs.png", SBS_TIME, ""));

		for(ArceuusSpell spell: ArceuusSpell.values())
		{
			SpellController controller;
			switch (spell){
				case THRALL:
					controller = new ThrallController(
							initData.get( spell ).getFile(),
							initData.get( spell ).getCooldown(),
							initData.get( spell ).getTooltip(),
							infoBoxManager,
							this,
							client);
					break;
				case MARK:
					controller = new MarkController(
							initData.get( spell ).getFile(),
							initData.get( spell ).getCooldown(),
							initData.get( spell ).getTooltip(),
							infoBoxManager,
							this,
							client);
					break;
				case WARD:
				case SHADOW:
					controller = new VariableTimerController(
							initData.get( spell ).getFile(),
							initData.get( spell ).getCooldown(),
							initData.get( spell ).getTooltip(),
							infoBoxManager,
							this,
							client);
					break;
				case CORRUPTION:
					controller = new CorruptionController(
							initData.get( spell ).getFile(),
							initData.get( spell ).getCooldown(),
							initData.get( spell ).getTooltip(),
							infoBoxManager,
							this);
					break;
				case CHARGE:
					controller = new DeathChargeController(
							initData.get(spell).getFile(),
							initData.get(spell).getCooldown(),
							initData.get(spell).getTooltip(),
							infoBoxManager,
							this);
					break;
				default:
					controller = new StandardController(
							initData.get( spell ).getFile(),
							initData.get( spell ).getCooldown(),
							initData.get( spell ).getTooltip(),
							infoBoxManager,
							this);
					break;

			}
			data.put(spell,controller);
		}
	}

	private static class InitialSpellData{
		private final String file;
		private final double cooldown;
		private final String tooltip;
		public InitialSpellData(String file, double cooldown, String tooltip) {
			this.file = file;
			this.cooldown = cooldown;
			this.tooltip = tooltip;
		}

		public String getFile() {
			return file;
		}

		public double getCooldown() {
			return cooldown;
		}

		public String getTooltip() {
			return tooltip;
		}
	}

	private static class ThrallType {
		final String fileName;
		final String tooltip;

		ThrallType(String fileName, String tooltip) {
			this.fileName = fileName;
			this.tooltip = tooltip;
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