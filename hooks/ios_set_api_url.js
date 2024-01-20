var fs = require('fs'), path = require('path');

module.exports = function(context) {
    var fileToPatch = path.join(context.opts.projectRoot, "plugins", "outsystems-experts-plugin-forgerockplugin", "src", "ios", "ForgeRockPlugin.swift");
    console.log("✅ fileToPatch: " + fileToPatch);    
    if (fs.existsSync(fileToPatch)) {
     
      fs.readFile(fileToPatch, 'utf8', function (err,data) {
        
        if (err) {
          throw new Error('🚨 Unable to read ' + fileToPatch + ' - Error: ' + err);
        }

        const args = process.argv;
        var apiUrl = "";

        var localizationStringsJSON;
        for (const arg of args) {  
          if (arg.includes('API_URL')){
            var stringArray = arg.split("=");
            apiUrl = stringArray.slice(-1).pop();
          }
        }
        
        if (!(apiUrl === "" || apiUrl === null)) {
          var result = data;
          var shouldBeSaved = false;

          if (!data.includes(apiUrl)){
            shouldBeSaved = true;
            result = data.replace(/API_URL/g, apiUrl);
          } else {
            console.log("🚨 File already modified");
          }

          if (shouldBeSaved){
            fs.writeFile(fileToPatch, result, 'utf8', function (err) {
            if (err) 
              {throw new Error('🚨 Unable to write file: ' + err);}
            else 
              {console.log("✅ File edited successfuly");}
          });
          }
        } else {
            throw new Error(`OUTSYSTEMS_PLUGIN_ERROR: apiUrl is null or empty`)
        }
      });
    } else {
        throw new Error("OUTSYSTEMS_PLUGIN_ERROR: File was not found. The build phase may not finish successfuly");
    }
  }
