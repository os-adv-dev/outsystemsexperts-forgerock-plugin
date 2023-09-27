package com.outsystems.experts.forgerockplugin;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
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
    FRAClient fraClient = null;
    /**
     * Default instance of FcmService expected to be instantiated by Android framework.
     */
    public FcmService() {}
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.d(TAG, "⭐ reached service onMessageReceived");
        // Check if setNativeNotification was saved in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("_", Context.MODE_PRIVATE);
        boolean isSet = sharedPreferences.getBoolean("nativeNotificationSet", false);
        if (isSet) {
            Log.d(TAG, "⭐ Native notification");
            try {
                Log.d(TAG, "⭐ FcmToken: " + this.getToken());
                fraClient = new FRAClient.FRAClientBuilder().withDeviceToken(this.getToken()).withContext(getApplicationContext()).start();
            } catch (AuthenticatorException e) {
                throw new RuntimeException(e);
            }
            try {
                PushNotification pushNotification = fraClient.handleMessage(message);
                // If it's a valid Push message from AM and not expired, create a system notification
                if (pushNotification != null && !pushNotification.isExpired()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createNotificationChannel();
                    }
                    createSystemNotification(pushNotification);
                }
                Log.d(TAG, "✅ message handled");
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
     *
     * @param pushNotification the PushNotification object
     */
    private static final String CHANNEL_ID = "1";
    private static final String CHANNEL_NAME = "forge_rock";
    private NotificationManager manager;
    private void createSystemNotification(PushNotification pushNotification) {
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
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
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
        getManager().notify(notificationId, notification);
    }
    /**
     * create the channel for specific android versions if we do not have it doesn't work
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            getManager().createNotificationChannel(channel);
        }
    }
    public NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
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