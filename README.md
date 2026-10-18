# NTM: Vehicles - Minecraft 1.7.10

Backport of [TechTastic's NTM: Vehicles](https://github.com/TechTastic/NTM-Vehicles)
([NTNewHorizons fork](https://github.com/NTNewHorizons/NTM-Vehicles)).
Requires Immersive Vehicles: Legacy, HBM 1.0.27_X5842, and UniMixins for 1.7.10.
Upstream changes integrated through `48a1648` (power-armour seat detection and bullet-effect compatibility).
This backport targets original 1.7.10 NTM, not ExtendedEdition or CE.

## Build

```sh
./gradlew build
```

Pinned development dependencies:

- HBM `com.hbm:HBM-NTM:1.0.27_X5842:dev` from `https://maven.ntmr.dev/releases/`
- IVL `THOMASS47/IVL` commit `d32bf23`, built locally with `patches/ivl-custom-hit-once.patch`
- UniMixins, supplied by the GTNH build's `usesMixins` integration

IVL is closed source for now: authenticate Git for `THOMASS47/IVL`, or set `IVL_SOURCE`
to an authorized mirror. The bridge compiles against the `minecrafttransportsimulator.*`
classes inside the IVL dev jar. Install `build/libs/ntm_vehicles-*.jar` alongside
normal IVL, NTM, and UniMixins jars in Forge 10.13.4.1614. The bridge jar bundles none.
Install the bridge on both client and server for armour-driven animations and conditions.

## Pack API

Unchanged `CUSTOM` bullet functions:

| Function | Behavior |
| --- | --- |
| `ntm_vehicles:nuke` | NTM MK5 explosion with standard mushroom cloud |
| `ntm_vehicles:gas` | NTM gas entities; count from blast strength |
| `ntm_vehicles:napalm` | 2.5-strength explosion with ignition and flames |

Upstream's CE/ExtendedEdition compatibility adapter maps to `NTMEffects` here:
original NTM uses `statFacStandard` for the standard Torex cloud, the unattributed
`ExplosionLarge.explode` overload, and integer-coordinate ignition helpers.
No CE/ExtendedEdition signature probing or 1.12.2 classes are needed.

`blastStrength` is used unless zero, when `diameter / 10` is used.
Gas tuning lives in `constantValues`: `gasType` (0 chlorine, 1 cloud,
2 pink cloud, else orange) and `gasSpreadSpeed` (default 1.25).
Effects run server-side only. The bridge adds no blocks, items, or saved
data; roll back by removing its jar.

### Power armour detection

IVL packs can use `seat_rider_power_armor` on a seat, or
`seat_rider_power_armor_1` from the vehicle for its first seat (normal numbered-part lookup).

- `1`: a player wearing a complete matching NTM battery- or fuel-powered armour set,
  including subclasses.
- `0`: empty seat, non-player rider, ordinary armour, or an incomplete/mixed set.

Detection uses NTM's `ArmorFSB.hasFSBArmorIgnoreCharge`, including its optional-helmet
rules. Empty batteries/fuel tanks still count. The value is recomputed on both sides
as riders and equipment change; it does not restrict mounting or alter armour behaviour.

```json
{
  "animationType": "visibility",
  "variable": "seat_rider_power_armor",
  "clampMin": 1,
  "clampMax": 1
}
```

Removing the bridge disables these bullet effects and the armour variable; no save migration is needed.
