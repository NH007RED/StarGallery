# StarGallery — Global Actress Explorer

A native Android app (Kotlin, Jetpack Compose, Material 3) for discovering performers from film industries around the world, powered by [TMDB](https://www.themoviedb.org/).

**This copy has the owner's TMDB v3 API key already set in `TmdbConfig.kt` (`API_KEY`), so it shows data immediately. Do not upload this copy to a public repository.** (Without a key, the app shows an "API not configured" screen.)

> **Build status (please read):** the code was written without access to an Android SDK, so it has **not been compiled or run** by the author. Expect to fix a few small compile errors on first sync (typically missing imports or API-version drift). The Gradle setup, dependency versions and architecture are standard, so fixes should be minor. See [Limitations](#limitations-and-honest-notes).

---

## 1. Quick start

1. Install **Android Studio Koala (2024.1.1) or newer** (JDK 17 is bundled).
2. Open the `StarGallery` folder (`File ▸ Open`) and let Gradle sync.
3. Add your TMDB credentials (next section).
4. Run on a device or emulator with Android 8.0+ (API 26).

## 2. Where to put your TMDB API key

Open **`app/src/main/java/com/stargallery/app/data/remote/TmdbConfig.kt`** and fill in **one** of:

```kotlin
val API_READ_ACCESS_TOKEN: String = "PASTE_YOUR_READ_ACCESS_TOKEN_HERE"   // recommended (Bearer auth)
val API_KEY: String = ""                                                   // or the v3 key
val BACKEND_PROXY_URL: String = ""                                         // or your own proxy (production)
```

Get credentials: create a free account at themoviedb.org, then **Settings ▸ API** ▸ request a key. Copy the *API Read Access Token* (long JWT) or the *API Key* (32 hex chars).

Precedence: if `BACKEND_PROXY_URL` is set it is used and no credential is attached by the app. Otherwise the token is used, otherwise the key.

**Do not commit your real key to a public repository.** If you keep this project in Git, either keep the key out of the commit (use `git update-index --skip-worktree TmdbConfig.kt`) or use the proxy.

Credentials are never logged: the app has no HTTP logging interceptor, and error messages never contain request URLs.

## 3. Build and generate an APK

**Android Studio:** `Build ▸ Build Bundle(s) / APK(s) ▸ Build APK(s)`. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

**Command line:**

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # release APK (unsigned unless you configure signing)
```

**Signed release:** `Build ▸ Generate Signed Bundle / APK`, create a keystore, and keep the keystore and passwords private (they are git-ignored by default).

**GitHub Actions:** `.github/workflows/build-apk.yml` builds a debug APK on every push to `main`/`master` (or manually via *Run workflow*) and uploads it as the `StarGallery-debug-apk` artifact. Remember the key must be in `TmdbConfig.kt` for the built app to show data.

## 4. Features

| Area | What it does |
|---|---|
| Splash | Animated StarGallery branding, shown once per launch |
| Home | Cinematic hero (trending #1), rounded search, industry chips, trending carousel, popular grid, International cinema rows (Korean, Japanese, Bollywood, French, Nigerian), recently viewed |
| Explore | Search with clear/cancel, region/industry filter, sort (popularity, name), department filter, 2-column grid, infinite scroll, skeleton loading, empty/error states with retry |
| Profile | Collapsing toolbar, parallax cinematic header, portrait, facts card, expandable biography, photos, known-for cards, related performers, filmography grouped by year, favourite button, offline banner |
| Favourites | Saved profiles in Room, search, remove with Undo |
| Settings | Theme (system/light/dark), content language, image quality, offline cache freshness, cache clearing, about, TMDB attribution, privacy and support links |
| Offline | Opened profiles (details, filmography, photo list) are cached in Room and shown when the network fails |

## 5. Architecture

MVVM + Repository, manual dependency container (`AppContainer`), unidirectional state via `StateFlow`, `collectAsStateWithLifecycle` in the UI.

```
app/src/main/java/com/stargallery/app/
├── data/
│   ├── remote/       TmdbConfig, TmdbApi (Retrofit), NetworkModule (OkHttp)
│   ├── model/        Typed TMDB DTOs
│   ├── local/        Room (entities, DAOs, database, migrations), DataStore (SettingsStore)
│   └── repository/   DiscoveryRepository, ProfileRepository, LibraryRepository
├── domain/           UI-facing models, Industry definitions
├── ui/
│   ├── screens/      Splash, Home, Explore, Profile, Favorites, Settings, NotConfigured
│   ├── components/   PersonCard, skeletons, image + state views
│   ├── navigation/   NavHost + bottom navigation
│   └── theme/        Colors, typography, Material 3 theme
├── viewmodel/        One ViewModel per screen + Paginator
└── util/             ApiResult/AppError, formatters
```

Stack: Retrofit 2.11, OkHttp 4.12, Gson, Coil 2.6, Room 2.6 (KSP), DataStore Preferences, Navigation Compose, Compose BOM 2024.06, Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7.

### Error handling
Every network call goes through `safeApiCall`, which maps failures to `AppError` (not configured, offline, timeout, 401/403 bad credentials, 404, 429, 5xx, other). Timeouts: 10 s connect, 20 s read, 30 s whole call. Each screen shows loading skeletons, a retry button on failure, and an empty state with an action when nothing matches.

## 6. TMDB endpoints used

| Endpoint | Used for |
|---|---|
| `GET /person/popular` | Popular performers (Explore default) |
| `GET /trending/person/week` | Home hero and trending carousel |
| `GET /search/person` | Search by name |
| `GET /person/{id}` | Biography, birthday, place of birth, department |
| `GET /person/{id}/images` | Photo gallery |
| `GET /person/{id}/movie_credits` | Filmography and known-for |
| `GET /discover/movie` | Films per industry (`with_original_language`, `with_origin_country`, `sort_by=popularity.desc`) |
| `GET /movie/{id}/credits` | Cast of those films (industry exploration, related performers) |

All requests send `language` (your content-language setting), `page`, and `include_adult=false` where supported.

## 7. How "country / industry" exploration works (and its limits)

TMDB has **no** endpoint that lists actresses by country, and **no** authoritative nationality field for people. StarGallery therefore does not pretend otherwise:

* **Industry filters** (Hollywood, Bollywood, Tamil, Telugu, Malayalam, Kannada, Korean, Japanese, Chinese, Hong Kong, British, French, German, Italian, Spanish, Nigerian, Egyptian, Mexican, Brazilian, Argentine, Turkish, Thai) are *film* filters. The app calls `/discover/movie` with a language/origin-country pair, then reads the cast of the top 8 films per page via `/movie/{id}/credits`, keeping women billed in the top cast.
* The UI states that this is an **association with the industry's films, not nationality.** Film production countries are never shown as a person's nationality.
* **Place of birth** is shown exactly as TMDB contributors entered it, labelled as such, or shown as "Not available on TMDB".
* The app never infers nationality from biography text.

Other known limits:

* "Actress" means TMDB `gender == 1`. People with gender not set (0) are excluded, so some actresses will be missing, and results can be incomplete.
* The popularity sort is TMDB's order. The name sort and the department filter are applied client-side to the results loaded so far.
* Searching by name searches all of TMDB and takes priority over the industry filter.
* Industry pages cost 1 + up to 8 requests each (they load lazily, as rows scroll into view on Home and per page in Explore). Watch your TMDB rate limit when testing aggressively.
* Biographies are often missing or only in English for less-known people; the app says so instead of showing nothing.

## 8. Local storage

**Room** (`stargallery.db`, version 1): `favorites`, `recent_persons` (capped at 30), `person_cache` (details + JSON filmography and photo list, with the language it was fetched in). Favourites are never deleted by cache clearing.

**Migrations:** this is the first schema version, so there is no migration to run yet. `AppDatabase.kt` contains the `Migrations` object and a worked example; destructive fallback is deliberately **not** enabled so users never lose favourites on upgrade. Schemas are exported to `app/schemas/` (commit them when you change the schema).

**DataStore** (`settings`): theme, content language, image quality, cache freshness hours.

## 9. Production: the risk of embedding a TMDB key, and the proxy option

**Risk.** Anything compiled into an APK (constants, `BuildConfig`, resources, obfuscated strings) can be extracted by decompiling the app or by inspecting its traffic. A leaked key can be used by others against your TMDB quota and may get your key revoked. R8/obfuscation only slows this down.

**Recommended for any app you publish: a backend proxy.**

1. Deploy the example in [`backend-proxy/`](backend-proxy/) (Flask). It stores the token in the `TMDB_READ_TOKEN` environment variable, forwards only an allow-list of the endpoints this app uses, strips any client-supplied `api_key`, rate-limits per IP and caches identical GETs for 5 minutes.
2. Serve it over HTTPS.
3. In `TmdbConfig.kt` leave both credentials empty and set `BACKEND_PROXY_URL = "https://your-domain.example/3/"`.
4. Harden further as needed: stronger rate limits, app attestation (Play Integrity API), WAF, monitoring.

Note that a proxy protects *your key*, not your quota: anyone who finds your proxy URL can call it, which is why the rate limiting matters.

Images are loaded directly from `image.tmdb.org`, which does not require a key.

## 10. Branding, legal and attribution

* The launcher icon (adaptive, vector) is original: a minimalist star inside a portrait-frame motif on a violet gradient. No studio logos are used.
* Required TMDB attribution is shown in **Settings ▸ Legal** and on every profile: *"This product uses the TMDB API but is not endorsed or certified by TMDB."* The app does not use the TMDB logo and does not imply affiliation.
* **Before publishing:** replace `PRIVACY_URL` and `SUPPORT_URL` in `ui/screens/SettingsScreen.kt` (they are placeholders on example.com), and review TMDB's [API terms](https://www.themoviedb.org/api-terms-of-use), including their rules for commercial use.

## 11. Accessibility and performance

Content descriptions on images and buttons, 48 dp touch targets via Material components, system back navigation, configuration-change safe (state in ViewModels and `rememberSaveable`), lifecycle-aware collection, all I/O on `Dispatchers.IO` (Retrofit suspend calls, Room) with no main-thread network. Coil uses a 150 MB disk cache and 20% memory cache; image sizes follow the quality setting (valid TMDB sizes: profiles `w185`/`h632`/`original`, posters `w154`/`w342`/`w500`).

## Limitations and honest notes

* **Not compiled or tested.** No Android SDK was available when this project was generated, so no build, unit test or device run was performed. Please sync and build, and treat any first-build error as a quick fix rather than a design problem. Runtime behaviour against the real TMDB API (response shapes, rate limits) is likewise untested.
* No unit or UI tests are included.
* The UI is English only; the *content language* setting changes what TMDB returns, not the app's labels.
* The three-letter language of an industry (for example Chinese `zh` vs Cantonese `cn`) follows TMDB's `original_language` codes, which are not always applied consistently to older films.
* Portrait only (`screenOrientation="portrait"`) as requested; layouts are fluid but have not been tuned for tablets.
* Related performers are computed from co-billed casts of a person's four most popular films, are not cached, and may be empty.
* The privacy and support links are placeholders.
