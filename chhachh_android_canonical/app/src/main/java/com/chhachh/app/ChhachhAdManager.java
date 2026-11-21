package com.chhachh.app;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.webkit.WebView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * One native AdMob banner, never an overlay or pop-up. The WebView gets
 * the remaining height so Chhachh's footer cannot be covered by an ad.
 * Admin Center is the only owner of the publisher's banner unit/placements.
 */
public final class ChhachhAdManager {
    private static final String CONFIG_URL = "https://chhachh.com/api/android/ad-config";
    private static final String PUBLISHER = "ca-app-pub-9541416611227313/";
    private final Activity activity;
    private final LinearLayout layout;
    private final FrameLayout bannerHost;
    private AdView banner;
    private String activePlacement = "none";
    private String requestedUnit = "";
    private JSONObject config;
    private long configCheckedAt = 0;
    private boolean fetching = false;
    private boolean loading = false;
    private boolean loaded = false;

    public ChhachhAdManager(Activity a, FrameLayout root, WebView web) {
        activity = a;
        layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        root.addView(layout, new FrameLayout.LayoutParams(-1, -1));
        layout.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
        bannerHost = new FrameLayout(a);
        bannerHost.setVisibility(View.GONE);
        layout.addView(bannerHost, new LinearLayout.LayoutParams(-1, -2));
    }

    public void show(String placement) {
        if (!("home".equals(placement) || "business_list".equals(placement)
                || "business_detail".equals(placement) || "search".equals(placement)))
            placement = "none";
        activePlacement = placement;
        if ("none".equals(placement)) { hide(); return; }
        if (config == null || android.os.SystemClock.elapsedRealtime() - configCheckedAt > 300000L)
            refreshConfig();
        else apply();
    }

    private void refreshConfig() {
        if (fetching) return;
        fetching = true;
        new Thread(() -> {
            JSONObject next = null;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(CONFIG_URL).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() == 200) {
                    try (InputStream input = conn.getInputStream()) {
                        byte[] bytes = new byte[6000];
                        int size = 0;
                        while (size < bytes.length) {
                            int n = input.read(bytes, size, bytes.length-size);
                            if (n < 0) break;
                            size += n;
                        }
                        if (size > 0 && size < bytes.length)
                            next = new JSONObject(new String(bytes, 0, size, StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception ignored) { /* Offline: no ad is requested. */ }
            finally { if (conn != null) conn.disconnect(); }
            final JSONObject result = next;
            activity.runOnUiThread(() -> {
                fetching = false;
                config = result;
                configCheckedAt = android.os.SystemClock.elapsedRealtime();
                apply();
            });
        }, "chhachh-native-ad-config").start();
    }

    private void apply() {
        if (config == null || !config.optBoolean("enabled",false)
                || "none".equals(activePlacement)) {hide();return;}
        JSONObject places = config.optJSONObject("placements");
        if (places == null || !places.optBoolean(activePlacement, false)) {hide();return;}
        String unit = config.optString("unit_id", "");
        if (!unit.matches("ca-app-pub-9541416611227313/\\d{10}")) {hide();return;}
        if (banner != null && unit.equals(requestedUnit)) {
            if (loaded && bannerHost.getChildCount() > 0) bannerHost.setVisibility(View.VISIBLE);
            return;
        }
        hide();
        if (activity.isFinishing() || activity.isDestroyed()) return;
        AdView view = new AdView(activity);
        requestedUnit = unit;
        banner = view;
        loaded = false;
        int widthPx = activity.getResources().getDisplayMetrics().widthPixels;
        float scale = activity.getResources().getDisplayMetrics().density;
        int widthDp = Math.max(320, Math.round(widthPx / Math.max(1f,scale)));
        view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,widthDp));
        view.setAdUnitId(BuildConfig.DEBUG ?
                "ca-app-pub-3940256099942544/6300978111" : unit);
        view.setAdListener(new AdListener() {
            @Override public void onAdLoaded() {
                if (banner != view) return;
                loading = false;
                loaded = true;
                bannerHost.setVisibility("none".equals(activePlacement) ? View.GONE : View.VISIBLE);
            }
            @Override public void onAdFailedToLoad(LoadAdError error) {
                if (banner != view) return;
                loading = false;
                loaded = false;
                bannerHost.setVisibility(View.GONE);
                // Never simulate impressions or insert a fake ad tile.
            }
        });
        bannerHost.addView(view, new FrameLayout.LayoutParams(-1, -2));
        bannerHost.setVisibility(View.GONE);
        loading = true;
        view.loadAd(new AdRequest.Builder().build());
    }

    private void hide() {
        bannerHost.setVisibility(View.GONE);
        if (banner != null) {
            AdView previous = banner;
            banner = null;
            requestedUnit = "";
            loading = false;
            loaded = false;
            bannerHost.removeView(previous);
            previous.destroy();
        }
    }

    public void detachWeb(WebView web) {
        hide();
        if (web != null && web.getParent() instanceof ViewGroup)
            ((ViewGroup)web.getParent()).removeView(web);
    }

    public void destroy() {hide();}
}
