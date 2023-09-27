package com.outsystems.experts.forgerockplugin;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.iid.FirebaseInstanceIdReceiver;
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
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
        Log.d(TAG, "reached service onMessageReceived");
        if (ForgeRockPlugin.instance == null) {
            try {
                fraClient = new FRAClient.FRAClientBuilder().withDeviceToken(this.getToken()).withContext(getApplicationContext()).start();
            } catch (AuthenticatorException e) {
                throw new RuntimeException(e);
            }
            try {
                PushNotification pushNotification = null;
                    pushNotification = fraClient.handleMessage(message);
                    // If it's a valid Push message from AM and not expired, create a system notification
                    if(pushNotification != null && !pushNotification.isExpired()) {
                        //createSystemNotification(pushNotification);
                        ForgeRockPlugin.instance.handleNotification(pushNotification);
                    }
                    Log.d(TAG, "message handled");
            } catch (InvalidNotificationException e) {
                throw new RuntimeException(e);
            }
        } else {
            ForgeRockPlugin.instance.handleNotification(message);
        }
    }

    /**
     * Create system notification to display to user the Push request received
     * @param pushNotification the PushNotification object
     */
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
            String channelId = "1";//TOOD
            String channelName = "channel name";//TODO
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