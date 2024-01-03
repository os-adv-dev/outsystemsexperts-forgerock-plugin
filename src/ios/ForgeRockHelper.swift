//
//  ForgeRockHelper.swift
//
//  Created by Andre Grillo on 05/09/2023.
//

import Foundation
import FRAuthenticator

@objc
public class ForgeRockHelper: NSObject {
    var notification: PushNotification?
    var completionHandler: ((UIBackgroundFetchResult) -> Void)?
    
    @objc
    public static let shared = ForgeRockHelper()
    
    // Define Objective-C block types
    public typealias MechanismCallbackBlock = (Mechanism) -> Void
    public typealias ErrorCallbackBlock = (Error) -> Void
    
    @objc
    public func registerDeviceToken(_ application: UIApplication, deviceToken: Data) {
        print("***✅ registerDeviceToken: \(String(decoding: deviceToken, as: UTF8.self))")
        FRAPushHandler.shared.application(application, didRegisterForRemoteNotificationsWithDeviceToken: deviceToken)
    }
    
    @objc
    public func handleRemoteNotificationFailure(_ application: UIApplication, error: Error) {
        print("***🚨 handleRemoteNotificationFailure: \(error.localizedDescription)")
        FRAPushHandler.shared.application(application, didFailToRegisterForRemoteNotificationsWithError: error)
    }
    
    @objc
    public func createMechanismFromUri(uri: URL, onSuccess: @escaping MechanismCallbackBlock, onError: @escaping ErrorCallbackBlock) {
        FRAClient.shared?.createMechanismFromUri(uri: uri, onSuccess: { mechanism in
            print("***✅ Success: createMechanismFromUri")
            onSuccess(mechanism)
        }, onError: { error in
            print("***🚨 createMechanismFromUri: \(error.localizedDescription)")
            onError(error)
        })
    }
    
    @objc
    public func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        print("***⭐️ didReceiveRemoteNotification (ForgeRockHelper): \(userInfo)")
        // Once you receive the remote notification, handle it with FRAPushHandler to get the PushNotification object.
        if let notification = FRAPushHandler.shared.application(application, didReceiveRemoteNotification: userInfo) {
            self.notification = notification
            self.completionHandler = completionHandler
            
            print("⭐️ userInfo: \(userInfo)")
            
            var userInfoWithMessage: [AnyHashable : Any] = [:]
            
            if let aps = userInfo["aps"] as? [String: Any],
               let alert = aps["alert"] as? String {
                print("⭐️ alert: \(alert)")
                userInfoWithMessage["message"] = alert
            }
            
            var isThereCustomPayload = false
            if let customPayloadString = notification.customPayload {
                
                if let jsonData = customPayloadString.data(using: .utf8) {
                    do {
                        let jsonObject = try JSONSerialization.jsonObject(with: jsonData, options: [])

                        if let jsonDict = jsonObject as? [String: Any], !jsonDict.isEmpty {
                            //Transactional PN received
                            print("🎯 Received a non-empty custom payload JSON object or a different type")
                            isThereCustomPayload = true
                            if let transactionId = jsonDict["transactionId"] as? String, let username = jsonDict["username"] as? String {//}, let locale = jsonDict["locale"] as? String {
                                //userInfoWithMessage["transactionId"] = transactionId
                                //userInfoWithMessage["locale"] = locale
                                
                                userInfoWithMessage["transactionId"] = transactionId
                                userInfoWithMessage["username"] = username
                                
                                // Read the URL from UserDefaults
                                if let urlString = UserDefaults.standard.string(forKey: "transactionalPNApiURL") {
                                    print("⭐️ urlString: \(urlString)")
                                    
                                    // Prepare URL
                                    //let urlString = "https://mobout-test.anb.com.sa/auth/json/realms/root/realms/iam/authenticate?authIndexType=service&authIndexValue=ANB-PushTransactionDataDummy"
                                    guard let url = URL(string: urlString) else { return }
                                    
                                    // Prepare URLRequest
                                    var request = URLRequest(url: url)
                                    request.httpMethod = "POST"
                                    
                                    // Set headers
                                    request.setValue(username, forHTTPHeaderField: "X-OpenAM-Username")
                                    request.setValue(transactionId, forHTTPHeaderField: "transactionId")
                                    
                                    // request.httpBody = ...
                                    
                                    // Clearing cookies
                                    let cookieStorage = HTTPCookieStorage.shared
                                    if let cookies = cookieStorage.cookies {
                                        for cookie in cookies {
                                            cookieStorage.deleteCookie(cookie)
                                        }
                                    }
                                    
                                    // Perform the request
                                    // Perform the request
                                    let task = URLSession.shared.dataTask(with: request) { data, response, error in
                                        if let error = error {
                                            // Handle error
                                            print("🚨 Error making API call: \(error.localizedDescription)")
                                            return
                                        }
                                        
                                        if let data = data {
                                            do {
                                                // Parse the JSON data into a dictionary
                                                if let jsonDictionary = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                                                    print("✅ Parsed dictionary: \(jsonDictionary)")
                                                    
                                                    if let successUrlString = jsonDictionary["successUrl"] as? String {
                                                        print("⭐️ successUrl: \(successUrlString)")
                                                        do {
                                                            // Convert the string back into Data for JSON parsing
                                                            if let data = successUrlString.data(using: .utf8) {
                                                                // Parse the data into a dictionary
                                                                if let successUrlDict = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                                                                    userInfoWithMessage["transactionInfo"] = successUrlDict
                                                                    // Was the app launched due to a push notification?
                                                                    let launchedFromPush = UserDefaults.standard.bool(forKey: "launchedFromPushNotification")
                                                                    if launchedFromPush {
                                                                        // The app was launched due to a push notification
                                                                        UserDefaults.standard.set(userInfoWithMessage, forKey: "pushNotificationData")
                                                                        print("✅ pushNotificationData with customPayload saved to UserDefaults")
                                                                    } else {
                                                                        NotificationCenter.default.post(name: .didReceivePushNotificationCallback, object: nil, userInfo: userInfoWithMessage)
                                                                    }
                                                                } else {
                                                                    print("Error: Unable to parse successUrl into a dictionary")
                                                                }
                                                            }
                                                        } catch {
                                                            print("Error parsing successUrl JSON: \(error.localizedDescription)")
                                                        }
                                                    } else {
                                                        print("Error: SuccessUrl string missing from custom payload")
                                                    }

                                                }
                                            } catch {
                                                // Handle JSON parsing error
                                                print("🚨 Error parsing JSON: \(error.localizedDescription)")
                                            }
                                        }
                                    }
                                    task.resume()
                                }
                            }

                        } else {
                            //Standard Authentication PN received
                            print("🎯 Received an empty custom payload json object")
                            NotificationCenter.default.post(name: .didReceivePushNotificationCallback, object: nil, userInfo: userInfoWithMessage)
                            
                        }
                    } catch {
                        print("Error parsing JSON: \(error)")
                        // Handle parsing error
                    }
                }

                
                
                if let jsonData = customPayloadString.data(using: .utf8) {
                    do {
                        if let jsonDict = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any] {
                            
                        }
                    } catch {
                        print("🚨 Error parsing JSON: \(error.localizedDescription)")
                    }
                }
            } else {
                print("❌ customPayload is nil")
            }
            
            if !isThereCustomPayload {
                //Was the app launched due to a push notification?
                let launchedFromPush = UserDefaults.standard.bool(forKey: "launchedFromPushNotification")
                if launchedFromPush {
                    // The app was launched due to a push notification
                    UserDefaults.standard.set(userInfoWithMessage, forKey: "pushNotificationData")
                    print("✅ pushNotificationData with customPayload saved to UserDefaults")
                } else {
                    NotificationCenter.default.post(name: .didReceivePushNotificationCallback, object: nil, userInfo: userInfoWithMessage)
                }
            }
        } else {
            print("***🚨 Push notification: no data received")
            completionHandler(.noData)
        }
    }
    
    @objc
    public func handleAcceptNotification() {
        self.notification?.accept(onSuccess: {
            print("***👍 notification.accept ✅")
            NotificationCenter.default.post(name: .acceptNotificationCallback, object: nil)
            self.completionHandler?(.newData)
        }) { (error) in
            print("***❌ notification.accept Error: \(error.localizedDescription)")
            let userInfo: [String: Any] = ["errorMessage": error.localizedDescription]
            NotificationCenter.default.post(name: .acceptNotificationCallback, object: nil, userInfo: userInfo)
            self.completionHandler?(.failed)
        }
    }
    
    @objc
    public func handleDenyNotification() {
        self.notification?.deny(onSuccess: {
            print("***👎 notification.deny ✅")
            NotificationCenter.default.post(name: .denyNotificationCallback, object: nil)
            self.completionHandler?(.newData)
        }) { (error) in
            print("***❌ notification.deny Error: \(error.localizedDescription)")
            let userInfo: [String: Any] = ["errorMessage": error.localizedDescription]
            NotificationCenter.default.post(name: .denyNotificationCallback, object: nil, userInfo: userInfo)
            self.completionHandler?(.failed)
        }
    }
    
    //MARK: Notification Center Observers
    public override init() {
        super.init()
        setupNotificationObservers()
    }

    private func setupNotificationObservers() {
        NotificationCenter.default.addObserver(self, selector: #selector(handleAcceptNotification), name: .acceptNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleDenyNotification), name: .denyNotification, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self, name: .acceptNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: .denyNotification, object: nil)
    }
}

extension Notification.Name {
    static let acceptNotification = Notification.Name("acceptNotification")
    static let denyNotification = Notification.Name("denyNotification")
}
