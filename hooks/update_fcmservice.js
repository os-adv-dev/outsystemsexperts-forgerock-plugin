const fs = require('fs');
const path = require('path');
const xml2js = require('xml2js'); // Make sure xml2js is installed

module.exports = function(context) {
    const rootdir = context.opts.projectRoot;
    const configXmlPath = path.join(rootdir, 'config.xml');

    function getPackageName(callback) {
        fs.readFile(configXmlPath, 'utf8', function (err, data) {
            if (err) {
                throw new Error('Error reading config.xml: ' + err);
            }

            xml2js.parseString(data, function (err, result) {
                if (err) {
                    throw new Error('Error parsing config.xml: ' + err);
                }

                const packageName = result.widget.$.id;
                console.log("⭐️ Current package name: " + packageName);
                callback(packageName);
            });
        });
    }

    function updateFcmService(packageName) {
        const platformRoot = path.join(rootdir, 'platforms/android/app/src/main/java');
        const oldPackagePath = 'com/outsystems/experts/forgerocksample';
        const newPackagePath = packageName.replace(/\./g, '/');
        const fcmServicePath = path.join(platformRoot, "com/outsystems/experts/forgerockplugin", 'FcmService.java');
        console.log("⭐️ FcmService.java path: " + fcmServicePath);

        if (fs.existsSync(fcmServicePath)) {
            let content = fs.readFileSync(fcmServicePath, 'utf-8');

            // Replace old package name with the new one
            const oldPackageName = 'com.outsystems.experts.forgerocksample';
            content = content.replaceAll(oldPackageName, packageName);

            fs.writeFileSync(fcmServicePath, content);
            console.log("✅ FcmService.java has been updated with the current package name!");
        } else {
            console.error("❌ FcmService.java not found at the expected path!");
        }
    }

    // Retrieve the current package name and update FcmService.java
    getPackageName(function(packageName) {
        updateFcmService(packageName);
    });
};
