# Privacy Policy

**Effective date:** 14 July 2026

**Last updated:** 3 September 2026

GeoJoystick is an open-source Android mock-location utility intended for emulator, development, and testing use.

## Data collection

GeoJoystick does not require an account and does not collect, sell, rent, or share personal data with the developer. The app contains no advertising, analytics, tracking, telemetry, crash-reporting service, billing, subscription system, or proprietary updater.

GeoJoystick does not collect device identifiers, contacts, messages, photos, media, or a location history for the developer.

## Data stored on the device

GeoJoystick stores only the information needed for its local features, such as:

- manually entered coordinates and altitude;
- the last successfully active position when the optional restore setting is enabled;
- named favorite locations;
- appearance, language, speed, overlay, and other app settings.

This information is stored in the app's private local storage and is not transmitted to the developer. GeoJoystick disables Android app backup/device-transfer of its private app data through its application and backup configuration. Clearing the app's storage or uninstalling the app removes the locally stored GeoJoystick data.

While simulation is active, GeoJoystick supplies the coordinates selected by the user to Android's standard mock-location providers. This is the core purpose of the app. GeoJoystick does not attempt to conceal mock-location status and does not send those coordinates to the developer.

## Clipboard use

The **Paste link** action reads the current clipboard only when the user explicitly invokes that action. GeoJoystick does not continuously monitor the clipboard.

Pasted text is parsed locally first. If it contains a supported web link that cannot be resolved locally, the documented map-link resolution behavior below may perform a bounded network request.

## Network access

GeoJoystick has no developer-operated app server. Network access is tied to user-invoked features.

### Map picker

Opening the built-in map picker downloads map tiles from OpenStreetMap. These requests may expose normal connection information, such as the device's IP address and standard request metadata, to the OpenStreetMap tile service. The requested tiles can also indicate the map area being viewed. OpenStreetMap handles that data under its own privacy policy:

https://osmfoundation.org/wiki/Privacy_Policy

The map interface itself is bundled with GeoJoystick. It does not load remote JavaScript and does not require a map API key.

### Place and address search

The map picker includes optional place/address search. GeoJoystick passes the submitted query to Android's geocoding implementation only when the user taps **Search**; it does not send search requests while the user types.

Depending on the device and installed system components, Android's geocoding implementation may use a network service and may expose the submitted query, the device's IP address, and standard request metadata to that service. GeoJoystick does not operate that geocoding service and does not receive the submitted query.

If no geocoder is available, no result is returned, multiple distinct matches are returned, the result is invalid, or the geocoding request fails, GeoJoystick leaves the current map selection unchanged.

### Map-link import

When a user pastes, imports, or shares a supported map link, GeoJoystick first attempts to extract coordinates locally. If a supported web link cannot be resolved locally, the app may contact that link, follow validated redirects, and read a limited response in order to extract coordinates.

Link resolution is bounded by scheme, host/public-address, redirect, response-size, and timeout checks. Unsupported or invalid input is rejected rather than replaced with a fallback location. The destination website may receive normal connection information, including the device's IP address and standard request metadata, and handles that information under its own privacy policy.

### External links

Links to external services, such as the source repository, licence information, support page, or a maps app, open in another installed app or browser. Any data handled after leaving GeoJoystick is governed by the privacy policy of that external service.

## Permissions and special access

GeoJoystick requests or uses only Android permissions/access needed for its visible features:

- Internet access for OpenStreetMap tiles, optional network-backed device geocoding, and explicit map-link resolution;
- display-over-other-apps access for the movable joystick overlay;
- foreground-service permissions for active user-controlled mock-location simulation;
- notification permission on supported Android versions for the foreground-service/status notification;
- Android's standard mock-location selection through Developer Options.

GeoJoystick does not request fine or coarse real-device location permission. These permissions and access paths are not used for analytics, advertising, tracking, or background data collection for the developer.

## Data sharing and retention

GeoJoystick does not operate a server and does not maintain a user database. The developer does not receive or retain app data.

Local GeoJoystick data remains on the device until the user changes it, clears the app's data, or uninstalls the app. External services reached through the user-invoked network features above may process normal request information under their own policies.

## Changes to this policy

Material changes to this policy will be published in this repository together with the corresponding app update when applicable.

## Contact

Questions about this privacy policy can be submitted through the public issue tracker:

https://github.com/Kamui2040/K2040-GeoJoystick/issues

The issue tracker is public. Do not include personal, sensitive, credential, or private-location information in an issue.
