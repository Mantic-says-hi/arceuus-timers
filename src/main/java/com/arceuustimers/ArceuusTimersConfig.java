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
			name = "Show Thrall",
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
			name = "Corruption",
			description = "Corruption spell option.",
			position = 7,
			closedByDefault = true
	)
	String SECTION_CORRUPTION = "corruption";

	String SHOW_CORRUPTION_COOLDOWN = "showCorruptionCooldown";
	@ConfigItem(
			section = SECTION_CORRUPTION,
			keyName = SHOW_CORRUPTION_COOLDOWN,
			name = "Show Cooldown",
			description = "Infobox with timer for Corruption spells.",
			position = 8
	)
	default boolean showCorruptionCooldown() { return true; }

	@ConfigSection(
			name = "Vile Vigour",
			description = "Vile Vigour spell option.",
			position = 9,
			closedByDefault = true
	)
	String SECTION_VILE_VIGOUR = "vileVigour";

	String SHOW_VILE_VIGOUR_COOLDOWN = "showVileVigourCooldown";
	@ConfigItem(
			section = SECTION_VILE_VIGOUR,
			keyName = SHOW_VILE_VIGOUR_COOLDOWN,
			name = "Show Cooldown",
			description = "Infobox with timer for Vile Vigour spell.",
			position = 10
	)
	default boolean showVileVigourCooldown() { return true; }

	@ConfigSection(
			name = "Shadow Veil",
			description = "Shadow Veil spell option.",
			position = 11,
			closedByDefault = true
	)
	String SECTION_SHADOW_VEIL = "shadowVeil";

	String SHOW_SHADOW_VEIL_COOLDOWN = "showShadowVeilCooldown";
	@ConfigItem(
			section = SECTION_SHADOW_VEIL,
			keyName = SHOW_SHADOW_VEIL_COOLDOWN,
			name = "Show Cooldown",
			description = "Infobox with timer for Shadow Veil spell.",
			position = 12
	)
	default boolean showShadowVeilCooldown() { return true; }

	String MAIN_TEXT_COLOUR = "mainText";
	@ConfigItem(
			keyName = MAIN_TEXT_COLOUR,
			name = "Text Colour",
			description = "Change the text colour of the time remaining.",
			position = 13
	)
	default Color textColour() { return Color.PINK.brighter(); }

	String LOW_TEXT_COLOUR = "lowTimeText";
	@ConfigItem(
			keyName = LOW_TEXT_COLOUR,
			name = "Low Time Colour",
			description = "Change text colour when the time remaining is low",
			position = 14
	)
	default Color lowTimeTextColour() { return Color.ORANGE; }

	String ARCEUUS_BOX_PRIORITY = "arceuusBoxPriority";
	@ConfigItem(
			keyName = ARCEUUS_BOX_PRIORITY,
			name = "Infobox Priority",
			description = "Change the priority of the infoboxes created by this plugin.",
			position = 15
	)
	default InfoBoxPriority arceuusBoxPriority() { return InfoBoxPriority.NONE; }

	String FORMAT_OPTION = "formatOption";
	@ConfigItem(
			keyName = FORMAT_OPTION,
			name = "RuneLite Text Format",
			description = "Off for whole numbers, on for '0:00' format",
			position = 16
	)
	default boolean formatOption() { return false; }
}
