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
    
    [application registerForRemoteNotifications];
    
    // Check if launched from push notification
    if (launchOptions[UIApplicationLaunchOptionsRemoteNotificationKey]) {
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"launchedFromPushNotification"];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }
    
    return YES;
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
