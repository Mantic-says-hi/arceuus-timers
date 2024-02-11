package com.arceuustimers;

import com.arceuustimers.controllers.CorruptionController;
import com.arceuustimers.controllers.SpellController;
import com.arceuustimers.controllers.StandardController;
import com.arceuustimers.controllers.ThrallController;
import com.arceuustimers.controllers.VariableTimerController;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
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

import java.util.HashMap;
import java.util.HashSet;
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
	private boolean shutdownOnInstanceLeave = false;
	private boolean fakeThrallBitNeeded = false;
	private boolean firstInstanceCast = true;

	private static final String TIMERS_PLUGIN = "timers";
	private static final String SHOW_ARCEUUS = "showArceuus";
	private static final String SHOW_ARCEUUS_COOLDOWN = "showArceuusCooldown";
	private static final String SHOW_SPELLBOOK_SWAP = "showSpellbookSwap";
	private static final int VILE_VIGOUR = 12292;
	private static final int OFFERINGS = 12423;
	private static final int DARK_LURE =  12289;
	private static final int VARBIT_MANUAL= -1;
    private static final int VARBIT_UP = 1;
    private static final int VARBIT_DOWN = 0;
    private static final int SOTE_ROOM = 13123;
	private static final int SOTE_ROOM_SHADOW = 13379;
	private static final double NO_TEXT = -1.0;
	private static final double CD_LONG = 61.2;
	private static final double CD_MED = 31.2;
	private static final double CD_SHORT = 10.8;
	private static final double CD_TINY = 6.0;
	private static final double SBS_TIME = 120;

	private int thrallTest = 0;
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
		setTimersPlugin(false);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove( overlay );
		//Only remove the active InfoBoxes that are created by this plugin
		removeActiveInfoboxes();
		//Turn on 'Timers' plugin implementation to replace this one
		setTimersPlugin(true);
	}

	private void setTimersPlugin(boolean state)
	{
		configManager.setConfiguration(TIMERS_PLUGIN, SHOW_ARCEUUS, state);
		configManager.setConfiguration(TIMERS_PLUGIN, SHOW_ARCEUUS_COOLDOWN, state);
		configManager.setConfiguration(TIMERS_PLUGIN, SHOW_SPELLBOOK_SWAP, state);
	}

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

		if(client.getVarbitValue(Varbits.RESURRECT_THRALL) == 1){
			thrallTest++;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals(ArceuusTimersConfig.GROUP)) {
			return;
		}

		switch (event.getKey()) {
			case ArceuusTimersConfig.SHOW_DEATH_CHARGE:
				updateConfigChange(config.showDeathChargeActive(), Varbits.DEATH_CHARGE, ArceuusSpell.CHARGE);
				break;
			case ArceuusTimersConfig.SHOW_DEATH_CHARGE_COOLDOWN:
				updateConfigChange(config.showDeathChargeCooldown(), Varbits.DEATH_CHARGE_COOLDOWN, ArceuusSpell.CHARGE_COOLDOWN);
				break;
			case ArceuusTimersConfig.SHOW_THRALL:
				updateConfigChange(config.showThrall(), Varbits.RESURRECT_THRALL, ArceuusSpell.THRALL);
				break;
			case ArceuusTimersConfig.SHOW_THRALL_COOLDOWN:
				updateConfigChange(config.showThrallCooldown(), Varbits.RESURRECT_THRALL_COOLDOWN, ArceuusSpell.THRALL_COOLDOWN);
				break;
			case ArceuusTimersConfig.SHOW_CORRUPTION_COOLDOWN:
				updateConfigChange(config.showCorruptionCooldown(), Varbits.CORRUPTION_COOLDOWN, ArceuusSpell.CORRUPTION);
				break;
			case ArceuusTimersConfig.SHOW_VILE_VIGOUR_COOLDOWN:
				updateConfigChange(config.showVileVigourCooldown(), VILE_VIGOUR, ArceuusSpell.VIGOUR);
				break;
			case ArceuusTimersConfig.SHOW_SHADOW_VEIL:
				if(!config.showShadowVeilCooldown() && !config.showShadowVeil())
				{
					updateConfigChange(config.showShadowVeilCooldown(), Varbits.SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
				}else if((!config.showShadowVeilCooldown() && config.showShadowVeil()))
				{
					//Need to force a true here for the cooldown incase it is not on, otherwise shadow veil
					//won't function properly if it is recast after a cooldown while it is still active
					updateConfigChange(true, Varbits.SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
					updateConfigChange(config.showShadowVeil(), Varbits.SHADOW_VEIL, ArceuusSpell.SHADOW);
				}else{
					updateConfigChange(config.showShadowVeil(), Varbits.SHADOW_VEIL, ArceuusSpell.SHADOW);
				}
				break;
			case ArceuusTimersConfig.SHOW_SHADOW_VEIL_COOLDOWN:
				if(config.showShadowVeil() && !config.showShadowVeilCooldown()) {
					updateConfigChange(ArceuusSpell.SHADOW_COOLDOWN);
				}else{
					updateConfigChange(config.showShadowVeilCooldown(), Varbits.SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW_COOLDOWN);
				}
				break;
			case ArceuusTimersConfig.SHOW_DARK_LURE_COOLDOWN:
				updateConfigChange(config.showDarkLureCooldown(), DARK_LURE, ArceuusSpell.LURE);
				break;
			case ArceuusTimersConfig.SHOW_WARD_OF_ARCEUUS_COOLDOWN:
				if(config.showWardOfArceuus() && !config.showWardOfArceuusCooldown()) {
					updateConfigChange(ArceuusSpell.WARD_COOLDOWN);
				}else{
					updateConfigChange(config.showWardOfArceuusCooldown(), Varbits.WARD_OF_ARCEUUS_COOLDOWN, ArceuusSpell.WARD_COOLDOWN);
				}
				break;
			case ArceuusTimersConfig.SHOW_WARD_OF_ARCEUUS:
				updateConfigChange(config.showWardOfArceuus(), ArceuusSpell.WARD);
				break;
			case ArceuusTimersConfig.SHOW_OFFERINGS_COOLDOWN:
				updateConfigChange(config.showOfferingsCooldown(), OFFERINGS, ArceuusSpell.OFFERING);
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

	private void updateConfigChange(boolean enabled,ArceuusSpell identifier)
	{
		if(!enabled) {
			data.get(identifier).shutdown();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varbit = event.getVarbitId();
		SpellController spellController = data.get(getSpellIdentifier( varbit ));

		handleSpellVarbitChanges(varbit, spellController, event);
		handleThrallIconLock(varbit, event);
		handleThrallEdgeCase(varbit,event);
		handleThrallInstances(varbit, event);
	}

	private void handleSpellVarbitChanges(int varbit, SpellController spellController, VarbitChanged event) {
		if (!spellVarbits.contains(varbit)) {
			return;
		}
		if(handleCooldownConfigs(varbit)) {

			spellController.varbitChange(client.getVarbitValue(varbit));
		}
		//Following two statements are for when a ward/veil is still active but
		//the cooldown has ended (i.e. boosted magic level is over 83) and is recast
		if(varbit == Varbits.WARD_OF_ARCEUUS_COOLDOWN && (event.getValue() == VARBIT_UP) && config.showWardOfArceuus()){
			VariableTimerController ward = (VariableTimerController) data.get(ArceuusSpell.WARD);
			ward.nonVarbitChange();
		}

		if(varbit == Varbits.SHADOW_VEIL_COOLDOWN && (event.getValue() == VARBIT_UP) && config.showShadowVeil()) {
			SpellController veil = data.get(getSpellIdentifier(Varbits.SHADOW_VEIL));
			veil.varbitChange(client.getVarbitValue(Varbits.SHADOW_VEIL));
		}
	}

	private boolean	handleCooldownConfigs(int varbit) {
		//These spells have cases where the timer relies on the cooldown varbit to re-trigger when already active
		//These boolean checks are to make sure the cooldown infoboxes do not get created if the config for
		//them is turned off but the program is allowed to process actions based off the cooldown varbits.
		boolean blockVeilCooldown = config.showShadowVeil() && !config.showShadowVeilCooldown() && varbit == Varbits.SHADOW_VEIL_COOLDOWN;
		boolean blockWardCooldown = config.showWardOfArceuus() && !config.showWardOfArceuusCooldown() && varbit == Varbits.WARD_OF_ARCEUUS_COOLDOWN;
		return !(blockVeilCooldown || blockWardCooldown);
	}


	private void handleThrallIconLock(int varbit, VarbitChanged event) {
		if (varbit != Varbits.RESURRECT_THRALL_COOLDOWN) {
			return;
		}
		ThrallController changeData = ( ThrallController ) data.get(ArceuusSpell.THRALL);
		changeData.setIconLock( false );
		//Bugfix for thrall infoboxes staying indefinitely after leaving an instance
		if(event.getValue() != VARBIT_UP) {
			return;
		}
		ThrallController thrallController = ( ThrallController ) data.get(ArceuusSpell.THRALL);

		if(client.isInInstancedRegion()) {
			shutdownOnInstanceLeave = true;
			fakeThrallBitNeeded = true;
		}
		//Edge case used for when thrall is active, then go into instance then cast a second+ thrall
		if(fakeThrallBitNeeded && firstInstanceCast) {
			thrallController.varbitChange(VARBIT_UP);
			fakeThrallBitNeeded = false;
			firstInstanceCast = false;
		}else if(!thrallController.getActive() && client.getVarbitValue(Varbits.RESURRECT_THRALL) == 1) {
			thrallController.varbitChange(VARBIT_MANUAL);
		}
	}

	private void handleThrallEdgeCase(int varbit, VarbitChanged event)
	{
		if(varbit != Varbits.RESURRECT_THRALL_COOLDOWN) { return; }
		if(event.getValue() != VARBIT_UP){return;}
		if(!config.showThrall()){return;}
		if(client.getVarbitValue(Varbits.RESURRECT_THRALL) == 0){return;}
		ThrallController thrallController = ( ThrallController ) data.get(ArceuusSpell.THRALL);
		if(!thrallController.getActive()){return;}
		thrallController.shutdown();
		thrallController.varbitChange(VARBIT_MANUAL);
	}

	private void handleThrallInstances(int varbit, VarbitChanged event) {
		if (!(varbit == Varbits.RESURRECT_THRALL && event.getValue() == VARBIT_UP)) {
			return;
		}
		firstInstanceCast = false;

		if(shutdownOnInstanceLeave && event.getValue() == VARBIT_DOWN && !firstInstanceCast) {
			shutdownOnInstanceLeave = false;
			fakeThrallBitNeeded = false;
		}
	}


	private void gameMessageIconLockRelease() {
		ThrallController thrall = (ThrallController) data.get(ArceuusSpell.THRALL);
		thrall.setIconLock(false);
	}

	private void gameMessageCorrLockRelease()
	{
		CorruptionController corruption = (CorruptionController) data.get(ArceuusSpell.CORRUPTION);
		corruption.setIconLock(false);
	}
	private void gameMessageMarkOfDarkness() {
		VariableTimerController mark = (VariableTimerController) data.get(ArceuusSpell.MARK);
		mark.nonVarbitChange();
	}
	private void expiredGameMessage(ArceuusSpell spell)
	{
		VariableTimerController responder = (VariableTimerController) data.get(spell);
		responder.chatExpiredResponse();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();
		message = message.replaceAll("<col=[a-z0-9]+>", "").replaceAll("</col>", "");
		//Unlocks corruption or thrall icons that are locked as a result of spam clicking
		if(!event.getType().equals(ChatMessageType.GAMEMESSAGE)) {
			return;
		}
		Runnable handler = gameMessageIconLockHandlers.get(message);
		if (handler != null) {
			handler.run();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if(event.getGameState().equals(GameState.LOGGED_IN) && shutdownOnInstanceLeave ) {
			int[] mapRegions = client.getMapRegions();
			if (mapRegions == null || mapRegions.length == 0) {
				return;
			}
			if(mapRegions[0] == SOTE_ROOM || mapRegions[0] == SOTE_ROOM_SHADOW){
				return;
				//This is for the edge case in the Sotetseg fight; being chosen causes thrall infoboxes to be removed
			}
			ThrallController thrall = (ThrallController) data.get(ArceuusSpell.THRALL);
			thrall.setIconLock(false);
			shutdownOnInstanceLeave = false;
			thrall.shutdown();
		}
		if(client.isInInstancedRegion()) {
			firstInstanceCast = true;
		}
	}

	private void modifyThrallData(String fileName, String tooltip) {
		gameMessageIconLockRelease();
		ThrallController changeData = (ThrallController) data.get(ArceuusSpell.THRALL);
		if (changeData.isIconLocked()) {
			return;
		}
		changeData.setFileName(fileName);
		changeData.setTooltip(tooltip);
		changeData.setIconLock(true);
	}

	private void modifyCorruptionData(String fileName, String tooltip) {
		CorruptionController changeData = (CorruptionController) data.get(ArceuusSpell.CORRUPTION);
		if (changeData.isIconLocked()) {
			return;
		}
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
		String option = cast.
				getMenuTarget().
				replaceAll("<col=[a-z0-9]+>", "").
				replaceAll("</col>", "");
		Runnable handler = menuOptionHandlers.get(option);
		if (handler != null) {
			handler.run();
		}
	}

	public ArceuusTimersConfig getConfig()
	{
		return config;
	}

	private ArceuusSpell getSpellIdentifier(int varbit)
	{
		switch(varbit) {
			case Varbits.RESURRECT_THRALL: return ArceuusSpell.THRALL;
			case Varbits.RESURRECT_THRALL_COOLDOWN: return ArceuusSpell.THRALL_COOLDOWN;
			case Varbits.DEATH_CHARGE: return ArceuusSpell.CHARGE;
			case Varbits.DEATH_CHARGE_COOLDOWN: return ArceuusSpell.CHARGE_COOLDOWN;
			case Varbits.CORRUPTION_COOLDOWN: return ArceuusSpell.CORRUPTION;
			case Varbits.SHADOW_VEIL: return ArceuusSpell.SHADOW;
			case Varbits.SHADOW_VEIL_COOLDOWN: return ArceuusSpell.SHADOW_COOLDOWN;
			case Varbits.WARD_OF_ARCEUUS_COOLDOWN: return ArceuusSpell.WARD_COOLDOWN;
			case VILE_VIGOUR: return ArceuusSpell.VIGOUR;
			case DARK_LURE: return ArceuusSpell.LURE;
			case OFFERINGS: return ArceuusSpell.OFFERING;
			case Varbits.SPELLBOOK_SWAP: return  ArceuusSpell.SPELLBOOK_SWAP;

			default: return null;
		}
	}

	private void initialSpellVarbits()
	{
		if (config.showDeathChargeActive()) {
			spellVarbits.add(Varbits.DEATH_CHARGE);
		}
		if (config.showDeathChargeCooldown()) {
			spellVarbits.add(Varbits.DEATH_CHARGE_COOLDOWN);
		}
		if (config.showThrall()) {
			spellVarbits.add(Varbits.RESURRECT_THRALL);
		}
		if (config.showThrallCooldown()) {
			spellVarbits.add(Varbits.RESURRECT_THRALL_COOLDOWN);
		}
		if (config.showCorruptionCooldown()) {
			spellVarbits.add(Varbits.CORRUPTION_COOLDOWN);
		}
		if (config.showShadowVeil() ) {
			spellVarbits.add(Varbits.SHADOW_VEIL);
		}
		if (config.showShadowVeilCooldown() || config.showShadowVeil() ){
			spellVarbits.add(Varbits.SHADOW_VEIL_COOLDOWN);
		}
		if (config.showVileVigourCooldown()) {
			spellVarbits.add(VILE_VIGOUR);
		}
		if (config.showOfferingsCooldown()) {
			spellVarbits.add(OFFERINGS);
		}
		if (config.showDarkLureCooldown()) {
			spellVarbits.add(DARK_LURE);
		}
		if (config.showWardOfArceuusCooldown() || config.showWardOfArceuus()){
			spellVarbits.add(Varbits.WARD_OF_ARCEUUS_COOLDOWN);
		}
		if (config.spellbookSwapToggle()){
			spellVarbits.add(Varbits.SPELLBOOK_SWAP);
		}
	}

	//Since icons can be locked without a spell being cast this gives a
	//chance to release an icon that has been locked before the spell gets cast
	private final Map<String, Runnable> gameMessageIconLockHandlers = new HashMap<String, Runnable>() {{
		put("You must have a Book of the Dead in your possession to use this spell.",
				() -> gameMessageIconLockRelease());
		put("You do not have enough Fire Runes to cast this spell.",
				() -> gameMessageIconLockRelease());
		put("You do not have enough Cosmic Runes to cast this spell.",
				() -> gameMessageIconLockRelease());
		put("You do not have enough Blood Runes to cast this spell.",
				() -> gameMessageIconLockRelease());
		put("You don't have enough Prayer points to cast that spell.",
				() -> gameMessageIconLockRelease());
		put("You can only cast corruption spells every 30 seconds.",
				() -> gameMessageCorrLockRelease());
		put("You resurrect a lesser ghostly  thrall.",
				() -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("You resurrect a lesser skeletal thrall.",
				() -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("You resurrect a lesser zombified thrall.",
				() -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("You resurrect a superior ghostly thrall.",
				() -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("You resurrect a superior skeletal thrall.",
				() -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("You resurrect a superior zombified thrall.",
				() -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("You resurrect a greater ghostly thrall.",
				() -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("You resurrect a greater skeletal thrall.",
				() -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("You resurrect a greater zombified thrall.",
				() -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("Your Ward of Arceuus has expired.",
				() -> expiredGameMessage(ArceuusSpell.WARD));
		put("Your Shadow Veil has faded away.",
				() ->  expiredGameMessage(ArceuusSpell.SHADOW));
		put("You have placed a Mark of Darkness upon yourself.",
				() ->  gameMessageMarkOfDarkness());
		put("Your Mark of Darkness has faded away.",
				() ->  expiredGameMessage(ArceuusSpell.MARK));
	}};

	private final Map<String, Runnable> menuOptionHandlers = new HashMap<String, Runnable>() {{
		put("Resurrect Lesser Ghost",
				() -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("Resurrect Lesser Skeleton",
				() -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("Resurrect Lesser Zombie",
				() -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("Resurrect Superior Ghost",
				() -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("Resurrect Superior Skeleton",
				() -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("Resurrect Superior Zombie",
				() -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("Resurrect Greater Ghost",
				() -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("Resurrect Greater Skeleton",
				() -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("Resurrect Greater Zombie",
				() -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("Greater Corruption",
				() -> modifyCorruptionData("/greater.png", "Greater Corruption cooldown"));
		put("Lesser Corruption",
				() -> modifyCorruptionData("/lesser.png", "Lesser Corruption cooldown"));
		put("Sinister Offering",
				() -> modifyOfferingData("/sinister_offering.png", "Sinister Offering cooldown"));
		put("Demonic Offering",
				() -> modifyOfferingData("/demonic_offering.png", "Demonic Offering cooldown"));
	}};

	private void createSpellControllers()
	{
		HashMap<ArceuusSpell, InitialSpellData> initData = new HashMap<>();
		initData.put(ArceuusSpell.THRALL,
				new InitialSpellData("/ghost.png", NO_TEXT, "Active thrall ( Ghost )"));
		initData.put(ArceuusSpell.THRALL_COOLDOWN,
				new InitialSpellData("/thrall_cooldown.png", CD_SHORT, "Thrall cooldown"));
		initData.put(ArceuusSpell.CHARGE,
				new InitialSpellData("/death_charge.png", NO_TEXT, "Death Charge active"));
		initData.put(ArceuusSpell.CHARGE_COOLDOWN,
				new InitialSpellData("/death_charge_cooldown.png", CD_LONG, "Death Charge cooldown"));
		initData.put(ArceuusSpell.SHADOW,
				new InitialSpellData("/shadow_veil.png", NO_TEXT, "Shadow Veil active"));
		initData.put(ArceuusSpell.SHADOW_COOLDOWN,
				new InitialSpellData("/shadow_veil_cooldown.png", CD_MED, "Shadow Veil cooldown"));
		initData.put(ArceuusSpell.VIGOUR,
				new InitialSpellData("/vile_vigour.png", CD_SHORT, "Vile Vigour cooldown"));
		initData.put(ArceuusSpell.CORRUPTION,
				new InitialSpellData("/greater.png", CD_MED, "Greater Corruption cooldown"));
		initData.put(ArceuusSpell.WARD,
				new InitialSpellData("/ward.png", NO_TEXT, "Ward of Arceuus active"));
		initData.put(ArceuusSpell.WARD_COOLDOWN,
				new InitialSpellData("/ward_cooldown.png", CD_MED, "Ward of Arceuus cooldown"));
		initData.put(ArceuusSpell.LURE,
				new InitialSpellData("/lure.png", CD_SHORT, "Dark Lure cooldown"));
		initData.put(ArceuusSpell.OFFERING,
				new InitialSpellData("/sinister_offering.png", CD_TINY, "Sinister Offering cooldown"));
		initData.put(ArceuusSpell.MARK,
				new InitialSpellData("/mark.png", NO_TEXT, "Mark of Darkness active"));
		initData.put(ArceuusSpell.SPELLBOOK_SWAP,
				new InitialSpellData("/sbs.png", SBS_TIME, ""));

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
				case WARD:
				case MARK:
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
}