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
	 * skaliert. Einheiten: Modell-Pixel. PoseStack.translate erwartet Block-Einheiten, daher werden Pixel-Offsets durch 16 geteilt; y zeigt nach unten.
	 */
	private void place(MinimeSlot slot, PoseStack p, float s, float t) {
		PlayerModel pm = getParentModel();

		switch (slot) {
			case HEAD -> {
				// Player head top is y=-8 from the head pivot. The mini's feet
				// are y=+24 from its own head pivot, so move the mini pivot up
				// by the head-top offset PLUS its scaled 24px body height.
				pm.head.translateAndRotate(p);
				p.translate(0f, (-8f - 24f * s) / 16f, 0f);
				p.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.25f) * 0.06f));
			}
			case SHOULDER -> {
				// The arm pivot is at y=2 and the arm's upper face is y=-2.
				// Put the mini's feet exactly on that upper face.
				pm.rightArm.translateAndRotate(p);
				p.translate(0f, (-2f - 24f * s) / 16f, 0f);
				p.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.10f) * 0.04f));
			}
			case HAND -> {
				// Anchor to the end of the player's right arm (y=10 from its
				// pivot). The mini hangs by raised hands: with its arm angle
				// near -PI, the mini's hand tips are about 8px above its head
				// pivot. Compensate for that scaled distance.
				pm.rightArm.translateAndRotate(p);
				p.translate(0f, (10f + 8f * s) / 16f, 0f);
				p.mulPose(Axis.XP.rotation(Mth.sin(t * 0.20f) * 0.04f));
			}
			case CHAIN -> {
				// Pendant hangs from the upper chest, in front of the torso.
				pm.body.translateAndRotate(p);
				p.translate(0f, 2f / 16f, -3.5f / 16f);
				p.mulPose(Axis.XP.rotation(Mth.sin(t * 0.15f) * 0.08f));
			}
			case ARM -> {
				// Sit directly on top of the left arm, following its animated
				// pivot and rotation.
				pm.leftArm.translateAndRotate(p);
				p.translate(0f, (-2f - 24f * s) / 16f, 0f);
				p.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.30f) * 0.04f));
			}
		}

		// Scale only the mini model. Anchor translations above remain in the
		// player's model coordinate system (1 model pixel = 1/16 block).
		p.scale(s, s, s);
	}
}
