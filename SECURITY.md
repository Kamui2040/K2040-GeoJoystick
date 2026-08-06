# Security Policy

## Supported versions

Security fixes are prepared for the current maintained source line. Older releases may be replaced rather than patched individually.

## Report a vulnerability privately

Do not publish vulnerability details, private coordinates, shared links, device information, signing material, or exploit steps in a public issue.

Use GitHub's private vulnerability-reporting function for this repository when it is available under the **Security** tab. If that private form is unavailable, open a minimal public issue asking the maintainer to establish a private contact channel, but include no sensitive technical details.

Include only what is necessary:

- affected GeoJoystick version and Android version;
- the affected feature;
- reproducible steps using non-private test data;
- expected and observed behavior;
- impact;
- suggested remediation, when known.

## Scope

Relevant reports include unsafe external-intent handling, WebView or JavaScript-bridge escape, map-link parsing or redirect abuse, private-network access, component exposure, mock-provider cleanup failure, overlay persistence after stop, permission misuse, signing or release-integrity defects, and accidental disclosure of local data.

GeoJoystick intentionally exposes Android mock-location status and does not provide concealment, integrity bypass, ban evasion, root, Shizuku, accessibility automation, injection, or game-specific behavior. Requests to add those capabilities are outside project scope.
