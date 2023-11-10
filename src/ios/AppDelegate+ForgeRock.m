#import "AppDelegate+ForgeRock.h"
#import "OutSystems-Swift.h"
#import <objc/runtime.h>
#import <OSFirebaseMessagingLib/OSFirebaseMessagingLib-Swift.h>
#import <UserNotifications/UserNotifications.h>
@import FRAuthenticator;

@implementation AppDelegate (ForgeRockPlugin)

+ (void)load {
    Method original = class_getInstanceMethod(self, @selector(application:didFinishLaunchingWithOptions:));
    Method swizzled = class_getInstanceMethod(self, @selector(application:firebaseCloudMessagingPluginDidFinishLaunchingWithOptions:));
    method_exchangeImplementations(original, swizzled);
}

- (BOOL)application:(UIApplication *)application firebaseCloudMessagingPluginDidFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    [self application:application firebaseCloudMessagingPluginDidFinishLaunchingWithOptions:launchOptions];

    (void)[FirebaseMessagingApplicationDelegate.shared application:application didFinishLaunchingWithOptions:launchOptions];
    
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert + UNAuthorizationOptionSound + UNAuthorizationOptionBadge)
                          completionHandler:^(BOOL granted, NSError * _Nullable error) {
                              
                              if (error) {
                                  NSLog(@"***🚨 Error requesting push notification authorization: %@", error.localizedDescription);
                                  return;
                              }

                              if (granted) {
                                  NSLog(@"***✅ Push notifications authorized by the user");
                                  dispatch_async(dispatch_get_main_queue(), ^{
                                      [application registerForRemoteNotifications];
                                  });
                              } else {
                                  NSLog(@"***🚨 Push notifications denied by the user");
                              }
                          }];
    
        //Define User Actions (add Accept and Deny Buttons)
        center.delegate = self;
        UNNotificationAction *acceptAction = [UNNotificationAction actionWithIdentifier:@"ACCEPT_ACTION" title:@"Accept" options:UNNotificationActionOptionNone];
        UNNotificationAction *denyAction = [UNNotificationAction actionWithIdentifier:@"DENY_ACTION" title:@"Deny" options:UNNotificationActionOptionDestructive];
        
        // Create a Category
        UNNotificationCategory *authCategory = [UNNotificationCategory categoryWithIdentifier:@"authentication" actions:@[acceptAction, denyAction] intentIdentifiers:@[] options:UNNotificationCategoryOptionNone];
        
        [center setNotificationCategories:[NSSet setWithObject:authCategory]];
    
    
    [application registerForRemoteNotifications];
    
    // Check if launched from push notification
    if (launchOptions[UIApplicationLaunchOptionsRemoteNotificationKey]) {
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"launchedFromPushNotification"];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }
    
    return YES;
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler {
    // Display the notification banner even when the app is in the foreground
    completionHandler(UNNotificationPresentationOptionBanner | UNNotificationPresentationOptionSound | UNNotificationPresentationOptionBadge);
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       didReceiveNotificationResponse:(UNNotificationResponse *)response
                withCompletionHandler:(void (^)(void))completionHandler {

    NSString *actionIdentifier = response.actionIdentifier;
    NSDictionary *userInfo = response.notification.request.content.userInfo;
    
    //Ver como fazer o transactional aqui. 

    // Check if the user tapped on Accept action
    if ([actionIdentifier isEqualToString:@"ACCEPT_ACTION"]) {
        NSLog(@"User accepted the authentication request");
        [[ForgeRockHelper shared] handleAcceptNotification];
    }
    // Check if the user tapped on Deny action
    else if ([actionIdentifier isEqualToString:@"DENY_ACTION"]) {
        NSLog(@"User denied the authentication request");
        [[ForgeRockHelper shared] handleDenyNotification];
    }
    // Handle other actions and the default case
    else {
        NSLog(@"User tapped the notification itself or another action");
        // Handle default case or other actions if any
    }

    completionHandler();
}

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    NSLog(@"***✅ didRegisterForRemoteNotificationsWithDeviceToken: %@",deviceToken);
    [[ForgeRockHelper shared] registerDeviceToken:application deviceToken:deviceToken];
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    NSLog(@"***🚨 didFailToRegisterForRemoteNotificationsWithError: %@",error.localizedDescription);
    [[ForgeRockHelper shared] handleRemoteNotificationFailure:application error:error];

}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {
    NSLog(@"***✅✅ didReceiveRemoteNotification (Firebase Swizzling): %@", userInfo);
    [[ForgeRockHelper shared] application:application didReceiveRemoteNotification:userInfo fetchCompletionHandler:completionHandler];

}

@end
