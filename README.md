# Snap & Cook

A native Android application developed as part of a Mobile Computing project at the Singapore Institute of Technology (SIT).

---

## Team Members

| Name | SIT ID |
|------|--------|
| Ernest Ho Yong Heng | 2301223 |
| Lucas Yee Junjie | 2301212 |
| Tan Shun Zhi Tomy | 2301341 |
| Wong Woon Li | 2301308 |
| Yan Yu | 2301213 |
| Muhammad Zikry Bin Zakaria | 2201751 |

---

## Introduction

**Snap & Cook** turns your fridge into a recipe book. Simply take a photo of the ingredients you have on hand, and the app uses Google Gemini AI to identify them. It then searches the Spoonacular recipe database to suggest matching recipes you can cook right away. Once you pick a recipe, a guided cooking mode reads each step aloud and responds to voice commands — no touching your phone with messy hands required.

---

## Language & Technology

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| Platform | Native Android (min SDK 26 / target SDK 35) |
| AI / Vision | Google Gemini 1.5 Flash |
| Recipe Data | Spoonacular API |
| Local Database | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Image Loading | Glide |
| Camera | CameraX |
| Architecture | MVVM with Repository pattern |
| Async | Kotlin Coroutines + LiveData |
| Build System | Gradle (Kotlin DSL) |

---

## Features

- **Ingredient Detection** — Photograph ingredients and let Gemini AI identify them automatically.
- **Manual Editing** — Review and edit the detected ingredient list before searching.
- **Recipe Search** — Fetches matching recipes from Spoonacular using detected ingredients.
- **Save Recipes** — Bookmark favourite recipes for offline access via a local Room database.
- **Guided Cooking Mode** — Step-by-step instructions read aloud via Text-to-Speech with automatic microphone activation after each step.
- **Voice Commands** — Say *"next"*, *"previous"*, or *"repeat"* hands-free while cooking.
- **API Key Rotation** — Multiple Spoonacular keys rotate automatically on quota exhaustion for uninterrupted use.

---

## App Flow

```
Splash Screen
    └── Main Screen (Home / Saved Recipes)
            └── Camera Screen (capture photo)
                    └── Verification Screen (review ingredients)
                            └── Recipe Results Screen
                                    └── Cooking Mode Screen
```

---

## Architecture Overview

The project follows **MVVM with a Repository pattern**:

```
ui/             — Activities, ViewModels, RecyclerView Adapters
data/remote/    — Retrofit API clients, Gemini client, Repository
data/local/     — Room database, DAO, SavedRecipe entity
data/model/     — POJOs for API responses
ml/             — IngredientDetector (Gemini vision pipeline)
util/           — Extension functions, RecipeConverter
```

---

## Setup & Build

### Prerequisites

- Android Studio Hedgehog or later
- Android device or emulator running API 26+
- A Google Gemini API key
- One or more Spoonacular API keys

### API Key Configuration

Create a `local.properties` file in the project root (it is gitignored) and add:

```properties
GEMINI_API_KEY=your_gemini_key_here
SPOONACULAR_API_KEY_1=your_spoonacular_key_here
SPOONACULAR_API_KEY_2=optional_second_key
```

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Clean and rebuild
./gradlew clean assembleDebug
```

Windows users can also run:

```bat
build_debug.bat
```

---

## Permissions Required

| Permission | Purpose |
|------------|---------|
| `CAMERA` | Capturing ingredient photos |
| `RECORD_AUDIO` | Voice commands in cooking mode |
| `INTERNET` | Gemini AI and Spoonacular API calls |

---

## License

This project was developed for academic purposes at the Singapore Institute of Technology. All rights reserved by the team members listed above.
