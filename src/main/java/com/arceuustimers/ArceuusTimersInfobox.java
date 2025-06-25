package com.arceuustimers;

import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ArceuusTimersInfobox extends InfoBox
{
	private final double time;
	private double timeLeft;
	private final boolean showText;
	InfoBoxManager manager;
	ArceuusTimersConfig config;
	private static final double GAME_TICK = 0.6;
	private static final double LOW_TIME = 0.175;

	public ArceuusTimersInfobox(BufferedImage image, ArceuusTimersPlugin plugin, double time,
								InfoBoxManager manager, String tooltip, boolean showText)
	{
		super(image, plugin);
		this.time = time;
		this.manager = manager;
		this.showText = showText;
		this.config = plugin.getConfig();
		timeLeft = time;

		setTooltip(tooltip);
		setImage(image);
		setPriority(config.arceuusBoxPriority());
	}

	public void decreaseByGameTick()
	{
		timeLeft -= GAME_TICK;
	}

	public String getText()
	{
		if (!showText || timeLeft < 0) return "";
		switch (config.textFormat()) {
			case MINUTES:
				int minutes = (int)(timeLeft / 60);
				int seconds = (int)(timeLeft % 60);
				return String.format("%d:%02d", minutes, seconds);
			case GAME_TICKS:
				return "" + (int)(timeLeft / GAME_TICK);
			case SECONDS:
			default:
				return "" + (int)(timeLeft);
		}
	}

	public Color getTextColor()
	{
		if(timeLeft <= time * LOW_TIME) return config.lowTimeTextColour();
		return config.textColour();
	}

	public boolean render()
	{
		return timeLeft >= 0;
	}

	public boolean cull()
	{
		return timeLeft < 0;
	}
}