# android-compose-design-system

Architectural standards for structuring a Jetpack Compose design system.

## Features
- **Concern Separation**: Defines colors, typography, and shapes as separate interfaces.
- **CompositionLocal**: Provides static access to theme values via `AppTheme.colors.primary`.
- **Material 3 Integration**: Includes automatic bridging to `MaterialTheme` color schemes and typography.

## How to use
1. Run `curl -fsSL https://rules.tbsten.me/i | bash -s -- android-compose-design-system`.
2. Reference files will be created in your `ui/designSystem/` directory.
3. Update the package names and define your project's brand colors and styles.
