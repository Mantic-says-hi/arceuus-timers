package com.arceuustimers;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DeathChargeEffectOverlay extends Overlay {
	private static final int EXTRA_CHARGE_OFFSET_X = 5;
	private static final int EXTRA_CHARGE_OFFSET_Y = 4;
	private static final int BASE_ICON_SIZE = 26;
	private static final int MIN_ICON_SIZE = 8;
	private static final int OUTLINE_PASSES = 6;

	private final Client client;
	private final ArceuusTimersConfig config;
	private final ArceuusTimersPlugin plugin;
	private static final int HIT_PADDING = 4;

	private final PartyService partyService;
	private BufferedImage icon;
	private volatile Rectangle localIconBounds;

	@Inject
	private DeathChargeEffectOverlay(Client client, ArceuusTimersConfig config, ArceuusTimersPlugin plugin, PartyService partyService) {
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.partyService = partyService;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.showDeathChargeOnPlayer() && !config.showDeathChargeParty() && !config.showDeathChargeOthers()) return null;

		int size = Math.max(MIN_ICON_SIZE, BASE_ICON_SIZE + config.deathChargeSizeOffset());
		if (icon == null || icon.getWidth() != size) icon = buildIcon(size);

		localIconBounds = null;
		if (config.showDeathChargeOnPlayer()) {
			int charges = client.getVarbitValue(VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE);
			Player local = client.getLocalPlayer();
			if (local != null && charges > 0) {
				drawCharges(graphics, local, charges, false);
			} else if (local != null && config.deathChargeReposition()) {
				drawPreview(graphics, local);
			}
		}

		if (config.deathChargeReposition() && localIconBounds != null) {
			graphics.setColor(Color.YELLOW);
			graphics.draw(localIconBounds);
		}

		renderOtherPlayers(graphics);
		return null;
	}

	private void renderOtherPlayers(Graphics2D graphics) {
		Map<String, Integer> party = new HashMap<>();
		if (config.showDeathChargeParty() && partyService.isInParty()) {
			PartyMember localMember = partyService.getLocalMember();
			for (Map.Entry<Long, Integer> entry : plugin.getPartyDeathCharges().entrySet()) {
				if (localMember != null && entry.getKey() == localMember.getMemberId()) continue;
				PartyMember member = partyService.getMemberById(entry.getKey());
				if (member == null || member.getDisplayName() == null) continue;
				party.put(Text.standardize(member.getDisplayName()), entry.getValue());
			}
		}
		Map<String, Integer> detected = config.showDeathChargeOthers()
				? plugin.getDetectedDeathCharges()
				: Collections.emptyMap();
		if (party.isEmpty() && detected.isEmpty()) return;

		Player local = client.getLocalPlayer();
		for (Player player : client.getTopLevelWorldView().players()) {
			if (player == null || player == local || player.getName() == null) continue;
			String name = Text.standardize(player.getName());
			Integer partyCharges = party.get(name);
			if (partyCharges != null) {
				drawCharges(graphics, player, partyCharges, false);
			} else {
				Integer detectedCharges = detected.get(name);
				if (detectedCharges != null) drawCharges(graphics, player, detectedCharges, true);
			}
		}
	}

	private void drawCharges(Graphics2D graphics, Player player, int charges, boolean uncertain) {
		Point location = player.getCanvasImageLocation(icon, anchorHeight(player));
		if (location == null) return;
		location = new Point(location.getX() + config.deathChargeOffsetX(), location.getY() + config.deathChargeOffsetY());

		if (player == client.getLocalPlayer()) localIconBounds = iconBounds(location);

		OverlayUtil.renderImageLocation(graphics, location, icon);
		if (charges >= 2) {
			Point extra = new Point(location.getX() + EXTRA_CHARGE_OFFSET_X, location.getY() + EXTRA_CHARGE_OFFSET_Y);
			OverlayUtil.renderImageLocation(graphics, extra, icon);
		}
		if (uncertain) drawUncertaintyMark(graphics, location);
	}

	private void drawPreview(Graphics2D graphics, Player player) {
		Point location = player.getCanvasImageLocation(icon, anchorHeight(player));
		if (location == null) return;
		location = new Point(location.getX() + config.deathChargeOffsetX(), location.getY() + config.deathChargeOffsetY());
		localIconBounds = iconBounds(location);

		Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		OverlayUtil.renderImageLocation(graphics, location, icon);
		graphics.setComposite(original);
	}

	private Rectangle iconBounds(Point location) {
		return new Rectangle(location.getX() - HIT_PADDING, location.getY() - HIT_PADDING,
				icon.getWidth() + HIT_PADDING * 2, icon.getHeight() + HIT_PADDING * 2);
	}

	public Rectangle getLocalIconBounds() {
		return localIconBounds;
	}

	private void drawUncertaintyMark(Graphics2D graphics, Point iconLocation) {
		graphics.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics metrics = graphics.getFontMetrics();
		int size = icon.getWidth();
		int x = iconLocation.getX() + size - metrics.stringWidth("?") / 2;
		int y = iconLocation.getY() + size;
		OverlayUtil.renderTextLocation(graphics, new Point(x, y), "?", Color.WHITE);
	}

	private int anchorHeight(Player player) {
		switch (config.deathChargeAnchor()) {
			case HEAD:
				return player.getLogicalHeight();
			case MIDDLE:
				return player.getLogicalHeight() / 2;
			case FEET:
				return 0;
			case NECK:
			default:
				return player.getLogicalHeight() * 3 / 4;
		}
	}

	private BufferedImage buildIcon(int size) {
		BufferedImage image = ImageUtil.loadImageResource(getClass(), "/death_charge.png");
		int padding = 2 * OUTLINE_PASSES * 2;
		image = ImageUtil.resizeCanvas(image, image.getWidth() + padding, image.getHeight() + padding);
		for (int i = 0; i < OUTLINE_PASSES; i++) image = ImageUtil.outlineImage(image, Color.BLACK);
		for (int i = 0; i < OUTLINE_PASSES; i++) image = ImageUtil.outlineImage(image, Color.WHITE);
		return IconUtil.smoothDownscale(image, size, size);
	}
}
