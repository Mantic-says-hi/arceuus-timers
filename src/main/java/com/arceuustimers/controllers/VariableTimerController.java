package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class VariableTimerController extends SpellController {
	protected final Client client;

	public VariableTimerController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		super(fileName, cooldown, tooltip, manager, plugin);
		this.client = client;
	}

	@Override
	public void varbitChange(int bit) {
		if (active) removeBox();
		if (bit == VARBIT_DOWN) return;
		recreate();
	}

	public void nonVarbitChange() {
		if (active) removeBox();
		recreate();
	}

	public void chatExpiredResponse() {
		if (active) removeBox();
	}

	private void recreate() {
		setCooldown(calculateUptime());
		createBox();
	}

	protected double calculateUptime() {
		return ArceuusTimersInfobox.GAME_TICK_SECONDS * (client.getRealSkillLevel(Skill.MAGIC) + 2);
	}
}
