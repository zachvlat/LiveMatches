# Scorito

<span>
<img width="200" alt="image" src="https://raw.githubusercontent.com/zachvlat/scorito/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" />
<img width="200" alt="image" src="https://raw.githubusercontent.com/zachvlat/scorito/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" />
</span>

A modern Android app for tracking live football (soccer) scores and match results.

## Features

- Real-time football/basketball/cricket/hockey match scores and live status updates
- Search per team/country/competition
- Team search functionality to filter matches by team names
- Date navigation (today, yesterday, tomorrow)
- Matches organized by competition/league
- Match details including team logos, scores, and status
- Manual refresh button with Material Design 3 UI

## Download
[<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/7/78/Google_Play_Store_badge_EN.svg/250px-Google_Play_Store_badge_EN.svg.png" alt="Get it on Obtainium" height="60">](https://play.google.com/store/apps/details?id=com.zachvlat.footballscores)

[<img src="https://raw.githubusercontent.com/jiangtian616/JHenTai/master/badges/get_it_on_obtainium.png" alt="Get it on Obtainium" height="60">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.zachvlat.footballscores%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fzachvlat%2Ffootballscores%22%2C%22author%22%3A%22zachvlat%22%2C%22name%22%3A%22Football%20Scores%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D)

## Tech Stack

- **Platform**: Android (Kotlin)
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with clean architecture principles
- **Networking**: Retrofit, OkHttp, Gson
- **Async**: Kotlin Coroutines
- **Image Loading**: Coil

## Requirements

- Android 7.0 (API 24) or higher
- Internet connection for live data

## Setup

1. Clone the repository
2. Open in Android Studio
3. Build and run

The app uses the LiveScore API for real-time football data.
