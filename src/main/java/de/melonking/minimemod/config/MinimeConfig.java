package de.melonking.minimemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.melonking.minimemod.MinimeMod;
import de.melonking.minimemod.MinimeSlot;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class MinimeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("minimemod.json");
	private static MinimeConfig instance = new MinimeConfig();

	public boolean enabled = true;
	public float scale = 1.0f;
	public float speed = 1.0f;
	public Map<String, SlotConfig> slots = new LinkedHashMap<>();

	public static class SlotConfig {
		public boolean enabled = false;
		public String name = "";
	}

	public static MinimeConfig get() {
		return instance;
	}

	public SlotConfig slot(MinimeSlot slot) {
		return slots.computeIfAbsent(slot.name(), k -> new SlotConfig());
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				MinimeConfig loaded = GSON.fromJson(Files.readString(FILE), MinimeConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} else {
				// Standardwerte beim ersten Start
				SlotConfig head = instance.slot(MinimeSlot.HEAD);
				head.enabled = true;
				head.name = "Dinnerbone";
			}
		} catch (Exception e) {
			MinimeMod.LOGGER.error("Could not read config, using defaults", e);
		}
		if (instance.slots == null) {
			instance.slots = new LinkedHashMap<>();
		}
		for (MinimeSlot s : MinimeSlot.values()) {
			instance.slot(s);
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException e) {
			MinimeMod.LOGGER.error("Could not save config", e);
		}
	}
}
