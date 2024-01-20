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

        let transactionalPNApiURLString = "API_URL",
        let userDefaultsKey = "transactionalPNApiURL"
        UserDefaults.standard.set(transactionalPNApiURLString, forKey: userDefaultsKey)
        UserDefaults.standard.synchronize()            
        

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
    func handleReceivedPushNotification(_ notification: Notification) {
        print("***❤️ handleReceivedPushNotification")
        if let callbackId = self.didReceivePnCallbackId {
            print("***👉 callbackId: \(callbackId)")

            let nativeNotificationIsSet = UserDefaults.standard.bool(forKey: "nativeNotificationIsSet")
            if !nativeNotificationIsSet {
                if let userInfo = notification.userInfo as? [String: Any] {
                    // Check and retrieve the nested dictionary
                    if let transactionInfo = userInfo["transactionInfo"] as? [String: Any] {
                        do {
                            // Convert the nested dictionary to JSON string
                            let jsonData = try JSONSerialization.data(withJSONObject: transactionInfo, options: .prettyPrinted)
                            if let jsonString = String(data: jsonData, encoding: .utf8) {
                                print("***✅ Sending callback with JSON string:\n\(jsonString)")
                                sendPluginResult(status: CDVCommandStatus_OK, message: jsonString, callbackId: callbackId, keepCallback: true, isPushNotification: true, isTransaction: true)
                            }
                        } catch {
                            print("🚨 Error converting transactionInfo to JSON: \(error.localizedDescription)")
                            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Error converting transactionInfo to JSON: \(error.localizedDescription)", callbackId: callbackId)
                        }
                    } else {
                        if let message = userInfo["message"] as? String{
                            //Standard Authorization PN request
                            sendPluginResult(status: CDVCommandStatus_OK, message: message, callbackId: callbackId, keepCallback: true, isPushNotification: true, isTransaction: false)
                        } else {
                            print("🚨 transactionInfo is nil")
                            sendPluginResult(status: CDVCommandStatus_ERROR, message: "transactionInfo is nil", callbackId: callbackId)
                        }
                        
                    }
                } else {
                    print("🚨 userInfo is nil")
                    sendPluginResult(status: CDVCommandStatus_ERROR, message: "userInfo is nil", callbackId: callbackId)
                }
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
       
    @objc(removeAccount:)
    func removeAccount (_ command: CDVInvokedUrlCommand){
        if let userToBeRemoved = command.arguments[0] as? String {
        
// Code proposed by ForgeRock that freezes the app
//        if let accounts = FRAClient.shared?.getAllAccounts(){
//            for account in accounts {
//                FRAClient.shared?.removeAccount(account: account)
//                print("User \(account.accountName) removed.")
//            }
//        }
            var mechanismToBeRemoved: Mechanism?
                
            if let allNotifications = FRAClient.shared?.getAllNotifications(){
                if !allNotifications.isEmpty{
                    for notification in allNotifications {
                        if let mechanism = FRAClient.shared?.getMechanism(notification: notification){
                            if mechanism.accountName == userToBeRemoved {
                                mechanismToBeRemoved = mechanism
                                break
                            }
                        }
                    }
                    
                    if let mechanism = mechanismToBeRemoved {
                        let userRemoved = FRAClient.shared?.removeMechanism(mechanism: mechanism)
                        if userRemoved == true {
                            print("⭐️ User account name: \(mechanism.accountName), Identifier: \(mechanism.identifier) removed")
                            self.sendPluginResult(status: CDVCommandStatus_OK, message: "User \(mechanism.accountName) removed", callbackId: command.callbackId)
                        } else {
                            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Error: Could not remove user", callbackId: command.callbackId)
                        }
                    } else {
                        sendPluginResult(status: CDVCommandStatus_ERROR, message: "Error: Could not extract mechanism from notification", callbackId: command.callbackId)
                    }
                } else {
                    sendPluginResult(status: CDVCommandStatus_ERROR, message: "Error: allNotifications Array is empty", callbackId: command.callbackId)
                }
            } else {
                sendPluginResult(status: CDVCommandStatus_ERROR, message: "Error: There are no notifications available", callbackId: command.callbackId)
            }
        } else {
            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Error: Missing mandatory username attribute", callbackId: command.callbackId)
        }
    }
    
    @objc(setNativeNotification:)
    func setNativeNotification(_ command: CDVInvokedUrlCommand){
        if let isSet = command.arguments[0] as? Bool {
            UserDefaults.standard.set(isSet, forKey: "nativeNotificationIsSet")
            UserDefaults.standard.synchronize()
            
            sendPluginResult(status: CDVCommandStatus_OK, callbackId: command.callbackId)
        } else {
            sendPluginResult(status: CDVCommandStatus_ERROR, message: "Failed to get IsSet (Boolean) from arguments", callbackId: command.callbackId)
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
    
    func sendPluginResult(status: CDVCommandStatus, message: String = "", callbackId: String, keepCallback: Bool = false, isPushNotification: Bool = false, isTransaction: Bool = false) {
        if isPushNotification {
            var callbackDictionary: Dictionary<String, Any> = [:]
            if isTransaction {
                callbackDictionary["isTransaction"] = true
            } else {
                callbackDictionary["isTransaction"] = false
            }
            callbackDictionary["message"] = message
            
            // Convert the dictionary into a JSON string
            do {
                let jsonData = try JSONSerialization.data(withJSONObject: callbackDictionary, options: [])
                if let jsonString = String(data: jsonData, encoding: .utf8) {
                    let pluginResult = CDVPluginResult(status: status, messageAs: jsonString)
                    pluginResult?.setKeepCallbackAs(keepCallback)
                    self.commandDelegate!.send(pluginResult, callbackId: callbackId)
                } else {
                    print("Error serializing json string")
                    let errorResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Error serializing json string")
                    errorResult?.setKeepCallbackAs(keepCallback)
                    self.commandDelegate!.send(errorResult, callbackId: callbackId)
                }
            } catch {
                print("Error serializing callback dictionary to JSON: \(error)")
                let errorResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Error: Failed to serialize callback message")
                errorResult?.setKeepCallbackAs(keepCallback)
                self.commandDelegate!.send(errorResult, callbackId: callbackId)
            }
            
        } else {
            let pluginResult = CDVPluginResult(status: status, messageAs: message)
            pluginResult?.setKeepCallbackAs(keepCallback)
            self.commandDelegate!.send(pluginResult, callbackId: callbackId)
        }
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
