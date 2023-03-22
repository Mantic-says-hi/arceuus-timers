package com.arceuustimers;

import com.google.inject.Provides;
import javax.inject.Inject;


import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
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
	name = "Arceuus Timers"
)
public class ArceuusTimersPlugin extends Plugin
{

	ArceuusSpellEnums thrallImage = ArceuusSpellEnums.Ghost;
	ArceuusSpellEnums corruptionImage = ArceuusSpellEnums.Greater;
	HashMap<ArceuusSpellEnums, Boolean> spellActive = new HashMap<>();
	HashMap<ArceuusSpellEnums, InfoBox> activeInfoBox = new HashMap<>();
	HashMap<ArceuusSpellEnums, String> filename = new HashMap<>();


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
		infoBoxManager.removeInfoBox(activeInfoBox.get(ArceuusSpellEnums.SHADOW));
		infoBoxManager.removeInfoBox(activeInfoBox.get(ArceuusSpellEnums.SUMMON));
		infoBoxManager.removeInfoBox(activeInfoBox.get(ArceuusSpellEnums.VIGOUR));
		infoBoxManager.removeInfoBox(activeInfoBox.get(ArceuusSpellEnums.CHARGE));
		infoBoxManager.removeInfoBox(activeInfoBox.get(ArceuusSpellEnums.CORRUPTION));
		//Turn on 'Timers' plugin implementation to replace this one
		configManager.setConfiguration("timers", "showArceuus", true);
		configManager.setConfiguration("timers", "showArceuusCooldown", true);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		//Death charge | varbit = 12138 | 1 = On Cooldown
		int chargeBit = client.getVarbitValue( Varbits.DEATH_CHARGE_COOLDOWN );
		if( chargeBit == 1 && !spellActive.get( ArceuusSpellEnums.CHARGE ) )
		{
			double chargeCooldownTime = 61.0;
			onSpellCast(chargeCooldownTime, ArceuusSpellEnums.CHARGE, filename.get(ArceuusSpellEnums.CHARGE),
					"Death Charge cooldown");
			spellActive.replace(ArceuusSpellEnums.CHARGE,true);
		}
		else if( chargeBit == 0 && spellActive.get(ArceuusSpellEnums.CHARGE))
		{
			removeBox(ArceuusSpellEnums.CHARGE);
		}


		//Corruption | varbit = 12288 | 1 = Cooling down
		int corruptionBit = client.getVarbitValue(Varbits.CORRUPTION_COOLDOWN);
		if(corruptionBit == 1 && !spellActive.get( ArceuusSpellEnums.CORRUPTION ) )
		{
			double corruptionCooldownTime = 31.0;
			onSpellCast(corruptionCooldownTime,ArceuusSpellEnums.CORRUPTION, filename.get(corruptionImage),
					    corruptionImage.toString() + " Corruption cooldown");
			spellActive.replace(ArceuusSpellEnums.CORRUPTION, true);
		}
		else if ( corruptionBit == 0 && spellActive.get(ArceuusSpellEnums.CORRUPTION))
		{
			removeBox(ArceuusSpellEnums.CORRUPTION);
		}

		//Summon Thrall | varbit = 12290 | 1 = Cooling down
		//Active Thrall | varbit = 12413 | 0 = Summon ended
		int summonCooldown = client.getVarbitValue(Varbits.RESURRECT_THRALL_COOLDOWN);
		int thrallActive = client.getVarbitValue(Varbits.RESURRECT_THRALL);
		if(thrallActive == 1 && !spellActive.get( ArceuusSpellEnums.SUMMON ))
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

			onSpellCast( (thrallUptime), ArceuusSpellEnums.SUMMON, filename.get(thrallImage),
					    "Active thrall ( " +thrallImage.toString()+" )");
			spellActive.replace(ArceuusSpellEnums.SUMMON,true);

		}
		else if(thrallActive == 0 && spellActive.get(ArceuusSpellEnums.SUMMON))
		{
			removeBox(ArceuusSpellEnums.SUMMON);
		}


		//Vile Vigour | varbit = 12292 | 1 = Active
		int vigourBit = client.getVarbitValue(12292);
		if( vigourBit == 1 && !spellActive.get(ArceuusSpellEnums.VIGOUR))
		{
			double vigourCooldownTime = 11.0;
			onSpellCast(vigourCooldownTime, ArceuusSpellEnums.VIGOUR, filename.get(ArceuusSpellEnums.VIGOUR),
					    "Vile Vigour cooldown");
			spellActive.replace(ArceuusSpellEnums.VIGOUR,true);
		}
		else if( vigourBit == 0 && spellActive.get(ArceuusSpellEnums.VIGOUR) )
		{
			removeBox(ArceuusSpellEnums.VIGOUR);
		}


		//Shadow Veil | varbit : 12414 | 1 = Active
		int shadowBit = client.getVarbitValue( Varbits.SHADOW_VEIL );
		if( shadowBit == 1 && !spellActive.get( ArceuusSpellEnums.SHADOW ) )
		{
			infoBoxManager.removeInfoBox(activeInfoBox.get(ArceuusSpellEnums.SHADOW));
			double shadowTime = 61.0;
			onSpellCast(shadowTime, ArceuusSpellEnums.SHADOW, filename.get(ArceuusSpellEnums.SHADOW),
					    "Shadow Veil active");
			spellActive.replace(ArceuusSpellEnums.SHADOW,true);
		}
		else if( shadowBit == 0 && spellActive.get(ArceuusSpellEnums.SHADOW) )
		{
			removeBox(ArceuusSpellEnums.SHADOW);
		}



	}


	public void onSpellCast(double time, ArceuusSpellEnums identifier, String filename, String tooltip)
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), filename);
		InfoBox box = new ArceuusTimersInfobox(icon, this, time, infoBoxManager,
				                               Instant.now(), identifier, tooltip);

		infoBoxManager.addInfoBox(box);
		activeInfoBox.replace(identifier, box);

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
		log.info(option);
		if(option.contains("Ghost")) {
			thrallImage = ArceuusSpellEnums.Ghost;
			return;
		}
		if(option.contains("Skeleton")) {
			thrallImage = ArceuusSpellEnums.Skeleton;
			return;
		}
		if(option.contains("Zombie")){
			thrallImage = ArceuusSpellEnums.Zombie;
			return;
		}
		if(option.contains("Greater")) {
			corruptionImage = ArceuusSpellEnums.Greater;
			return;
		}
		if(option.contains("Lesser")){
			corruptionImage = ArceuusSpellEnums.Lesser;
		}

	}


	public void removeBox(ArceuusSpellEnums identifier)
	{
		spellActive.replace(identifier, false);
		infoBoxManager.removeInfoBox(activeInfoBox.get(identifier));
		activeInfoBox.replace(identifier,null);
	}

	private void setupHashMaps()
	{
		spellActive.put(ArceuusSpellEnums.SUMMON,false);
		spellActive.put(ArceuusSpellEnums.CHARGE,false);
		spellActive.put(ArceuusSpellEnums.SHADOW,false);
		spellActive.put(ArceuusSpellEnums.VIGOUR,false);
		spellActive.put(ArceuusSpellEnums.CORRUPTION,false);
		activeInfoBox.put(ArceuusSpellEnums.SUMMON,null);
		activeInfoBox.put(ArceuusSpellEnums.CHARGE,null);
		activeInfoBox.put(ArceuusSpellEnums.SHADOW,null);
		activeInfoBox.put(ArceuusSpellEnums.VIGOUR,null);
		activeInfoBox.put(ArceuusSpellEnums.CORRUPTION,null);
		filename.put(ArceuusSpellEnums.Ghost,"/ghost.png");
		filename.put(ArceuusSpellEnums.Skeleton,"/skeleton.png");
		filename.put(ArceuusSpellEnums.Zombie,"/zombie.png");
		filename.put(ArceuusSpellEnums.CHARGE,"/death_charge.png");
		filename.put(ArceuusSpellEnums.SHADOW,"/shadow_veil.png");
		filename.put(ArceuusSpellEnums.VIGOUR,"/vile_vigour.png");
		filename.put(ArceuusSpellEnums.Lesser,"/lesser.png");
		filename.put(ArceuusSpellEnums.Greater,"/greater.png");
	}
  
}