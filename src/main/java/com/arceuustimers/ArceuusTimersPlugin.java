package com.arceuustimers;

import com.arceuustimers.controllers.*;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.WidgetInfo;
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

/*
* NEED TO FIX
* 	ON DEATH/TELEPORT/INSTANCE Changes
* 	THRALL ICON LOCK
*
*
* */

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
		configManager.setConfiguration("timers", "showArceuus", false);
		configManager.setConfiguration("timers", "showArceuusCooldown", false);
   }

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove( overlay );
		//Only remove the active InfoBoxes that are created by this plugin
		activeInfoboxRemoval();
		//Turn on 'Timers' plugin implementation to replace this one
		configManager.setConfiguration("timers", "showArceuus", true);
		configManager.setConfiguration("timers", "showArceuusCooldown", true);
	}


	private void activeInfoboxRemoval()
	{
		for (ArceuusSpell identifier : ArceuusSpell.values()) {
			SpellController spellController = data.get( identifier );
			spellController.shutdown();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varbit = event.getVarbitId();
		SpellController spellController = data.get(getSpellIdentifier( varbit ));

		//If the config is turned on its on the list so we potentially make a new infobox
		if(spellVarbits.contains( varbit )) { spellController.varbitChange(client.getVarbitValue( varbit )); }

		//Extra precaution to make sure the wrong icon doesn't get accidentally locked
		if(varbit == Varbits.RESURRECT_THRALL_COOLDOWN) {
			ThrallController changeData = ( ThrallController ) data.get(ArceuusSpell.THRALL);
			changeData.setIconLock( false );
			//Bugfix for thrall infoboxes staying indefinitely after leaving an instance
			if(event.getValue() == 1)
			{

				if(client.isInInstancedRegion())
				{
					shutdownOnInstanceLeave = true;
					fakeThrallBitNeeded = true;
				}

				//Edge case used for when thrall is active, then go into instance then cast a second+ trall
				if(fakeThrallBitNeeded && firstInstanceCast)
				{
					ThrallController thrallController = ( ThrallController ) data.get(ArceuusSpell.THRALL);
					thrallController.varbitChange(client.getVarbitValue(Varbits.RESURRECT_THRALL));
					fakeThrallBitNeeded = false;
				}
			}
		}

		//Edge case used for when thrall is active, then go into instance then cast a new trall
		if(varbit == Varbits.RESURRECT_THRALL && event.getValue() == 1)
		{
			firstInstanceCast = false;

			if(shutdownOnInstanceLeave && event.getValue() == 0 && !firstInstanceCast)
			{
				shutdownOnInstanceLeave = false;
				fakeThrallBitNeeded = false;
			}
		}

	}


	//Since icons can be locked without a spell being cast this gives a
	//chance to release an icon that has been locked before the spell gets cast
	private final Map<String, Runnable> gameMessageIconLockHandlers = new HashMap<String, Runnable>() {{
		put("You must have a Book of the Dead in your possession to use this spell.", () -> gameMessageIconLockRelease());
		put("You do not have enough Fire Runes to cast this spell.", () -> gameMessageIconLockRelease());
		put("You do not have enough Cosmic Runes to cast this spell.", () -> gameMessageIconLockRelease());
		put("You do not have enough Blood Runes to cast this spell.", () -> gameMessageIconLockRelease());
		put("You don't have enough Prayer points to cast that spell.", () -> gameMessageIconLockRelease());
		put("You can only cast corruption spells every 30 seconds.", () -> gameMessageCorrLockRelease());
		put("You resurrect a lesser ghostly  thrall.", () -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("You resurrect a lesser skeletal thrall.", () -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("You resurrect a lesser zombified thrall.", () -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("You resurrect a superior ghostly thrall.", () -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("You resurrect a superior skeletal thrall.", () -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("You resurrect a superior zombified thrall.", () -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("You resurrect a greater ghostly thrall.", () -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("You resurrect a greater skeletal thrall.", () -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("You resurrect a greater zombified thrall.", () -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
	}};

	//Unlock thrall icon
	private void gameMessageIconLockRelease() {
		ThrallController thrall = (ThrallController) data.get(ArceuusSpell.THRALL);
		thrall.setIconLock(false);
	}

	//Unlock corruption icon
	private void gameMessageCorrLockRelease()
	{
		CorruptionController corruption = (CorruptionController) data.get(ArceuusSpell.CORRUPTION);
		corruption.setIconLock(false);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();
		message = message.replaceAll("<col=[a-z0-9]+>", "").replaceAll("</col>", "");
		//Unlocks corruption or thrall icons that are locked as a result of spam clicking
		if(event.getType().equals(ChatMessageType.GAMEMESSAGE)) {
			Runnable handler = gameMessageIconLockHandlers.get(message);
			if (handler != null) {
				handler.run();
			}
		}

		//Important to be able to reset the timer when recasting before time has expired
		if(message.equals("Your thieving abilities have been enhanced.")) {
			ShadowVeilController spell = (ShadowVeilController) data.get(ArceuusSpell.SHADOW);
			spell.chatMessageResponse();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		//&& !client.isInInstancedRegion()
		if(event.getGameState().equals(GameState.LOGGED_IN) && shutdownOnInstanceLeave ) {
			ThrallController thrall = (ThrallController) data.get(ArceuusSpell.THRALL);
			thrall.shutdown();
			shutdownOnInstanceLeave = false;
		}

		if(client.isInInstancedRegion())
		{
			firstInstanceCast = true;
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
				updateConfigChange(config.showVileVigourCooldown(), 12292, ArceuusSpell.VIGOUR);
				break;
			case ArceuusTimersConfig.SHOW_SHADOW_VEIL_COOLDOWN:
				updateConfigChange(config.showShadowVeilCooldown(), Varbits.SHADOW_VEIL_COOLDOWN, ArceuusSpell.SHADOW);
				break;
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


	private final Map<String, Runnable> menuOptionHandlers = new HashMap<String, Runnable>() {{
		put("<col=00ff00>Resurrect Lesser Ghost</col>", () -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("<col=00ff00>Resurrect Lesser Skeleton</col>", () -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("<col=00ff00>Resurrect Lesser Zombie</col>", () -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("<col=00ff00>Resurrect Superior Ghost</col>", () -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("<col=00ff00>Resurrect Superior Skeleton</col>", () -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("<col=00ff00>Resurrect Superior Zombie</col>", () -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("<col=00ff00>Resurrect Greater Ghost</col>", () -> modifyThrallData("/ghost.png", "Active thrall ( Ghost )"));
		put("<col=00ff00>Resurrect Greater Skeleton</col>", () -> modifyThrallData("/skeleton.png", "Active thrall ( Skeleton )"));
		put("<col=00ff00>Resurrect Greater Zombie</col>", () -> modifyThrallData("/zombie.png", "Active thrall ( Zombie )"));
		put("<col=00ff00>Greater Corruption</col>", () -> modifyCorruptionData("/greater.png"));
		put("<col=00ff00>Lesser Corruption</col>", () -> modifyCorruptionData("/lesser.png"));
	}};

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

	private void modifyCorruptionData(String fileName) {
		CorruptionController changeFile = (CorruptionController) data.get(ArceuusSpell.CORRUPTION);
		if (changeFile.isIconLocked()) {
			return;
		}
		changeFile.setFileName(fileName);
		changeFile.setIconLock(true);
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked cast) {
		Runnable handler = menuOptionHandlers.get(cast.getMenuTarget());
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
		switch(varbit)
		{
			case Varbits.RESURRECT_THRALL: return ArceuusSpell.THRALL;
			case Varbits.RESURRECT_THRALL_COOLDOWN: return ArceuusSpell.THRALL_COOLDOWN;
			case Varbits.DEATH_CHARGE: return ArceuusSpell.CHARGE;
			case Varbits.DEATH_CHARGE_COOLDOWN: return ArceuusSpell.CHARGE_COOLDOWN;
			case Varbits.CORRUPTION_COOLDOWN: return ArceuusSpell.CORRUPTION;
			case Varbits.SHADOW_VEIL: return ArceuusSpell.SHADOW;
			case 12292: return ArceuusSpell.VIGOUR;
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
		if (config.showShadowVeilCooldown()) {
			spellVarbits.add(Varbits.SHADOW_VEIL);
		}
		if (config.showVileVigourCooldown()) {
			spellVarbits.add(12292); // VILE_VIGOUR
		}
	}

	private void createSpellControllers()
	{
		HashMap<ArceuusSpell, InitialSpellData> initData = new HashMap<>();
		initData.put(ArceuusSpell.THRALL, new InitialSpellData("/ghost.png", -1.0, "Active thrall ( Ghost )"));
		initData.put(ArceuusSpell.THRALL_COOLDOWN, new InitialSpellData("/thrall_cooldown.png", 11.0, "Thrall cooldown"));
		initData.put(ArceuusSpell.CHARGE, new InitialSpellData("/death_charge.png", -1.0, "Death Charge active"));
		initData.put(ArceuusSpell.CHARGE_COOLDOWN, new InitialSpellData("/death_charge_cooldown.png", 61.0, "Death Charge cooldown"));
		initData.put(ArceuusSpell.SHADOW, new InitialSpellData("/shadow_veil.png", -1.0, "Shadow Veil active"));
		initData.put(ArceuusSpell.VIGOUR, new InitialSpellData("/vile_vigour.png", 11.0, "Vile Vigour cooldown"));
		initData.put(ArceuusSpell.CORRUPTION, new InitialSpellData("/greater.png", 31.0, "Corruption cooldown"));

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
				case SHADOW:
					controller = new ShadowVeilController(
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