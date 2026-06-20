package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class StandardController extends SpellController {
	public StandardController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin) {
		super(fileName, cooldown, tooltip, manager, plugin);
	}

	public static StandardController create(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		return new StandardController(fileName, cooldown, tooltip, manager, plugin);
	}
}
