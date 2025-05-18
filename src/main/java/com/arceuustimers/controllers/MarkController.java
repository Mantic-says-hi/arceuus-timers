package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class MarkController extends SpellController
{
	private final Client client;
	private static final double GAME_TICK = 0.6;
	private static final int VARBIT_DOWN = 0;
	private static final int PURGING_STAFF = 29594;

	public MarkController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client)
	{
		super(fileName,cooldown,tooltip,manager,plugin);
		this.client = client;
	}

	@Override
	public void varbitChange(int bit)
	{
		if(super.getActive()) {
			removeBox();
		}
		if (bit == VARBIT_DOWN) {
			return;
		}
		creationHandler();
	}

	public void nonVarbitChange() {
		if(super.getActive()) {
			removeBox();
		}
		creationHandler();
	}

	private void creationHandler()
	{
		double upTime = (GAME_TICK * client.getRealSkillLevel(Skill.MAGIC)) + (GAME_TICK * 2);
		ItemContainer inv = client.getItemContainer(InventoryID.EQUIPMENT);
		if(inv != null)
			for(Item it : inv.getItems()) {
				if(it == null) continue;
				if(it.getId() != PURGING_STAFF) continue;
				upTime = ((GAME_TICK * client.getRealSkillLevel(Skill.MAGIC)) * 5)+ (GAME_TICK);
			}
		super.setCooldown(upTime);
		createBox();
	}

	@Override
	protected void createBox()
	{
		super.createBox();
	}

	@Override
	protected void removeBox()
	{
		super.removeBox();
	}

	public void chatExpiredResponse()
	{
		if(super.getActive())removeBox();
	}
}
