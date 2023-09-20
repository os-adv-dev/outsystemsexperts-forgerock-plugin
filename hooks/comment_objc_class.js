const fs = require('fs');
const path = require('path');

function getProjectName() {
    var config = fs.readFileSync('config.xml').toString();
    var parseString = require('xml2js').parseString;
    var name;
    parseString(config, function (err, result) {
        name = result.widget.name.toString();
        const r = /\B\s+|\s+\B/g;  //Removes trailing and leading spaces
        name = name.replace(r, '');
    });
    return name || null;
}

function commentObjCFile(context) {
    return new Promise((resolve, reject) => {
        const APP_ROOT = context.opts.projectRoot;
        const PROJECT_NAME = getProjectName();
        const PLUGIN_DIR = path.join(APP_ROOT, 'platforms','ios', PROJECT_NAME,'Plugins','com.outsystems.firebase.cloudmessaging');
        const OBJC_FILE_NAME = 'AppDelegate+OSFirebaseCloudMessaging.m';
        const HEADER_FILE_NAME = 'AppDelegate+OSFirebaseCloudMessaging.h';

        let filesCommented = 0;

        try {
            [OBJC_FILE_NAME, HEADER_FILE_NAME].forEach(fileName => {
                const filePath = path.join(PLUGIN_DIR, fileName);
                console.log("⭐️ filePath: " + filePath);
                if (fs.existsSync(filePath)) {
                    const data = fs.readFileSync(filePath, 'utf8');
                    const commentedData = data.split('\n').map(line => `// ${line}`).join('\n');
                    console.log("⭐️ commentedData: " + "\n\n" + commentedData + "\n\n");
                    fs.writeFileSync(filePath, commentedData);
                    filesCommented++;
                }
            });

            if (filesCommented === 2) {
                resolve();
            } else {
                reject(new Error('One or both Objective-C files not found.'));
            }

        } catch (error) {
            reject(error);
        }
    });
}

module.exports = function(context) {
    console.log("👉 Running hook for commenting Firebase AppDelegate Swizzling Category");
    return commentObjCFile(context)
        .then(() => {
            console.log('✅ Objective-C file and header commented successfully.');
        })
        .catch(error => {
            console.error('❌ Error:', error.message);
        });
};
