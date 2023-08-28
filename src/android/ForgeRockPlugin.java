package com.outsystems.experts.forgerockplugin;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
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
    FRAClient fraClientInstance;
    OathMechanism oathMechanism;
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
            fraClientInstance = new FRAClient.FRAClientBuilder()
                    .withContext(this.cordova.getContext())
                    .start();
            callbackContext.success();
        } catch (AuthenticatorException e) {
            callbackContext.error("Error starting forge rock. Error was" + e.getMessage());
        }
    }

    private void registerForRemoteNotifications(String fcmToken, CallbackContext callbackContext){
        try {
            fraClientInstance.registerForRemoteNotifications(fcmToken);
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
        fraClientInstance.createMechanismFromUri(uri, new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(Mechanism mechanism) {
                Log.d("ForgeRockPlugin", "mechanism");
                oathMechanism = ((OathMechanism) mechanism);
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
}
