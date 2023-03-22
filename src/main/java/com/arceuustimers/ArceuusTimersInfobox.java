package com.arceuustimers;


import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;

public class ArceuusTimersInfobox extends InfoBox
{
	private final ArceuusTimersPlugin plugin;
	private double time;
	private Instant start;
	private Instant end;
	private ArceuusSpellEnums identifier;
	private long left;
	InfoBoxManager manager;

	public ArceuusTimersInfobox(BufferedImage image, ArceuusTimersPlugin plugin, double time,
								InfoBoxManager manager, Instant start, ArceuusSpellEnums identifier, String tooltip)
	{
		super(image, plugin);
		this.plugin = plugin;
		this.time = time;
		this.manager = manager;
		this.start = start;
		this.identifier = identifier;
		this.end = start.plusSeconds((long)time);
		setTooltip(tooltip);
		setImage(image);
		setPriority(InfoBoxPriority.HIGH);

	}

	public String getText()
	{
		Duration timeLeft = Duration.between(Instant.now(), end);
		left = timeLeft.toMillis()/1000;

		if(left < 0)
		{
			return "";
		}

		return "" + left;
	}

	public Color getTextColor()
	{
		if((double)left <= time*0.17)
		{
			return Color.ORANGE;
		}

		return Color.PINK.brighter();
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