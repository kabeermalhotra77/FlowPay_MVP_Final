# FlowPay Android App

A modern Android payment application built with Kotlin and Jetpack Compose.

## 🚀 Features

- Modern Material Design 3 UI
- Jetpack Compose for declarative UI
- Kotlin-first development
- MVVM architecture ready
- Navigation component integration
- Dark/Light theme support

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Navigation Compose
- **Dependency Injection**: Ready for Hilt/Dagger
- **Build System**: Gradle 8.2
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- [Android Studio](https://developer.android.com/studio) (latest version recommended)
- [JDK 8 or higher](https://www.oracle.com/java/technologies/downloads/)
- Android SDK with API level 34
- Git

## 🏗️ Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd FLOWPAYYYYY
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the project directory and select it

3. **Sync the project**
   - Android Studio will automatically prompt you to sync
   - Click "Sync Now" or go to File → Sync Project with Gradle Files

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green play icon) or press Shift+F10

## 📱 Building the App

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Run Tests
```bash
./gradlew test
```

## 📁 Project Structure

```
app/
├── src/main/
│   ├── java/
│   │   └── ui/
│   │       └── theme/                   # UI theme components
│   │           ├── Color.kt             # Color definitions
│   │           ├── Theme.kt             # Material 3 theme
│   │           └── Type.kt              # Typography
│   ├── res/                             # Resources
│   │   ├── values/                      # String, color, theme resources
│   │   ├── drawable/                    # Drawable resources
│   │   └── mipmap/                      # App icons
│   └── AndroidManifest.xml              # App manifest
├── build.gradle                         # App-level build configuration
└── proguard-rules.pro                   # ProGuard rules
```

## 🎨 Customization

### Brand Colors
The app includes FlowPay brand colors defined in `Color.kt`:
- FlowPay Blue: `#2196F3`
- FlowPay Green: `#4CAF50`
- FlowPay Orange: `#FF9800`

### Adding New Screens
1. Create a new Composable function
2. Add navigation routes in your navigation setup
3. Update the main navigation graph

### Adding Dependencies
Add new dependencies in `app/build.gradle`:
```gradle
dependencies {
    implementation 'your.dependency:version'
}
```

## 🔧 Development Tips

1. **Use Android Studio's Layout Inspector** to debug Compose UI
2. **Enable Compose Preview** for faster UI development
3. **Use the Compose Compiler** for better performance
4. **Follow Material Design 3 guidelines** for consistent UI

## 📚 Next Steps

- [ ] Set up authentication flow
- [ ] Implement payment processing
- [ ] Add user profile management
- [ ] Integrate with backend APIs
- [ ] Add unit and UI tests
- [ ] Set up CI/CD pipeline

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support, email support@flowpay.com or create an issue in this repository.
