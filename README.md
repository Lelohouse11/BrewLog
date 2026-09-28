# BrewLog – Warm Modern Barista Luxury ☕✨

A specialty coffee tracking and dial-in application for espresso enthusiasts and home baristas. Built with Jetpack Compose and designed around the **"BrewLog Crema & Vellum"** brand identity, blending modern translucent glass aesthetics with barista precision metrics.

---

## 🌟 Key Features

### 🎨 "Crema Glass & Vellum" Design System
- **Dual-Mode Aesthetics**:
  - 🌙 **Dark Mode ("Espresso Roast")**: Deep Charcoal Espresso background (`#120E0C`), roasted surfaces, and warm Crema Amber glow borders (`#E2A86B`).
  - ☀️ **Light Mode ("Filter Paper & Oat Milk")**: Unbleached filter paper off-white background (`#FAF7F2`), warm oat milk surfaces, and copper accents.
- **Editorial Brand Typography**: Serif display headlines for coffee names, clean sans-serif for UI, and **Barista Monospace (`tnum`)** for zero-wobble timers, doses, ratios, and flow rates.
- **Floating Glass Navigation Bar**: Modern schwebende Pill-Navigation with fluid **"Crema Crossfade & Micro-Scale"** page transitions.
- **Animated Crema Splash Screen**: Luxurious startup transition featuring the glowing golden crema bean emblem and brand tagline *"CRAFT & PRECISION"*.

---

### 📸 AI Coffee Label Scanner (Gemini AI)
- **Instant Label Extraction**: Take a photo of your coffee bag to automatically detect:
  - Coffee Name & Roaster Branding
  - Roast Level (Light, Medium, Dark)
  - **Roast Date (`roastDate`)**: Automatically extracts stamped/printed roast dates on coffee bags (`YYYY-MM-DD`).
  - Blend Percentages (Arabica, Robusta, Excelsa, Liberica)
  - SCA Flavor Notes & Sensory Profile (Sweetness, Acidity, Body, Bitterness)

---

### 📅 Roast Date & Degassing Freshness Window
- **Roast Date Tracking**: Input roast dates during bean setup with a 1-tap "Heute" (Today) shortcut.
- **Dynamic Freshness Window Badges**: Calculates days since roast to display flavor peak status:
  - 🟠 **Entgasung / Degassing** (Days 0–6)
  - 🟢 **Peak Flavor** (Days 7–30)
  - ⚪ **Reif / Aged** (Days > 30)
- **Quick-Edit Roast Date**: Tap the freshness badge on any coffee card to quickly update the roast date when buying a new bag, featuring a 1-tap "Heute" button and live freshness preview.

---

### ⏱️ Fluid Dial-In & Extraction Engine
- **Live Extraction Wave Ring**: Pulsating Crema Amber timer ring tracking time down to tenths of a second.
- **Live Ratio & Flow Rate Gauge**: Real-time ratio calculation ($1:2.0$) and extraction speed ($g/s$).
- **Rule-Based Recommendation Engine**: Analyzes yield, time, and sensory evaluation (sourness, bitterness, body) to suggest exact grind size and dose adjustments.
- **Channeling & Puck Prep Detection**: Identifies puck failures and suggests puck-prep fixes (WDT, distribution) instead of incorrect grind changes.

---

### 🎴 Specialty Coffee Bag Collection (Home Screen)
- **Ticket-Style Coffee Cards**: Ticket-inspired cards showing roaster branding, origin, and processing method.
- **Color-Coded SCA Flavor Chips**: Visual flavor note tags (Berry Pink, Cocoa Brown, Citrus Yellow, Jasmine Lavender).
- **Sensory Radar Chart**: Interactive 4-axis flavor radar chart for Sweetness, Acidity, Body, and Bitterness.

---

### ☕ Espresso Machine & Maintenance Health Hub
- **Machine Specifications**: Track portafilter diameter ($58\text{mm}$, $54\text{mm}$), steam wand, and integrated grinder specs.
- **AI Machine Spec Search**: Auto-detect machine specifications using Gemini AI.
- **Quick-Edit Weekly Consumption**: Quick edit button directly next to weekly cups consumption with live interval recalculation preview.
- **Segmented Glow-Pill Progress Meters**: Tactile 6-segment status pills changing color dynamically (Emerald Green ➔ Amber ➔ Crimson Red).
- **Localized Date & Time Metrics**: Concise remaining days badge (*"Noch 90 Tage"*) with exact last-done (*"Zuletzt: 15.01.2025"*) and next-due (*"Nächstes Mal fällig: 15.04.2025"*) dates.
- **AI Step-by-Step Maintenance Guides (Gemini AI)**: Generates 6–10 concise, numbered micro-steps tailored specifically to your machine model and architecture (Thermoblock vs. E61 vs. Dual Boiler) in your display language (German/English).
- **Full-Screen Maintenance Guide Reader**: Dedicated full-screen reader view with native top bar navigation, scroll-safe bottom padding, and quick pencil edit button.
- **Custom Guide Editor & Clear Tool**: Enter custom maintenance notes, generate AI guides, or clear instructions with 1 tap in Machine Settings.

---

### 💾 Data Portability & Barista Settings
- **Room Migration v13**: Safe schema migration (`MIGRATION_12_13`) preserving all user logs, shot histories, and custom machine instructions.
- **JSON Import & Export**: Full database backup and restore functionality integrated into Settings.
- **Barista Precision Controls**: Configurable step sizes for doses ($0.1\text{g}$, $0.5\text{g}$) and grind sizes with haptic vibration feedback.

---

## 🛠️ Tech Stack

- **UI Framework:** Jetpack Compose (Material 3 Expressive)
- **AI SDK:** Google AI Client (Gemini AI)
- **Database:** Room Persistence Library (Version 12)
- **Concurrency & State:** Kotlin Coroutines & Flow (`StateFlow`)
- **Navigation:** AndroidX Navigation Compose & AndroidX SplashScreen
- **Background Tasks:** AndroidX WorkManager
- **Image Loading:** Coil Compose
- **Serialization:** kotlinx.serialization

---

## 🚀 Getting Started

### Installation
1. Locate the pre-compiled APK file at: `app\build\outputs\apk\debug\app-debug.apk`
2. Transfer the file to your Android phone.
3. Tap the file to install (allow "Install from unknown sources" if prompted).

### AI Setup (Optional)
To enable the **AI Coffee Label Scanner**:
1. Obtain an API Key from **[Google AI Studio](https://aistudio.google.com/)**.
2. Open `app/src/main/java/com/example/brewlog/ui/GeminiViewModel.kt`.
3. Set your key in `apiKey`.

### Building from Source
Run the included build script to compile a fresh APK:
```cmd
generate_apk.bat
```
The output APK will be placed in `app\build\outputs\apk\debug\app-debug.apk`.

---

## 📁 Repository
Official Repository: **[Lelohouse11/BrewLog](https://github.com/Lelohouse11/BrewLog)**
