package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class CorruptionController extends SpellController
{
	private boolean iconLock;

	public CorruptionController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin)
	{
		super(fileName,cooldown,tooltip,manager,plugin);
		this.iconLock = false;
	}


	public boolean isIconLocked() {
		return iconLock;
	}

	public void setIconLock(boolean iconLock) {
		this.iconLock = iconLock;
	}

	@Override
	public void varbitChange(int bit)
	{
		if( bit == 1 && !super.getActive() ) {
			createBox();
			setIconLock(false);
		}
		else if( bit == 0 && super.getActive()) {
			removeBox();
		}
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
