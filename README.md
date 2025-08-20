# Manga Translation Tool - Architecture Documentation

## Overview

This application has been refactored from a singleton-based architecture to a clean, maintainable architecture using
Koin DI, ViewModels, and proper separation of concerns.

## Architecture Layers

### 1. Presentation Layer (`app/`)

- **Composables**: UI components that only handle rendering and user interactions
- **Navigation**: Centralized navigation using `NavigationController`
- **Error Display**: Centralized error handling with `ErrorHandler`

### 2. Domain Layer (`*/domain/`)

- **ViewModels**: Handle UI state and business logic
- **Use Cases**: Encapsulate business operations (where applicable)
- **State Management**: Reactive state using Compose's `mutableStateOf`

### 3. Data Layer (`*/data/`)

- **Repositories**: Abstract data access with clean interfaces
- **Models**: Data classes for domain entities
- **Network**: gRPC communication for OCR and translation services

### 4. Core (`core/`)

- **DI**: Dependency injection setup with Koin
- **Base Classes**: Common base classes for ViewModels and Repositories
- **Navigation**: Navigation controller and destination definitions
- **Error Handling**: Centralized error management
- **Utils**: Shared utilities

## Module Structure

```
src/main/kotlin/
├── core/
│   ├── di/                 # Dependency injection modules
│   ├── base/              # Base classes (ViewModel, Repository, etc.)
│   ├── navigation/        # Navigation controller and destinations
│   ├── error/             # Error handling system
│   └── utils/             # Shared utilities
├── settings/
│   ├── data/              # Settings repository and models
│   ├── domain/            # Settings ViewModel
│   └── di/                # Settings DI module
├── fonts/
│   ├── data/              # Font repository and models
│   ├── domain/            # Font ViewModel and resolver
│   └── di/                # Font DI module
├── project/
│   ├── data/              # Project repository and models
│   ├── domain/            # Project ViewModels
│   └── di/                # Project DI module
├── translation/
│   ├── data/              # OCR/Translation repositories and models
│   ├── domain/            # Translation ViewModels
│   └── di/                # Translation DI module
└── app/                   # UI layer (legacy structure being cleaned up)
```

## Dependency Injection

The application uses Koin for dependency injection. All modules are defined in their respective `di/` packages and
combined in `core/di/appModule.kt`.

### Key DI Principles:

- **Repositories**: Singletons (shared state)
- **ViewModels**: Factories (new instance per injection)
- **Utilities**: Singletons where appropriate

### DI Setup:

```kotlin
// In Main.kt
val app = MangaTranslationApplication()
app.initialize() // Sets up Koin with all modules
```

## ViewModel Architecture

All ViewModels extend `BaseViewModel` which provides:

- Coroutine scope management
- Common loading/error state
- Integration with centralized error handler

### Example ViewModel Pattern:

```kotlin
class ExampleViewModel(
  private val repository: ExampleRepository
) : BaseViewModel() {

  private val _data = mutableStateOf<Data?>(null)
  val data: State<Data?> = _data

  fun loadData() {
    viewModelScope.launch {
      setLoading(true)
      repository.getData()
        .onSuccess { _data.value = it }
        .onFailure { setError("Failed to load data: ${it.message}") }
      setLoading(false)
    }
  }
}
```

## Repository Pattern

Repositories provide clean abstractions over data sources:

```kotlin
interface ExampleRepository {
  suspend fun getData(): Result<Data>
  suspend fun saveData(data: Data): Result<Unit>
}

class ExampleRepositoryImpl : ExampleRepository, Repository {
  override suspend fun getData(): Result<Data> = safeCall {
    // Implementation
  }
}
```

## Error Handling

Centralized error handling using `ErrorHandler`:

- **UI Display**: Modern snackbar-style notifications
- **Console Logging**: Structured logging with timestamps
- **Error Types**: Error, Warning, Info, Success
- **Auto-dismiss**: Configurable duration

## Navigation

Navigation is managed through `NavigationController`:

- **Type-safe**: Using sealed class `NavigationDestination`
- **Centralized**: Single source of truth for navigation state
- **Reactive**: Compose-friendly state management

## Development Setup

### Prerequisites

- Kotlin 2.0.20
- Compose Desktop 1.6.10
- Java 17+

### Running the Application

```bash
./gradlew run
```

### Key Dependencies

- **Koin**: `io.insert-koin:koin-core:3.5.0`
- **Compose Desktop**: `org.jetbrains.compose`
- **Serialization**: `kotlinx-serialization-json`
- **gRPC**: For OCR and translation services

## Breaking Changes from Legacy Architecture

1. **Singleton Removal**: All singletons replaced with DI
2. **State Management**: Moved from global state to ViewModel-scoped state
3. **Navigation**: Replaced enum-based navigation with type-safe navigation
4. **Error Handling**: Centralized instead of scattered throughout UI

## Migration Notes

### Deprecated Services (Still in use, being phased out):

- `BatchService` → Use `ImageDataRepository`
- `ImageDataService` → Use `ImageDataRepository`
- `OCRService` → Use `WorkDataRepository`
- `TextDataService` → Use `TextDataRepository`

## Future Improvements

1. **Testing**: Add unit tests for ViewModels and Repositories
2. **Performance**: Optimize image processing and memory usage
3. **UI**: Complete migration of remaining Composables
4. **Features**: Add more robust error recovery mechanisms

## Troubleshooting

### Common Issues:

1. **DI Errors**: Ensure all modules are included in `appModule`
2. **Navigation Issues**: Check `NavigationDestination` enum coverage
3. **State Issues**: Verify ViewModel lifecycle management

### Debugging:

- Check console output for error logs
- Use Koin's built-in dependency resolution logs
- Monitor error handler for centralized error information
