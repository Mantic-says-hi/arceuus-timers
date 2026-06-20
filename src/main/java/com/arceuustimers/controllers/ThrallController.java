package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersInfobox;
import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class ThrallController extends SpellController {
	private static final int CA_TIER_UNLOCKED = 2;
	private static final double THRALL_MULTIPLIER = 2.0;

	private final Client client;

	public ThrallController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		super(fileName, cooldown, tooltip, manager, plugin);
		this.client = client;
	}

	public void createThrall() {
		if (active) shutdown();
		double thrallUptime = ArceuusTimersInfobox.GAME_TICK_SECONDS * client.getBoostedSkillLevel(Skill.MAGIC);
		if (client.getVarbitValue(VarbitID.CA_TIER_STATUS_MASTER) == CA_TIER_UNLOCKED) thrallUptime *= THRALL_MULTIPLIER;
		thrallUptime -= ArceuusTimersInfobox.GAME_TICK_SECONDS;
		setCooldown(thrallUptime);
		createBox();
	}

	@Override
	public void varbitChange(int bit) {
		//Thrall infoboxes use chat messages
	}
}
