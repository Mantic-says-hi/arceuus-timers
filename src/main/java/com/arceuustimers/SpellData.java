package com.arceuustimers;

import net.runelite.client.ui.overlay.infobox.InfoBox;

public class SpellData
{
	private boolean isActive;
	private InfoBox infoBox;
	private String fileName;
	private double cooldown;
	private String tooltip;

	public SpellData(boolean isActive, InfoBox infoBox, String fileName, double cooldown, String tooltip) {
		this.isActive = isActive;
		this.infoBox = infoBox;
		this.fileName = fileName;
		this.cooldown = cooldown;
		this.tooltip = tooltip;
	}

	public boolean isActive() {
		return isActive;
	}

	public InfoBox getInfoBox() {
		return infoBox;
	}

	public String getFileName() {
		return fileName;
	}

	public double getCooldown() {
		return cooldown;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setActive(boolean active) {
		isActive = active;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setInfoBox(InfoBox infoBox) {
		this.infoBox = infoBox;
	}

	public void setCooldown(double cooldown) {
		this.cooldown = cooldown;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
}
