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

## Project Overview

**Snap & Cook** is a 100% native Android application developed in Kotlin. The application leverages computer vision and machine learning to detect available ingredients through image capture or gallery selection. Based on the detected ingredients, the app searches the Spoonacular recipe database to retrieve personalised, step-by-step recipes for the user.

The app also supports barcode scanning using ML Kit, an on-device machine learning library, which detects barcodes from the camera and queries the Open Food Facts API to retrieve the product name. The identified product is then automatically added to the ingredient list before searching for matching recipes via Spoonacular.

The system integrates camera functionality, computer vision, machine learning, networking, barcode product lookup, and local storage to create a smart cooking assistant that enhances everyday meal preparation.

---

## Features

- **Ingredient Detection** — Photograph one or more ingredients and let Google Gemini 1.5 Flash identify them automatically via cloud-based computer vision.
- **Multi-Image Capture** — Take multiple photos in one session; all detected ingredients are merged and deduplicated.
- **Manual Editing** — Review and edit the detected ingredient list before searching — add items manually or delete wrong ones.
- **Barcode Scanning** — Scan packaged product barcodes using ML Kit; the Open Food Facts API resolves the barcode to a product name and adds it as an ingredient automatically.
- **Recipe Search** — Fetches up to 6 matching recipes from Spoonacular ranked by ingredient coverage; paginate through results with Prev / Next.
- **Ingredient Matching** — Recipe ingredients are highlighted: items you already own appear with a green ✓, while missing items show a bullet point.
- **YouTube Video** — A relevant cooking video is fetched from Spoonacular and shown as a tappable thumbnail that opens directly in YouTube.
- **Fullscreen Image** — Tap the recipe hero image to view it fullscreen.
- **Save Recipes** — Bookmark favourite recipes for offline access via a local Room (SQLite) database.
- **Guided Cooking Mode** — Step-by-step instructions displayed one at a time with a progress bar and equipment chips per step.
- **Text-to-Speech** — Each step is read aloud automatically via Android TTS; a congratulations message plays on completion.
- **Voice Commands** — Say *"next"*, *"previous"* / *"back"*, or *"repeat"* / *"again"* hands-free while cooking.
- **API Key Rotation** — Multiple Spoonacular keys rotate automatically on quota exhaustion (HTTP 402) for uninterrupted service.
- **Dark Mode** — Supported via `values-night` resource directories and Android 12+ dynamic colour theming.
- **Time-Based Greeting** — The home screen greets the user with Good Morning / Afternoon / Evening based on the current time.
- **Recently Saved Strip** — The home screen displays the last three saved recipes for quick access.

---

## App Flow

```
Splash Screen
    └── Main Screen (Home / Recently Saved / Saved Recipes)
            └── Camera Screen (live preview, multi-capture, flash, flip)
                    └── Verification Screen (review ingredients, add, delete, barcode scan)
                            └── Recipe Results Screen (paginated cards, ingredient matching, YouTube)
                                    ├── Cooking Mode Screen (TTS + voice commands)
                                    └── Saved Recipes Screen
```

---

## Architecture

The project follows **MVVM with a Repository pattern**:

```
ui/             — Activities, ViewModels, RecyclerView Adapters
data/remote/    — Retrofit API clients, Gemini client, Repository
data/local/     — Room database, DAO, SavedRecipe entity
data/model/     — POJOs for API responses (Spoonacular, Gemini, Open Food Facts)
ml/             — IngredientDetector (Gemini vision pipeline)
util/           — Extension functions, RecipeConverter
```

### Screen & ViewModel Mapping

| Screen | ViewModel |
|--------|-----------|
| MainActivity | MainViewModel |
| CameraActivity | — |
| VerificationActivity | VerificationViewModel |
| RecipeResultActivity | RecipeViewModel |
| CookingModeActivity | CookingViewModel |
| SavedRecipesActivity | SavedRecipesViewModel |

### Key Design Decisions

- **API key rotation** — `SpoonacularKeyManager` maintains a pool of keys read from `local.properties`. `RecipeRepository.withKeyRotation()` catches HTTP 402 and transparently retries with the next key. Thread-safe via `Collections.synchronizedSet`.
- **Gemini vision pipeline** — `IngredientDetector` compresses bitmaps to JPEG quality 85 (no fixed dimension cap) and base64-encodes them before sending to Gemini 1.5 Flash. A structured prompt prohibits generic labels (e.g. "vegetable") and enforces specific ingredient names. Returns an empty list on any failure (graceful degradation).
- **Local persistence** — Room stores saved recipes as two JSON-serialised string columns (`ingredientsJson`, `stepsJson`) using `RecipeConverter` (Gson). Primary key is the Spoonacular recipe ID.
- **Cooking mode** — `CookingModeActivity` sets `FLAG_KEEP_SCREEN_ON`, reads steps via Android `TextToSpeech`, and listens for voice commands via `SpeechRecognizer`. Voice recognition uses top-3 results and `ERROR_RECOGNIZER_BUSY` retry handling.
- **LiveData throughout** — All ViewModels expose `LiveData`; Activities observe and update UI reactively.

---

## Technology Stack

| Category | Technology / Version |
|----------|----------------------|
| Language | Kotlin 2.0.21 |
| Platform | Native Android — min SDK 26 / target SDK 35 |
| Build | Android Gradle Plugin 8.13.2 (Kotlin DSL) |
| AI / Vision | Google Gemini 1.5 Flash (REST API) |
| Recipe Data | Spoonacular API |
| Barcode Data | Open Food Facts REST API |
| Barcode Scanner | Google ML Kit Code Scanner 16.1.0 |
| Local Database | Room 2.6.1 (SQLite ORM) |
| Networking | Retrofit 2.11.0 + OkHttp 4.12.0 |
| Image Loading | Glide 4.16.0 |
| Camera | CameraX 1.4.2 |
| Architecture | MVVM + Repository pattern |
| Async | Kotlin Coroutines 1.8.1 + LiveData |
| UI | Material Design 3 1.12.0, ViewBinding |
| Voice | Android TextToSpeech + SpeechRecognizer (built-in) |

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
SPOONACULAR_API_KEY_1=your_first_spoonacular_key
SPOONACULAR_API_KEY_2=optional_second_key
SPOONACULAR_API_KEY_3=optional_third_key
```

Additional Spoonacular keys can be added by continuing the numbering — no code changes needed.

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

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
| `INTERNET` | Gemini AI, Spoonacular, and Open Food Facts API calls |
| `ACCESS_NETWORK_STATE` | Network availability check |
| `READ_EXTERNAL_STORAGE` | Gallery access on Android ≤ 12 |
| `READ_MEDIA_IMAGES` | Gallery access on Android 13+ |

---

## Links

- **GitHub Repository:** https://github.com/sTsenre/MobileComputingProject
- **App Demo Video:** https://youtu.be/JWJMpj2Y8c0

---

## License

This project was developed for academic purposes at the Singapore Institute of Technology. All rights reserved by the team members listed above.
