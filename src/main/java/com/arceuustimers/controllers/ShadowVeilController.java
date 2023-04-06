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
		if( bit == 1 && !super.getActive())
		{
			double shadowTime = 0.6 * client.getRealSkillLevel(Skill.MAGIC);
			super.setCooldown(shadowTime);
			createBox();
		}
		else if( bit == 0 && super.getActive())
		{
			removeBox();
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

	public void chatMessageResponse()
	{
		if(super.getActive())
		{
			removeBox();
			double shadowTime = 0.6 * client.getRealSkillLevel(Skill.MAGIC);
			super.setCooldown(shadowTime);
			createBox();
		}
	}
}
