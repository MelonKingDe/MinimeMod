package de.melonking.minimemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.melonking.minimemod.MinimeSlot;
import de.melonking.minimemod.config.MinimeConfig;
import de.melonking.minimemod.skin.SkinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/** Rendert die Minimes am lokalen Spieler. */
public class MinimeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	// pro Slot ein eigenes Modell (klassisch / slim), damit sich Posen nicht ueberschreiben
	private final Map<MinimeSlot, MinimeModel[]> models = new EnumMap<>(MinimeSlot.class);

	public MinimeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		for (MinimeSlot slot : MinimeSlot.values()) {
			models.put(slot, new MinimeModel[] {
					new MinimeModel(context.bakeLayer(ModelLayers.PLAYER), false),
					new MinimeModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true)
			});
		}
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		MinimeConfig cfg = MinimeConfig.get();
		Minecraft mc = Minecraft.getInstance();
		if (!cfg.enabled || mc.player == null || state.id != mc.player.getId()) {
			return;
		}

		float baseTime = state.ageInTicks * cfg.speed;

		for (MinimeSlot slot : MinimeSlot.values()) {
			MinimeConfig.SlotConfig sc = cfg.slot(slot);
			if (!sc.enabled || sc.name == null || sc.name.isBlank()) {
				continue;
			}
			SkinCache.Entry skin = SkinCache.get(sc.name.trim());
			if (skin.status != SkinCache.Status.READY || skin.texture == null) {
				continue;
			}

			// leichter Zeitversatz je Slot, damit nicht alle im Gleichtakt laufen
			float t = baseTime + slot.ordinal() * 7f;
			MinimeModel model = models.get(slot)[skin.slim ? 1 : 0];
			model.pose(slot, t);

			float s = slot.baseScale * cfg.scale;
			pose.pushPose();
			place(slot, pose, s, t);
			collector.submitModel(model, state, pose, model.renderType(skin.texture),
					light, OverlayTexture.NO_OVERLAY, 0, null);
			pose.popPose();
		}
	}

	/**
	 * Setzt den Ursprung (Hals-Drehpunkt) des Minimes an die richtige Stelle und
	 * skaliert. Einheiten: Modell-Pixel (16 Pixel entsprechen einem Block); y zeigt nach unten.
	 */
	private void place(MinimeSlot slot, PoseStack p, float s, float t) {
		PlayerModel pm = getParentModel();
		switch (slot) {
			case HEAD -> {
				pm.head.translateAndRotate(p);
				// Head pivot is at the bottom of the player's head. Move the
				// mini's feet to the top surface (8 px above that pivot).
				p.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.25f) * 0.18f));
				p.translate(0f, -8f - 24f * s, 0f);
			}
			case SHOULDER -> {
				pm.body.translateAndRotate(p);
				// Sit on the right shoulder, with the mini's feet on the arm top.
				p.translate(-5f, 2f - 24f * s, 0f);
				p.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.10f) * 0.06f));
			}
			case HAND -> {
				pm.rightArm.translateAndRotate(p);
				// The mini raises both arms, so its head/body hang below the hand.
				p.translate(0f, 12f, 0f);
				p.mulPose(Axis.XP.rotation(Mth.sin(t * 0.20f) * 0.12f));
			}
			case CHAIN -> {
				pm.body.translateAndRotate(p);
				// Front of the chest; negative Z is the front of the player model.
				p.translate(0f, 2f, -3.5f);
				p.mulPose(Axis.XP.rotation(Mth.sin(t * 0.15f) * 0.18f));
			}
			case ARM -> {
				pm.leftArm.translateAndRotate(p);
				// Sit on top of the left upper arm.
				p.translate(0f, 2f - 24f * s, 0f);
			}
			case SIT -> {
				pm.body.translateAndRotate(p);
				// Sit on the upper back, facing away from the player.
				p.translate(0f, -24f * s, 3.5f);
				p.mulPose(Axis.YP.rotation((float) Math.PI));
			}
		}
		p.scale(s, s, s);
	}
}
