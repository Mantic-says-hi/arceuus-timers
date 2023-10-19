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
	private static final double GAME_TICK = 0.6;
	private static final int VARBIT_DOWN = 0;
	private static final int VARBIT_MANUAL= -1;
	private static final int VARBIT_UP = 1;
	private static final int UNLOCKED = 2;
	private static final double GM_MULTIPLIER = 2.0;
	private static final double M_MULTIPLIER = 1.5;

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
		if((bit == VARBIT_UP || bit == VARBIT_MANUAL) && !super.getActive())
		{
			double thrallUptime = GAME_TICK * client.getBoostedSkillLevel(Skill.MAGIC);
			if( client.getVarbitValue( Varbits.COMBAT_ACHIEVEMENT_TIER_GRANDMASTER ) == UNLOCKED)
			{
				thrallUptime = ( thrallUptime * GM_MULTIPLIER );
			}
			else if( client.getVarbitValue( Varbits.COMBAT_ACHIEVEMENT_TIER_MASTER ) == UNLOCKED)
			{
				thrallUptime = ( thrallUptime * M_MULTIPLIER );
			}
			thrallUptime -= GAME_TICK;//Accounts for 1 tick delay when setting box
			if(bit == VARBIT_MANUAL){thrallUptime += ( 5 * GAME_TICK);}//Accounts for 5 tick delay in special cases
			super.setCooldown(thrallUptime);
			createBox();

		}
		else if(bit == VARBIT_DOWN && super.getActive())
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
