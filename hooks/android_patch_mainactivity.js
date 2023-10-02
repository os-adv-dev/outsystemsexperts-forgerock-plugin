const fs = require('fs');
const path = require('path');

module.exports = function(context) {
    const rootdir = context.opts.projectRoot;
    const platformRoot = path.join(rootdir, 'platforms/android/app/src/main/java/com/outsystems/experts/forgerocksample/');
    const mainActivityPath = path.join(platformRoot, 'MainActivity.java');

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
            content = content.replace("import org.apache.cordova.*;", "import org.apache.cordova.*;\nimport android.util.Log;\nimport android.content.Intent;\nimport com.google.firebase.messaging.RemoteMessage;\nimport com.outsystems.experts.forgerockplugin.ForgeRockPlugin;\nimport java.util.HashMap;\nimport java.util.Map;");
            content = content.replace("@Override", `${targetContent}\n\n\t@Override`);
            //content = content.replace("loadUrl(launchUrl);", `loadUrl(launchUrl);${targetContent}`);
            fs.writeFileSync(mainActivityPath, content);
            console.log("✅ MainActivity.java has been updated!");
        } else {
            console.log("🚨 MainActivity.java is already updated.");
        }
    } else {
        console.error("❌ MainActivity.java not found!");
    }
};
