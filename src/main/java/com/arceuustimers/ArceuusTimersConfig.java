package com.arceuustimers;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import java.awt.*;

@ConfigGroup(ArceuusTimersConfig.GROUP)
public interface ArceuusTimersConfig extends Config
{
	enum TextFormat
	{
		SECONDS,
		MINUTES,
		GAME_TICKS
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

	@ConfigSection(
			name = "Resurrected Thralls",
			description = "Thrall options.",
			position = 4,
			closedByDefault = true
	)
	String SECTION_THRALL = "thrall";

	String SHOW_THRALL = "showThrall";
	@ConfigItem(
			section = SECTION_THRALL,
			keyName = SHOW_THRALL,
			name = "Show Thrall Timer",
			description = "Infobox with timer for Resurrected Thralls.",
			position = 5
	)
	default boolean showThrall() { return true; }

	String SHOW_THRALL_COOLDOWN = "showThrallCooldown";
	@ConfigItem(
			section = SECTION_THRALL,
			keyName = SHOW_THRALL_COOLDOWN,
			name = "Show Cooldown",
			description = "Infobox with timer for Resurrect Thrall cooldown.",
			position = 6
	)
	default boolean showThrallCooldown() { return true; }

	@ConfigSection(
			name = "Other Spells",
			description = "Options for the less popular Arceuus Spells",
			position = 7,
			closedByDefault = true
	)
	String SECTION_OTHER = "other";

	String SHOW_SHADOW_VEIL = "showShadowVeil";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_SHADOW_VEIL,
			name = "Show Shadow Veil Timer",
			description = "Infobox with timer for Shadow Veil spell.",
			position = 8
	)
	default boolean showShadowVeil() { return true; }

	String SHOW_SHADOW_VEIL_COOLDOWN = "showShadowVeilCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_SHADOW_VEIL_COOLDOWN,
			name = "Show Shadow Veil Cooldown",
			description = "Infobox with timer for Shadow Veil spell cooldown.",
			position = 9
	)
	default boolean showShadowVeilCooldown() { return true; }

	String SHOW_WARD_OF_ARCEUUS = "showWardOfArceuus";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_WARD_OF_ARCEUUS,
			name = "Show Ward Of Arceuus Timer",
			description = "Infobox with timer for Ward of Arceuus when active.",
			position = 10
	)
	default boolean showWardOfArceuus() { return true; }

	String SHOW_WARD_OF_ARCEUUS_COOLDOWN = "showWardOfArceuusCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_WARD_OF_ARCEUUS_COOLDOWN,
			name = "Show Ward Of Arceuus Cooldown",
			description = "Infobox with timer for Ward of Arceuus spell cooldown.",
			position = 11
	)
	default boolean showWardOfArceuusCooldown() { return true; }

	String SHOW_CORRUPTION_COOLDOWN = "showCorruptionCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_CORRUPTION_COOLDOWN,
			name = "Show Corruption Cooldown",
			description = "Infobox with timer for Lesser and Greater Corruption spells.",
			position = 12
	)
	default boolean showCorruptionCooldown() { return true; }

	String SHOW_DARK_LURE_COOLDOWN = "showDarkLureCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_DARK_LURE_COOLDOWN,
			name = "Show Dark Lure Cooldown",
			description = "Infobox with timer for Dark Lure spell.",
			position = 13
	)
	default boolean showDarkLureCooldown() { return true; }

	String SHOW_MARK_OF_DARKNESS = "showMarkTimer";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_MARK_OF_DARKNESS,
			name = "Show Mark of Darkness Timer",
			description = "Infobox with timer for Mark of Darkness spell.",
			position = 14
	)
	default boolean showMarkTimer() { return true; }

	String SHOW_OFFERINGS_COOLDOWN = "showOfferingsCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_OFFERINGS_COOLDOWN,
			name = "Show Offering Cooldown",
			description = "Infobox with timer for Demonic and Sinister Offering spells.",
			position = 14
	)
	default boolean showOfferingsCooldown() { return true; }

	String SHOW_VILE_VIGOUR_COOLDOWN = "showVileVigourCooldown";
	@ConfigItem(
			section = SECTION_OTHER,
			keyName = SHOW_VILE_VIGOUR_COOLDOWN,
			name = "Show Vile Vigour Cooldown",
			description = "Infobox with timer for Vile Vigour spell.",
			position = 15
	)
	default boolean showVileVigourCooldown() { return true; }

	String MAIN_TEXT_COLOUR = "mainText";
	@ConfigItem(
			keyName = MAIN_TEXT_COLOUR,
			name = "Text Colour",
			description = "Change the text colour of the time remaining.",
			position = 16
	)
	default Color textColour() { return Color.PINK.brighter(); }

	String LOW_TEXT_COLOUR = "lowTimeText";
	@ConfigItem(
			keyName = LOW_TEXT_COLOUR,
			name = "Low Time Colour",
			description = "Change text colour when the time remaining is low",
			position = 17
	)
	default Color lowTimeTextColour() { return Color.ORANGE; }

	String ARCEUUS_BOX_PRIORITY = "arceuusBoxPriority";
	@ConfigItem(
			keyName = ARCEUUS_BOX_PRIORITY,
			name = "Infobox Priority",
			description = "Change the priority of the Infoboxes created by this plugin.",
			position = 18
	)
	default InfoBoxPriority arceuusBoxPriority() { return InfoBoxPriority.NONE; }

	String TEXT_FORMAT = "textFormat";
	@ConfigItem(
			keyName = TEXT_FORMAT,
			name = "Text Format",
			description = "Choose the format for displaying time: seconds, minutes, or game ticks",
			position = 19
	)
	default TextFormat textFormat() { return TextFormat.SECONDS; }
}