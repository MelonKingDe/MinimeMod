package de.melonking.minimemod.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import de.melonking.minimemod.MinimeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** Laedt Skins anhand des Spielernamens (Mojang API) und registriert sie als Textur. */
public final class SkinCache {
	public enum Status { LOADING, READY, FAILED }

	public static final class Entry {
		public volatile Status status = Status.LOADING;
		public volatile Identifier texture;
		public volatile boolean slim;
		volatile long failedAt;
	}

	private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_]{1,16}");
	private static final long RETRY_AFTER_MS = 60_000L;
	private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "minimemod-skin-loader");
		t.setDaemon(true);
		return t;
	});

	private SkinCache() {}

	public static Entry get(String name) {
		String key = name.toLowerCase(Locale.ROOT);
		Entry entry;
		boolean start = false;
		synchronized (ENTRIES) {
			entry = ENTRIES.get(key);
			if (entry == null) {
				entry = new Entry();
				ENTRIES.put(key, entry);
				start = true;
			} else if (entry.status == Status.FAILED && System.currentTimeMillis() - entry.failedAt > RETRY_AFTER_MS) {
				entry.status = Status.LOADING;
				start = true;
			}
		}
		if (start) {
			final Entry e = entry;
			if (!VALID_NAME.matcher(key).matches()) {
				fail(e, key, null);
			} else {
				POOL.execute(() -> load(key, e));
			}
		}
		return entry;
	}

	private static void fail(Entry e, String key, Exception ex) {
		e.failedAt = System.currentTimeMillis();
		e.status = Status.FAILED;
		MinimeMod.LOGGER.warn("Could not load skin for '{}': {}", key, ex == null ? "invalid name" : ex.toString());
	}

	private static void load(String key, Entry e) {
		try {
			JsonObject profile = JsonParser.parseString(
					fetchString("https://api.mojang.com/users/profiles/minecraft/" + key)).getAsJsonObject();
			String uuid = profile.get("id").getAsString();

			JsonObject session = JsonParser.parseString(
					fetchString("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).getAsJsonObject();
			String encoded = null;
			JsonArray props = session.getAsJsonArray("properties");
			for (JsonElement el : props) {
				JsonObject p = el.getAsJsonObject();
				if ("textures".equals(p.get("name").getAsString())) {
					encoded = p.get("value").getAsString();
				}
			}
			if (encoded == null) {
				throw new IllegalStateException("no textures property");
			}
			JsonObject textures = JsonParser.parseString(
					new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8))
					.getAsJsonObject().getAsJsonObject("textures");
			JsonObject skin = textures.getAsJsonObject("SKIN");
			String url = skin.get("url").getAsString();
			boolean slim = skin.has("metadata")
					&& skin.getAsJsonObject("metadata").has("model")
					&& "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());

			HttpResponse<byte[]> png = HTTP.send(
					HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).build(),
					HttpResponse.BodyHandlers.ofByteArray());
			if (png.statusCode() != 200) {
				throw new IllegalStateException("skin HTTP " + png.statusCode());
			}
			NativeImage read = NativeImage.read(new ByteArrayInputStream(png.body()));
			final NativeImage image = convertLegacy(read);

			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> {
				Identifier id = Identifier.fromNamespaceAndPath(MinimeMod.MOD_ID, "skin/" + key);
				mc.getTextureManager().register(id, new DynamicTexture(() -> "minimemod skin " + key, image));
				e.texture = id;
				e.slim = slim;
				e.status = Status.READY;
				MinimeMod.LOGGER.info("Loaded minime skin for {} (slim={})", key, slim);
			});
		} catch (Exception ex) {
			fail(e, key, ex);
		}
	}

	private static String fetchString(String url) throws Exception {
		HttpResponse<String> res = HTTP.send(
				HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build(),
				HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() != 200) {
			throw new IllegalStateException("HTTP " + res.statusCode() + " for " + url);
		}
		return res.body();
	}

	/** Alte 64x32-Skins auf das 64x64-Format bringen (wie Vanilla). */
	private static NativeImage convertLegacy(NativeImage image) {
		if (image.getHeight() != 32) {
			return image;
		}
		NativeImage out = new NativeImage(64, 64, true);
		out.copyFrom(image);
		image.close();
		out.copyRect(4, 16, 16, 32, 4, 4, true, false);
		out.copyRect(8, 16, 16, 32, 4, 4, true, false);
		out.copyRect(0, 20, 24, 32, 4, 12, true, false);
		out.copyRect(4, 20, 16, 32, 4, 12, true, false);
		out.copyRect(8, 20, 8, 32, 4, 12, true, false);
		out.copyRect(12, 20, 16, 32, 4, 12, true, false);
		out.copyRect(44, 16, -8, 32, 4, 4, true, false);
		out.copyRect(48, 16, -8, 32, 4, 4, true, false);
		out.copyRect(40, 20, 0, 32, 4, 12, true, false);
		out.copyRect(44, 20, -8, 32, 4, 12, true, false);
		out.copyRect(48, 20, -16, 32, 4, 12, true, false);
		out.copyRect(52, 20, -8, 32, 4, 12, true, false);
		return out;
	}
}
