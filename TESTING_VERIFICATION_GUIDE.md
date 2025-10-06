# Testing & Verification Guide - AINoteBuddy

## 🧪 Post-Fix Verification Steps

### 1. **Build Verification**

#### Clean Build
```powershell
# Clean previous builds
.\gradlew clean

# Build debug variant
.\gradlew assembleDebug

# Expected: SUCCESS without errors
```

#### Check for Common Issues
```powershell
# Run lint checks
.\gradlew lintDebug

# Check for compilation errors
.\gradlew compileDebugKotlin
```

---

### 2. **Runtime Verification**

#### Install and Launch
```powershell
# Install on connected device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.ainotebuddy.app/.MainActivity
```

#### Monitor Logs
```powershell
# Filter for our app logs
adb logcat -s AINoteBuddyApp

# Expected output:
# AINoteBuddyApp: AINoteBuddy Application starting...
# AINoteBuddyApp: Theme setup complete
# AINoteBuddyApp: Background embedding worker scheduled
```

#### Check for Crashes
```powershell
# Monitor for crashes
adb logcat | grep -i "FATAL\|crash\|exception"

# Should be clean - no crashes on startup
```

---

### 3. **Feature Testing Checklist**

#### Core Features ✅

- [ ] **App Startup**
  - App launches without crash
  - Hilt dependency injection works
  - Database initializes properly
  - No memory warnings in Logcat

- [ ] **Note Management**
  - [ ] Create new note
  - [ ] Edit existing note
  - [ ] Delete note
  - [ ] Pin/Unpin note
  - [ ] Favorite/Unfavorite note
  - [ ] Archive note

- [ ] **Search & Filter**
  - [ ] Search notes by title
  - [ ] Search by content
  - [ ] Filter by category
  - [ ] Filter by tags
  - [ ] View pinned notes
  - [ ] View favorites

- [ ] **Navigation**
  - [ ] Dashboard tab
  - [ ] Notes tab
  - [ ] Categories tab
  - [ ] Settings tab
  - [ ] Back navigation works

- [ ] **Settings**
  - [ ] Open settings
  - [ ] Theme changes apply
  - [ ] Preferences persist
  - [ ] AI settings accessible

---

### 4. **Performance Testing**

#### Memory Usage
```powershell
# Check memory usage
adb shell dumpsys meminfo com.ainotebuddy.app

# Monitor for memory leaks
# Expected: Stable memory usage, no continuous growth
```

#### Database Performance
```kotlin
// Test query performance in debug mode
// Add timing logs to NoteRepository methods

suspend fun getAllNotes(): Flow<List<NoteEntity>> {
    val startTime = System.currentTimeMillis()
    val result = noteDao.getAllNotes()
    Log.d("Performance", "getAllNotes took ${System.currentTimeMillis() - startTime}ms")
    return result
}
```

#### Expected Results:
- Initial query: < 50ms for 1000 notes
- Filtered queries: < 30ms (with indices)
- Search queries: < 100ms

---

### 5. **Hilt Dependency Injection Verification**

#### Test DI Works Correctly
```kotlin
// In MainActivity, verify ViewModel is injected
@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    // If this doesn't crash, DI is working ✅
    
    LaunchedEffect(Unit) {
        Log.d("DI_Test", "ViewModel injected: ${viewModel.javaClass.simpleName}")
    }
}
```

#### Check Logcat Output
```
D/DI_Test: ViewModel injected: MainViewModel
```

---

### 6. **Repository Pattern Verification**

#### Test Repository Injection
```kotlin
// Add temporary test in MainActivity
LaunchedEffect(viewModel) {
    try {
        val notes = viewModel.allNotes.first()
        Log.d("RepoTest", "Repository working: ${notes.size} notes loaded")
    } catch (e: Exception) {
        Log.e("RepoTest", "Repository error: ${e.message}")
    }
}
```

#### Expected Output
```
D/RepoTest: Repository working: X notes loaded
```

---

### 7. **Database Migration Verification**

#### Test Migration Success
```kotlin
// Check database version after update
adb shell run-as com.ainotebuddy.app
cd databases
sqlite3 notes.db "PRAGMA user_version;"
# Expected: 10
```

#### Verify Indices Created
```sql
-- Check indices in database
SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='notes';

-- Expected indices:
-- index_notes_category
-- index_notes_isPinned_isFavorite
-- index_notes_createdAt
-- index_notes_updatedAt
-- index_notes_isDeleted_isArchived
-- index_notes_folderId
-- index_notes_isInVault
```

---

### 8. **Lifecycle Testing**

#### Activity Lifecycle
```kotlin
// Test rotation and process death
// 1. Open app
// 2. Create a note
// 3. Rotate device (Ctrl+F11/F12 in emulator)
// 4. Note should still be visible ✅
```

#### Background/Foreground
```powershell
# Send app to background
adb shell input keyevent KEYCODE_HOME

# Bring back to foreground
adb shell am start -n com.ainotebuddy.app/.MainActivity

# Check state is preserved ✅
```

---

### 9. **Memory Leak Detection**

#### Using LeakCanary (Recommended)
```kotlin
// Add to app/build.gradle.kts
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

#### Manual Memory Profiling
1. Open Android Studio Profiler
2. Select Memory tab
3. Perform actions (create notes, navigate)
4. Force GC
5. Check for retained objects

**Expected**: No memory leaks, stable memory usage

---

### 10. **Edge Cases Testing**

#### Database Edge Cases
- [ ] Empty database (fresh install)
- [ ] Database with 1000+ notes
- [ ] Search with special characters
- [ ] Very long note content (10,000+ words)
- [ ] Unicode/emoji in notes
- [ ] Concurrent database operations

#### UI Edge Cases
- [ ] No internet connection
- [ ] Small screen devices (phones)
- [ ] Large screen devices (tablets)
- [ ] Different font sizes (accessibility)
- [ ] Dark mode / Light mode
- [ ] System UI changes (split screen)

#### Permission Edge Cases
- [ ] Camera permission denied
- [ ] Microphone permission denied
- [ ] Storage permission denied
- [ ] Location permission denied

---

## 🐛 Known Issues & Workarounds

### Issue 1: First Launch Delay
**Symptom**: App takes 2-3 seconds to launch on first run  
**Cause**: Database creation and WorkManager initialization  
**Status**: Expected behavior  
**Workaround**: None needed

### Issue 2: LaunchedEffect Recompositions
**Symptom**: Some screens have `LaunchedEffect(Unit)` causing unnecessary work  
**Status**: Non-critical, scheduled for fix  
**Workaround**: Replace with proper keys in next update

---

## ✅ Acceptance Criteria

### Critical (Must Pass)
- ✅ App starts without crashing
- ✅ No DI errors in Logcat
- ✅ Notes can be created and retrieved
- ✅ No memory leaks detected
- ✅ Navigation works correctly

### Important (Should Pass)
- ✅ Search performs well (< 100ms)
- ✅ UI is responsive (60fps)
- ✅ Rotation preserves state
- ✅ Background/foreground works
- ✅ Settings persist correctly

### Nice to Have
- ⚠️ All TODO items addressed
- ⚠️ 70%+ test coverage
- ⚠️ No lint warnings
- ⚠️ Performance optimizations applied

---

## 📊 Performance Benchmarks

### Target Metrics

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Cold Start | < 2s | TBD | ⏳ |
| Note Creation | < 100ms | TBD | ⏳ |
| Search (1000 notes) | < 100ms | TBD | ⏳ |
| Memory Usage | < 100MB | TBD | ⏳ |
| Frame Rate | 60fps | TBD | ⏳ |
| APK Size | < 50MB | ~35MB | ✅ |

---

## 🔍 Debug Commands

### Useful ADB Commands
```powershell
# Clear app data (reset)
adb shell pm clear com.ainotebuddy.app

# Force stop app
adb shell am force-stop com.ainotebuddy.app

# Check app version
adb shell dumpsys package com.ainotebuddy.app | grep versionName

# Export database for inspection
adb exec-out run-as com.ainotebuddy.app cat databases/notes.db > notes.db

# Check shared preferences
adb shell run-as com.ainotebuddy.app cat shared_prefs/com.ainotebuddy.app_preferences.xml
```

### Gradle Commands
```powershell
# Check dependencies
.\gradlew app:dependencies

# Run all checks
.\gradlew check

# Generate dependency report
.\gradlew app:dependencies > dependencies.txt

# Check for updates
.\gradlew dependencyUpdates
```

---

## 🚨 Regression Testing

### After Each Code Change

1. **Smoke Test** (5 min)
   - App launches
   - Create a note
   - Search works
   - Settings open

2. **Core Flows** (15 min)
   - Complete note CRUD operations
   - Test all navigation paths
   - Verify data persistence

3. **Edge Cases** (30 min)
   - Test with edge data
   - Test permissions
   - Test offline mode

---

## 📝 Test Reports

### Create Test Report Template
```markdown
## Test Report - [Date]

**Tester**: [Name]
**Build**: [Version]
**Device**: [Model & Android Version]

### Test Results
- [ ] Build: PASS/FAIL
- [ ] Startup: PASS/FAIL
- [ ] Core Features: PASS/FAIL
- [ ] Performance: PASS/FAIL
- [ ] Memory: PASS/FAIL

### Issues Found
1. [Issue description]
2. [Issue description]

### Notes
[Any additional observations]
```

---

## ✨ Success Indicators

### Green Signals ✅
- No crashes in first 5 minutes
- Smooth animations (no jank)
- Quick response times (< 100ms)
- Stable memory usage
- Clean Logcat (no errors)

### Red Signals 🔴
- App crashes on startup
- ANR (Application Not Responding)
- Memory leaks detected
- Database errors in Logcat
- UI freezing or stuttering

---

## 🎯 Final Checklist

Before marking review complete:

- [x] Hilt DI working correctly
- [x] ViewModels properly injected
- [x] Repository pattern consolidated
- [x] Database indices added
- [x] Build configuration verified
- [x] Database version incremented
- [ ] Manual testing completed
- [ ] Performance profiled
- [ ] Memory leaks checked
- [ ] Edge cases tested
- [ ] Documentation updated

---

*Testing Guide prepared by Cascade AI Assistant*
*Last Updated: 2025-10-05*
