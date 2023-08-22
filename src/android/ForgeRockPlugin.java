package com.outsystems.experts.forgerockplugin;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.exception.AuthenticatorException;


/**
 * This class echoes a string called from JavaScript.
 */
public class ForgeRockPlugin extends CordovaPlugin {
    public static FRAClient fraClientInstance;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("start")) {
            String message = args.getString(0);
            this.start(message, callbackContext);
            return true;
        } else if(action.equals("createMechanismFromUri")){
            String message = args.getString(0);
            this.createMechanismFromUri(message, callbackContext);
            return true;
        }
        return false;
    }

    private void start(String message, CallbackContext callbackContext) {
        try {
            fraClientInstance = new FRAClient.FRAClientBuilder()
                    .withContext(this.cordova.getContext())
                    .start();
        } catch (AuthenticatorException e) {
            callbackContext.error(e.getMessage());
        }
    }


    private void getCurrentCode(String message, CallbackContext callbackContext) {
        /*OathTokenCode token = oath.getOathTokenCode();
        String otp = token.getCurrentCode();

        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }*/
    }

    private void createMechanismFromUri(String message, CallbackContext callbackContext) {
        fraClientInstance.createMechanismFromUri("qrcode_scan_result", new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(Mechanism mechanism) {
                Log.d("ForgeRockPlugin", "mechanism");
                //((OathMechanism) mechanism).getOathTokenCode();
                // called when device enrollment was successful.
            }

            @Override
            public void onException(Exception e) {
                // called when device enrollment has failed.
            }
        });

        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
