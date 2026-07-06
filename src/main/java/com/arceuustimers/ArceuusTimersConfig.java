package com.arceuustimers;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import java.awt.*;

@ConfigGroup(ArceuusTimersConfig.GROUP)
public interface ArceuusTimersConfig extends Config {
	enum TextFormat {
		SECONDS,
		MINUTES,
		GAME_TICKS
	}

	enum DeathChargeAnchor {
		HEAD("Head"),
		NECK("Neck"),
		MIDDLE("Middle"),
		FEET("Feet");

		private final String name;

		DeathChargeAnchor(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	String GROUP = "arceuustimers";

	@ConfigSection(
			name = "Death Charge Infobox",
			description = "Death Charge infobox timers.",
			position = 1,
			closedByDefault = true
	)
	String SECTION_DEATH_CHARGE = "deathCharge";

	@ConfigSection(
			name = "Death Charge On Player",
			description = "Death Charge icons drawn on players.",
			position = 2,
			closedByDefault = true
	)
	String SECTION_DEATH_CHARGE_PLAYER = "deathChargePlayer";

	String SHOW_DEATH_CHARGE = "showDeathChargeActive";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = SHOW_DEATH_CHARGE,
			name = "Show Active Charge",
			description = "Infobox for when Death Charge is active.",
			position = 2
	)
	default boolean showDeathChargeActive() { return true; }

	String SHOW_DEATH_CHARGE_COOLDOWN = "showDeathChargeCooldown";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = SHOW_DEATH_CHARGE_COOLDOWN,
			name = "Show Cooldown",
			description = "Infobox with timer for Death Charge's cooldown.",
			position = 3
	)
	default boolean showDeathChargeCooldown() { return true; }

    String STACK_DEATH_CHARGE = "stackDeathCharge";

    @ConfigItem(
            section = SECTION_DEATH_CHARGE,
            keyName = STACK_DEATH_CHARGE,
            name = "Stack Death Charge Boxes",
            description = "Merges multiple Death Charge infoboxes into one when active.",
            position = 4
    )
    default boolean stackDeathCharge() { return false; }

	String DEATH_CHARGE_SMALL_ICON = "deathChargeSmallIcon";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = DEATH_CHARGE_SMALL_ICON,
			name = "Small Icon",
			description = "Use the small 24px sprite centred in the active Death Charge infobox instead of filling the box.",
			position = 5
	)
	default boolean deathChargeSmallIcon() { return false; }

	String SHOW_DEATH_CHARGE_ON_PLAYER = "showDeathChargeOnPlayer";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = SHOW_DEATH_CHARGE_ON_PLAYER,
			name = "Show On Player",
			description = "Draw the Death Charge icon above your character while Death Charge is active.",
			position = 1
	)
	default boolean showDeathChargeOnPlayer() { return true; }

	String DEATH_CHARGE_COUNT_SUBSCRIPT = "deathChargeCountSubscript";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_COUNT_SUBSCRIPT,
			name = "One Charge Icon",
			description = "Only ever draw one icon on your character: a small '2' shows under its right side while you hold two charges and goes away when a charge is used. Turn off to stack a second offset icon instead.",
			position = 4
	)
	default boolean deathChargeCountSubscript() { return true; }

	String DEATH_CHARGE_TEXT_SIZE = "deathChargeTextSize";
	@Range(min = 8, max = 32)
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_TEXT_SIZE,
			name = "Text Size",
			description = "Size of the charge number, stacked player count and '?' drawn beside the on-player icon. The font follows the RuneLite infobox font setting.",
			position = 5
	)
	default int deathChargeTextSize() { return 16; }

	String DEATH_CHARGE_TEXT_COLOUR = "deathChargeTextColour";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_TEXT_COLOUR,
			name = "Charge Text Colour",
			description = "Colour of the charge number and '?' under the icon's right side.",
			position = 6
	)
	default Color deathChargeTextColour() { return new Color(0xE4E4E4); }

	String DEATH_CHARGE_COUNT_COLOUR = "deathChargeCountColour";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_COUNT_COLOUR,
			name = "Player Count Colour",
			description = "Colour of the stacked player count under the icon's left side.",
			position = 7
	)
	default Color deathChargeCountColour() { return new Color(0x9CFFDB); }

	String SHOW_DEATH_CHARGE_PARTY = "showDeathChargeParty";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = SHOW_DEATH_CHARGE_PARTY,
			name = "Show On Party Members",
			description = "Draw Death Charge icons on RuneLite Party members who have Death Charge active. They need this plugin too.",
			position = 2
	)
	default boolean showDeathChargeParty() { return true; }

	String SHOW_DEATH_CHARGE_OTHERS = "showDeathChargeOthers";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = SHOW_DEATH_CHARGE_OTHERS,
			name = "Detect Other Casters",
			description = "Draw a Death Charge icon on any player you see cast it, even if they aren't in your party or don't have the plugin. The count is a guess, shown with a '?', and clears on a nearby kill or when the spell would expire.",
			position = 3
	)
	default boolean showDeathChargeOthers() { return true; }

	String DEATH_CHARGE_ANCHOR = "deathChargeAnchor";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_ANCHOR,
			name = "Icon Anchor Point",
			description = "Where on your character the Death Charge icon is anchored.",
			position = 8
	)
	default DeathChargeAnchor deathChargeAnchor() { return DeathChargeAnchor.NECK; }

	String DEATH_CHARGE_SIZE_OFFSET = "deathChargeSizeOffset";
	@Range(min = -16, max = 32)
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_SIZE_OFFSET,
			name = "Size Offset",
			description = "Grow or shrink the on-player icon by this many pixels.",
			position = 9
	)
	default int deathChargeSizeOffset() { return 0; }

	String DEATH_CHARGE_OFFSET_Y = "deathChargeOffsetY";
	@Range(min = -100, max = 100)
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_OFFSET_Y,
			name = "Y-Offset",
			description = "Vertical offset in pixels from the anchor point. Negative moves the icon up, positive down.",
			position = 10
	)
	default int deathChargeOffsetY() { return 0; }

	String DEATH_CHARGE_OFFSET_X = "deathChargeOffsetX";
	@Range(min = -100, max = 100)
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_OFFSET_X,
			name = "X-Offset",
			description = "Horizontal offset in pixels from the anchor point. Negative moves the icon left, positive right.",
			position = 11
	)
	default int deathChargeOffsetX() { return 0; }

	String DEATH_CHARGE_REPOSITION = "deathChargeReposition";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE_PLAYER,
			keyName = DEATH_CHARGE_REPOSITION,
			name = "Reposition Mode",
			description = "While on, a preview icon shows on your character and you can drag it with the left mouse button to position it. Shift-right-click the icon for reposition, reset and hide options.",
			position = 12
	)
	default boolean deathChargeReposition() { return false; }

	@ConfigSection(
			name = "Resurrected Thralls",
			description = "Thrall options.",
			position = 5,
			closedByDefault = true
	)
	String SECTION_THRALL = "thrall";

	String SHOW_THRALL = "showThrall";
	@ConfigItem(
			section = SECTION_THRALL,
			keyName = SHOW_THRALL,
			name = "Show Thrall Timer",
			description = "Infobox with timer for Resurrected Thralls.",
			position = 6
	)
	default boolean showThrall() { return true; }

	String SHOW_THRALL_COOLDOWN = "showThrallCooldown";
	@ConfigItem(
			section = SECTION_THRALL,
			keyName = SHOW_THRALL_COOLDOWN,
			name = "Show Cooldown",
			description = "Infobox with timer for Resurrect Thrall cooldown.",
			position = 7
	)
	default boolean showThrallCooldown() { return true; }

	String IMPISH_THRALL_ICONS = "impishThrallIcons";
	@ConfigItem(
			section = SECTION_THRALL,
			keyName = IMPISH_THRALL_ICONS,
			name = "Impish Thrall Icons",
			description = "Use the impish icons when an impish thrall is resurrected. Turn off to keep the classic thrall icons.",
			position = 8
	)
	default boolean impishThrallIcons() { return true; }

	@ConfigSection(
			name = "Other Spells",
			description = "Options for the less popular Arceuus Spells",
			position = 8,
			closedByDefault = true
	)
	String SECTION_OTHER = "other";

	String SHOW_SHADOW_VEIL = "showShadowVeil";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_SHADOW_VEIL,
			name = "Show Shadow Veil Timer",
			description = "Infobox with timer for Shadow Veil spell.",
			position = 9
	)
	default boolean showShadowVeil() { return true; }

	String SHOW_SHADOW_VEIL_COOLDOWN = "showShadowVeilCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_SHADOW_VEIL_COOLDOWN,
			name = "Show Shadow Veil Cooldown",
			description = "Infobox with timer for Shadow Veil spell cooldown.",
			position = 10
	)
	default boolean showShadowVeilCooldown() { return true; }

	String SHOW_WARD_OF_ARCEUUS = "showWardOfArceuus";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_WARD_OF_ARCEUUS,
			name = "Show Ward Of Arceuus Timer",
			description = "Infobox with timer for Ward of Arceuus when active.",
			position = 11
	)
	default boolean showWardOfArceuus() { return true; }

	String SHOW_WARD_OF_ARCEUUS_COOLDOWN = "showWardOfArceuusCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_WARD_OF_ARCEUUS_COOLDOWN,
			name = "Show Ward Of Arceuus Cooldown",
			description = "Infobox with timer for Ward of Arceuus spell cooldown.",
			position = 12
	)
	default boolean showWardOfArceuusCooldown() { return true; }

	String SHOW_CORRUPTION_COOLDOWN = "showCorruptionCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_CORRUPTION_COOLDOWN,
			name = "Show Corruption Cooldown",
			description = "Infobox with timer for Lesser and Greater Corruption spells.",
			position = 13
	)
	default boolean showCorruptionCooldown() { return true; }

	String SHOW_DARK_LURE_COOLDOWN = "showDarkLureCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_DARK_LURE_COOLDOWN,
			name = "Show Dark Lure Cooldown",
			description = "Infobox with timer for Dark Lure spell.",
			position = 14
	)
	default boolean showDarkLureCooldown() { return true; }

	String SHOW_MARK_OF_DARKNESS = "showMarkTimer";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_MARK_OF_DARKNESS,
			name = "Show Mark of Darkness Timer",
			description = "Infobox with timer for Mark of Darkness spell.",
			position = 15
	)
	default boolean showMarkTimer() { return true; }

	String SHOW_MARK_OF_DARKNESS_COOLDOWN = "showMarkCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_MARK_OF_DARKNESS_COOLDOWN,
			name = "Show Mark of Darkness Cooldown",
			description = "Infobox with timer for the cooldown after casting Mark of Darkness.",
			position = 16
	)
	default boolean showMarkCooldown() { return true; }

	String SHOW_OFFERINGS_COOLDOWN = "showOfferingsCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_OFFERINGS_COOLDOWN,
			name = "Show Offering Cooldown",
			description = "Infobox with timer for Demonic and Sinister Offering spells.",
			position = 17
	)
	default boolean showOfferingsCooldown() { return true; }

	String SHOW_VILE_VIGOUR_COOLDOWN = "showVileVigourCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_VILE_VIGOUR_COOLDOWN,
			name = "Show Vile Vigour Cooldown",
			description = "Infobox with timer for Vile Vigour spell.",
			position = 18
	)
	default boolean showVileVigourCooldown() { return true; }

	String SPELLBOOK_SWAP = "spellbookSwap";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SPELLBOOK_SWAP,
			name = "Spellbook Swap Timer",
			description = "Show time left to cast a spell from the Lunar spell, Spellbook Swap.",
			position = 19
	)
	default boolean spellbookSwapToggle() { return true; }

	String MAIN_TEXT_COLOUR = "mainText";
	@ConfigItem(
			keyName = MAIN_TEXT_COLOUR,
			name = "Text Colour",
			description = "Change the text colour of the time remaining.",
			position = 18
	)
	default Color textColour() { return Color.PINK.brighter(); }

	String LOW_TEXT_COLOUR = "lowTimeText";
	@ConfigItem(
			keyName = LOW_TEXT_COLOUR,
			name = "Low Time Colour",
			description = "Change text colour when the time remaining is low",
			position = 19
	)
	default Color lowTimeTextColour() { return Color.ORANGE; }

	String ARCEUUS_BOX_PRIORITY = "arceuusBoxPriority";
	@ConfigItem(
			keyName = ARCEUUS_BOX_PRIORITY,
			name = "Infobox Priority",
			description = "Change the priority of the Infoboxes created by this plugin.",
			position = 20
	)
	default InfoBoxPriority arceuusBoxPriority() { return InfoBoxPriority.NONE; }

	String TEXT_FORMAT = "textFormat";
	@ConfigItem(
			keyName = TEXT_FORMAT,
			name = "Text Format",
			description = "Choose the format for displaying time: seconds, minutes, or game ticks",
			position = 21
	)
	default TextFormat textFormat() { return TextFormat.SECONDS; }

	String DARK_DEATH_CHARGE_COOLDOWN = "darkDeathChargeCooldown";
	@ConfigItem(
			keyName = DARK_DEATH_CHARGE_COOLDOWN,
			name = "Dark Cooldown Icons",
			description = "Use blacked-out icons for the Death Charge, Shadow Veil and Ward of Arceuus cooldown timers, like the standard 'Timers' plugin.",
			position = 32
	)
	default boolean darkDeathChargeCooldown() { return false; }

	String SEPARATE_INFOBOXES = "separateInfoboxes";
	@ConfigItem(
			keyName = SEPARATE_INFOBOXES,
			name = "Separate Infoboxes",
			description = "Give each timer its own infobox group so they can be detached and positioned individually.<br>"
					+ "Off (default) keeps every timer in one group that detaches and moves together.<br>"
					+ "Changing this applies to timers created afterwards, not ones already on screen.",
			position = 33
	)
	default boolean separateInfoboxes() { return false; }

}