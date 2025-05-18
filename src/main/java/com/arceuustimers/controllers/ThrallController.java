package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
@Slf4j
public class ThrallController extends SpellController
{
	private final Client client;
	private static final double GAME_TICK = 0.6;
	private static final int UNLOCKED = 2;
	private static final double GM_MULTIPLIER = 2.0;
	private static final double M_MULTIPLIER = 1.5;

	public ThrallController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client)
	{
		super(fileName,cooldown,tooltip,manager,plugin);
		this.client = client;
	}

	public void createThrall()
	{
		if (getActive()) shutdown();
		double thrallUptime = GAME_TICK * client.getBoostedSkillLevel(Skill.MAGIC);
		if (client.getVarbitValue(VarbitID.CA_TIER_STATUS_GRANDMASTER ) == UNLOCKED)
		{
			thrallUptime *= GM_MULTIPLIER;
		}
		else if (client.getVarbitValue(VarbitID.CA_TIER_STATUS_MASTER) == UNLOCKED)
		{
			thrallUptime *= M_MULTIPLIER;
		}
		thrallUptime -= GAME_TICK; // 1-tick delay for box creation
		setCooldown(thrallUptime);
		createBox();
	}

	@Override
	public void varbitChange(int bit)
	{
		//Remove creation of thrall infoboxes via varbits
	}

	@Override
	protected void createBox()
	{
		super.createBox();
	}

	@Override
	protected void removeBox()
	{
		super.removeBox();
	}
}
