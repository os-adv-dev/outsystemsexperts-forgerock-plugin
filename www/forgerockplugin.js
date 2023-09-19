var exec = require('cordova/exec');

module.exports = {
    start: function (success, error) {
        exec(success, error, 'ForgeRockPlugin', 'start');
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
    }

}
