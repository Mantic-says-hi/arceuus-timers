package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class DeathChargeController extends SpellController {
	private ArceuusTimersInfobox secondBox;
	private int currentCharges;
	private boolean stacked;
	private final Client client;
	private int firstCastTick;
	private int secondCastTick;

	public DeathChargeController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		super(fileName, cooldown, tooltip, manager, plugin);
		this.stacked = plugin.getConfig().stackDeathCharge();
		this.client = client;
	}

	public static DeathChargeController create(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		return new DeathChargeController(fileName, cooldown, tooltip, manager, plugin, client);
	}

	//Charges are tracked even while the config is off so the boxes can restore with correct times
	@Override
	public void varbitChange(int bit) {
		if (bit == currentCharges) return;
		boolean show = plugin.getConfig().showDeathChargeActive();
		int tick = client.getTickCount();
		if (bit == 0) {
			removeBox();
		} else if (bit == 1) {
			if (currentCharges == 0) {
				firstCastTick = tick;
				if (show) createBox(bit);
			} else if (currentCharges == 2) {
				if (stacked && box != null) box.changeText("1");
				removeSecondBox();
			}
		} else if (bit == 2) {
			if (currentCharges == 0) {
				firstCastTick = tick;
				secondCastTick = tick;
				if (show) {
					createBox(bit);
					createSecondBox();
				}
			} else if (currentCharges == 1) {
				secondCastTick = tick;
				if (show) createSecondBox();
			}
		}
		currentCharges = bit;
	}

	protected void createBox(int count) {
		if (!stacked) {
			box = new ArceuusTimersInfobox(loadIcon(), plugin, cooldown, "Death Charge (1)", false);
		} else {
			box = new ArceuusTimersInfobox(loadIcon(), plugin, cooldown,
					count == 2 ? "Death Charge's" : "Death Charge", true, Integer.toString(count));
		}
		box.setTimeLeft(remainingTime(firstCastTick));
		nameBox(box);
		manager.addInfoBox(box);
		active = true;
	}

	private void createSecondBox() {
		if (stacked) return;
		secondBox = new ArceuusTimersInfobox(loadIcon(), plugin, cooldown, "Death Charge (2)", false);
		secondBox.setTimeLeft(remainingTime(secondCastTick));
		nameBox(secondBox);
		manager.addInfoBox(secondBox);
	}

	private double remainingTime(int castTick) {
		return cooldown - (client.getTickCount() - castTick) * ArceuusTimersInfobox.GAME_TICK_SECONDS;
	}

	@Override
	protected void removeBox() {
		if (box != null) {
			manager.removeInfoBox(box);
			box = null;
		}
		removeSecondBox();
		active = false;
		currentCharges = 0;
	}

	private void removeSecondBox() {
		if (secondBox == null) return;
		manager.removeInfoBox(secondBox);
		secondBox = null;
	}

	@Override
	public void shutdown() {
		removeBox();
	}

	@Override
	public void refreshIcon() {
		super.refreshIcon();
		if (secondBox != null) {
			secondBox.setImage(loadIcon());
			manager.updateInfoBoxImage(secondBox);
		}
	}

	@Override
	public void updateTime() {
		if (box != null) {
			box.decreaseByGameTick();
			if (box.cull()) {
				manager.removeInfoBox(box);
				box = null;
			}
		}
		if (secondBox != null) {
			secondBox.decreaseByGameTick();
			if (secondBox.cull()) removeSecondBox();
		}
		if (box == null && secondBox == null) active = false;
	}

	public void reCreate() {
		if (box == null && secondBox == null) return;
		setVisible(true);
	}

	public void setVisible(boolean visible) {
		int charges = currentCharges;
		removeBox();
		currentCharges = charges;
		if (!visible || charges == 0) return;
		stacked = plugin.getConfig().stackDeathCharge();
		createBox(charges);
		if (charges == 2 && !stacked) createSecondBox();
	}
}
