package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class ShadowVeilController extends SpellController
{
	private final Client client;

	public ShadowVeilController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client)
	{
		super(fileName,cooldown,tooltip,manager,plugin);
		this.client = client;
	}

	@Override
	public void varbitChange(int bit)
	{
		if(super.getActive())
		{
			removeBox();
		}
		if (bit == 1)
		{
			double wardTime = (0.6 * client.getRealSkillLevel(Skill.MAGIC)) + 1.2; //Add extra 2 ticks
			super.setCooldown(wardTime);
			createBox();
		}

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

	public void veilFadedResponse()
	{
		if(super.getActive())
		{
			removeBox();
		}
	}
}
