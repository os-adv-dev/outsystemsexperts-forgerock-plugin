const fs = require('fs');
const path = require('path');

function commentObjCFile(context) {
    return new Promise((resolve, reject) => {
        const APP_ROOT = context.opts.projectRoot;
        const PLUGIN_DIR = path.join(APP_ROOT, 'plugins','com.outsystems.firebase.cloudmessaging', 'src', 'ios');
        const OBJC_FILE_NAME = 'AppDelegate+OSFirebaseCloudMessaging.m';
        const HEADER_FILE_NAME = 'AppDelegate+OSFirebaseCloudMessaging.h';

        try {
            fs.readdirSync(PLUGIN_DIR, { withFileTypes: true })
              .filter(dirent => dirent.isDirectory())
              .forEach(dirent => {
                const pluginPath = path.join(PLUGIN_DIR, dirent.name);
                
                [OBJC_FILE_NAME, HEADER_FILE_NAME].forEach(fileName => {
                    const filePath = path.join(pluginPath, fileName);
                    if (fs.existsSync(filePath)) {
                        const data = fs.readFileSync(filePath, 'utf8');
                        const commentedData = data.split('\n').map(line => `// ${line}`).join('\n');
                        fs.writeFileSync(filePath, commentedData);
                    }
                });
              });
            resolve();
        } catch (error) {
            reject(error);
        }
    });
}

module.exports = function(context) {
    return commentObjCFile(context)
        .then(() => {
            console.log('✅ Objective-C file and header commented successfully.');
        })
        .catch(error => {
            console.error('❌ Error commenting Objective-C file and header:', error);
        });
};
