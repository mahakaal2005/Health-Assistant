# Health Assistant

A comprehensive Android health management application designed to help users track their fitness, manage prescriptions, keep a health journal, and stay informed with AI-curated health content.

## 📱 Features

*   **Dashboard**: Get a quick overview of your daily health stats including steps, calories burned, sleep duration, and water intake.
*   **Step Tracking**: Real-time step counting using device sensors (Step Counter/Detector) and a robust background service to track your activity throughout the day. Includes detailed charts and history.
*   **Prescription Management**: Easily capture and manage your prescriptions using the camera. Keep track of your medications in one place.
*   **Health Journal**: Log your daily thoughts, feelings, and health notes to track your mental and emotional well-being over time.
*   **Discover**: Explore health news and articles curated from top sources (Guardian, News API) and YouTube videos.
    *   **AI-Powered Insights**: Uses **Google AI (Gemini)** to provide personalized health summaries and content.
*   **Profile**: Manage your personal information, view your achievements, and track your progress.
*   **Onboarding**: A smooth, guided introduction to help you set up your profile and goals.
*   **Authentication**: Secure user login and signup powered by **Firebase Authentication** and **Supabase**.

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **Architecture**: MVVM with Clean Architecture principles (Modularized by feature).
*   **Dependency Injection**: Hilt (Dagger).
*   **Asynchronous Programming**: Coroutines & Flow.
*   **UI**:
    *   XML Layouts with ViewBinding & DataBinding.
    *   Material Design Components.
    *   MPAndroidChart for data visualization.
    *   Lottie for animations.
    *   Glide & Coil for image loading.
*   **Android Components**:
    *   Jetpack Navigation Component.
    *   WorkManager for background tasks.
    *   Room Database for local caching.
    *   DataStore for preferences.
    *   CameraX for capturing prescriptions.
*   **Cloud & APIs**:
    *   **Firebase**: Authentication, Firestore.
    *   **Supabase**: Storage (for images), Authentication integration.
    *   **Google AI (Gemini)**: Generative AI for content.
    *   **External APIs**: News API, The Guardian API, YouTube Data API v3.

## 📋 Prerequisites

*   **Android Studio**: Latest version recommended (Ladybug or newer).
*   **JDK**: Java 11 or 17.
*   **Android Device/Emulator**: Running Android 10 (API level 30) or higher.

## 🚀 Setup & Installation

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/yourusername/health-assistant.git
    cd health-assistant
    ```

2.  **Firebase Setup**
    *   Create a project in the [Firebase Console](https://console.firebase.google.com/).
    *   Add an Android app with package name `com.example.health_assistant`.
    *   Download `google-services.json` and place it in the `app/` directory.

3.  **API Keys Configuration**
    The app requires several API keys to function (News, Guardian, YouTube, Google AI).

    *   Open `app/src/main/java/com/example/health_assistant/features/discover/data/remote/ApiKeyManager.kt`.
    *   Replace the placeholder keys with your own:
        *   **News API**: Get from [newsapi.org](https://newsapi.org/).
        *   **Guardian API**: Get from [open-platform.theguardian.com](https://open-platform.theguardian.com/).
        *   **YouTube API**: Get from [Google Cloud Console](https://console.cloud.google.com/).
        *   **Google AI (Gemini)**: Get from [Google AI Studio](https://makersuite.google.com/app/apikey).

    > **Note**: For better security, consider moving these keys to `local.properties` or `BuildConfig` in a production environment.

4.  **Supabase Setup**
    *   Set up a Supabase project for storage.
    *   Configure your Supabase URL and Key in the code (check `app/build.gradle.kts` or where Supabase is initialized).

5.  **Build and Run**
    *   Open the project in Android Studio.
    *   Sync Gradle files.
    *   Run the app on your emulator or physical device.

## 📂 Project Structure

The project follows a modular structure by feature:

*   `app/src/main/java/com/example/health_assistant/`
    *   `auth`: Authentication logic and screens.
    *   `core`: Common utilities, extensions, and base classes.
    *   `data`: Repositories and data sources.
    *   `di`: Hilt modules for dependency injection.
    *   `features`: Feature-specific code (e.g., `step_tracking`, `journal`, `discover`).
    *   `ui`: Common UI components.

## 🔧 Troubleshooting

*   **App Icon Issues**: If you encounter issues with the app icon, refer to `FIX_APP_ICON_GUIDE.md` in the root directory for instructions on how to regenerate it.
*   **Gradle Sync Failures**: Ensure you have the correct JDK version set in Android Studio (Settings > Build, Execution, Deployment > Build Tools > Gradle).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
