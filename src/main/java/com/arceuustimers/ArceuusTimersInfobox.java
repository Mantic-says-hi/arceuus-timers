package com.arceuustimers;


import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;

public class ArceuusTimersInfobox extends InfoBox
{
	private final double time;
	private double timeLeft;
	InfoBoxManager manager;
	ArceuusTimersConfig config;

	public ArceuusTimersInfobox(BufferedImage image, ArceuusTimersPlugin plugin, double time,
								InfoBoxManager manager, String tooltip)
	{
		super(image, plugin);
		this.time = time;
		this.manager = manager;
		this.config = plugin.getConfig();
		timeLeft = time;
		setTooltip(tooltip);
		setImage(image);
		setPriority(config.arceuusBoxPriority());
	}

	public void decreaseByGameTick()
	{
		timeLeft -= 0.6;
	}

	public String getText()
	{
		if(timeLeft < 0)
		{
			return "";
		}

		switch (config.textFormat()) {
			case MINUTES:
				int minutes = (int)(timeLeft / 60);
				int seconds = (int)(timeLeft % 60);
				return String.format("%d:%02d", minutes, seconds);
			case GAME_TICKS:
				return "" + (int)(timeLeft / 0.6);
			case SECONDS:
			default:
				return "" + (int)(timeLeft);
		}
	}

	public Color getTextColor()
	{
		if(timeLeft <= time*0.17) {
			return config.lowTimeTextColour();
		}
		return config.textColour();
	}

	public boolean render()
	{
		return true;
	}

	public boolean cull()
	{
		return false;
	}
  
}