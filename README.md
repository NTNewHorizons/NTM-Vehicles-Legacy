# NTM: Vehicles - Minecraft 1.7.10

Backport of [TechTastic's NTM: Vehicles](https://github.com/TechTastic/NTM-Vehicles)
([NTNewHorizons fork](https://github.com/NTNewHorizons/NTM-Vehicles)).
Requires Immersive Vehicles: Legacy and HBM 1.0.27_X5808 for 1.7.10.

## Build

```sh
./gradlew build
```

Pinned development dependencies:

- HBM `com.hbm:HBM-NTM:1.0.27_X5808:dev` from `https://maven.ntmr.dev/releases/`
- IVL `THOMASS47/IVL` commit `d32bf23`, built locally with `patches/ivl-custom-hit-once.patch`

IVL is closed source for now: authenticate Git for `THOMASS47/IVL`, or set `IVL_SOURCE`
to an authorized mirror. The bridge compiles against the `minecrafttransportsimulator.*`
classes inside the IVL dev jar. Install `build/libs/ntm_vehicles-*.jar` alongside
normal IVL and NTM jars in Forge 10.13.4.1614. The bridge jar bundles neither.

## Pack API

Unchanged `CUSTOM` bullet functions:

| Function | Behavior |
| --- | --- |
| `ntm_vehicles:nuke` | NTM MK5 explosion with standard mushroom cloud |
| `ntm_vehicles:gas` | NTM gas entities; count from blast strength |
| `ntm_vehicles:napalm` | 2.5-strength explosion with ignition and flames |

`blastStrength` is used unless zero, when `diameter / 10` is used.
Gas tuning lives in `constantValues`: `gasType` (0 chlorine, 1 cloud,
2 pink cloud, else orange) and `gasSpreadSpeed` (default 1.25).
Effects run server-side only. The bridge adds no blocks, items, or saved
data; roll back by removing its jar.
