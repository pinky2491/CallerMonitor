# CallerMonitor

A comprehensive Android application for monitoring and managing unknown incoming calls with automatic notification and SMS alert capabilities.

## Features

### 🔔 **Call Monitoring**
- **Automatic Detection**: Monitors incoming calls from unknown numbers
- **Real-time Notifications**: Instant alerts when unknown callers are detected
- **SMS Alerts**: Automatically sends SMS messages to unknown callers
- **Background Service**: Persistent monitoring with foreground service

### 📱 **Call Management**
- **Unknown Call Logs**: View and manage all unknown incoming calls
- **Call Details**: Tap on any call log to view detailed information
- **Action Buttons**: Call, SMS, Block, and Report as Spam options
- **Pull-to-Refresh**: Swipe down to refresh and import new call logs

### 🛡️ **Security & Privacy**
- **Permission Management**: Comprehensive permission handling
- **Contact Verification**: Checks against device contacts
- **Spam Detection**: Identifies and reports spam numbers
- **Blocking Support**: System-level and local blocking options

### 📊 **Data Management**
- **Local Database**: Room database for persistent storage
- **Auto-Import**: Automatically imports unknown calls from device call log
- **Call History**: Complete call history with timestamps
- **Data Persistence**: Survives app restarts and device reboots

## Technical Architecture

### **Core Components**
- **CallReceiver**: Broadcast receiver for phone state changes
- **CallMonitoringService**: Foreground service for persistent monitoring
- **NotifyCallerService**: Intent service for SMS sending
- **CallViewModel**: ViewModel for UI data management
- **CallerRepository**: Repository pattern for data operations

### **Database Schema**
- **UnknownCallLog**: Stores unknown incoming call details
- **BlockedNumber**: Manages blocked phone numbers
- **ReportedSpamNumber**: Tracks reported spam numbers

### **Permissions Required**
- `READ_PHONE_STATE`: Monitor incoming calls
- `READ_CONTACTS`: Check against contact list
- `SEND_SMS`: Send alert messages
- `READ_CALL_LOG`: Import call history
- `POST_NOTIFICATIONS`: Display notifications
- `FOREGROUND_SERVICE`: Background monitoring

## Installation & Setup

### **Prerequisites**
- Android 6.0 (API 23) or higher
- Required permissions granted at runtime
- SMS capability for alert functionality

### **Build Instructions**
```bash
# Clone the repository
git clone <repository-url>
cd CallerMonitor

# Build the project
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### **First Run Setup**
1. **Grant Permissions**: Allow all requested permissions
2. **Test Notification**: Use the test button to verify functionality
3. **View Logs**: Navigate to unknown call logs screen
4. **Auto-Import**: Call logs are automatically imported on first visit

## Usage Guide

### **Main Screen**
- **View Unknown Call Logs**: Access the call history screen
- **Test Notification**: Verify notification functionality
- **Status Display**: Shows current monitoring status

### **Unknown Call Logs Screen**
- **Call List**: Displays all unknown incoming calls
- **Pull-to-Refresh**: Swipe down to refresh data
- **Call Details**: Tap any call for detailed view
- **Action Options**: Call, SMS, Block, Report as Spam

### **Call Details**
- **Phone Number**: Display with formatting
- **Timestamp**: Date and time of call
- **Call Button**: Direct dial to the number
- **SMS Button**: Open SMS app with number
- **Block Button**: Block the number
- **Report Spam**: Mark as spam number

## Development

### **Technology Stack**
- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Database**: Room with SQLite
- **UI**: Material Design 3 components
- **Coroutines**: Asynchronous operations
- **LiveData**: Reactive UI updates

### **Key Dependencies**
```gradle
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
implementation "androidx.room:room-runtime:2.6.0"
implementation "com.google.dagger:hilt-android:2.48"
implementation "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
implementation "com.google.android.material:material:1.9.0"
```

### **Project Structure**
```
app/src/main/java/com/rvp/callermonitor/
├── di/                    # Dependency injection
├── model/                 # Data models and database
├── receiver/              # Broadcast receivers
├── repository/            # Data repository
├── ui/                    # Activities and adapters
└── viewmodel/             # ViewModels
```

## Troubleshooting

### **Common Issues**
1. **Notifications Not Appearing**: Check notification permissions
2. **SMS Not Sending**: Verify SMS permissions and SIM state
3. **No Call Detection**: Ensure phone state permission is granted
4. **Empty Call Logs**: Check call log permission and import functionality

### **Debug Features**
- **Extensive Logging**: Detailed logs for troubleshooting
- **Test Buttons**: Verify functionality without real calls
- **Permission Checks**: Runtime permission validation
- **Service Status**: Monitor background service state

## Contributing

### **Development Guidelines**
- Follow Kotlin coding conventions
- Use Material Design 3 components
- Implement proper error handling
- Add comprehensive logging
- Test on multiple Android versions

### **Testing**
- Test on Android 6.0+ devices
- Verify all permissions work correctly
- Test with real incoming calls
- Validate SMS sending functionality

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and feature requests, please create an issue in the repository.

---

**Note**: This app requires appropriate permissions and may not work on all devices due to manufacturer-specific restrictions on call monitoring and SMS sending capabilities. 