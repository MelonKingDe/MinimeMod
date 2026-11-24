# MinimeMod (attachment and skin-layer correction)

Minecraft 26.2 / Fabric client mod.

This revision addresses two concrete model issues:

- Sitting minimes are anchored using the actual lower extent of their bent leg cuboids, rather than the full straight-leg model height.
- The player skin overlay parts (hat, jacket, sleeves, pants) explicitly copy the transforms of their matching base parts in `MinimeModel.setupAnim`.

The back-mounted slot remains removed. The settings screen and the five remaining slots are retained.

Build with Java 25 and Gradle 9.8+ using `gradle build`.

The code has been checked against the Minecraft 26.2 mapped `ModelPart.copyTransform` API. An actual in-game visual test is still required to confirm the final attachment placement.
