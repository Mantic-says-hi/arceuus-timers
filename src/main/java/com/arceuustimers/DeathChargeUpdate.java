package com.arceuustimers;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.client.party.messages.PartyMemberMessage;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DeathChargeUpdate extends PartyMemberMessage {
	private final int charges;

	public DeathChargeUpdate(int charges) {
		this.charges = charges;
	}
}
