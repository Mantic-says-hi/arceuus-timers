package com.arceuustimers;

import com.arceuustimers.controllers.CorruptionController;
import com.arceuustimers.controllers.DeathChargeController;
import com.arceuustimers.controllers.MarkController;
import com.arceuustimers.controllers.SpellController;
import com.arceuustimers.controllers.StandardController;
import com.arceuustimers.controllers.ThrallController;
import com.arceuustimers.controllers.VariableTimerController;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public enum ArceuusSpell {
	THRALL("/ghost.png", 61.2, "Active thrall ( Mage )",
			null,
			ArceuusTimersConfig.SHOW_THRALL, ArceuusTimersConfig::showThrall,
			ThrallController::new),
	THRALL_COOLDOWN("/thrall_cooldown.png", 10.8, "Thrall cooldown",
			VarbitID.ARCEUUS_RESURRECTION_COOLDOWN,
			ArceuusTimersConfig.SHOW_THRALL_COOLDOWN, ArceuusTimersConfig::showThrallCooldown,
			StandardController::create),
	CHARGE("/death_charge.png", 61.2, "Death Charge active",
			VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE,
			ArceuusTimersConfig.SHOW_DEATH_CHARGE, ArceuusTimersConfig::showDeathChargeActive,
			DeathChargeController::create),
	CHARGE_COOLDOWN("/death_charge_cooldown.png", 61.2, "Death Charge cooldown",
			VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN,
			ArceuusTimersConfig.SHOW_DEATH_CHARGE_COOLDOWN, ArceuusTimersConfig::showDeathChargeCooldown,
			StandardController::create),
	WARD("/ward.png", 61.2, "Ward of Arceuus active",
			null,
			ArceuusTimersConfig.SHOW_WARD_OF_ARCEUUS, ArceuusTimersConfig::showWardOfArceuus,
			VariableTimerController::new),
	WARD_COOLDOWN("/ward_cooldown.png", 31.2, "Ward of Arceuus cooldown",
			VarbitID.ARCEUUS_WARD_COOLDOWN,
			ArceuusTimersConfig.SHOW_WARD_OF_ARCEUUS_COOLDOWN,
			config -> config.showWardOfArceuusCooldown() || config.showWardOfArceuus(),
			StandardController::create),
	SHADOW("/shadow_veil.png", 61.2, "Shadow Veil active",
			VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE,
			ArceuusTimersConfig.SHOW_SHADOW_VEIL, ArceuusTimersConfig::showShadowVeil,
			VariableTimerController::new),
	SHADOW_COOLDOWN("/shadow_veil_cooldown.png", 31.2, "Shadow Veil cooldown",
			VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN,
			ArceuusTimersConfig.SHOW_SHADOW_VEIL_COOLDOWN,
			config -> config.showShadowVeilCooldown() || config.showShadowVeil(),
			StandardController::create),
	VIGOUR("/vile_vigour.png", 10.8, "Vile Vigour cooldown",
			VarbitID.ARCEUUS_VILE_VIGOUR_COOLDOWN,
			ArceuusTimersConfig.SHOW_VILE_VIGOUR_COOLDOWN, ArceuusTimersConfig::showVileVigourCooldown,
			StandardController::create),
	CORRUPTION("/greater.png", 31.2, "Greater Corruption cooldown",
			VarbitID.ARCEUUS_CORRUPTION_COOLDOWN,
			ArceuusTimersConfig.SHOW_CORRUPTION_COOLDOWN, ArceuusTimersConfig::showCorruptionCooldown,
			CorruptionController::create),
	OFFERING("/sinister_offering.png", 6.0, "Sinister Offering cooldown",
			VarbitID.ARCEUUS_OFFERING_COOLDOWN,
			ArceuusTimersConfig.SHOW_OFFERINGS_COOLDOWN, ArceuusTimersConfig::showOfferingsCooldown,
			StandardController::create),
	LURE("/lure.png", 10.8, "Dark Lure cooldown",
			VarbitID.ARCEUUS_DARK_LURE_COOLDOWN,
			ArceuusTimersConfig.SHOW_DARK_LURE_COOLDOWN, ArceuusTimersConfig::showDarkLureCooldown,
			StandardController::create),
	MARK("/mark.png", -1.0, "Mark of Darkness active",
			null,
			ArceuusTimersConfig.SHOW_MARK_OF_DARKNESS, ArceuusTimersConfig::showMarkTimer,
			MarkController::new),
	MARK_COOLDOWN("/mark.png", 6.6, "Mark of Darkness cooldown",
			null,
			ArceuusTimersConfig.SHOW_MARK_OF_DARKNESS_COOLDOWN, ArceuusTimersConfig::showMarkCooldown,
			StandardController::create),
	SPELLBOOK_SWAP("/sbs.png", 120, "",
			VarbitID.LUNAR_SPELLBOOK_CHANGE,
			ArceuusTimersConfig.SPELLBOOK_SWAP, ArceuusTimersConfig::spellbookSwapToggle,
			StandardController::create);

	@FunctionalInterface
	public interface ControllerFactory {
		SpellController create(String iconFile, double cooldown, String tooltip,
							   InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client);
	}

	private final String iconFile;
	private final double cooldown;
	private final String tooltip;
	private final Integer varbitId;
	private final String configKey;
	private final Predicate<ArceuusTimersConfig> configGate;
	private final ControllerFactory factory;

	private static final Map<Integer, ArceuusSpell> BY_VARBIT = new HashMap<>();
	private static final Map<String, ArceuusSpell> BY_CONFIG_KEY = new HashMap<>();

	static {
		for (ArceuusSpell spell : values()) {
			if (spell.varbitId != null) BY_VARBIT.put(spell.varbitId, spell);
			BY_CONFIG_KEY.put(spell.configKey, spell);
		}
	}

	ArceuusSpell(String iconFile, double cooldown, String tooltip, Integer varbitId,
				 String configKey, Predicate<ArceuusTimersConfig> configGate, ControllerFactory factory) {
		this.iconFile = iconFile;
		this.cooldown = cooldown;
		this.tooltip = tooltip;
		this.varbitId = varbitId;
		this.configKey = configKey;
		this.configGate = configGate;
		this.factory = factory;
	}

	public SpellController createController(InfoBoxManager manager, ArceuusTimersPlugin plugin, Client client) {
		return factory.create(iconFile, cooldown, tooltip, manager, plugin, client);
	}

	public boolean hasVarbit() {
		return varbitId != null;
	}

	public int getVarbitId() {
		return varbitId;
	}

	public boolean isEnabled(ArceuusTimersConfig config) {
		return configGate.test(config);
	}

	public static ArceuusSpell fromVarbit(int varbitId) {
		return BY_VARBIT.get(varbitId);
	}

	public static ArceuusSpell fromConfigKey(String configKey) {
		return BY_CONFIG_KEY.get(configKey);
	}
}
