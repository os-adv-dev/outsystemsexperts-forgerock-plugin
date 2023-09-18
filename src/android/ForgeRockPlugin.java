package com.outsystems.experts.forgerockplugin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.outsystems.experts.forgerocksample.MainActivity;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

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


/**
 * This class echoes a string called from JavaScript.
 */
public class ForgeRockPlugin extends CordovaPlugin {
    public static ForgeRockPlugin instance;
    OathMechanism oathMechanism;
    FRAClient fraClient;

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
        } else if(action.equals("getCurrentCode")){
            this.getCurrentCode(callbackContext);
            return true;
        }
        
        return false;
    }

    private void start(CallbackContext callbackContext) {
        try {
            fraClient = new FRAClient.FRAClientBuilder()
                    .withContext(this.cordova.getContext())
                    .start();
            instance = this;
            if (mechanism!=null && notification!=null){
                handleNotification(notification);
            }
            callbackContext.success();
        } catch (AuthenticatorException e) {
            callbackContext.error("Error starting forge rock. Error was" + e.getMessage());
        }
    }

    private void handleNotification(PushNotification pushNotification){
        showConfirm(pushNotification);
    }

    public void handleNotification(RemoteMessage message){
        try {
            PushNotification notification = fraClient.handleMessage(message);
            showConfirm(notification);
        } catch (InvalidNotificationException e) {
            throw new RuntimeException(e);
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

    public static Intent setupIntent(Context context,
                                     Class<? extends MainActivity> notificationActivity,
                                     PushNotification pushNotification, Mechanism pushMechanism) {
        Intent intent = new Intent(context, notificationActivity);
        notification = pushNotification;
        mechanism = pushMechanism;
        return intent;
    }


}
