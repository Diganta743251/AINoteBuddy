# Firebase Setup Guide for AINoteBuddy Collaborative Features

## Overview

This guide provides step-by-step instructions for setting up Firebase to support AINoteBuddy's real-time collaborative editing features while staying within the free tier limits.

## Firebase Services Used

- **Firebase Authentication** - User authentication for collaborative sessions
- **Firebase Realtime Database** - Real-time synchronization of collaborative operations
- **Firebase Analytics** - Usage tracking and performance monitoring

## Free Tier Limits (Monitor These)

| Service | Free Tier Limit | Monitoring Strategy |
|---------|----------------|-------------------|
| Realtime Database | 1GB storage, 10GB/month transfer | Set billing alerts at 80% |
| Authentication | 10,000 phone auths/month | Monitor monthly usage |
| Analytics | Unlimited | No cost concerns |

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Enter project name: `ainotebuddy-collaborative`
4. Enable Google Analytics (recommended)
5. Choose or create Analytics account
6. Click "Create project"

## Step 2: Configure Authentication

### Enable Authentication Providers

1. In Firebase Console, go to **Authentication** > **Sign-in method**
2. Enable the following providers:

#### Google Sign-In (Primary)
```
- Click "Google" provider
- Toggle "Enable"
- Add your app's SHA-1 fingerprint
- Download updated google-services.json
```

#### Email/Password (Fallback)
```
- Click "Email/Password" provider  
- Toggle "Enable"
- Leave "Email link" disabled for now
```

### Get SHA-1 Fingerprint

For debug builds:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

For release builds:
```bash
keytool -list -v -keystore path/to/your/release.keystore -alias your-key-alias
```

## Step 3: Configure Realtime Database

### Create Database

1. Go to **Realtime Database** in Firebase Console
2. Click "Create Database"
3. Choose location closest to your users
4. Start in **test mode** (we'll secure it next)

### Security Rules

Replace the default rules with these production-ready rules:

```json
{
  "rules": {
    // Sessions - users can read/write their own sessions
    "sessions": {
      "$sessionId": {
        ".read": "auth != null && (root.child('sessions').child($sessionId).child('participants').child(auth.uid).exists() || root.child('sessions').child($sessionId).child('ownerId').val() == auth.uid)",
        ".write": "auth != null && (root.child('sessions').child($sessionId).child('participants').child(auth.uid).exists() || root.child('sessions').child($sessionId).child('ownerId').val() == auth.uid)",
        ".validate": "newData.hasChildren(['sessionId', 'noteId', 'ownerId', 'participants', 'createdAt', 'lastActivity', 'isActive', 'permissions'])"
      }
    },
    
    // Operations - session participants can read/write
    "operations": {
      "$sessionId": {
        ".read": "auth != null && root.child('sessions').child($sessionId).child('participants').child(auth.uid).exists()",
        ".write": "auth != null && root.child('sessions').child($sessionId).child('participants').child(auth.uid).exists()",
        "$operationId": {
          ".validate": "newData.hasChildren(['operationId', 'userId', 'timestamp', 'version']) && newData.child('userId').val() == auth.uid"
        }
      }
    },
    
    // Presence - session participants can read/write
    "presence": {
      "$sessionId": {
        ".read": "auth != null && root.child('sessions').child($sessionId).child('participants').child(auth.uid).exists()",
        "$userId": {
          ".write": "auth != null && $userId == auth.uid",
          ".validate": "newData.hasChildren(['sessionId', 'userId', 'isActive', 'lastActivity']) && newData.child('userId').val() == auth.uid"
        }
      }
    },
    
    // Comments - session participants can read/write
    "comments": {
      "$sessionId": {
        ".read": "auth != null && root.child('sessions').child($sessionId).child('participants').child(auth.uid).exists()",
        ".write": "auth != null && root.child('sessions').child($sessionId).child('participants').child(auth.uid).exists()",
        "$commentId": {
          ".validate": "newData.hasChildren(['commentId', 'noteId', 'sessionId', 'userId', 'content', 'timestamp']) && newData.child('userId').val() == auth.uid"
        }
      }
    }
  }
}
```

### Database Structure

The database will have this structure:
```
ainotebuddy-collaborative/
├── sessions/
│   └── {sessionId}/
│       ├── sessionId: string
│       ├── noteId: string  
│       ├── ownerId: string
│       ├── participants: object
│       ├── createdAt: number
│       ├── lastActivity: number
│       ├── isActive: boolean
│       └── permissions: object
├── operations/
│   └── {sessionId}/
│       └── {operationId}/
│           ├── operationId: string
│           ├── userId: string
│           ├── timestamp: number
│           ├── version: number
│           └── [operation-specific fields]
├── presence/
│   └── {sessionId}/
│       └── {userId}/
│           ├── sessionId: string
│           ├── userId: string
│           ├── isActive: boolean
│           ├── lastActivity: number
│           ├── cursorPosition: object
│           └── isTyping: boolean
└── comments/
    └── {sessionId}/
        └── {commentId}/
            ├── commentId: string
            ├── noteId: string
            ├── sessionId: string
            ├── userId: string
            ├── content: string
            ├── position: object
            ├── timestamp: number
            └── replies: array
```

## Step 4: Add Firebase Configuration to App

### Download Configuration File

1. In Firebase Console, go to **Project Settings**
2. Click **Add app** > **Android**
3. Enter package name: `com.ainotebuddy.app`
4. Download `google-services.json`
5. Place in `app/` directory (replace existing if present)

### Verify Dependencies

Ensure these are in `app/build.gradle.kts`:
```kotlin
// Firebase for collaborative features
implementation("com.google.firebase:firebase-database-ktx:20.3.0")
implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
```

## Step 5: Set Up Monitoring and Alerts

### Usage Monitoring

1. Go to **Usage and billing** in Firebase Console
2. Set up budget alerts:
   - Realtime Database: Alert at 8GB transfer (80% of 10GB limit)
   - Storage: Alert at 800MB (80% of 1GB limit)

### Performance Monitoring

1. Enable **Performance Monitoring** in Firebase Console
2. Add to `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-perf-ktx:20.4.1")
```

### Crashlytics (Optional but Recommended)

1. Enable **Crashlytics** in Firebase Console
2. Add to `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.1")
```

## Step 6: Test Configuration

### Local Testing

1. Run the app in debug mode
2. Try creating a collaborative session
3. Check Firebase Console for data appearing in Realtime Database
4. Verify authentication is working

### Production Testing Checklist

- [ ] Authentication works with Google Sign-In
- [ ] Realtime Database rules prevent unauthorized access
- [ ] Collaborative sessions create and sync properly
- [ ] Presence tracking works across multiple devices
- [ ] Comments system functions correctly
- [ ] Performance monitoring shows acceptable latency

## Step 7: Production Deployment

### Security Checklist

- [ ] Database rules are restrictive (no public read/write)
- [ ] Authentication is required for all collaborative features
- [ ] API keys are not exposed in client code
- [ ] google-services.json is properly configured for release

### Performance Optimization

- [ ] Enable database persistence for offline support
- [ ] Implement connection pooling
- [ ] Set up proper indexing for queries
- [ ] Monitor bandwidth usage

### Monitoring Setup

- [ ] Billing alerts configured
- [ ] Performance monitoring enabled
- [ ] Error tracking with Crashlytics
- [ ] Usage analytics dashboard

## Cost Management Strategies

### Stay Within Free Tier

1. **Limit Session Duration**: Auto-cleanup inactive sessions after 2 hours
2. **Optimize Data Transfer**: Only sync necessary data, use compression
3. **User Limits**: Consider limiting concurrent collaborative sessions per user
4. **Cleanup Strategy**: Regular cleanup of old operations and presence data

### Scaling Considerations

If you exceed free tier limits:
1. **Paid Plan**: Firebase Blaze plan with pay-as-you-go pricing
2. **Alternative**: Consider self-hosted solutions or other real-time databases
3. **Optimization**: Implement more aggressive data cleanup and compression

## Troubleshooting

### Common Issues

1. **Authentication Fails**
   - Verify SHA-1 fingerprint is correct
   - Check google-services.json is latest version
   - Ensure package name matches exactly

2. **Database Permission Denied**
   - Verify security rules are correctly configured
   - Check user is authenticated before accessing data
   - Ensure user is participant in session

3. **Real-time Updates Not Working**
   - Check internet connection
   - Verify Firebase project configuration
   - Test with Firebase Console directly

### Debug Commands

```kotlin
// Enable Firebase debug logging
FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)

// Test authentication state
FirebaseAuth.getInstance().addAuthStateListener { auth ->
    Log.d("Firebase", "Auth state: ${auth.currentUser?.uid}")
}

// Test database connection
FirebaseDatabase.getInstance().reference.child(".info/connected")
    .addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val connected = snapshot.getValue(Boolean::class.java) ?: false
            Log.d("Firebase", "Connected: $connected")
        }
        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Connection error: ${error.message}")
        }
    })
```

## Support and Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firebase Console](https://console.firebase.google.com/)
- [Firebase Support](https://firebase.google.com/support)
- [Pricing Calculator](https://firebase.google.com/pricing)

---

*This setup ensures AINoteBuddy's collaborative features work reliably while staying cost-effective within Firebase's free tier limits.*
