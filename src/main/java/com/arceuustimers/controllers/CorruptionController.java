package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class CorruptionController extends SpellController
{
	private boolean iconLock;
	private static final int VARBIT_DOWN = 0;
	private static final int VARBIT_UP = 1;

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
		if( bit == VARBIT_UP && !super.getActive() ) {
			createBox();
			setIconLock(false);
		}
		else if( bit == VARBIT_DOWN && super.getActive()) {
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
