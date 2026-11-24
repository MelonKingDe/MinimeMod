package de.melonking.minimemod.client;

import de.melonking.minimemod.MinimeSlot;
import de.melonking.minimemod.config.MinimeConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Einstellungsmenue: pro Slot an/aus + Spielername, dazu Groesse und Tempo. */
public class MinimeSettingsScreen extends Screen {
	private final Screen parent;

	public MinimeSettingsScreen(Screen parent) {
		super(Component.translatable("minimemod.settings.title"));
		this.parent = parent;
	}

	private static Component onOff(boolean value) {
		return Component.translatable(value ? "minimemod.settings.on" : "minimemod.settings.off");
	}

	@Override
	protected void init() {
		MinimeConfig cfg = MinimeConfig.get();
		int cx = this.width / 2;
		int y = 12;

		addRenderableWidget(new StringWidget(cx - 150, y, 300, 12, this.title, this.font).alignCenter());
		y += 20;

		addRenderableWidget(Button.builder(
				Component.translatable("minimemod.settings.enabled", onOff(cfg.enabled)),
				b -> {
					cfg.enabled = !cfg.enabled;
					b.setMessage(Component.translatable("minimemod.settings.enabled", onOff(cfg.enabled)));
				}).bounds(cx - 150, y, 300, 20).build());
		y += 24;

		addRenderableWidget(new FloatSlider(cx - 150, y, 148, 20, "minimemod.settings.scale",
				0.5f, 2.0f, cfg.scale, v -> cfg.scale = v));
		addRenderableWidget(new FloatSlider(cx + 2, y, 148, 20, "minimemod.settings.speed",
				0.25f, 3.0f, cfg.speed, v -> cfg.speed = v));
		y += 32;

		for (MinimeSlot slot : MinimeSlot.values()) {
			MinimeConfig.SlotConfig sc = cfg.slot(slot);

			addRenderableWidget(Button.builder(slotLabel(slot, sc.enabled), b -> {
				sc.enabled = !sc.enabled;
				b.setMessage(slotLabel(slot, sc.enabled));
			}).bounds(cx - 150, y, 110, 20).build());

			EditBox box = new EditBox(this.font, cx - 36, y, 186, 20, slot.label());
			box.setMaxLength(16);
			box.setFilter(s -> s.matches("[A-Za-z0-9_]*"));
			box.setHint(Component.translatable("minimemod.settings.name_hint"));
			box.setValue(sc.name == null ? "" : sc.name);
			box.setResponder(v -> sc.name = v.trim());
			addRenderableWidget(box);

			y += 24;
		}

		addRenderableWidget(Button.builder(Component.translatable("minimemod.settings.done"),
				b -> onClose()).bounds(cx - 100, y + 8, 200, 20).build());
	}

	private static Component slotLabel(MinimeSlot slot, boolean enabled) {
		return Component.empty().append(slot.label()).append(": ").append(onOff(enabled));
	}

	@Override
	public void onClose() {
		MinimeConfig.save();
		this.minecraft.setScreen(parent);
	}

	private static class FloatSlider extends AbstractSliderButton {
		private final String key;
		private final float min;
		private final float max;
		private final Consumer<Float> onChange;

		FloatSlider(int x, int y, int w, int h, String key, float min, float max, float value, Consumer<Float> onChange) {
			super(x, y, w, h, Component.empty(), (value - min) / (max - min));
			this.key = key;
			this.min = min;
			this.max = max;
			this.onChange = onChange;
			updateMessage();
		}

		private float current() {
			return (float) (min + (max - min) * this.value);
		}

		@Override
		protected void updateMessage() {
			if (key == null) {
				return;
			}
			setMessage(Component.translatable(key, String.format("%.2f", current())));
		}

		@Override
		protected void applyValue() {
			onChange.accept(current());
		}
	}
}
