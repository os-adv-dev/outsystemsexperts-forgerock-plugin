package com.outsystems.experts.forgerockplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.outsystems.experts.forgerocksample.MainActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.exception.AuthenticatorException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;


/**
 * This class echoes a string called from JavaScript.
 */
public class ForgeRockPlugin extends CordovaPlugin {
    public static ForgeRockPlugin instance;
    OathMechanism oathMechanism;
    FRAClient fraClient;
    CallbackContext  didReceivePnCallbackContext;

    private static PushNotification notification;
    private static Mechanism mechanism;

    private  String customPayload;

    private static final String TAG = "ForgeRockPlugin";
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("start")) {
            String transactionalPNApiURLString = args.getString(0);
            this.start(transactionalPNApiURLString, callbackContext);
            return true;
        } else if(action.equals("registerForRemoteNotifications")){
            String fcmToken = args.getString(0);
            this.registerForRemoteNotifications(fcmToken, callbackContext);
            return true;
        } else if(action.equals("createMechanismFromUri")){
            String uri = args.getString(0);
            this.createMechanismFromUri(uri, callbackContext);
            return true;
        }  else if(action.equals("acceptAction")){
            this.acceptAction(callbackContext);
            return true;
        } else if(action.equals("denyAction")){
            this.denyAction(callbackContext);
            return true;
        } else if(action.equals("didReceivePushNotificationSetCallback")){
            this.didReceivePushNotificationSetCallback(callbackContext);
            return true;
        } else if(action.equals("setNativeNotification")){
            Boolean isSet = args.getBoolean(0);
            this.setNativeNotification(isSet, callbackContext);
            return true;
        } else if(action.equals("setNativeNotificationTitleMessage")){
            String title = args.getString(0);
            String message = args.getString(1);
            this.setNativeNotificationTitleMessage(title, message, callbackContext);
            return true;
        } else if(action.equals("removeAccount")){
            String userToBeRemoved = args.getString(0);
            this.removeAccount(userToBeRemoved,callbackContext);
            return true;
        }

        return false;
    }

    private void start(String transactionalPNApiURLString, CallbackContext callbackContext) {
        try {
            // Save the value in SharedPreferences
            SharedPreferences sharedPreferences = cordova.getActivity().getSharedPreferences("_", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("transactionalPNApiURL", transactionalPNApiURLString);
            editor.apply();
            fraClient = new FRAClient.FRAClientBuilder()
                    .withContext(this.cordova.getContext())
                    .start();
            instance = this;
            callbackContext.success();
        } catch (AuthenticatorException e) {
            callbackContext.error("Error starting forge rock. Error was: " + e.getMessage());
        }
    }

    private void setNativeNotificationTitleMessage(String title, String message, CallbackContext callbackContext){
        try {
            SharedPreferences sharedPreferences = cordova.getContext().getSharedPreferences("_", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("NotificationTitle", title);
            editor.putString("NotificationMessage", message);
            editor.apply();
            callbackContext.success();
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }

    private void setNativeNotification(Boolean isSet, CallbackContext callbackContext) {
        try {
            SharedPreferences sharedPreferences = cordova.getContext().getSharedPreferences("_", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("nativeNotificationSet", isSet);
            editor.apply();
            callbackContext.success();
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }


    void handleNotification(PushNotification pushNotification){
        if (didReceivePnCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK);
            result.setKeepCallback(true);
            didReceivePnCallbackContext.sendPluginResult(result);
        }
    }

    public void handleNotification(RemoteMessage message, JSONObject inAppJsonObject){
        try {
            notification = fraClient.handleMessage(message);
            if (didReceivePnCallbackContext != null) {

                String callbackMessage = "";
                String jwtToken = message.getData().get("message");
                if (jwtToken != null) {
                    String messageContent = extractMessageFromJWT(jwtToken);
                    callbackMessage = messageContent;
                }

                JSONObject jsonResultObject = new JSONObject();

                PushNotification pushNotification = fraClient.handleMessage(message);
                if (pushNotification != null) {
                    String customPayload = pushNotification.getCustomPayload();

                    try {
                        JSONObject jsonObject = new JSONObject(customPayload);
                        // An empty JSON object will have a length of 0
                        if (customPayload != null && jsonObject.length() > 0){
                            if (inAppJsonObject != null) {
                                jsonResultObject.put("message", inAppJsonObject.getString("successUrl"));
                                jsonResultObject.put("isTransaction", true);
                            } else {
                                //CALLBACK
                            }
                        } else {
                            jsonResultObject.put("message", callbackMessage);
                            jsonResultObject.put("isTransaction", false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        //CALLBACK
                    }
                }

                PluginResult result = new PluginResult(PluginResult.Status.OK, jsonResultObject.toString());
                result.setKeepCallback(true);
                didReceivePnCallbackContext.sendPluginResult(result);
            }
        } catch (InvalidNotificationException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeAccount(String userToBeRemoved, CallbackContext callbackContext){
        if (userToBeRemoved != null && !userToBeRemoved.isEmpty()) {
            List<PushNotification> allNotifications = fraClient.getAllNotifications();

            if (allNotifications != null && !allNotifications.isEmpty()) {
                Mechanism mechanismToBeRemoved = null;

                for (PushNotification notification : allNotifications) {
                    Mechanism mechanism = fraClient.getMechanism(notification);
                    if (mechanism != null && userToBeRemoved.equals(mechanism.getAccountName())) {
                        mechanismToBeRemoved = mechanism;
                        break;
                    }
                }

                if (mechanismToBeRemoved != null) {
                    boolean userRemoved = fraClient.removeMechanism(mechanismToBeRemoved);
                    if (userRemoved) {
                        callbackContext.success("User " + mechanismToBeRemoved.getAccountName() + " removed");
                    } else {
                        callbackContext.error("Error: Could not remove user");
                    }
                } else {
                    callbackContext.error("Error: Could not extract mechanism from notification");
                }
            } else {
                callbackContext.error("Error: user not found");
            }
        } else {
            callbackContext.error("Error: Missing mandatory username attribute");
        }
    }


    public String parseJsonForMessage(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.getString("message");
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

    private void acceptAction(final CallbackContext callbackContext) {
        try {
            notification.accept(new FRAListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    notification = null;
                    callbackContext.success();
                }

                @Override
                public void onException(Exception e) {
                    notification = null;
                    callbackContext.error(e.getMessage());
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error accepting action. Error: " + e.getMessage());
        }
    }


    private void denyAction(final CallbackContext callbackContext) {
        try {
            notification.deny(new FRAListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    callbackContext.success();
                }

                @Override
                public void onException(Exception e) {
                callbackContext.error(e.getMessage());
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error accepting action. Error: " + e.getMessage());
        }
    }

    private void didReceivePushNotificationSetCallback(CallbackContext callbackContext) throws JSONException {
        this.didReceivePnCallbackContext = callbackContext;

        // Check if the app was opened by a PN click
        SharedPreferences sharedPreferences = cordova.getContext().getSharedPreferences("_", Context.MODE_PRIVATE);

        boolean launchedFromPush = sharedPreferences.getBoolean("launchedFromPush", false);

        if (launchedFromPush) {
            //Check if is transactional PN or not
            long messageTimestamp = sharedPreferences.getLong("messageTimestamp", 0);

            // Check if the message was sent in the last 5 minutes

            if (System.currentTimeMillis() - messageTimestamp < 5 * 60 * 1000) {
                String jsonMessage = sharedPreferences.getString("remoteMessage", null);
                String inAppJsonString = sharedPreferences.getString("inAppJsonObject", null);
                if (jsonMessage != null) {
                    Gson gson = new Gson();
                    RemoteMessage message = gson.fromJson(jsonMessage.toString(), RemoteMessage.class);
                    JSONObject inAppJsonObject = new JSONObject(inAppJsonString);

                    // Remove data from SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("remoteMessage");
                    editor.remove("inAppJsonObject");
                    editor.remove("messageTimestamp");
                    editor.remove("launchedFromPush");
                    editor.apply();

                    if (callbackContext != null) {
                        // Extract and parse the successUrl JSON string
                        String successUrlString = inAppJsonObject.getString("successUrl");
                        boolean isTransaction = inAppJsonObject.getBoolean("isTransaction");
                        JSONObject resultJson = new JSONObject();

                        resultJson.put("message", inAppJsonObject.getString("successUrl"));
                        resultJson.put("isTransaction", isTransaction);

                        PluginResult result = new PluginResult(PluginResult.Status.OK, resultJson.toString());
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                }


            } else {
                // Message is older than 5 minutes, remove it
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("remoteMessage");
                editor.remove("messageTimestamp");
                editor.apply();
                notification = null;
            }

        } else {
            //CALLBACK
            Log.d(TAG, "Not launched from push!");
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);

        // Getting the activity's intent
        Intent intent = cordova.getActivity().getIntent();
        String action = intent.getAction();

        // Check if the app was awakened by a push notification
        String packageName = cordova.getActivity().getPackageName();
        String actionToCheck = packageName + ".PUSH_NOTIFICATION";
    }

    private void registerForRemoteNotifications(String fcmToken, CallbackContext callbackContext){
        try {
            fraClient.registerForRemoteNotifications(fcmToken);
            cordova.getContext().getSharedPreferences("_", Context.MODE_PRIVATE).edit().putString("fcm_token", fcmToken).apply();
            callbackContext.success();
        } catch (AuthenticatorException e) {
            callbackContext.error("Error registering for remote notifications. Error was" + e.getMessage());
        }
    }

    private void createMechanismFromUri(String uri, CallbackContext callbackContext) {

        fraClient.createMechanismFromUri(uri, new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(Mechanism mechanism) {
                //PushMechanism push = ((PushMechanism) mechanism);

                // called when device enrollment was successful.
                callbackContext.success();
            }

            @Override
            public void onException(Exception e) {
                // called when device enrollment has failed.
                callbackContext.error("Error creating mechanism from Uri. Error was" + e.getMessage());
            }
        });
    }

    public static Intent setupIntent(Context context,
                                     Class<? extends MainActivity> notificationActivity,
                                     PushNotification pushNotification, Mechanism pushMechanism) {
        Intent intent = new Intent(context, notificationActivity);
        notification = pushNotification;
        mechanism = pushMechanism;
        return intent;
    }

    public static class ForgeRockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle the received intent here
        }
    }
}


