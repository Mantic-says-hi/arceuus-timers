package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import com.arceuustimers.IconUtil;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

public abstract class SpellController {
	protected static final int VARBIT_UP = 1;
	protected static final int VARBIT_DOWN = 0;

	private static final float DARKEN_SCALE = 0f;
	private static final int SPRITE_SIZE = 25;

	@Getter
	protected boolean active;
	protected ArceuusTimersInfobox box;
	@Setter
	protected String fileName;
	@Setter
	protected String spellName;
	@Setter
	protected boolean darkenIcon;
	@Setter
	protected double cooldown;
	@Setter
	protected String tooltip;
	protected final InfoBoxManager manager;
	protected final ArceuusTimersPlugin plugin;

	public SpellController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin) {
		this.fileName = fileName;
		this.cooldown = cooldown;
		this.tooltip = tooltip;
		this.manager = manager;
		this.plugin = plugin;
	}

	public void varbitChange(int bit) {
		if (bit == VARBIT_UP && !active) {
			createBox();
		} else if (bit == VARBIT_DOWN && active) {
			removeBox();
		}
	}

	public void shutdown() {
		if (active) removeBox();
	}

	public void updateTime() {
		if (!active || box == null) return;
		box.decreaseByGameTick();
		if (box.cull()) removeBox();
	}

	protected BufferedImage loadIcon() {
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
		if (plugin.getConfig().spriteSizedIcons()) icon = spriteSize(icon);
		return darkenIcon ? ImageUtil.luminanceScale(icon, DARKEN_SCALE) : icon;
	}

	private static BufferedImage spriteSize(BufferedImage image) {
		int largestSide = Math.max(image.getWidth(), image.getHeight());
		if (largestSide <= SPRITE_SIZE) return image;
		int width = image.getWidth() * SPRITE_SIZE / largestSide;
		int height = image.getHeight() * SPRITE_SIZE / largestSide;
		return IconUtil.smoothDownscale(image, width, height);
	}

	public void refreshIcon() {
		if (box != null) {
			box.setImage(loadIcon());
			manager.updateInfoBoxImage(box);
		}
	}

	protected void createBox() {
		box = new ArceuusTimersInfobox(loadIcon(), plugin, cooldown, tooltip, true);
		nameBox(box);
		manager.addInfoBox(box);
		active = true;
	}

	protected void nameBox(ArceuusTimersInfobox box) {
		if (spellName != null && plugin.getConfig().separateInfoboxes()) {
			box.setName("ArceuusTimersInfobox_" + spellName);
		}
	}

	protected void removeBox() {
		if (box != null) {
			manager.removeInfoBox(box);
			box = null;
		}
		active = false;
	}
}
