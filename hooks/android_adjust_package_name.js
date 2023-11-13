module.exports = function (context) {
    var fs = require('fs');
    var path = require('path');
    var xml2js = require('xml2js'); 

    // Retrieve the root directory of the Cordova project from the context supplied by Cordova
    var rootdir = context.opts.projectRoot;
    console.log("⭐️ Project root directory: " + rootdir);

    // Path to config.xml
    var configXmlPath = path.join(rootdir, 'config.xml');
    console.log("⭐️ Path to config.xml: " + configXmlPath);

    // Path to the ForgeRockPlugin.java file
    var targetFilePath = path.join(rootdir, 'platforms/android/app/src/main/java/com/outsystems/experts/forgerockplugin/ForgeRockPlugin.java');
    console.log("⭐️ Path to ForgeRockPlugin.java: " + targetFilePath);

    // Helper function to parse config.xml and extract the package name
    function getPackageName(callback) {
        fs.readFile(configXmlPath, 'utf8', function (err, data) {
            if (err) {
                throw new Error('Error reading config.xml: ' + err);
            }

            xml2js.parseString(data, function (err, result) {
                if (err) {
                    throw new Error('Error parsing config.xml: ' + err);
                }

                var packageName = result.widget.$.id;
                console.log("⭐️ packageName: " + packageName);
                callback(packageName);
            });
        });
    }

    // Function to update the import statement in ForgeRockPlugin.java
    function updateImportStatement(packageName) {
        fs.readFile(targetFilePath, 'utf8', function (err, data) {
            if (err) {
                throw new Error('Error reading ForgeRockPlugin.java: ' + err);
            }

            // Define the pattern to find the import statement
            var importPattern = /(import\s+)(com\.outsystems\.experts\.forgerocksample\.)(MainActivity;)/;
            var replacement = '$1' + packageName + '.$3';
            console.log("⭐️ Replacement import statement: " + replacement);

            // Replace the import statement with the new package name
            var modifiedData = data.replace(importPattern, replacement);

            // Write the new content back to the file
            fs.writeFile(targetFilePath, modifiedData, 'utf8', function (err) {
                if (err) throw new Error('Error writing ForgeRockPlugin.java: ' + err);

                console.log("⭐️ ForgeRockPlugin.java has been updated with the new package name.");
            });
        });
    }

    // Retrieve the package name and update the import statement
    getPackageName(function(packageName) {
        updateImportStatement(packageName);
    });
};
