//
//  ForgeRockPlugin.swift
//
//  Created by Andre Grillo on 29/08/2023.
//

import Foundation
import FRAuthenticator

@objc
class ForgeRockPlugin: CDVPlugin {
//    var command: CDVInvokedUrlCommand?
    var callbackId: String?
    var didReceivePnCallbackId: String?
    var mechanism: Mechanism?
    
    @objc(start:)
    func start(_ command: CDVInvokedUrlCommand){
        if FRAClient.shared == nil {
            FRAClient.start()
        }
        sendPluginResult(status: CDVCommandStatus_OK, message: "Plugin Started", callbackId: command.callbackId)
    }
    
    @objc(didReceivePushNotificationSetCallback:)
    func didReceivePushNotificationSetCallback(_ command: CDVInvokedUrlCommand){
        print("***✅ Setting didReceivePushNotificationSetCallback")
        // Remove existing observer if any
        NotificationCenter.default.removeObserver(self, name: .didReceivePushNotificationCallback, object: nil)
        
        // Add the observer
        NotificationCenter.default.addObserver(self, selector: #selector(handleReceivedPushNotification), name: .didReceivePushNotificationCallback, object: nil)
        self.didReceivePnCallbackId = command.callbackId
        
        //Checking if the app was started by a PN click
        let launchedFromPush = UserDefaults.standard.bool(forKey: "launchedFromPushNotification")
        if launchedFromPush {
            // The app was launched due to a push notification
            if let userInfo = UserDefaults.standard.dictionary(forKey: "pushNotificationData") {
                NotificationCenter.default.post(name: .didReceivePushNotificationCallback, object: nil, userInfo: userInfo)
                // Clear the UserDefaults flags
                UserDefaults.standard.removeObject(forKey: "launchedFromPushNotification")
                UserDefaults.standard.removeObject(forKey: "pushNotificationData")
            }
        }
    }
    
    @objc
    func handleReceivedPushNotification(_ notification: Notification){
        print("***❤️ handleReceivedPushNotification")
        if let callbackId = self.didReceivePnCallbackId {
            print("***👉 callbackId: \(callbackId)")
            
            if let userInfo = notification.userInfo {
//                //Checking if a transactional PN was received
//                if let customPayload = userInfo["customPayload"] {
//                    print("***✅ Sending custom payload callback")
//                    sendPluginResult(status: CDVCommandStatus_OK, message: customPayload as! String, callbackId: callbackId, keepCallback: true)
//                        return
//                }
                if let userInfoMessage = userInfo["message"]{
                    print("***✅ Sending callback")
                    sendPluginResult(status: CDVCommandStatus_OK, message: userInfoMessage as! String, callbackId: callbackId, keepCallback: true)
                } else {
                    print("🚨 userInfo is empty")
                }
            } else {
                print("🚨 userInfo is nil")
            }
        } else {
            print("🚨 There are no callbacks set for receiving push notifications!")
        }
    }
    
    @objc(registerForRemoteNotifications:)
    func registerForRemoteNotifications(_ command: CDVInvokedUrlCommand){
        if let fcmToken = command.arguments[0] as? String {
            print("***⭐️ Token: \(fcmToken)")
            self.sendPluginResult(status: CDVCommandStatus_OK, message: "Registered for remote notifications", callbackId: command.callbackId)
        } else {
            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Failed to get FCM token from arguments", callbackId: command.callbackId)
        }
    }
    
    
    @objc(createMechanismFromUri:)
    func createMechanismFromUri(_ command: CDVInvokedUrlCommand){
        if let urlString = command.arguments[0] as? String {
            guard let url = URL(string: urlString) else {
                print("***⭐️ Invalid URL: \(urlString)")
                sendPluginResult(status: CDVCommandStatus_ERROR, message: "Invalid URI", callbackId: command.callbackId)
                return
            }

            if FRAClient.shared == nil {
                sendPluginResult(status: CDVCommandStatus_ERROR, message: "FRAuthenticator SDK is not initialized", callbackId: command.callbackId)
                return
            }
            FRAClient.shared!.createMechanismFromUri(uri: url, onSuccess: { (mechanism) in
                self.mechanism = mechanism
                print("***ℹ️ Mechanism Identifier: \(mechanism.identifier)")
                print("***ℹ️ Mechanism accountIdentifier: \(mechanism.accountIdentifier)")
                print("***ℹ️ Mechanism accountName: \(mechanism.accountName)")
                print("***❤️ Mechanism created from URI")
                
                self.sendPluginResult(status: CDVCommandStatus_OK, message: "Mechanism created from URI", callbackId: command.callbackId)
            }, onError: { (error) in
                print("***🚨 Error creating Mechanism from URI: \(error.localizedDescription)")
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
    
    func sendPluginResult(status: CDVCommandStatus, message: String = "", callbackId: String, keepCallback: Bool = false) {
        let pluginResult = CDVPluginResult(status: status, messageAs: message)
        pluginResult?.setKeepCallbackAs(keepCallback)
        self.commandDelegate!.send(pluginResult, callbackId: callbackId)
    }
    
    //MARK: Accept/Deny 2FA Push Notifications
    @objc(acceptAction:)
    func acceptAction(_ command: CDVInvokedUrlCommand) {
        NotificationCenter.default.addObserver(self, selector: #selector(handleAcceptCallback), name: .acceptNotificationCallback, object: nil)
        self.callbackId = command.callbackId
        NotificationCenter.default.post(name: .acceptNotification, object: nil)
    }
    
    @objc(denyAction:)
    func denyAction(_ command: CDVInvokedUrlCommand) {
        NotificationCenter.default.addObserver(self, selector: #selector(handleDenyCallback), name: .denyNotificationCallback, object: nil)
        self.callbackId = command.callbackId
        NotificationCenter.default.post(name: .denyNotification, object: nil)
    }
    
    @objc
    func handleAcceptCallback(_ notification: Notification){
        removeObservers()
        if let callbackId = self.callbackId{
            if let errorMessage = notification.userInfo?["errorMessage"] as? String {
                print("***🚨 handleAcceptCallback: \(errorMessage)")
                self.callbackId = nil
                sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage, callbackId: callbackId)
            }
            else {
                print("***✅ handleAcceptCallback")
                self.callbackId = nil
                sendPluginResult(status: CDVCommandStatus_OK, callbackId: callbackId)
            }
        }
    }
    
    @objc
    func handleDenyCallback(_ notification: Notification){
        removeObservers()
        if let callbackId = self.callbackId{
            if let errorMessage = notification.userInfo?["errorMessage"] as? String {
                print("***🚨 handleDenyCallback: \(errorMessage)")
                print(errorMessage)
                self.callbackId = nil
                sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage, callbackId: callbackId)
            } else {
                print("***✅ handleDenyCallback")
                self.callbackId = nil
                sendPluginResult(status: CDVCommandStatus_OK, callbackId: callbackId)
            }
        }
    }
    
    func jsonString(from dictionary: [AnyHashable: Any]) -> String? {
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: dictionary, options: [])
            let jsonString = String(data: jsonData, encoding: .utf8)
            return jsonString
        } catch {
            print("Error converting dictionary to JSON string: \(error)")
            return nil
        }
    }

    
    //MARK: Notification Center
    
    func removeObservers(){
        NotificationCenter.default.removeObserver(self, name: .acceptNotificationCallback, object: nil)
        NotificationCenter.default.removeObserver(self, name: .denyNotificationCallback, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self, name: .didReceivePushNotificationCallback, object: nil)
    }
    
}

extension Notification.Name {
    static let acceptNotificationCallback = Notification.Name("acceptNotificationCallback")
    static let denyNotificationCallback = Notification.Name("denyNotificationCallback")
    static let didReceivePushNotificationCallback = Notification.Name("didReceivePushNotificationCallback")
}
