# Licence and attribution

GeoJoystick contains K2040-authored material together with upstream and third-party material. Licence scope follows authorship and provenance; K2040 project licences do not relicense third-party material.

## Application code

GeoJoystick application code is licensed under GNU GPLv3 only (`GPL-3.0-only`). The unmodified GNU GPLv3 text is in the repository root `LICENSE`.

K2040-authored code that is explicitly marked in its source file as subject to the separate GPLv3 section 7(b) attribution-preservation term also carries `LICENSES/GPL-3.0-Section-7b-K2040.txt`. That term requires preservation of the specified notice:

`Copyright (c) 2026 K2040.`

The section 7(b) term applies only to marked K2040-authored material for which K2040 has or can give appropriate copyright permission. It does not apply to GoGoGo-derived or other third-party material merely because it is distributed in this repository.

The current explicitly marked scope includes the K2040-authored `GeoSettings.java` and `GeoUi.java` files and K2040-authored portions of `MainActivity.java`. Older mixed-origin source files remain under their controlling GPLv3 terms without the K2040 section 7(b) term unless a later provenance review establishes and marks specific K2040-authored material.

## Upstream code and design provenance

GeoJoystick is a GPL-3.0-only derivative work informed by the open-source project:

- GoGoGo / 影梭
- Copyright © ZCShou and contributors
- Upstream: `https://github.com/ZCShou/GoGoGo`
- Baseline inspected: `de0d596190c57b8ca71481f60ce6b9e50af5107f`
- Upstream licence: GNU GPLv3

The mock-location service and joystick movement design were adapted and substantially simplified. This version removes the Baidu SDK and related native binaries, advertising is not present, and no proprietary premium entitlement is bypassed.

GoGoGo-derived material retains its upstream GPLv3 licence and attribution. The K2040 section 7(b) term is not imposed on upstream material for which K2040 does not control the relevant copyright.

## K2040 artwork and UI artwork

Original artwork and UI artwork authored by K2040 and for which K2040 can grant the relevant rights is licensed under Creative Commons Attribution 4.0 International (`CC-BY-4.0`). See `LICENSES/CC-BY-4.0.txt` for the licence reference and canonical Creative Commons legal-code links.

When sharing covered K2040 artwork, provide attribution to **K2040**, identify `CC BY 4.0`, link to the licence where reasonably practicable, and indicate modifications as required by CC BY 4.0.

The bundled K2040 avatar at `app/src/main/res/drawable-nodpi/k2040_avatar.png` is explicitly documented as original user-approved K2040 artwork. The repository copy is a 512 × 512 resized derivative of the approved source image and is covered by the K2040 CC BY 4.0 artwork licence described above.

Other visual assets are covered by this K2040 artwork licence only where their K2040 authorship or rights clearance is established by repository provenance. Do not infer CC BY 4.0 for upstream or third-party assets.

## OpenStreetMap

The built-in map uses OpenStreetMap map data and tiles only when the map is opened. OpenStreetMap data is © OpenStreetMap contributors and is made available under the Open Data Commons Open Database License (`ODbL 1.0`). GeoJoystick preserves visible OpenStreetMap contributor attribution in the map.

Official OpenStreetMap copyright and licensing information: `https://www.openstreetmap.org/copyright`

## Other third-party material

Third-party code, assets, dependencies, data, and notices retain their own controlling licences and attribution requirements. Nothing in the GeoJoystick GPL, K2040 section 7(b), or K2040 CC BY 4.0 notices should be read as relicensing third-party material.
