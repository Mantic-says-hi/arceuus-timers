package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class MarkController extends VariableTimerController {
	private static final int MARK_DURATION_TICKS = 300;
	private static final int MARK_PURGING_STAFF_DURATION_TICKS = 1500;

	public MarkController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		super(fileName, cooldown, tooltip, manager, plugin, client);
	}

	@Override
	protected double calculateUptime() {
		int durationTicks = wearingPurgingStaff() ? MARK_PURGING_STAFF_DURATION_TICKS : MARK_DURATION_TICKS;
		return ArceuusTimersInfobox.GAME_TICK_SECONDS * (durationTicks + 2);
	}

	private boolean wearingPurgingStaff() {
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null) return false;
		for (Item item : worn.getItems()) {
			if (item != null && item.getId() == ItemID.PURGING_STAFF) return true;
		}
		return false;
	}
}
