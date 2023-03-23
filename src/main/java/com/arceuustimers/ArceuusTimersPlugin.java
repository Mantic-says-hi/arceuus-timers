package com.arceuustimers;

import com.google.inject.Provides;
import javax.inject.Inject;


import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.HashMap;


@Slf4j
@PluginDescriptor(
	name = "Arceuus Timers",
	description = "Arceuus spellbook timers with an alternate design to the 'Timers' plugin."
)
public class ArceuusTimersPlugin extends Plugin
{

	HashMap<ArceuusSpell, SpellData> data = new HashMap<>();

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

	@Override
	protected void startUp() throws Exception
	{
		log.info("Arceuus Timers plugin started!");
		overlayManager.add(overlay);
		setupHashMaps();
		//Turn off 'Timers' plugin implementation
		configManager.setConfiguration("timers", "showArceuus", false);
		configManager.setConfiguration("timers", "showArceuusCooldown", false);
   }

	@Override
	protected void shutDown() throws Exception
	{

		log.info("Arceuus Timers plugin stopped!");
		overlayManager.remove(overlay);
		//Only remove InfoBoxes that are created by this plugin
		for (ArceuusSpell spell : ArceuusSpell.values())
		{
			infoBoxManager.removeInfoBox(data.get(spell).getInfoBox());
		}
		//Turn on 'Timers' plugin implementation to replace this one
		configManager.setConfiguration("timers", "showArceuus", true);
		configManager.setConfiguration("timers", "showArceuusCooldown", true);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{

		//Death charge cooldown | varbit = 12138 | 1 = On Cooldown
		int chargeCooldownBit = client.getVarbitValue( Varbits.DEATH_CHARGE_COOLDOWN );
		updateInfoBox(chargeCooldownBit, ArceuusSpell.CHARGE_COOLDOWN);


		//Death charge | varbit = 12411 | 1 = Active
		int chargeActiveBit = client.getVarbitValue( Varbits.DEATH_CHARGE );
		updateInfoBox(chargeActiveBit, ArceuusSpell.CHARGE);


		//Corruption | varbit = 12288 | 1 = Cooling down
		int corruptionCooldownBit = client.getVarbitValue(Varbits.CORRUPTION_COOLDOWN);
		updateInfoBox(corruptionCooldownBit, ArceuusSpell.CORRUPTION);


		//Thrall Cooldown | varbit = 12290 | 1 = Cooling down
		int thrallCooldownBit = client.getVarbitValue(Varbits.RESURRECT_THRALL_COOLDOWN);
		updateInfoBox(thrallCooldownBit, ArceuusSpell.THRALL_COOLDOWN);


		//Active Thrall | varbit = 12413 | 1 = Summon active
		int thrallActive = client.getVarbitValue(Varbits.RESURRECT_THRALL);
		updateInfoBox(thrallActive, ArceuusSpell.THRALL);


		//Vile Vigour | varbit = 12292 | 1 = Active
		int vigourBit = client.getVarbitValue(12292);
		updateInfoBox(vigourBit, ArceuusSpell.VIGOUR);


		//Shadow Veil | varbit : 12414 | 1 = Active
		int shadowBit = client.getVarbitValue( Varbits.SHADOW_VEIL );
		updateInfoBox(shadowBit, ArceuusSpell.SHADOW);

	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final String message = event.getMessage();

		if(message.contains("Your thieving abilities have been enhanced."))
		{
			SpellData spellData = data.get(ArceuusSpell.SHADOW);
			if(spellData.isActive())
			{
				removeBox(ArceuusSpell.SHADOW);
				double shadowTime = 0.6 * client.getRealSkillLevel(Skill.MAGIC);
				spellData.setCooldown(shadowTime);
				onSpellCast(spellData);
			}
		}

	}

	@Provides
	ArceuusTimersConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ArceuusTimersConfig.class);
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked cast)
	{
		String option = cast.getMenuTarget();
		if(option.contains("Ghost")) {
			SpellData changeData = data.get(ArceuusSpell.THRALL);
			changeData.setFileName("/ghost.png");
			changeData.setTooltip("Active thrall ( Ghost )");
			return;
		}
		if(option.contains("Skeleton")) {
			SpellData changeData = data.get(ArceuusSpell.THRALL);
			changeData.setFileName("/skeleton.png");
			changeData.setTooltip("Active thrall ( Skeleton )");
			return;
		}
		if(option.contains("Zombie")){
			SpellData changeData = data.get(ArceuusSpell.THRALL);
			changeData.setFileName("/zombie.png");
			changeData.setTooltip("Active thrall ( Zombie )");
			return;
		}
		if(option.contains("Greater")) {
			SpellData changeFile = data.get(ArceuusSpell.CORRUPTION);
			changeFile.setFileName("/greater.png");
			return;
		}
		if(option.contains("Lesser")){
			SpellData changeFile = data.get(ArceuusSpell.CORRUPTION);
			changeFile.setFileName("/lesser.png");
		}
	}

	public void onSpellCast(SpellData spellData)
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), spellData.getFileName());
		InfoBox box = new ArceuusTimersInfobox(
				icon,
				this,
				spellData.getCooldown(),
				infoBoxManager,
				Instant.now(),
				spellData.getTooltip());

		infoBoxManager.addInfoBox(box);
		spellData.setInfoBox(box);
		spellData.setActive(true);
	}

	public void updateInfoBox(int varbitValue, ArceuusSpell identifier)
	{
		SpellData spellData = data.get(identifier);
		if(identifier.equals(ArceuusSpell.THRALL))
		{
			if(varbitValue == 1 && !spellData.isActive())
			{
				double thrallUptime = 0.6 * client.getBoostedSkillLevel(Skill.MAGIC);
				if( client.getVarbitValue( Varbits.COMBAT_ACHIEVEMENT_TIER_GRANDMASTER ) == 2)
				{
					thrallUptime = ( thrallUptime * 2.0 );
				}
				else if( client.getVarbitValue( Varbits.COMBAT_ACHIEVEMENT_TIER_MASTER ) == 2)
				{
					thrallUptime = ( thrallUptime * 1.5 );
				}
				spellData.setCooldown(thrallUptime);
				onSpellCast(spellData);
			}
			else if(varbitValue == 0 && spellData.isActive())
			{
				removeBox(identifier);
			}
			return;
		}

		if( identifier.equals(ArceuusSpell.SHADOW))
		{
			if( varbitValue == 1 && !spellData.isActive() )
			{
				double shadowTime = 0.6 * client.getRealSkillLevel(Skill.MAGIC);
				spellData.setCooldown(shadowTime);
				onSpellCast(spellData);
			}
			else if( varbitValue == 0 && spellData.isActive() )
			{
				removeBox(ArceuusSpell.SHADOW);
			}
			return;
		}


		if( varbitValue == 1 && !spellData.isActive() )
		{
			onSpellCast(spellData);
		}
		else if( varbitValue == 0 && spellData.isActive())
		{
			removeBox(identifier);
		}
	}

	public void removeBox(ArceuusSpell identifier)
	{
		SpellData updateSpellData = data.get(identifier);
		updateSpellData.setActive(false);
		infoBoxManager.removeInfoBox(updateSpellData.getInfoBox());
		updateSpellData.setInfoBox(null);
	}

	private void setupHashMaps()
	{

		//THRALL
		//THRALL_COOLDOWN
		//CHARGE
		//CHARGE_COOLDOWN
		//SHADOW
		//VIGOUR
		//CORRUPTION

		String[] filenames = {
				"/ghost.png",//THRALL
				"/thrall_cooldown.png",//THRALL_COOLDOWN
				"/death_charge.png",//CHARGE
				"/death_charge_cooldown.png",//CHARGE_COOLDOWN
				"/shadow_veil.png",//SHADOW
				"/vile_vigour.png",//VIGOUR
				"/greater.png"//CORRUPTION
		};

		double[] cooldowns = {
				-1.0,//THRALL
				11.0,//THRALL_COOLDOWN
				-1.0,//CHARGE
				61.0,//CHARGE_COOLDOWN
				-1.0,//SHADOW
				11.0,//VIGOUR
				31.0//CORRUPTION
		};

		String[] tooltips = {
				"Active thrall ( Ghost )",//THRALL
				"Thrall cooldown",//THRALL_COOLDOWN
				"Death Charge active",//CHARGE
				"Death Charge cooldown",//CHARGE_COOLDOWN
				"Shadow Veil active",//SHADOW
				"Vile Vigour cooldown",//VIGOUR
				"Corruption cooldown"//CORRUPTION
		};

		for (int i = 0; i < ArceuusSpell.values().length; i++)
		{
			data.put(ArceuusSpell.values()[i],
					new SpellData(false,null,filenames[i], cooldowns[i],tooltips[i]));
		}

	}
  
}