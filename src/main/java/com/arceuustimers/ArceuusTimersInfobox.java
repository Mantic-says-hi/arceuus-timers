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
	private final Instant end;
	private long left;
	InfoBoxManager manager;
	ArceuusTimersConfig config;

	public ArceuusTimersInfobox(BufferedImage image, ArceuusTimersPlugin plugin, double time,
								InfoBoxManager manager, Instant start, String tooltip)
	{
		super(image, plugin);
		this.time = time;
		this.manager = manager;
		this.config = plugin.getConfig();
		this.end = start.plusSeconds((long)time);
		setTooltip(tooltip);
		setImage(image);
		setPriority(config.arceuusBoxPriority());
	}

	public String getText()
	{
		Duration timeLeft = Duration.between(Instant.now(), end);
		left = timeLeft.toMillis()/1000;

		if(left < 0)
		{
			return "";
		}

		if(config.formatOption()){
			int minutes = (int) left / 60;
			int seconds = (int) left % 60;
			return String.format("%d:%02d", minutes, seconds);
		}else {
			return "" + left;
		}

	}

	public Color getTextColor()
	{
		if((double)left <= time*0.17)
		{
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