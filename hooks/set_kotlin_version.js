#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// Define the path to the cdv-gradle-config.json file

const configPath = path.join('platforms', 'android', 'cdv-gradle-config.json');

// Define the new value for KOTLIN_VERSION
const newKotlinVersion = '1.7.20';

// Read and modify the cdv-gradle-config.json file
fs.readFile(configPath, 'utf8', (err, data) => {
    if (err) {
        console.error('❌ Error reading cdv-gradle-config.json:', err);
        process.exit(1);
    }

    // Parse the JSON data
    const config = JSON.parse(data);

    // Update the KOTLIN_VERSION variable
    config.KOTLIN_VERSION = newKotlinVersion;

    // Convert the modified data back to JSON
    const updatedData = JSON.stringify(config, null, 2);

    // Write the updated content back to the file
    fs.writeFile(configPath, updatedData, 'utf8', (err) => {
        if (err) {
            console.error('❌ Error writing to cdv-gradle-config.json:', err);
            process.exit(1);
        }

        console.log(`✅ Updated KOTLIN_VERSION to ${newKotlinVersion} in cdv-gradle-config.json`);
    });
});
