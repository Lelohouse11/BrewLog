# BrewLog

A minimalistic, monochrome Android application for coffee enthusiasts to log and track their espresso brew settings and sensory evaluations.

## Features

- **Structured Brew Settings:** Log coffee name, roaster, and grind size.
- **Flexible Basket Support:** Independent settings for Single and Double baskets with specific gram weights.
- **Evaluation & Sensory Profile:** Optional tracking for overall rating (1-10) and sensory characteristics (Sweetness, Acidity, Body, Bitterness).
- **Flavor Tags:** Select from a predefined list of flavor notes (e.g., Chocolate, Fruity, Floral).
- **Interactive UI:** Clean, dark monochrome aesthetic with an accordion-style list for easy browsing.
- **Search & Filter:** Find your favorite brews by name or roaster, and filter by rating, roast level, or sensory profile.
- **Data Portability:** Export your logs to JSON for backup or share them across devices with the Import feature.

## Tech Stack

- **UI:** Jetpack Compose (Material 3)
- **Database:** Room Persistence Library
- **Concurrency:** Kotlin Coroutines & Flow
- **Dependency Management:** Gradle Version Catalog (TOML)
- **Serialization:** kotlinx.serialization
