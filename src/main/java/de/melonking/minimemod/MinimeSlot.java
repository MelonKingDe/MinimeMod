package de.melonking.minimemod;

import net.minecraft.network.chat.Component;

/** Positions am Spieler, an denen ein Minime erscheinen kann. */
public enum MinimeSlot {
	HEAD(0.40f),
	SHOULDER(0.30f),
	HAND(0.25f),
	CHAIN(0.20f),
	ARM(0.28f),
	SIT(0.30f);

	public final float baseScale;

	MinimeSlot(float baseScale) {
		this.baseScale = baseScale;
	}

	public Component label() {
		return Component.translatable("minimemod.slot." + name().toLowerCase());
	}
}
