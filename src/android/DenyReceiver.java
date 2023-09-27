package com.outsystems.experts.forgerockplugin;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;

import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.PushNotification;

public class DenyReceiver extends BroadcastReceiver {

    private static final String TAG = "ForgeRockPlugin-DenyReceiver";

    FRAClient fraClient;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "⭐⭐⭐ DENY ⭐⭐⭐ ");

        // Retrieve mechanismUID from the intent
        String mechanismUID = intent.getStringExtra("mechanismUID");
        int notificationId = intent.getIntExtra("notificationId", -1);
        Log.i(TAG, "👉 DenyReceiver - mechanismUID: " + mechanismUID);

        // Check if ForgeRockPlugin instance is initialized
        if (ForgeRockPlugin.instance == null) {
            Log.e(TAG, "🚨 ForgeRockPlugin instance is not initialized. Trying to start it");

            // Retrieve the fcmToken from SharedPreferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("_", Context.MODE_PRIVATE);
            String fcmToken = sharedPreferences.getString("fcm_token", null);
            if (fcmToken == null) {
                // Handle the case where the token is not found in SharedPreferences
                Log.e(TAG, "fcmToken not found in SharedPreferences");
                return;
            }

            // Start the FRAClient instance and register for remote notifications
            try {
                fraClient = new FRAClient.FRAClientBuilder()
                        .withContext(context)
                        .start();
                fraClient.registerForRemoteNotifications(fcmToken);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error starting FRAClient or registering for remote notifications. Error: " + e.getMessage());
            }
        }

        // Retrieve the PushNotification using the ForgeRockPlugin instance
        String serializedPushNotification = intent.getStringExtra("serializedPushNotification");

        if (serializedPushNotification != null) {
            Log.d(TAG, "✅ 2 Serialized PushNotification: " + serializedPushNotification);
        } else {
            Log.e(TAG, "🚨 2 Serialized PushNotification is null.");
        }

        Gson gson = new Gson();
        PushNotification pushNotification = gson.fromJson(serializedPushNotification, PushNotification.class);

        // Cancel the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel("1", notificationId);

        if (pushNotification != null) {
            pushNotification.deny(new FRAListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (result == null) {
                        Log.d(TAG, "❌ Received null result in onSuccess.");
                    } else {
                        Log.d(TAG, "✅ Successfully denied the notification.");
                    }
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    //REMOVER
                    if (notificationManager.areNotificationsEnabled()) {
                        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
                        for (StatusBarNotification sbn : activeNotifications) {
                            if (sbn.getId() == notificationId) {
                                Log.d(TAG, "🤔 Notification with ID: " + notificationId + " is active.");
                                break;
                            } else {
                                Log.d(TAG, "🤔 Notification NOT FOUND.");
                            }
                        }
                    }


                    if (notificationManager != null) {
                        Log.d(TAG, "🤔 Attempting to cancel notification with ID: " + notificationId);
                        notificationManager.cancel("1", notificationId);
                        Log.d(TAG, "🤔 Cancel method called for notification with ID: " + notificationId);
                    } else {
                        Log.e(TAG, "🚨 NotificationManager is null.");
                    }


                }

                @Override
                public void onException(Exception e) {
                    Log.e(TAG, "🚨 Error denying notification: " + e.getMessage());
                }
            });
        } else {
            Log.e(TAG, "🚨 PushNotification object is null.");
        }
    }
}
