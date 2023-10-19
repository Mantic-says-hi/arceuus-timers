package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class VariableTimerController extends SpellController
{
	private final Client client;
	private static final double GAME_TICK = 0.6;
	private static final int VARBIT_DOWN = 0;

	public VariableTimerController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client)
	{
		super(fileName,cooldown,tooltip,manager,plugin);
		this.client = client;
	}

	@Override
	public void varbitChange(int bit)
	{
		if(super.getActive()) {
			removeBox();
		}
		if (bit == VARBIT_DOWN) {
			return;
		}
		creationHandler();
	}

	public void nonVarbitChange() {
		if(super.getActive()) {
			removeBox();
		}
		creationHandler();
	}

	private void creationHandler()
	{
		double upTime = (GAME_TICK * client.getRealSkillLevel(Skill.MAGIC)) + (GAME_TICK * 2);
		super.setCooldown(upTime);
		createBox();
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

	public void chatExpiredResponse()
	{
		if(super.getActive())
		{
			removeBox();
		}
	}
}
