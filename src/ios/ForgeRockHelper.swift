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
        // If RemoteNotification does not contain the expected payload structured from AM, the Authenticator module does not return the PushNotification object.
        if let notification = FRAPushHandler.shared.application(application, didReceiveRemoteNotification: userInfo) {
            // With the PushNotification object, you can either accept or deny
            self.notification = notification
            self.completionHandler = completionHandler
            
            
            
            //Was the app launched due to a push notification?
            let launchedFromPush = UserDefaults.standard.bool(forKey: "launchedFromPushNotification")
            if launchedFromPush {
                // The app was launched due to a push notification
                UserDefaults.standard.set(userInfo, forKey: "pushNotificationData")                
            } else {
                NotificationCenter.default.post(name: .didReceivePushNotificationCallback, object: nil, userInfo: userInfo)
            }
            
//                        notification.accept(onSuccess: {
//                            // Handle success here
//                            print("***👍 notification.accept: \(userInfo)")
//                            completionHandler(.newData)
//                        }) { (error) in
//                            // Handle error here
//                            print("***❌ didReceiveRemoteNotification: \(error.localizedDescription)")
//                            completionHandler(.failed)
//                        }
        } else {
            //Tratar aqui as push notifications normais!
            print("***🚨 Push notification: no data received")
            completionHandler(.noData)
        }
    }
    
    @objc
    public func handleAcceptNotification() {
        //MARK: TODO Check if notification is nil and send the callback accordingly
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
