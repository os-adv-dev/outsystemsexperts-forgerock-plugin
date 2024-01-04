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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import android.os.Bundle;
import java.util.Base64;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;


public class FcmService extends FirebaseMessagingService {
    private static final String TAG = "FcmService";
    FRAClient fraClient = null;
    /**
     * Default instance of FcmService expected to be instantiated by Android framework.
     */
    public FcmService() {}
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        boolean isTransactional = false;
        Log.d(TAG, "⭐ reached service onMessageReceived");

        // Check if setNativeNotification was saved in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("_", Context.MODE_PRIVATE);
        boolean isSet = sharedPreferences.getBoolean("nativeNotificationSet", false);

        String callbackMessage = "";

        String jwtToken = message.getData().get("message");
        if (jwtToken != null) {
            String messageContent = extractMessageFromJWT(jwtToken);
            callbackMessage = messageContent;
            Log.d(TAG, "Message Content: " + messageContent);
        }

        PushNotification pushNotification;
        JSONObject inAppJsonObject = new JSONObject();
        try {
            fraClient = new FRAClient.FRAClientBuilder().withDeviceToken(this.getToken()).withContext(getApplicationContext()).start();
            System.out.println("✉️ Message: " + message);
            pushNotification = fraClient.handleMessage(message);
            if (pushNotification != null) {
                String customPayload = pushNotification.getCustomPayload();
                JSONObject jsonObject = new JSONObject(customPayload);

                if (customPayload != null && jsonObject.length() > 0) {
//                    String customPayloadMessage = parseJsonForMessage(customPayload);
//                    if (customPayloadMessage != null){
//                        callbackMessage = customPayloadMessage;
//                    }
                    //1-Buscar o username e a transaction ID ✅
                    String username = parseJsonForObject("username", customPayload);
                    String transactionId = parseJsonForObject("transactionId", customPayload);

                    if (username != null && transactionId != null) {
                        isTransactional = true;
                        // Make the API call in a separate thread
                        String finalCallbackMessage = callbackMessage;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Get the SharedPreferences instance
                                    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                                    String transactionalPNApiURL = sharedPreferences.getString("transactionalPNApiURL", null);

                                    if (transactionalPNApiURL != null) {
                                        Log.d("ForgeRock", "Transactional PN API URL: " + transactionalPNApiURL);

                                        //2-Chamar a API com os dois inputs ✅
                                        // Prepare URL and HttpURLConnection
                                        URL url = new URL(transactionalPNApiURL);
                                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                                        // Set method and headers
                                        connection.setRequestMethod("POST");
                                        connection.setRequestProperty("Content-Type", "application/json; utf-8");
                                        connection.setRequestProperty("Accept", "application/json");
                                        connection.setRequestProperty("X-OpenAM-Username", username);
                                        connection.setRequestProperty("transactionId", transactionId);

                                        // Enable input and output streams
                                        connection.setDoOutput(true);

                                        // Read the response
                                        try (BufferedReader br = new BufferedReader(
                                                new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                                            StringBuilder response = new StringBuilder();
                                            String responseLine = null;
                                            while ((responseLine = br.readLine()) != null) {
                                                response.append(responseLine.trim());
                                            }
                                            //System.out.println(response.toString());

                                            //3-Alterar a variável "callbackMessage" para passar todo o conteúdo (transaction detail e etc) e encaminhá-la aos métodos que criam a notificação abaixo
                                            //4-Ao receber a resposta da API,formatar o json para que fique igual ao iOS

                                            try {
                                                // Parse the original JSON string
                                                JSONObject originalJson = new JSONObject(response.toString());

                                                // Extract and parse the successUrl JSON string
                                                String successUrlString = originalJson.getString("successUrl");
                                                JSONObject successUrlJson = new JSONObject(successUrlString);

                                                inAppJsonObject.put("successUrl", successUrlJson);
                                                inAppJsonObject.put("isTransaction", true);

                                                // Convert the new JSON object to string
                                                //inAppJsonObject.toString();

                                                //creating the notification
                                                if (isSet) {
                                                    Log.d(TAG, "⭐ Native notification");
                                                    try {

                                                        // If it's a valid Push message from AM and not expired, create a system notification
                                                        if (pushNotification != null && !pushNotification.isExpired()) {
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                createNotificationChannel();
                                                            }
                                                            createSystemNotification(pushNotification, finalCallbackMessage);
                                                        }
                                                        Log.d(TAG, "✅ message handled");
                                                    } catch (Exception e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                } else {
                                                    Log.d(TAG, "⭐ In-app Notifications in use");
                                                    Log.d(TAG, "Value of ForgeRockPlugin.instance: " + (ForgeRockPlugin.instance == null ? "null" : "not null"));

                                                    if (isAppInForeground()) {
                                                        Log.d(TAG, "⭐ In-app: App is in foreground!");
                                                        if (ForgeRockPlugin.instance != null) {
                                                            Log.d(TAG, "⭐ In-app: ForgeRockPlugin is instantiated!");
                                                            ForgeRockPlugin.instance.handleNotification(message, inAppJsonObject);
                                                        } else {
                                                            Log.d(TAG, "🚨 In-app: ForgePlugin not started?");
                                                        }
                                                    } else {
                                                        //💡💡💡 PRECISO TRATAR ISTO AINDA
                                                        Log.d(TAG, "⭐ In-app: App is NOT in foreground!");
                                                        String senderId = message.getFrom();
                                                        showPushNotification(message, senderId, finalCallbackMessage);
                                                    }
                                                }

                                            } catch (Exception e) {
                                                e.printStackTrace();

                                            }


                                        }
                                    } else {
                                        //DEVOLVER CALLBACK DE ERRO AQUI?
                                        Log.d("ForgeRock", "🚨 No Transactional PN API URL found in SharedPreferences");
                                    }


                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // Handle exceptions and errors
                                }
                            }
                        }).start();
                    } else {
                        //FALTA CALLBACK DE ERRO
                    }


                    //5-Devolver o resultado para o callback OS
                    //6-Depois falta arrumar o plugin wrapper para acomodar o novo json


                }
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!isTransactional) {
            if (isSet) {
                Log.d(TAG, "⭐ Native notification");
                try {

                    // If it's a valid Push message from AM and not expired, create a system notification
                    if (pushNotification != null && !pushNotification.isExpired()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            createNotificationChannel();
                        }
                        createSystemNotification(pushNotification, callbackMessage);
                    }
                    Log.d(TAG, "✅ message handled");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                Log.d(TAG, "⭐ In-app Notifications in use");
                Log.d(TAG, "Value of ForgeRockPlugin.instance: " + (ForgeRockPlugin.instance == null ? "null" : "not null"));


                if (isAppInForeground()) {
                    Log.d(TAG, "⭐ In-app: App is in foreground!");
                    if (ForgeRockPlugin.instance != null) {
                        Log.d(TAG, "⭐ In-app: ForgeRockPlugin is instantiated!");
                        ForgeRockPlugin.instance.handleNotification(message, inAppJsonObject);
                    } else {
                        Log.d(TAG, "🚨 In-app: ForgePlugin not started?");
                    }
                } else {
                    //💡💡💡 PRECISO TRATAR QUANDO A APP ESTÁ FECHADA OU EM BACKGROUND
                    //É preciso salvar as coisas nas shared preferences aqui!
                    //DEPOIS NA OUTRA CLASSE, NO MÉTODO didReceivePushNotificationSetCallback É PRECISO LER O SHARED PREFERENCE E CRIAR A IN-APP NOTIFICATION
                    Log.d(TAG, "⭐ In-app: App is NOT in foreground!");
                    String senderId = message.getFrom();
                    showPushNotification(message, senderId, callbackMessage);
                }
            }
        }
    }

    public String parseJsonForObject(String key, String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extractMessageFromJWT(String jwtToken) {
        String[] parts = jwtToken.split("\\."); // Split the JWT into its parts
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        String payload = parts[1]; // Get the payload part
        Base64.Decoder decoder = Base64.getUrlDecoder();
        byte[] decodedBytes = decoder.decode(payload); // Decode the payload
        String decodedPayload = new String(decodedBytes); // Convert to string

        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> payloadMap = new Gson().fromJson(decodedPayload, type); // Convert JSON string to Map

        return payloadMap.get("m"); // Return the message from the 'm' field
    }


    // Method to show a Notification when the App is in the background or killed and In-App notification is set.
    private void showPushNotification(RemoteMessage message, String senderId, String callbackMessage) {
        //ring title = "Attention Required";
        //String text = "An authorization request just arrived. Tap to view.";
        String title = "Please respond";
        String text = callbackMessage;

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("_", Context.MODE_PRIVATE);
        String savedTitle = sharedPreferences.getString("NotificationTitle", null);
        String savedMessage = sharedPreferences.getString("NotificationMessage", null);

        if(savedTitle != null && savedMessage != null) {
            title = savedTitle;
            text = savedMessage;
            Log.d("SavedNotification", "Title: " + savedTitle + ", Message: " + savedMessage);
        } else {
            // Values not found in SharedPreferences
            Log.d("SavedNotification", "No saved notification found");
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Convert RemoteMessage to JSON
        Gson gson = new Gson();
        String jsonMessage = gson.toJson(message);

        // Save JSON string in SharedPreferences
        //SharedPreferences sharedPreferences = getSharedPreferences("_", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("remoteMessage", jsonMessage);
        editor.putLong("messageTimestamp", System.currentTimeMillis());
        editor.putBoolean("launchedFromPush", true);
        editor.apply();

        // Add senderId to the intent
        intent.putExtra("senderId", senderId);

//        // Add the RemoteMessage data to the intent
//        for (Map.Entry<String, String> entry : message.getData().entrySet()) {
//            intent.putExtra(entry.getKey(), entry.getValue());
//        }

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // For Android Oreo and above, you need to create a Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }




    /**
     * Create system notification to display to user the Push request received
     *
     * @param pushNotification the PushNotification object
     */
    private static final String CHANNEL_ID = "1";
    private static final String CHANNEL_NAME = "forge_rock";
    private NotificationManager manager;
    private void createSystemNotification(PushNotification pushNotification, String callbackMessage) {
        int notificationId = pushNotification.getMessageId().hashCode();
        Log.d(TAG, "🤔 1 notificationId: " + notificationId);
        Mechanism mechanism = fraClient.getMechanism(pushNotification);
        Intent intent = ForgeRockPlugin.setupIntent(this, MainActivity.class, pushNotification, mechanism);
        //String title = String.format("Login attempt from %1$s at %2$s", mechanism.getAccountName(), mechanism.getIssuer());
        //String title = String.format(callbackMessage, mechanism.getAccountName(), mechanism.getIssuer());
        String title = "Please respond";
        //String body = "Please respond";
        String body = callbackMessage;
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

        // Set up PendingIntents with the appropriate flags for different Android versions
        int acceptPendingIntentFlags;
        int denyPendingIntentFlags;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            acceptPendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            denyPendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            acceptPendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            denyPendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            // For versions below Android M, use FLAG_ONE_SHOT for simplicity
            acceptPendingIntentFlags = PendingIntent.FLAG_ONE_SHOT;
            denyPendingIntentFlags = PendingIntent.FLAG_ONE_SHOT;
        }

        acceptIntent.putExtra("serializedPushNotification", serializedPushNotification);
        acceptIntent.putExtra("mechanismUID", mechanism.getMechanismUID()); // Passing the mechanismUID to the AcceptReceiver
        acceptIntent.putExtra("notificationId", notificationId); // Passing the notificationId to the AcceptReceiver
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 0, acceptIntent, acceptPendingIntentFlags);
        // Intent for the "Deny" action
        Intent denyIntent = new Intent(this, DenyReceiver.class);
        denyIntent.putExtra("serializedPushNotification", serializedPushNotification);
        denyIntent.putExtra("mechanismUID", mechanism.getMechanismUID()); // Passing the mechanismUID to the DenyReceiver
        denyIntent.putExtra("notificationId", notificationId); // Passing the notificationId to the DenyReceiver
        PendingIntent denyPendingIntent = PendingIntent.getBroadcast(this, 1, denyIntent, denyPendingIntentFlags);

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
            int importance = NotificationManager.IMPORTANCE_HIGH; // Use high importance for notification channel
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
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

    private boolean isAppInForeground() {
        Lifecycle.State currentState = ProcessLifecycleOwner.get().getLifecycle().getCurrentState();
        return currentState.isAtLeast(Lifecycle.State.RESUMED);
    }

}