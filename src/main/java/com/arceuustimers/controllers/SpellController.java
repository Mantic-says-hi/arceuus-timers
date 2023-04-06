package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.time.Instant;

public abstract class SpellController
{
	private boolean active;
	private InfoBox box;
	private String fileName;
	private double cooldown;
	private String tooltip;
	private final InfoBoxManager manager;
	private final ArceuusTimersPlugin plugin;



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
		if( bit == 1 && !this.active ) {
			createBox();
		}
		else if( bit == 0 && this.active) {
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

	protected void createBox()
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
		this.box = new ArceuusTimersInfobox(
				icon,
				this.plugin,
				this.cooldown,
				this.manager,
				Instant.now(),
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
