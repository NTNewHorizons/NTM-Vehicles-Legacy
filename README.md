# NTM: Vehicles — Minecraft 1.7.10

Backport of [TechTastic's NTM: Vehicles](https://github.com/TechTastic/NTM-Vehicles),
from the [NTNewHorizons fork](https://github.com/NTNewHorizons/NTM-Vehicles).
Requires **Immersive Vehicles: Legacy** (`immersivevehicleslegacy`) and **Hbm's
Nuclear Tech Mod** (`hbm`). This repository requires HBM 1.0.27_X5808,
not the 1.12.2 NTM fork. No mixins are required. Use the patched IVL build described below.

## Build

The build uses the supplied GTNH starter template: its convention plugin, Gradle
wrapper, Java 25 daemon, formatting rules, and shared GitHub workflows. Compilation
and Minecraft use an automatically provisioned Java 8 toolchain.

Build from a clean checkout (requires Git, Bash, and network access):

```sh
./gradlew build
```

Dependencies are pinned; no sibling checkout or prebuilt JAR is needed:

- HBM: `com.hbm:HBM-NTM:1.0.27_X5808:dev` from `https://maven.ntmr.dev/releases/`.
- IVL: `THOMASS47/IVL` commit `d32bf237d2eb741ce11f86285a7bbe6157722be1`.
- IV core: `DonBruce64/MinecraftTransportSimulator` commit `cd9cfb8fe74dbcc426eb830f14822adf7402261f`.

`dependencies.gradle` declares the development dependencies; `repositories.gradle`
adds NTM's Maven repository. `addon.gradle` prepares patched IVL and supplies the
local default version from `modVersion` in `gradle.properties`. Release builds use
the shared workflow's `VERSION` environment variable instead.

`scripts/prepare-dependencies.sh` builds IVL under `.dependencies/IVL` and applies
`patches/ivl-custom-hit-once.patch`. The patch removes the second CUSTOM dispatch
from delayed block-state processing and keeps server-side CUSTOM callbacks independent
of `bulletExplosions`. Effects run once at the precise impact position.
Install the resulting **`immersivevehicleslegacy-0.1.0-ntmv2.jar`** instead of an
unpatched IVL JAR. IVL is built locally, not redistributed in bridge releases.
Its license restricts public redistribution of derivatives. The bridge
cannot deduplicate safely by itself: IVL's callback API exposes no projectile ID.
Forge requires IVL version `0.1.0-ntmv2`, rejecting older builds at startup.
Use a fresh checkout when upgrading a previously patched dependency build.

HBM compatibility is limited to the tested `1.0.27_X5808` artifact, whose Forge
version is `1.0.27 BETA (5808)`. The dependency declaration requires HBM 1.0.27 or
later; an initialization check requires that exact runtime version because Forge
cannot parse its closing parenthesis in a version range. Older and newer builds
remain unsupported until tested; this is not a claim that X5808
is the oldest compatible HBM build. Development JAR overrides do not relax this requirement.

For explicitly supplied development artifacts (IVL must include the dispatcher fix):

```sh
./gradlew build -PivlJar=/path/to/ivl-dev.jar -PntmJar=/path/to/ntm-dev.jar
```

Install **`build/libs/ntm_vehicles-1.0.0-1.7.10.jar`** alongside normal
(non-development) IVL and NTM jars in a Forge 10.13.4.1614 instance. Do not install
this alongside the original 1.12.2 bridge; both use the same mod ID.
The bridge jar bundles neither dependency.

`./gradlew runClient` loads the two development jars, development NEI and
CodeChickenCore, and IVL's audio libraries. Do not duplicate those jars in `run/client/mods`.
The upstream `libs/Immersive Vehicles-1.12.2-22.18.0.jar` is retained but unused.

## Unchanged pack-creator API

Existing bullet JSON uses the same `CUSTOM` type and `customHitFunctions` IDs:

| Function | Behavior |
| --- | --- |
| `ntm_vehicles:nuke` | NTM MK5 nuclear explosion and standard Torex mushroom cloud |
| `ntm_vehicles:gas` | NTM gas entities with the original numeric type selection |
| `ntm_vehicles:napalm` | 2.5-strength explosion, ignition bounds 9 and 14, five flame bursts |

For nuke and gas, `bullet.blastStrength` is used unless zero, when
`bullet.diameter / 10` is used. The result is truncated to an integer, just as
upstream. For gas this is the **entity count**, not a radius.

Gas constants remain in the top-level `constantValues` object:

- `gasSpreadSpeed`: Gaussian velocity multiplier; default **1.25**.
- `gasType`: **0** chlorine (default), **1** cloud, **2** pink cloud,
  all other values orange cloud. Values are truncated to integers.

```json
{
  "bullet": {
    "diameter": 120,
    "blastStrength": 8,
    "types": ["CUSTOM"],
    "customHitFunctions": ["ntm_vehicles:gas"]
  },
  "constantValues": {
    "gasType": 0,
    "gasSpreadSpeed": 1.25
  }
}
```

This is a fragment of a normal IV bullet definition, not a complete pack item.
Use `ntm_vehicles:nuke` or `ntm_vehicles:napalm` in that list for those effects.
No pack JSON changes are required by the bridge port. This does not translate
unrelated 1.12.2 NTM item IDs or add cross-mod fluid/energy integrations that the
upstream bridge never provided.

Effects execute only on the server, even when IVL's `bulletExplosions` setting is
disabled. That setting still controls IVL's EXPLOSIVE bullets. Effect implementation,
gas damage, radiation, tracking, and visuals follow the installed **1.7.10 NTM**; they are
not a backport of the entire 1.12.2 NTM engine or its dimension restrictions.

## Verification and rollback

`./gradlew cleanTest build` checks unchanged JSON/constants, effect IDs, Forge
dependencies, IVL world unwrapping, all gas types, count, position, velocity,
nuclear factory arguments, napalm ignition/bursts, registered server callback,
and client-side suppression against the real dependency classes.

Six lifecycle tests run actual IVL collision detection and next-tick block processing
for nuclear, gas, and napalm callbacks with `bulletExplosions` enabled and disabled.
They also check that two bullets at the same location both fire, without suppressing
the second impact.
Gameplay verification remains separate from automated tests.

For gameplay verification use a disposable world: fire each custom bullet at
blocks and entities, check nuclear damage/cloud/radiation, gas types/protection,
and napalm ignition/bursts. Repeat on a dedicated server with `bulletExplosions=true`
and `bulletExplosions=false`. Startup and unit tests alone do not establish gameplay parity.

The bridge adds no blocks, items, saved data, or migration writes. Roll back by
removing its jar; packs may stay installed, but those custom effects will no
longer run. The 1.7.10 backport belongs in **NTNewHorizons/NTM-Vehicles-Legacy**:
merge `backport` into this repository's `master`. Do not merge it into
**NTNewHorizons/NTM-Vehicles**, which retains the 1.12.2 implementation.

## Releases

The template's `build-and-test.yml` calls GTNH's shared workflow for pull requests
targeting `master` or `main` and pushes to those branches. It builds, checks formatting,
runs regression tests, and performs the shared automated server startup check.
Local verification does not launch Minecraft; gameplay testing remains a user task.

`release-tags.yml` uses GTNH's shared release workflow, restricted to `*-1.7.10`
tags. Versions must follow `[v]x.y.z-1.7.10`, for example `v1.0.0-1.7.10`.
`assemble` depends on the regression tests so tag builds cannot publish after a
test failure. The shared workflow publishes the bridge artifacts in `build/libs`;
dependency JARs remain outside that directory. Install the plain bridge JAR, not
the `-dev` or `-sources` artifacts.

GTNH conventions handle Maven publications. Remote Maven publishing requires
`MAVEN_USER` and `MAVEN_PASSWORD`; override the destination with `-PmavenPublishUrl=...`
or the workflow's `MAVEN_PUBLISHING_URL` repository variable. Modrinth and CurseForge
remain disabled until their project IDs are configured in `gradle.properties`.

To roll back the build migration, revert its commit. It changes no saved world data.
