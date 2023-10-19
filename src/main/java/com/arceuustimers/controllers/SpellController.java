package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;


public abstract class SpellController
{
	private boolean active;
	private ArceuusTimersInfobox box;
	private String fileName;
	private double cooldown;
	private String tooltip;
	private final InfoBoxManager manager;
	private final ArceuusTimersPlugin plugin;
	private static final int VARBIT_UP = 1;
	private static final int VARBIT_DOWN = 0;



	public SpellController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin) {
		this.fileName = fileName;
		this.cooldown = cooldown;
		this.tooltip = tooltip;
		this.manager = manager;
		this.plugin = plugin;
		this.active = false;
		this.box = null;
	}


	public boolean getActive() {
		return active;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setCooldown(double cooldown) {
		this.cooldown = cooldown;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public void varbitChange(int bit)
	{
		if( bit == VARBIT_UP && !this.active ) {
			createBox();
		}
		else if( bit == VARBIT_DOWN && this.active) {
			removeBox();
		}
	}

	public void shutdown()
	{
		if(this.active)
		{
			removeBox();
		}
	}

	public void updateTime()
	{
		this.box.decreaseByGameTick();
	}

	protected void createBox()
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
		this.box = new ArceuusTimersInfobox(
				icon,
				this.plugin,
				this.cooldown,
				this.manager,
				this.tooltip);
		this.manager.addInfoBox(this.box);
		this.active = true;
	}

	protected void removeBox()
	{
		this.manager.removeInfoBox(this.box);
		this.active = false;
		this.box = null;
	}
}
