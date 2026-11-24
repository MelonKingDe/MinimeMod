# MinimeMod (position adjustment v8)

This version adjusts the attachment transforms for the head, shoulder, hand, chain, and arm slots.

- The head minime is moved down to sit close to the player's head surface.
- Shoulder and arm minimes are anchored to the corresponding arm model pivots instead of the body origin.
- The hand minime is positioned relative to the right hand point.
- The retired back slot remains removed.

Build with Java 25 and Gradle 9.8+ using `gradle build`.

These placement changes still require an in-game visual check because the exact result depends on the Minecraft renderer's model pose and the selected global/slot scale.
