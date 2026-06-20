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
			name = "Death Charge",
			description = "Death charge options.",
			position = 1,
			closedByDefault = true
	)
	String SECTION_DEATH_CHARGE = "deathCharge";

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

	String DARK_DEATH_CHARGE_COOLDOWN = "darkDeathChargeCooldown";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = DARK_DEATH_CHARGE_COOLDOWN,
			name = "Dark Cooldown Icon",
			description = "Use a blacked-out Death Charge icon for the cooldown timer, like the standard 'Timers' plugin.",
			position = 30
	)
	default boolean darkDeathChargeCooldown() { return false; }

	String SHOW_DEATH_CHARGE_ON_PLAYER = "showDeathChargeOnPlayer";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = SHOW_DEATH_CHARGE_ON_PLAYER,
			name = "Show On Player",
			description = "Draw the Death Charge icon above your character while Death Charge is active. Shows a second offset icon when you have two charges.",
			position = 31
	)
	default boolean showDeathChargeOnPlayer() { return true; }

	String SHOW_DEATH_CHARGE_PARTY = "showDeathChargeParty";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = SHOW_DEATH_CHARGE_PARTY,
			name = "Show On Party Members",
			description = "Draw Death Charge icons on RuneLite Party members who have Death Charge active. They need this plugin too.",
			position = 32
	)
	default boolean showDeathChargeParty() { return true; }

	String SHOW_DEATH_CHARGE_OTHERS = "showDeathChargeOthers";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = SHOW_DEATH_CHARGE_OTHERS,
			name = "Detect Other Casters",
			description = "Draw a Death Charge icon on any player you see cast it, even if they aren't in your party or don't have the plugin. The count is a guess, shown with a '?', and clears on a nearby kill or when the spell would expire.",
			position = 33
	)
	default boolean showDeathChargeOthers() { return true; }

	String DEATH_CHARGE_ANCHOR = "deathChargeAnchor";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = DEATH_CHARGE_ANCHOR,
			name = "Icon Anchor Point",
			description = "Where on your character the Death Charge icon is anchored.",
			position = 34
	)
	default DeathChargeAnchor deathChargeAnchor() { return DeathChargeAnchor.NECK; }

	String DEATH_CHARGE_SIZE_OFFSET = "deathChargeSizeOffset";
	@Range(min = -16, max = 32)
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = DEATH_CHARGE_SIZE_OFFSET,
			name = "Size Offset",
			description = "Grow or shrink the on-player icon by this many pixels.",
			position = 35
	)
	default int deathChargeSizeOffset() { return 0; }

	String DEATH_CHARGE_REPOSITION = "deathChargeReposition";
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = DEATH_CHARGE_REPOSITION,
			name = "Reposition Mode",
			description = "While on, a preview icon shows on your character and you can drag it with the left mouse button to position it. Right-click the icon to reset the offset to 0,0.",
			position = 38
	)
	default boolean deathChargeReposition() { return false; }

	String DEATH_CHARGE_OFFSET_Y = "deathChargeOffsetY";
	@Range(min = -100, max = 100)
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = DEATH_CHARGE_OFFSET_Y,
			name = "Y-Offset",
			description = "Vertical offset in pixels from the anchor point. Negative moves the icon up, positive down.",
			position = 36
	)
	default int deathChargeOffsetY() { return 0; }

	String DEATH_CHARGE_OFFSET_X = "deathChargeOffsetX";
	@Range(min = -100, max = 100)
	@ConfigItem(
			section = SECTION_DEATH_CHARGE,
			keyName = DEATH_CHARGE_OFFSET_X,
			name = "X-Offset",
			description = "Horizontal offset in pixels from the anchor point. Negative moves the icon left, positive right.",
			position = 37
	)
	default int deathChargeOffsetX() { return -20; }

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

	String SPELLBOOK_SWAP = "spellbookSwap";
	@ConfigItem(
			keyName = SPELLBOOK_SWAP,
			name = "Spellbook Swap Timer",
			description = "Show time left to cast a spell from the Lunar spell, Spellbook Swap.",
			position = 22
	)
	default boolean spellbookSwapToggle() { return true; }

	String SPRITE_SIZED_ICONS = "spriteSizedIcons";
	@ConfigItem(
			keyName = SPRITE_SIZED_ICONS,
			name = "Sprite-sized Icons",
			description = "Scale the icons down to native game sprite size (~25px) with padding, matching other infobox plugins, instead of filling the infobox.",
			position = 32
	)
	default boolean spriteSizedIcons() { return false; }

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