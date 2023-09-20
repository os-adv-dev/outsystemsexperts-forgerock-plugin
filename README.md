# OutSystems ForgeRockPlugin for Cordova

A Cordova plugin that provides integration with ForgeRock services.

## Installation

```bash
cordova plugin add https://github.com/os-adv-dev/outsystemsexperts-forgerock-plugin.git
```

## Usage

The ForgeRockPlugin provides several methods to interact with ForgeRock services:

### 1. Start the ForgeRock service

```javascript
cordova.plugins.outsystemsexperts.forgerockplugin.start(successCallback, errorCallback);
```

### 2. Create a mechanism from a URI

```javascript
cordova.plugins.outsystemsexperts.forgerockplugin.createMechanismFromUri(uri, successCallback, errorCallback);
```

### 3. Register for remote notifications

```javascript
cordova.plugins.outsystemsexperts.forgerockplugin.registerForRemoteNotifications(fcmToken, successCallback, errorCallback);
```

### 4. Accept action

```javascript
cordova.plugins.outsystemsexperts.forgerockplugin.acceptAction(successCallback, errorCallback);
```

### 5. Deny action

```javascript
cordova.plugins.outsystemsexperts.forgerockplugin.denyAction(successCallback, errorCallback);
```

### 6. Set a callback for push notifications

```javascript
cordova.plugins.outsystemsexperts.forgerockplugin.didReceivePushNotificationSetCallback(successCallback, errorCallback);
```

## Dependencies

- [com.outsystems.firebase.cloudmessaging](https://github.com/OutSystems/cordova-outsystems-firebase-cloud-messaging.git#2.0.0)

## Platforms Supported

- Android
- iOS

## Authors

- [André Gonçalves](https://github.com/agoncalvesos) - OutSystems
- [André Grillo](https://github.com/andregrillo) - OutSystems


## License

This project is licensed under the MIT License.
