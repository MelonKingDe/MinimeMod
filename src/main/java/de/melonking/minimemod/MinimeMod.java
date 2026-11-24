package de.melonking.minimemod;

import com.mojang.blaze3d.platform.InputConstants;
import de.melonking.minimemod.client.MinimeLayer;
import de.melonking.minimemod.client.MinimeSettingsScreen;
import de.melonking.minimemod.config.MinimeConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.AvatarRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinimeMod implements ClientModInitializer {
	public static final String MOD_ID = "minimemod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping openSettings;

	@Override
	@SuppressWarnings("unchecked")
	public void onInitializeClient() {
		MinimeConfig.load();

		openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.minimemod.open_settings",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_K,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openSettings.consumeClick()) {
				client.setScreen(new MinimeSettingsScreen(client.screen));
			}
		});

		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
				helper.register(new MinimeLayer(
						(RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) avatarRenderer, context));
			}
		});

		LOGGER.info("Minime Mod loaded");
	}
}
