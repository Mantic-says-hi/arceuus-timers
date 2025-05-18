package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;

public class DeathChargeController extends SpellController {
	private ArceuusTimersInfobox secondBox; // second infobox for charge 2
	private int currentCharges; // track current varbit value
	private boolean hideText = false;
	public DeathChargeController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin) {
		super(fileName, cooldown, tooltip, manager, plugin);
		this.secondBox = null;
		this.currentCharges = 0;
	}

	@Override
	public void varbitChange(int bit) {
		if (bit == currentCharges) return;

		if (bit == 0) {
			removeBox();
		} else if (bit == 1) {
			if (currentCharges == 0) {
				createBox();
			} else if (currentCharges == 2) {
				removeSecondBox();
			}
		} else if (bit == 2) {
			if (currentCharges == 0) {
				createBox();
				createSecondBox();
			} else if (currentCharges == 1) {
				createSecondBox();
			}
		}
		currentCharges = bit;
	}

	@Override
	protected void createBox() {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
		box = new ArceuusTimersInfobox(
				icon,
				plugin,
				cooldown,
				manager,
				"Death Charge (1)",
				hideText);
		manager.addInfoBox(box);
		active = true;
	}

	private void createSecondBox() {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
		secondBox = new ArceuusTimersInfobox(
				icon,
				plugin,
				cooldown,
				manager,
				"Death Charge (2)",
				hideText);
		manager.addInfoBox(secondBox);
	}

	@Override
	protected void removeBox() {
		if (box != null)
		{
			manager.removeInfoBox(box);
			box = null;
		}
		removeSecondBox();
		active = false;
		currentCharges = 0;
	}

	private void removeSecondBox()
	{
		if (secondBox == null) return;
		manager.removeInfoBox(secondBox);
		secondBox = null;
	}

	@Override
	public void shutdown() {
		removeBox();
	}

	@Override
	public void updateTime() {
		if (box != null) box.decreaseByGameTick();
		if (secondBox != null) secondBox.decreaseByGameTick();
	}
}