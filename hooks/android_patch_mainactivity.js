const fs = require('fs');
const path = require('path');
const xml2js = require('xml2js'); // Make sure xml2js is installed

module.exports = function(context) {
    const rootdir = context.opts.projectRoot;
    const configXmlPath = path.join(rootdir, 'config.xml');

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
                // Convert the package name to a path by replacing dots with slashes
                var packagePath = packageName.replace(/\./g, '/');
                callback(packagePath);
            });
        });
    }

    // Function to update MainActivity.java with the new content
    function updateMainActivity(packagePath) {
        const platformRoot = path.join(rootdir, 'platforms/android/app/src/main/java', packagePath);
        const mainActivityPath = path.join(platformRoot, 'MainActivity.java');
        console.log("⭐️ mainActivityPath: " + mainActivityPath);

        if (fs.existsSync(mainActivityPath)) {
            let content = fs.readFileSync(mainActivityPath, 'utf-8');

            const targetContent = `
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handleIntent(intent);
}

private void handleIntent(Intent intent) {
    if (intent != null && ForgeRockPlugin.instance != null) {
        String senderId = intent.getStringExtra("senderId");

        if (senderId == null || senderId.isEmpty()) {
            Log.e("🚨 MainActivity", "Sender ID is null or empty");
            return;
        }

        Map<String, String> data = new HashMap<>();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                data.put(key, extras.getString(key));
            }
            RemoteMessage message = new RemoteMessage.Builder(senderId)
                    .setData(data)
                    .build();
            ForgeRockPlugin.instance.handleNotification(message);
        }
    }
}
`;

            if (!content.includes("handleIntent(Intent intent)")) {
                content = content.replace("import org.apache.cordova.*;", "import org.apache.cordova.*;\nimport android.util.Log;\nimport android.content.Intent;\nimport com.google.firebase.messaging.RemoteMessage;\nimport java.util.HashMap;\nimport java.util.Map;");
                content = content.replace("@Override", `${targetContent}\n\n\t@Override`);
                fs.writeFileSync(mainActivityPath, content);
                console.log("✅ MainActivity.java has been updated!");
            } else {
                console.log("🚨 MainActivity.java is already updated.");
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
