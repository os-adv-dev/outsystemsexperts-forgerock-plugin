//
//  ForgeRockPlugin.swift
//
//  Created by Andre Grillo on 29/08/2023.
//

import Foundation
import FRAuthenticator

@objc
class ForgeRockPlugin: CDVPlugin {
    var command: CDVInvokedUrlCommand?
    
    @objc(start:)
    func start(_ command: CDVInvokedUrlCommand){
        FRAClient.start()
        sendPluginResult(status: CDVCommandStatus_OK, message: "Plugin Started")
    }
    
    @objc(registerForRemoteNotifications:)
    func registerForRemoteNotifications(_ command: CDVInvokedUrlCommand){
        if let fcmToken = command.arguments[0] as? String {
            
            self.sendPluginResult(status: CDVCommandStatus_OK, message: "Registered for remote notifications")
            
        } else {
            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Failed to get FCM token from arguments")
        }
    }

    @objc(createMechanismFromUri:)
    func createMechanismFromUri(_ command: CDVInvokedUrlCommand){
        if let url = command.arguments[0] as? URL {
            guard let fraClient = FRAClient.shared else {
                print("FRAuthenticator SDK is not initialized")
                return
            }
            fraClient.createMechanismFromUri(uri: url, onSuccess: { (mechanism) in
                self.sendPluginResult(status: CDVCommandStatus_OK, message: "Mechanism created from URI")
            }, onError: { (error) in
                self.sendPluginResult(status: CDVCommandStatus_ERROR, message: error.localizedDescription)
            })
            
        } else {
            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Failed to get URI from arguments")
        }
    }

    @objc(getCurrentCode:)
    func getCurrentCode(_ command: CDVInvokedUrlCommand){
        
        
        
        sendPluginResult(status: CDVCommandStatus_OK, message: "{\"code\":\"123456\"}")
    }
    
    func sendPluginResult(status: CDVCommandStatus, message: String) {
        var pluginResult = CDVPluginResult(status: status, messageAs: message)
        if let command = self.command {
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        }
    }
}

