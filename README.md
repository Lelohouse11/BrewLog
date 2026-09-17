# BrewLog

A minimalistic, monochrome Android application for coffee enthusiasts to log and track their espresso brew settings, blend compositions, and sensory evaluations.

## Features

- **AI Coffee Label Scanner:** Instantly extract coffee details from a photo using **Gemini 3.1 Flash Lite**. Automatically detects coffee name, roaster, roast level, blend percentages, and sensory notes.
- **Advanced Dial-In & Shot-Tracking:** A deterministic, rule-based module to help you find the perfect extraction:
    - **Live Timer:** Integrated stopwatch to track extraction time down to tenths of a second.
    - **Smart Recommendation Engine:** Evaluates physical parameters (time, yield) and sensory feedback to suggest specific grind and ratio adjustments.
    - **Channeling Detection:** Identifies puck failures and provides puck-prep advice (e.g., WDT) instead of incorrect grind changes.
    - **Persistence:** Save recommended parameters directly to your bean profile to streamline the dial-in process.
- **Global Shot History:** A dedicated chronological list of all your extractions:
    - **Icon-based Metrics:** Clean overview of dose, yield, time, and grind size.
    - **Descriptive Feedback:** Human-readable sensory tags (e.g., "Too Sour", "Balanced", "Heavy Body").
    - **Quick Recall:** Click any history entry to instantly load its parameters back into the Dial-In workflow.
- **Smart Brew Settings:** Track grind sizes and independent gram weights for both **Single** and **Double** baskets within a single entry.
- **Dynamic Content Sections:** Keep your logs clean with optional, toggleable sections for:
    - **Overall Rating:** 1-10 quality score.
    - **Roast Level:** Light, Medium, or Dark.
    - **Blend Composition:** Percentages for Arabica, Robusta, Excelsa, and Liberica.
    - **Sensory Profile:** Visual 1-5 scales for Sweetness, Acidity, Body, and Bitterness.
    - **Flavor Tags:** Multi-select from a predefined list of coffee flavor notes.
- **Interactive Accordion UI:** Sleek, dark monochrome aesthetic with an accordion list where only one detailed view is expanded at a time for focus.
- **Real-time Search & Filter:** Powerful persistent search bar for names and roasters, combined with a comprehensive filter panel for all coffee characteristics.
- **Data Portability:** Full **JSON Import/Export** support to back up your data or share your favorite brew settings with friends.
- **Professional Validation:** Clear visual feedback ensures you never forget mandatory details (Coffee Name & Roaster).

### AI Setup (Optional)
To use the **AI Coffee Label Scanner**, you must provide your own Google Gemini API key:
1.  Obtain a free API Key from **[Google AI Studio](https://aistudio.google.com/)**.
2.  Open the file: `app/src/main/java/com/example/brewlog/ui/GeminiViewModel.kt`.
3.  Replace `"YOUR_GEMINI_API_KEY"` with your actual key.

### Project Repository
Visit the official repository for updates and contributions: **[Lelohouse11/BrewLog](https://github.com/Lelohouse11/BrewLog)**

---

## Tech Stack

- **UI:** Jetpack Compose (Material 3)
- **AI:** Google AI SDK (Gemini AI)
- **Database:** Room Persistence Library
- **Concurrency:** Kotlin Coroutines & Flow
- **Dependency Management:** Gradle Version Catalog (TOML)
- **Serialization:** kotlinx.serialization

## Getting Started

### Installation
The easiest way to get started is to install the application via the APK:
1. Locate the APK file at: `app\build\outputs\apk\debug\app-debug.apk`
2. Transfer this file to your Android phone.
3. Open the file on your phone to install (you may need to allow "Install from unknown sources").

### Re-generating the APK
If you make changes to the code and want to create a new APK:
1. Double-click the `generate_apk.bat` file in the project root folder.
2. Wait for the process to finish.
3. Your updated APK will be ready in the same location mentioned above.
