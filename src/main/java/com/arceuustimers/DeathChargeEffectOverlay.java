package com.arceuustimers;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DeathChargeEffectOverlay extends Overlay {
	private static final int EXTRA_CHARGE_OFFSET_X = 5;
	private static final int EXTRA_CHARGE_OFFSET_Y = 4;
	private static final int BASE_ICON_SIZE = 24;
	private static final int MIN_ICON_SIZE = 8;
	private static final int SUBSCRIPT_DROP = 4;
	private static final int SUBSCRIPT_PUSH_RIGHT = 3;
	private static final int SUBSCRIPT_PUSH_LEFT = 5;

	private final Client client;
	private final ArceuusTimersConfig config;
	private final RuneLiteConfig runeLiteConfig;
	private final ArceuusTimersPlugin plugin;
	private static final int HIT_PADDING = 4;

	private final PartyService partyService;
	private BufferedImage icon;
	private volatile Rectangle localIconBounds;

	@Inject
	private DeathChargeEffectOverlay(Client client, ArceuusTimersConfig config, RuneLiteConfig runeLiteConfig, ArceuusTimersPlugin plugin, PartyService partyService) {
		this.client = client;
		this.config = config;
		this.runeLiteConfig = runeLiteConfig;
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
		Player local = client.getLocalPlayer();

		//Group active charges by tile so stacked players collapse into one icon
		Map<WorldPoint, TileGroup> groups = new HashMap<>();
		if (config.showDeathChargeOnPlayer() && local != null) {
			int charges = client.getVarbitValue(VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE);
			if (charges > 0) addEntry(groups, local, charges, false, true);
		}
		collectOtherPlayers(groups, local);

		for (TileGroup group : groups.values()) drawGroup(graphics, group);

		if (config.showDeathChargeOnPlayer() && config.deathChargeReposition() && local != null && localIconBounds == null) {
			drawPreview(graphics, local);
		}

		if (config.deathChargeReposition() && localIconBounds != null) {
			graphics.setColor(Color.YELLOW);
			graphics.draw(localIconBounds);
		}
		return null;
	}

	private void collectOtherPlayers(Map<WorldPoint, TileGroup> groups, Player local) {
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

		for (Player player : client.getTopLevelWorldView().players()) {
			if (player == null || player == local || player.getName() == null) continue;
			String name = Text.standardize(player.getName());
			Integer partyCharges = party.get(name);
			if (partyCharges != null) {
				addEntry(groups, player, partyCharges, false, false);
			} else {
				Integer detectedCharges = detected.get(name);
				if (detectedCharges != null) addEntry(groups, player, detectedCharges, true, false);
			}
		}
	}

	private void addEntry(Map<WorldPoint, TileGroup> groups, Player player, int charges, boolean uncertain, boolean isLocal) {
		WorldPoint tile = player.getWorldLocation();
		if (tile == null) return;
		TileGroup group = groups.get(tile);
		if (group == null) {
			group = new TileGroup();
			groups.put(tile, group);
		}
		group.players++;
		if (!uncertain) group.certain = true;
		if (isLocal || group.anchor == null) {
			group.anchor = player;
			group.anchorCharges = charges;
		}
	}

	private void drawGroup(Graphics2D graphics, TileGroup group) {
		if (group.anchor == null) return;
		if (group.players <= 1) {
			drawCharges(graphics, group.anchor, group.anchorCharges, !group.certain);
		} else {
			drawStacked(graphics, group.anchor, group.players, group.anchorCharges, !group.certain);
		}
	}

	private void drawStacked(Graphics2D graphics, Player player, int players, int charges, boolean uncertain) {
		Point location = player.getCanvasImageLocation(icon, anchorHeight(player));
		if (location == null) return;
		location = new Point(location.getX() + config.deathChargeOffsetX(), location.getY() + config.deathChargeOffsetY());

		if (player == client.getLocalPlayer()) localIconBounds = iconBounds(location);

		OverlayUtil.renderImageLocation(graphics, location, icon);
		drawSubscript(graphics, location, Integer.toString(players), false);
		String subscript = chargeSubscript(charges, uncertain, true);
		if (!subscript.isEmpty()) drawSubscript(graphics, location, subscript, true);
	}

	private void drawCharges(Graphics2D graphics, Player player, int charges, boolean uncertain) {
		Point location = player.getCanvasImageLocation(icon, anchorHeight(player));
		if (location == null) return;
		location = new Point(location.getX() + config.deathChargeOffsetX(), location.getY() + config.deathChargeOffsetY());

		if (player == client.getLocalPlayer()) localIconBounds = iconBounds(location);

		if (charges <= 0) {
			Composite original = graphics.getComposite();
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			OverlayUtil.renderImageLocation(graphics, location, icon);
			graphics.setComposite(original);
		} else {
			OverlayUtil.renderImageLocation(graphics, location, icon);
			if (charges >= 2 && !config.deathChargeCountSubscript()) {
				Point extra = new Point(location.getX() + EXTRA_CHARGE_OFFSET_X, location.getY() + EXTRA_CHARGE_OFFSET_Y);
				OverlayUtil.renderImageLocation(graphics, extra, icon);
			}
		}
		String subscript = chargeSubscript(charges, uncertain, false);
		if (!subscript.isEmpty()) drawSubscript(graphics, location, subscript, true);
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

	//Stacked tiles can't show the second icon, so the number steps in regardless of the option
	private String chargeSubscript(int charges, boolean uncertain, boolean stacked) {
		String text = (stacked || config.deathChargeCountSubscript()) && charges >= 2 ? Integer.toString(charges) : "";
		return uncertain ? text + "?" : text;
	}

	private void drawSubscript(Graphics2D graphics, Point iconLocation, String text, boolean rightSide) {
		graphics.setFont(runeLiteConfig.infoboxFont().getFont().deriveFont((float) config.deathChargeTextSize()));
		int width = graphics.getFontMetrics().stringWidth(text);
		int x = rightSide
				? iconLocation.getX() + icon.getWidth() - width / 2 + SUBSCRIPT_PUSH_RIGHT
				: iconLocation.getX() - width / 2 - SUBSCRIPT_PUSH_LEFT;
		int y = iconLocation.getY() + icon.getHeight() + SUBSCRIPT_DROP;
		OverlayUtil.renderTextLocation(graphics, new Point(x, y), text,
				rightSide ? config.deathChargeTextColour() : config.deathChargeCountColour());
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
		BufferedImage image = ImageUtil.loadImageResource(getClass(), "/death_charge_player_24.png");
		if (image.getWidth() == size) return image;
		return nearestNeighbour(image, size);
	}

	//Nearest neighbour keeps the pixel art crisp when resized
	private static BufferedImage nearestNeighbour(BufferedImage source, int size) {
		BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(source, 0, 0, size, size, null);
		graphics.dispose();
		return scaled;
	}

	private static class TileGroup {
		private Player anchor;
		private int anchorCharges;
		private int players;
		private boolean certain;
	}
}
