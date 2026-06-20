package com.arceuustimers;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

@Singleton
public class DeathChargeIconDrag extends MouseAdapter {
	private static final int OFFSET_LIMIT = 100;

	private final ArceuusTimersConfig config;
	private final ConfigManager configManager;
	private final DeathChargeEffectOverlay overlay;

	private boolean dragging;
	private int dragStartX;
	private int dragStartY;
	private int startOffsetX;
	private int startOffsetY;

	@Inject
	private DeathChargeIconDrag(ArceuusTimersConfig config, ConfigManager configManager, DeathChargeEffectOverlay overlay) {
		this.config = config;
		this.configManager = configManager;
		this.overlay = overlay;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e) {
		if (!config.showDeathChargeOnPlayer() || !config.deathChargeReposition()) return e;
		Rectangle bounds = overlay.getLocalIconBounds();
		if (bounds == null || !bounds.contains(e.getPoint())) return e;
		if (e.getButton() == MouseEvent.BUTTON1) {
			dragging = true;
			dragStartX = e.getX();
			dragStartY = e.getY();
			startOffsetX = config.deathChargeOffsetX();
			startOffsetY = config.deathChargeOffsetY();
			e.consume();
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.DEATH_CHARGE_OFFSET_X, 0);
			configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.DEATH_CHARGE_OFFSET_Y, 0);
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e) {
		if (!dragging) return e;
		int x = clamp(startOffsetX + e.getX() - dragStartX);
		int y = clamp(startOffsetY + e.getY() - dragStartY);
		configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.DEATH_CHARGE_OFFSET_X, x);
		configManager.setConfiguration(ArceuusTimersConfig.GROUP, ArceuusTimersConfig.DEATH_CHARGE_OFFSET_Y, y);
		e.consume();
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e) {
		if (dragging) {
			dragging = false;
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e) {
		//Swallow the click so it never reaches the game while repositioning over the icon
		if (e.getButton() != MouseEvent.BUTTON1 && e.getButton() != MouseEvent.BUTTON3) return e;
		if (!config.showDeathChargeOnPlayer() || !config.deathChargeReposition()) return e;
		Rectangle bounds = overlay.getLocalIconBounds();
		if (bounds != null && bounds.contains(e.getPoint())) e.consume();
		return e;
	}

	private static int clamp(int value) {
		return Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, value));
	}
}
