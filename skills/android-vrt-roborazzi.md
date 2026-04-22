# android-vrt-roborazzi

Automated Visual Regression Testing (VRT) for Android with Roborazzi.

## Features
- **Roborazzi Integration**: Capture screenshots of Compose Previews using Robolectric.
- **Compose Preview Scanner**: Automatically find all previews in the project and test them.
- **GitHub Actions**: Automated screenshot capture for both base and head branches, followed by a comparison report.
- **PR Commenting**: Updates the PR body or adds a comment with the comparison results (screenshots).

## Prerequisites
- Android project using Jetpack Compose.
- GitHub Actions for CI.
- Node.js for `reg-suit` report generation.

## How to use
1. Run `npx skills add tbsten/skills --skill android-vrt-roborazzi`.
2. Follow the instructions in `SKILL.md` to set up the Gradle plugin and CI workflows.
