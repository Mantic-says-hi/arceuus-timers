package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class CorruptionController extends SpellController {
	@Getter
	@Setter
	private boolean iconLocked;

	public CorruptionController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin) {
		super(fileName, cooldown, tooltip, manager, plugin);
	}

	public static CorruptionController create(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		return new CorruptionController(fileName, cooldown, tooltip, manager, plugin);
	}

	@Override
	public void varbitChange(int bit) {
		if (bit == VARBIT_UP && !active) {
			createBox();
			setIconLocked(false);
		} else if (bit == VARBIT_DOWN && active) {
			removeBox();
		}
	}
}
