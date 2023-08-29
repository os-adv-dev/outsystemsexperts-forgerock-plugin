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
    var mechanism: Mechanism?
//    var fraClient: FRAClient?
    
    @objc(start:)
    func start(_ command: CDVInvokedUrlCommand){
//        guard let fraClient = FRAClient.shared else {
//            sendPluginResult(status: CDVCommandStatus_ERROR, message: "FRAuthenticator SDK is not initialized", callbackId: command.callbackId)
//            return
//        }
        FRAClient.start()
        sendPluginResult(status: CDVCommandStatus_OK, message: "Plugin Started", callbackId: command.callbackId)
    }
    
    @objc(registerForRemoteNotifications:)
    func registerForRemoteNotifications(_ command: CDVInvokedUrlCommand){
        if let fcmToken = command.arguments[0] as? String {
            
            self.sendPluginResult(status: CDVCommandStatus_OK, message: "Registered for remote notifications", callbackId: command.callbackId)
            
        } else {
            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Failed to get FCM token from arguments", callbackId: command.callbackId)
        }
    }

    @objc(createMechanismFromUri:)
    func createMechanismFromUri(_ command: CDVInvokedUrlCommand){
        if let urlString = command.arguments[0] as? String {
            guard let url = URL(string: urlString) else {
                sendPluginResult(status: CDVCommandStatus_ERROR, message: "Invalid URI", callbackId: command.callbackId)
                return
            }
            guard let fraClient = FRAClient.shared else {
                sendPluginResult(status: CDVCommandStatus_ERROR, message: "FRAuthenticator SDK is not initialized", callbackId: command.callbackId)
                return
            }
            fraClient.createMechanismFromUri(uri: url, onSuccess: { (mechanism) in
                self.mechanism = mechanism
                self.sendPluginResult(status: CDVCommandStatus_OK, message: "Mechanism created from URI", callbackId: command.callbackId)
            }, onError: { (error) in
                self.sendPluginResult(status: CDVCommandStatus_ERROR, message: error.localizedDescription, callbackId: command.callbackId)
            })
            
        } else {
            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Failed to get URI from arguments", callbackId: command.callbackId)
        }
    }

    @objc(getCurrentCode:)
    func getCurrentCode(_ command: CDVInvokedUrlCommand){
//        do {
//            // Generate OathTokenCode
//            if let oathMechanism: OathMechanism? = mechanism as! OathMechanism {
//                let code = try oathMechanism.generateCode()
//                // Update UI with generated code
//                codeLabel?.text = code.code
//            }
//
//        } catch {
//            // Handle errors for generating OATH code
//        }
//
        
        sendPluginResult(status: CDVCommandStatus_OK, message: "{\"code\":\"123456\"}", callbackId: command.callbackId)
    }
    
    func sendPluginResult(status: CDVCommandStatus, message: String, callbackId: String) {
        let pluginResult = CDVPluginResult(status: status, messageAs: message)
        self.commandDelegate!.send(pluginResult, callbackId: callbackId)
    }
}

