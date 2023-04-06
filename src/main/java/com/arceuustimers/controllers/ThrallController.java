package com.arceuustimers.controllers;

import com.arceuustimers.ArceuusTimersPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
@Slf4j
public class ThrallController extends SpellController
{


	private final Client client;
	private boolean iconLock;

	public ThrallController(String fileName, double cooldown, String tooltip, InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client)
	{
		super(fileName,cooldown,tooltip,manager,plugin);
		this.client = client;
		this.iconLock = false;
	}

	@Override
	public void varbitChange(int bit)
	{
		setIconLock(false);
		if(bit == 1 && !super.getActive())
		{
			double thrallUptime = 0.6 * client.getBoostedSkillLevel(Skill.MAGIC);
			if( client.getVarbitValue( Varbits.COMBAT_ACHIEVEMENT_TIER_GRANDMASTER ) == 2)
			{
				thrallUptime = ( thrallUptime * 2.0 );
			}
			else if( client.getVarbitValue( Varbits.COMBAT_ACHIEVEMENT_TIER_MASTER ) == 2)
			{
				thrallUptime = ( thrallUptime * 1.5 );
			}
			super.setCooldown(thrallUptime);
			createBox();

		}
		else if(bit == 0 && super.getActive())
		{
			removeBox();
		}
	}

	public boolean isIconLocked() {
		return iconLock;
	}

	public void setIconLock(boolean iconLock) {
		this.iconLock = iconLock;
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

}
