# Privacy Policy

**Effective date:** 14 July 2026

GeoJoystick is an open-source Android mock-location utility intended for emulator, development, and testing use.

## Data collection

GeoJoystick does not require an account and does not collect, sell, rent, or share personal data with the developer. The app contains no advertising, analytics, tracking, telemetry, crash-reporting service, billing, subscription system, or proprietary updater.

GeoJoystick does not collect device identifiers, contacts, messages, photos, media, or a location history.

## Data stored on the device

GeoJoystick stores only the information needed for its local features, such as:

- manually entered coordinates and altitude;
- the last active position when the optional restore setting is enabled;
- named favorite locations;
- appearance, language, speed, overlay, and other app settings.

This information is stored locally in the app's private storage. GeoJoystick does not transmit it to the developer. Clearing the app's storage or uninstalling the app removes this locally stored data.

While simulation is active, GeoJoystick supplies the coordinates selected by the user to Android's standard mock-location providers. This is the core purpose of the app. GeoJoystick does not attempt to conceal mock-location status and does not send those coordinates to the developer.

## Network access

GeoJoystick uses network access only after an explicit user action:

### Map picker

Opening the built-in map picker downloads map tiles from OpenStreetMap. These requests may expose normal connection information, such as the device's IP address and standard request metadata, to the OpenStreetMap tile service. The requested map tiles can also indicate the map area being viewed. OpenStreetMap handles that data under its own privacy policy:

https://osmfoundation.org/wiki/Privacy_Policy

### Map-link import

When a user imports or shares a map link, GeoJoystick first attempts to extract coordinates locally. If the text contains a web link that cannot be resolved locally, the app may contact that link, follow redirects, and read a limited response in order to extract coordinates. The destination website may receive normal connection information, including the device's IP address and standard request metadata, and handles that information under its own privacy policy.

### External links

Links to external services, such as the source repository, support page, or a maps app, open in another installed app or browser. Any data handled after leaving GeoJoystick is governed by the privacy policy of that external service.

## Permissions

GeoJoystick requests only permissions needed for its visible features:

- Internet access for the map picker and explicit map-link resolution;
- display-over-other-apps permission for the movable joystick overlay;
- foreground-service permission for active simulation;
- notification permission on supported Android versions for the active foreground-service notification;
- Android's standard mock-location selection through Developer Options.

These permissions are not used for analytics, advertising, tracking, or background data collection.

## Data sharing and retention

GeoJoystick does not operate a server and does not maintain a user database. The developer does not receive or retain app data. Any data stored by GeoJoystick remains on the device until it is changed by the user, cleared through Android settings, or removed by uninstalling the app.

## Changes to this policy

Material changes to this policy will be published in this repository together with the corresponding app update when applicable.

## Contact

Questions about this privacy policy can be submitted through the public issue tracker:

https://github.com/Kamui2040/K2040-GeoJoystick/issues
