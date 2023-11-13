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

                var packageName = result.widget.$.id;
                console.log("⭐️ packageName: " + packageName);
                var packagePath = packageName.replace(/\./g, '/');
                callback(packagePath);
            });
        });
    }

    function updateMainActivity(packagePath) {
        const platformRoot = path.join(rootdir, 'platforms/android/app/src/main/java', packagePath);
        const mainActivityPath = path.join(platformRoot, 'MainActivity.java');
        console.log("⭐️ mainActivityPath: " + mainActivityPath);

        if (fs.existsSync(mainActivityPath)) {
            let content = fs.readFileSync(mainActivityPath, 'utf-8');

            // This is the import statement that needs to be added
            const importStatement = 'import com.outsystems.experts.forgerockplugin.ForgeRockPlugin;\n';

            // Check if the import statement is already there to avoid duplication
            if (!content.includes(importStatement)) {
                // Add the import statement right after the package declaration
                content = content.replace(/(package [a-zA-Z0-9_.]+;)/, `$1\n${importStatement}`);

                fs.writeFileSync(mainActivityPath, content);
                console.log("✅ ForgeRockPlugin import statement added to MainActivity.java!");
            } else {
                console.log("ℹ️ ForgeRockPlugin import statement already exists in MainActivity.java.");
            }
        } else {
            console.error("❌ MainActivity.java not found!");
        }
    }

    // Retrieve the package path and update MainActivity.java
    getPackageName(function(packagePath) {
        updateMainActivity(packagePath);
    });
};
