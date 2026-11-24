package de.melonking.minimemod.client;

import de.melonking.minimemod.MinimeSlot;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * Ein Mini-Spielermodell. Die Pose wird pro Slot und Zeit selbst berechnet
 * (Vanilla-setupAnim wird bewusst nicht aufgerufen).
 */
public class MinimeModel extends PlayerModel {
	private float headX, headY, headZ;
	private float rArmX, rArmZ, lArmX, lArmZ;
	private float rLegX, lLegX;

	public MinimeModel(ModelPart root, boolean slim) {
		super(root, slim);
	}

	/** Animation fuer den jeweiligen Slot berechnen. t = Zeit in Ticks (mit Tempo-Faktor). */
	public void pose(MinimeSlot slot, float t) {
		headX = headY = headZ = 0f;
		rArmX = rArmZ = lArmX = lArmZ = 0f;
		rLegX = lLegX = 0f;
		final float sit = -1.45f;

		switch (slot) {
			case HEAD -> {
				// Sitzt auf dem Kopf: Beine nach vorne angewinkelt und locker baumelnd.
				rLegX = sit + Mth.sin(t * 0.30f) * 0.12f;
				lLegX = sit - Mth.sin(t * 0.30f) * 0.12f;
				rArmX = -0.35f + Mth.sin(t * 0.45f) * 0.18f;
				lArmX = -0.35f - Mth.sin(t * 0.45f) * 0.18f;
				headZ = Mth.sin(t * 0.25f) * 0.08f;
			}
			case SHOULDER -> {
				// Sitzt auf der Schulter, baumelt mit den Beinen, schaut sich um
				rLegX = sit + Mth.sin(t * 0.30f) * 0.22f;
				lLegX = sit - Mth.sin(t * 0.30f) * 0.22f;
				rArmX = -0.3f;
				lArmX = -0.3f;
				headY = Mth.sin(t * 0.10f) * 0.45f;
			}
			case HAND -> {
				// Haengt mit hochgestreckten Armen an der Hand und strampelt
				rArmX = -3.0f;
				lArmX = -3.0f;
				rArmZ = 0.15f;
				lArmZ = -0.15f;
				rLegX = Mth.sin(t * 0.35f) * 0.45f;
				lLegX = -Mth.sin(t * 0.35f) * 0.45f;
			}
			case CHAIN -> {
				// Haengt als Anhaenger an der Kette, Arme und Beine schwingen locker
				rArmX = Mth.sin(t * 0.15f) * 0.3f;
				lArmX = -Mth.sin(t * 0.15f) * 0.3f;
				rArmZ = 0.25f;
				lArmZ = -0.25f;
				rLegX = Mth.sin(t * 0.15f + 1f) * 0.2f;
				lLegX = -Mth.sin(t * 0.15f + 1f) * 0.2f;
			}
			case ARM -> {
				// Sitzt auf dem Arm und winkt
				rLegX = sit;
				lLegX = sit;
				lArmX = -2.6f + Mth.sin(t * 0.6f) * 0.5f;
				lArmZ = -0.25f;
				rArmX = -0.4f;
				headZ = Mth.sin(t * 0.3f) * 0.1f;
			}
		}
	}

	/**
	 * Hoechster Y-Wert (nach unten positiv) der sitzenden Fuesse relativ zum
	 * Modellursprung. Die Bein-Cuboids reichen von y=0 bis y=12 und von z=-2
	 * bis z=2. Die Rotation wird mitgerechnet, damit der Sitz-Anchor nicht
	 * faelschlich die volle, senkrechte Beinlaenge verwendet.
	 */
	public float sittingBottomY() {
		return Math.max(legBottomY(rLegX), legBottomY(lLegX));
	}

	private static float legBottomY(float xRot) {
		return 12.0f + 12.0f * Mth.cos(xRot) + 2.0f * Math.abs(Mth.sin(xRot));
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		head.xRot = headX;
		head.yRot = headY;
		head.zRot = headZ;
		rightArm.xRot = rArmX;
		rightArm.zRot = rArmZ;
		leftArm.xRot = lArmX;
		leftArm.zRot = lArmZ;
		rightLeg.xRot = rLegX;
		leftLeg.xRot = lLegX;

		// Die Skin-Outer-Layer sind separate ModelParts (keine Kinder der
		// Basisteile). Deshalb muessen sie nach dem Setzen der Pose explizit
		// dieselben Transformationen erhalten.
		hat.copyTransform(head);
		jacket.copyTransform(body);
		rightSleeve.copyTransform(rightArm);
		leftSleeve.copyTransform(leftArm);
		rightPants.copyTransform(rightLeg);
		leftPants.copyTransform(leftLeg);
	}
}
