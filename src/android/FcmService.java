package com.outsystems.experts.forgerockplugin;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.iid.FirebaseInstanceIdReceiver;
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.outsystems.experts.forgerocksample.MainActivity;
import com.outsystems.experts.forgerocksample.R;

import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.exception.InvalidNotificationException;

public class FcmService extends FirebaseMessagingService {

    private static final String TAG = "FcmService";
    private static int messageCount = 1;
    FRAClient fraClient = null;
    /**
     * Default instance of FcmService expected to be instantiated by Android framework.
     */
    public FcmService() {

    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.d(TAG, "⭐ reached service onMessageReceived");

        // Check if setNativeNotification was saved in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("_", Context.MODE_PRIVATE);
        boolean isSet = sharedPreferences.getBoolean("nativeNotificationSet", false);
        if (isSet){
                Log.d(TAG, "⭐ Native notification");
                try {
                    fraClient = new FRAClient.FRAClientBuilder().withDeviceToken(this.getToken()).withContext(getApplicationContext()).start();
                } catch (AuthenticatorException e) {
                    throw new RuntimeException(e);
                }
                try {
                    PushNotification pushNotification = fraClient.handleMessage(message);
                    // If it's a valid Push message from AM and not expired, create a system notification
                    if(pushNotification != null && !pushNotification.isExpired()) {

                        // Serialize the PushNotification object
                        //Gson gson = new Gson();
                        //String serializedPushNotification = gson.toJson(pushNotification);

                        // Store the serialized PushNotification in SharedPreferences
//                        sharedPreferences = getSharedPreferences("_", Context.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        editor.putString(pushNotification.getMechanismUID(), serializedPushNotification);
//                        editor.apply();

                        createSystemNotification(pushNotification);
//                    if (ForgeRockPlugin.instance != null) {
//                        ForgeRockPlugin.instance.handleNotification(pushNotification);
//                    } else {
//                        Log.e(TAG, "ForgeRockPlugin.instance is still null after initialization attempt.");
//                    }
                    }
                    Log.d(TAG, "message handled");
                } catch (InvalidNotificationException e) {
                    throw new RuntimeException(e);
                }
        } else {
            if (ForgeRockPlugin.instance != null) {
                Log.d(TAG, "⭐ In-app notification");
                ForgeRockPlugin.instance.handleNotification(message);
            }
        }
    }

    /**
     * Create system notification to display to user the Push request received
     * @param pushNotification the PushNotification object
     */

    private void createSystemNotification(PushNotification pushNotification) {
        int id = messageCount++;

        int notificationId = pushNotification.getMessageId().hashCode();
        Log.d(TAG, "🤔 1 notificationId: " + notificationId);

        Mechanism mechanism = fraClient.getMechanism(pushNotification);
        Intent intent = ForgeRockPlugin.setupIntent(this, MainActivity.class, pushNotification, mechanism);
        String title = String.format("Login attempt from %1$s at %2$s", mechanism.getAccountName(), mechanism.getIssuer());
        String body = "Tap to log in";

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "1")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, pendingIntentFlags))
                .setAutoCancel(true);

        // Intent for the "Accept" action
        Intent acceptIntent = new Intent(this, AcceptReceiver.class);

        Gson gson = new Gson();
        String serializedPushNotification = gson.toJson(pushNotification);
        if (serializedPushNotification != null) {
            Log.d(TAG, "✅ 1 Serialized PushNotification: " + serializedPushNotification);
        } else {
            Log.e(TAG, "🚨 1 Serialized PushNotification is null.");
        }
        acceptIntent.putExtra("serializedPushNotification", serializedPushNotification);
        acceptIntent.putExtra("mechanismUID", mechanism.getMechanismUID()); // Passing the mechanismUID to the AcceptReceiver
        acceptIntent.putExtra("notificationId", notificationId); // Passing the notificationId to the AcceptReceiver
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 0, acceptIntent, pendingIntentFlags);

        // Intent for the "Deny" action
        Intent denyIntent = new Intent(this, DenyReceiver.class);
        denyIntent.putExtra("serializedPushNotification", serializedPushNotification);
        denyIntent.putExtra("mechanismUID", mechanism.getMechanismUID()); // Passing the mechanismUID to the DenyReceiver
        denyIntent.putExtra("notificationId", notificationId); // Passing the notificationId to the DenyReceiver
        PendingIntent denyPendingIntent = PendingIntent.getBroadcast(this, 1, denyIntent, pendingIntentFlags);

        notificationBuilder.addAction(R.drawable.common_google_signin_btn_icon_dark, "Accept", acceptPendingIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_dark, "Deny", denyPendingIntent);

        Notification notification = notificationBuilder.build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(id, notification);
    }



//    private void createSystemNotification(PushNotification pushNotification) {
//        int id = messageCount++;
//
//        Mechanism mechanism = fraClient.getMechanism(pushNotification);
//        Intent intent = ForgeRockPlugin.setupIntent(this, MainActivity.class,
//                pushNotification, mechanism);
//        String title = String.format("Login attempt from %1$s at %2$s",
//                mechanism.getAccountName(), mechanism.getIssuer());//TODO
//        String body = "Tap to log in";//TODO
//
//        Notification notification = generatePending(this, id, title, body, intent);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        notificationManager.notify(id, notification);
//    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private Notification generatePending(Context context, int requestCode, String title, String message, Intent intent) {
        createNotificationChannel(context);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        }else {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        String channelId = "1";
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)//TODO
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                //.setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //String channelId = context.getString(R.string.channel_id);
            //String channelName = context.getString(R.string.channel_name);
            String channelId = "1";
            String channelName = "forgerock_channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // This FCM method is called if InstanceID token is updated. This may occur if the security
        // of the previous token had been compromised.
        // Currently OpenAM does not provide an API to receives updates for those tokens. So, there
        // is no method available to handle it FRAClient. The current workaround is removed the Push
        // mechanism and add it again by scanning a new QRCode.
    }

    /*
    Gets the token that was previously saved by the ForgeRockPlugin
     */
    public String getToken() {
        return getSharedPreferences("_", MODE_PRIVATE).getString("fcm_token", "empty");
    }
}



