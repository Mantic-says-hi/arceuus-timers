package com.arceuustimers;

import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;

import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;

public class ArceuusTimersOverlay extends OverlayPanel
{
	@Inject
	private ArceuusTimersOverlay(ArceuusTimersPlugin plugin)
	{
		super(plugin);
		setPosition(OverlayPosition.BOTTOM_LEFT);
		setPriority(OverlayPriority.LOW);
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Arceuus Overlay "));
	}
}