package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class StandardController extends SpellController
{

	public StandardController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin)
	{
		super(fileName,cooldown,tooltip,manager,plugin);
	}


	@Override
	public void varbitChange(int bit)
	{
		super.varbitChange(bit);
	}

	@Override
	protected void createBox() {
		super.createBox();
	}

	@Override
	protected void removeBox()
	{
		super.removeBox();
	}


}
