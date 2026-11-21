package com.chhachh.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.json.JSONObject;

/**
 * Firebase client identification contains no service-account private keys.
 * It can be saved by the owner later in Admin Center and fetched over HTTPS.
 * The stored options are loaded at process startup, before background FCM
 * services receive messages when the main activity is not running.
 */
public final class ChhachhApplication extends Application {
    private static final String PREFS = "chhachh_firebase_client";

    @Override public void onCreate() {
        super.onCreate();
        final String cached = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString("client_options", "");
        if (cached == null || cached.isEmpty()) return;
        try { initializeFromOptions(this, new JSONObject(cached)); }
        catch (Exception ignored) { /* Client not configured yet. */ }
    }

    public static synchronized boolean ready(Context context) {
        return !FirebaseApp.getApps(context.getApplicationContext()).isEmpty();
    }

    public static synchronized boolean initializeFromOptions(Context context, JSONObject data) {
        try {
            if (data == null || !data.optBoolean("configured", false) ||
                    !"com.chhachh.app".equals(data.optString("package_name"))) return false;
            final String appId = data.optString("app_id");
            final String apiKey = data.optString("api_key");
            final String projectId = data.optString("project_id");
            final String number = data.optString("project_number");
            if (!appId.matches("1:[0-9]{5,30}:android:[a-fA-F0-9]+") ||
                    !apiKey.matches("AIza[A-Za-z0-9_\\-]{20,100}") ||
                    !projectId.matches("[a-z0-9][a-z0-9-]{2,70}") ||
                    !number.matches("[0-9]{5,30}")) return false;
            if (!ready(context)) {
                FirebaseOptions opts = new FirebaseOptions.Builder()
                        .setApplicationId(appId)
                        .setApiKey(apiKey)
                        .setGcmSenderId(number)
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(context.getApplicationContext(), opts);
            }
            if (!ready(context)) return false;
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("client_options", data.toString()).apply();
            return true;
        } catch (Exception ignored) {return false;}
    }
}
