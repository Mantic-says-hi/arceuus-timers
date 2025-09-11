package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;

public class DeathChargeController extends SpellController {
	private ArceuusTimersInfobox secondBox; // second infobox for charge 2
	private int currentCharges; // track current varbit value
    private boolean stacked = false;
	public DeathChargeController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin) {
		super(fileName, cooldown, tooltip, manager, plugin);
		this.secondBox = null;
		this.currentCharges = 0;
        this.stacked = plugin.getConfig().stackDeathCharge();
	}

	@Override
	public void varbitChange(int bit) {
		if (bit == currentCharges) return;
		if (bit == 0) {
			removeBox();
		} else if (bit == 1) {
			if (currentCharges == 0) {
				createBox(bit);
			} else if (currentCharges == 2) {
                if (stacked) box.changeText("1");
				removeSecondBox();
			}
		} else if (bit == 2) {
			if (currentCharges == 0) {
				createBox(bit);
				createSecondBox();
			} else if (currentCharges == 1) {
				createSecondBox();
			}
		}
		currentCharges = bit;
	}


	protected void createBox(int count) {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
        if(!stacked) {
            box = new ArceuusTimersInfobox(
                    icon,
                    plugin,
                    cooldown,
                    manager,
                    "Death Charge (1)",
                    stacked);
        }
        else {
            box = new ArceuusTimersInfobox(
                    icon,
                    plugin,
                    cooldown,
                    manager,
                    count == 2 ? "Death Charge's" : "Death Charge",
                    stacked,
                    Integer.toString(count));
        }
		manager.addInfoBox(box);
		active = true;
	}

	private void createSecondBox() {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), fileName);
        if (stacked) return;
        secondBox = new ArceuusTimersInfobox(
				icon,
				plugin,
				cooldown,
				manager,
				"Death Charge (2)",
                false);
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

    public void reCreate()
    {
        if (box == null) return;
        int charge = currentCharges;
        shutdown();
        removeSecondBox();
        this.stacked = plugin.getConfig().stackDeathCharge();
        this.currentCharges = charge;
        if (charge == 1) {
                createBox(charge);
        } else if (charge == 2) {
            if (!stacked) {
                createBox(charge);
                createSecondBox();
            } else {
                createBox(charge);
            }
        }
    }

}