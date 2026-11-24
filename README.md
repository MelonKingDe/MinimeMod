# Minime Mod

Fabric mod for **Minecraft 26.2** – small animated mini versions of any player skin that follow you around (like the Minimes in NoRiskClient / LabyMod).

## Features
- 6 slots: **Head, Shoulder, Hand, Chain, Arm, Sit**
- Every slot has its own animation (e.g. the head Minime waves and wobbles left/right, the chain Minime swings like a pendulum)
- Settings menu (default key: `K`): enable/disable each slot, enter the **player name** whose skin is loaded, size and animation speed
- Skins are loaded by name via the Mojang API (classic + slim, legacy 64x32 skins too)
- Config: `config/minimemod.json`

## Build
Requires **Java 25**.

```
gradle wrapper      # once, creates gradlew
./gradlew build     # jar ends up in build/libs/
```

Every push also builds on GitHub Actions (`.github/workflows/build.yml`); the jar is uploaded as an artifact.

## Versions
```
minecraft_version=26.2
loader_version=0.20.1
loom_version=1.19-SNAPSHOT
fabric_api_version=0.161.0+26.2
```

## License
MIT
