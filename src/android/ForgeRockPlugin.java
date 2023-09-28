package com.outsystems.experts.forgerockplugin;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.outsystems.experts.forgerocksample.MainActivity;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.PushMechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.exception.AuthenticatorException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


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

    private static final String TAG = "ForgeRockPlugin";
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("start")) {
            this.start(callbackContext);
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
        }
        
        return false;
    }

    private void start(CallbackContext callbackContext) {
        Log.d(TAG, "⭐️ Start CallbackId: " + callbackContext.getCallbackId());
        try {
            fraClient = new FRAClient.FRAClientBuilder()
                    .withContext(this.cordova.getContext())
                    .start();
            instance = this;
            callbackContext.success();
        } catch (AuthenticatorException e) {
            callbackContext.error("Error starting forge rock. Error was: " + e.getMessage());
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
        Log.d(TAG, "⭐️ handleNotification-PushNotification CallbackId: " + didReceivePnCallbackContext.getCallbackId());
        if (didReceivePnCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK);
            result.setKeepCallback(true);
            didReceivePnCallbackContext.sendPluginResult(result);
        }
    }

    public void handleNotification(RemoteMessage message){
        Log.d(TAG, "⭐️ handleNotification-RemoteMessage CallbackId: " + didReceivePnCallbackContext.getCallbackId());
        try {
            notification = fraClient.handleMessage(message);
            if (didReceivePnCallbackContext != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK);
                result.setKeepCallback(true);
                didReceivePnCallbackContext.sendPluginResult(result);
            }
        } catch (InvalidNotificationException e) {
            throw new RuntimeException(e);
        }
    }


    private void acceptAction(final CallbackContext callbackContext) {
        try {
            notification.accept(new FRAListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, ">***✅ Accept notification success");
                    notification = null;

//                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "Accept Action Successful");
//                    pluginResult.setKeepCallback(true); // This will keep the callback
//                    callbackContext.sendPluginResult(pluginResult);
                    callbackContext.success();
                }

                @Override
                public void onException(Exception e) {
                    notification = null;
                    Log.d(TAG, ">***❌ Accept notification exception");

//                    PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "Error: " + e.getMessage());
//                    pluginResult.setKeepCallback(true); // This will keep the callback
//                    callbackContext.sendPluginResult(pluginResult);
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
                    Log.d(TAG, ">***✅ Deny notification success");

//                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "Deny Action Successful");
//                    pluginResult.setKeepCallback(true); // This will keep the callback
//                    callbackContext.sendPluginResult(pluginResult);
                    callbackContext.success();
                }

                @Override
                public void onException(Exception e) {
                    Log.d(TAG, ">***❌ Deny notification exception");

//                    PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "Error: " + e.getMessage());
//                    pluginResult.setKeepCallback(true); // This will keep the callback
//                    callbackContext.sendPluginResult(pluginResult);
                    callbackContext.error(e.getMessage());
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error accepting action. Error: " + e.getMessage());
        }
    }


    private void didReceivePushNotificationSetCallback(CallbackContext callbackContext) {
        this.didReceivePnCallbackContext = callbackContext;
        Log.d(TAG, "🤔 just received a pushnotification");

        // Check if the app was opened by a PN click
        SharedPreferences sharedPreferences = cordova.getContext().getSharedPreferences("_", Context.MODE_PRIVATE);
        boolean launchedFromPush = sharedPreferences.getBoolean("launchedFromPush", false);
        Log.d(TAG, "👉 launchedFromPush: " + launchedFromPush);

        if (launchedFromPush) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("💡 launchedFromPush", false);
            editor.apply();

            if (callbackContext != null) {
                Log.d(TAG, "👉 Callback sent");
                PluginResult result = new PluginResult(PluginResult.Status.OK);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
            }
        } else {
            Log.d(TAG, "👉 Not launched from push!");
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
        if (actionToCheck.equals(action)) {

            // The app was opened by a push notification click
            Context context = cordova.getActivity().getApplicationContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences("_", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("launchedFromPush", true);
            editor.apply();

        }
    }

    private void showConfirm(PushNotification notification){
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(cordova.getContext())
                        .setTitle("OutSystems app")
                        .setMessage("Would you like to accept this request?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) ->{
                            Toast.makeText(cordova.getContext(), "Accepted", Toast.LENGTH_SHORT).show();
                            notification.accept(new FRAListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Log.d(TAG, "notification success");
                                }

                                @Override
                                public void onException(Exception e) {
                                    Log.d(TAG, "notification exception");
                                }
                            });
                        })
                        .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {
                            Toast.makeText(cordova.getContext(), "Declined", Toast.LENGTH_SHORT).show();
                            notification.deny(new FRAListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Log.d(TAG, "notification success");
                                }

                                @Override
                                public void onException(Exception e) {
                                    Log.d(TAG, "notification exception");
                                }
                            });
                        }).show();

            }
        });
    }

    private void registerForRemoteNotifications(String fcmToken, CallbackContext callbackContext){
        try {
            fraClient.registerForRemoteNotifications(fcmToken);
            cordova.getContext().getSharedPreferences("_", Context.MODE_PRIVATE).edit().putString("fcm_token", fcmToken).apply();
            Log.d(TAG, "new token received" + fcmToken);
            //FCM myFirebase = new MyFirebase();
            callbackContext.success();
        } catch (AuthenticatorException e) {
            callbackContext.error("Error registering for remote notifications. Error was" + e.getMessage());
        }
    }


    private void getCurrentCode(CallbackContext callbackContext) {
        try {
            OathTokenCode token = oathMechanism.getOathTokenCode();
            String tokenJson = tokenToJson(token);
            //String otp = token.getCurrentCode();
            callbackContext.success(tokenJson);
        } catch (OathMechanismException e) {
            callbackContext.error("Error getting current code. Error was" + e.getMessage());
        }
    }

    private void createMechanismFromUri(String uri, CallbackContext callbackContext) {

        fraClient.createMechanismFromUri(uri, new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(Mechanism mechanism) {
                Log.d("ForgeRockPlugin", "mechanism");
                //oathMechanism = ((OathMechanism) mechanism);
                PushMechanism push = ((PushMechanism) mechanism);
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

    /**
     * OathTokenCode already provides a toJson function that returns a JSON that we could use.
     * This is just to make sure we have and control the output for both iOS and Android
     * @return
     */
    private String tokenToJson(OathTokenCode oathTokenCode){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("code", oathTokenCode.getCurrentCode());
            jsonObject.put("start", oathTokenCode.getStart());
            jsonObject.put("until", oathTokenCode.getUntil());
            jsonObject.put("oathType", oathTokenCode.getOathType());
        } catch (JSONException e) {
            Logger.warn(TAG, e, "Error parsing OathTokenCode object to JSON");
            throw new RuntimeException("Error parsing OathTokenCode object to JSON string representation.", e);
        }
        return jsonObject.toString();
    }

    //TODO! Check
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


