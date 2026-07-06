package com.arceuustimers;

import net.runelite.api.Constants;
import net.runelite.client.ui.overlay.infobox.InfoBox;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ArceuusTimersInfobox extends InfoBox {
	public static final double GAME_TICK_SECONDS = Constants.GAME_TICK_LENGTH / 1000.0;
	private static final double LOW_TIME = 0.175;

	private final double time;
	private double timeLeft;
	private final boolean showText;
	private final ArceuusTimersConfig config;
	private String text;
	private String name;

	public ArceuusTimersInfobox(BufferedImage image, ArceuusTimersPlugin plugin, double time, String tooltip, boolean showText) {
		this(image, plugin, time, tooltip, showText, "");
	}

	public ArceuusTimersInfobox(BufferedImage image, ArceuusTimersPlugin plugin, double time, String tooltip, boolean showText, String text) {
		super(image, plugin);
		this.time = time;
		this.timeLeft = time;
		this.showText = showText;
		this.text = text;
		this.config = plugin.getConfig();
		setTooltip(tooltip);
		setPriority(config.arceuusBoxPriority());
	}

	public void decreaseByGameTick() {
		timeLeft -= GAME_TICK_SECONDS;
	}

	public void changeText(String text) {
		this.text = text;
	}

	public void setTimeLeft(double timeLeft) {
		this.timeLeft = timeLeft;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name != null ? name : super.getName();
	}

	@Override
	public String getText() {
		if (!showText || timeLeft < 0) return "";
		if (!text.isEmpty()) return text;
		switch (config.textFormat()) {
			case MINUTES:
				int minutes = (int) (timeLeft / 60);
				int seconds = (int) (timeLeft % 60);
				return String.format("%d:%02d", minutes, seconds);
			case GAME_TICKS:
				return Integer.toString((int) (timeLeft / GAME_TICK_SECONDS));
			case SECONDS:
			default:
				return Integer.toString((int) timeLeft);
		}
	}

	@Override
	public Color getTextColor() {
		if (!text.isEmpty()) return config.textColour();
		if (timeLeft <= time * LOW_TIME) return config.lowTimeTextColour();
		return config.textColour();
	}

	@Override
	public boolean render() {
		return timeLeft >= 0;
	}

	@Override
	public boolean cull() {
		return timeLeft < 0;
	}
}
