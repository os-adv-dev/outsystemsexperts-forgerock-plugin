var exec = require('cordova/exec');

module.exports = {
//    start: function (success, error) {
//        exec(success, error, 'ForgeRockPlugin', 'start');
//    },
    start: function (transactionalApiURLsuccess, success, error) {
        exec(success, error, 'ForgeRockPlugin', 'start',[transactionalApiURLsuccess]);
    },
    createMechanismFromUri: function(uri, success, error){
        exec(success, error, 'ForgeRockPlugin', 'createMechanismFromUri', [uri]);
    },
    registerForRemoteNotifications: function(fcmToken, success, error){
        exec(success, error, 'ForgeRockPlugin', 'registerForRemoteNotifications', [fcmToken]);
    },
    acceptAction: function(success, error){
        exec(success, error, 'ForgeRockPlugin', 'acceptAction');
    },
    denyAction: function(success, error){
        exec(success, error, 'ForgeRockPlugin', 'denyAction');
    },
    didReceivePushNotificationSetCallback: function(success, error){
        exec(success, error, 'ForgeRockPlugin', 'didReceivePushNotificationSetCallback');
    },
    setNativeNotification: function(isSet, success, error){
        exec(success, error, 'ForgeRockPlugin', 'setNativeNotification', [isSet]);
    },
    setNativeNotificationTitleMessage: function(title, message, success, error){
        exec(success, error, 'ForgeRockPlugin', 'setNativeNotificationTitleMessage', [title, message]);
    },
    removeAccount: function(account, success, error){
        exec(success, error, 'ForgeRockPlugin', 'removeAccount', [account]);
    }
}
