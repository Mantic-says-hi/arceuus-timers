package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;


public abstract class SpellController
{
	protected boolean active;
	protected ArceuusTimersInfobox box;
	protected String fileName;
	protected double cooldown;
	protected String tooltip;
	protected final InfoBoxManager manager;
	protected final ArceuusTimersPlugin plugin;
	protected static final int VARBIT_UP = 1;
	protected static final int VARBIT_DOWN = 0;
	private boolean showText = true;

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
		if (!active || box == null) return;
		box.decreaseByGameTick();
		if (box.cull()) removeBox();
	}

	protected void createBox()
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
		this.box = new ArceuusTimersInfobox(
				icon,
				this.plugin,
				this.cooldown,
				this.manager,
				this.tooltip,
				showText);
		this.manager.addInfoBox(this.box);
		this.active = true;
	}

	protected void removeBox() {
		if (box != null)
		{
			manager.removeInfoBox(box);
			box = null;
		}
		active = false;
	}
}
