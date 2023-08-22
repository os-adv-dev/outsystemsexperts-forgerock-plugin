var exec = require('cordova/exec');

module.exports = {
    start: function (success, error) {
        exec(success, error, 'ForgeRockPlugin', 'start');
    },
    createMechanismFromUri: function(success, error){
        exec(success, error, 'ForgeRockPlugin', 'createMechanismFromUri');
    }


}
