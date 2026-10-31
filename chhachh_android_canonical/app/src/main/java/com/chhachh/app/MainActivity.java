package com.chhachh.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.location.LocationManager;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.util.Base64;
import android.media.MediaScannerConnection;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final String HOME = "https://chhachh.com/";
    private static final String BUILD_ENDPOINT = "https://chhachh.com/__build";
    private final Handler releaseHandler = new Handler(Looper.getMainLooper());
    private boolean releaseRunning = false, releaseForeground = false;
    private long lastReleaseCheck = 0L;
    private String releaseReloaded = "";
    private final Runnable releaseTimer = new Runnable() {
        @Override public void run() {
            if (!releaseForeground || web == null) return;
            checkCloudflareRelease();
            releaseHandler.postDelayed(this, 90000L);
        }
    };
    private static final int REQUEST_FILES = 401;
    private static final int REQUEST_MEDIA_PERMISSION = 402;
    private static final int REQUEST_LEGACY_STORAGE = 403;
    private static final int REQUEST_GEOLOCATION_PERMISSION = 404;
    private static final int REQUEST_CAMERA_CAPTURE_PERMISSION = 405;
    private WebView web;
    private ProgressBar loadingRing;
    private long navigationToken = 0;
    private float refreshGestureStartY = -1;
    private FrameLayout root, offline;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private ValueCallback<Uri[]> fileCallback;
    private final ArrayList<Uri> cameraFiles = new ArrayList<>();
    private boolean pickerLaunched = false;
    private WebChromeClient.FileChooserParams pendingChooserParams;
    private PermissionRequest pendingPermission;
    private String[] pendingResources;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private volatile String nativeDownloadToken = UUID.randomUUID().toString();
    private final NativeDownloads nativeDownloads = new NativeDownloads();
    private final NativeShare nativeShare = new NativeShare();
    private boolean returningFromLocationSettings = false;

    private boolean isTrusted(Uri uri) {
        String host = uri == null ? null : uri.getHost();
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) &&
                host != null && (host.equalsIgnoreCase("chhachh.com") ||
                host.equalsIgnoreCase("www.chhachh.com"));
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        // Keep the approved round cyan loading feedback, never the old blue line
        // or a full-screen white overlay. Only actual main-frame navigation shows it.
        loadingRing = new ProgressBar(this);
        loadingRing.setIndeterminateTintList(ColorStateList.valueOf(Color.rgb(0, 242, 255)));
        int ringSize = Math.round(26 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams ringLayout = new FrameLayout.LayoutParams(
                ringSize, ringSize, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        ringLayout.topMargin = Math.round(6 * getResources().getDisplayMetrics().density);
        root.addView(loadingRing, ringLayout);
        loadingRing.setVisibility(View.GONE);
        enableSafeHomeRefresh();
        configureWindowInsets();
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        // Keep image/video cache for performance; clear only for a new release.
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Respect the site's device-width viewport and prevent WebView-only desktop scaling.
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true); // Android document picker content URIs
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) s.setSafeBrowsingEnabled(true);
        s.setUserAgentString(s.getUserAgentString() + " ChhachhNativeAndroid/1.0");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isTrusted(uri)) return false;
                openOutside(uri);
                return true;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) showOffline();
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel(); // Never ignore invalid certificates.
                showOffline();
            }
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                nativeDownloadToken = UUID.randomUUID().toString();
                long token = ++navigationToken;
                view.postDelayed(() -> {
                    if (token == navigationToken && loadingRing != null)
                        loadingRing.setVisibility(View.VISIBLE);
                }, 180);
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (url != null && isTrusted(Uri.parse(url))) {
                    view.evaluateJavascript("Object.defineProperty(window,'__chhachhNativeToken',{value:'"
                        + nativeDownloadToken + "',configurable:true})", null);
                }
                ++navigationToken;
                if (loadingRing != null) loadingRing.setVisibility(View.GONE);
                // Deliberately no progress bar. Website owns its round loader.
                if (url != null && isTrusted(Uri.parse(url))) hideOffline();
            }
            @Override public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                recreate();
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                clearUnusedCameraFiles(null);
                fileCallback = callback;
                // A live camera request is the only upload action that asks for CAMERA.
                if (params.isCaptureEnabled()
                        && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                        && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    pendingChooserParams = params;
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CAPTURE_PERMISSION);
                    return true;
                }
                boolean needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                        && (accepts(params.getAcceptTypes(), "image") || accepts(params.getAcceptTypes(), "video"))
                        && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
                if (needsLegacyPermission) {
                    pendingChooserParams = params;
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_LEGACY_STORAGE);
                } else {
                    launchFileChooser(params, true);
                }
                return true;
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                runOnUiThread(() -> handleGeolocationRequest(origin, callback));
            }
            @Override public void onPermissionRequest(PermissionRequest req) {
                runOnUiThread(() -> handleMediaRequest(req));
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest req) {
                runOnUiThread(() -> {
                    if (pendingPermission == req) {
                        pendingPermission = null;
                        pendingResources = null;
                    }
                });
            }
            @Override public void onShowCustomView(View view, CustomViewCallback cb) {
                // Chhachh owns inline/fullscreen video UI, never Android native chrome.
                if (web != null && isTrusted(Uri.parse(String.valueOf(web.getUrl())))) {
                    cb.onCustomViewHidden(); return;
                }
                if (fullscreenView != null) { cb.onCustomViewHidden(); return; }
                fullscreenView = view;
                fullscreenCallback = cb;
                web.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                root.requestApplyInsets();
                getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
            }
            @Override public void onHideCustomView() { exitVideoFullscreen(); }
        });
        web.addJavascriptInterface(nativeDownloads, "ChhachhNativeDownloads");
        web.addJavascriptInterface(nativeShare, "ChhachhNativeShare");
        web.setDownloadListener((url, ua, disposition, mime, len) -> download(url, ua, disposition, mime));
        web.loadUrl(HOME);
    }

    // Independent native release check. An old WebView document may still run
    // old JS even after the Cloudflare site has been deployed.
    private void checkCloudflareRelease() {
        if (!releaseForeground || web == null || releaseRunning || fullscreenView != null) return;
        String here = web.getUrl();
        if (here == null || !isTrusted(Uri.parse(here))) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (lastReleaseCheck != 0L && now - lastReleaseCheck < 60000L) return;
        lastReleaseCheck = now;
        releaseRunning = true;
        new Thread(() -> {
            String remote = null;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(BUILD_ENDPOINT).openConnection();
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.setRequestProperty("Cache-Control", "no-cache, no-store");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        remote = reader.readLine();
                    }
                }
            } catch (Exception ignored) {
                // Offline is not an error: preserve the current page.
            } finally {
                if (conn != null) conn.disconnect();
            }
            final String target = remote == null ? "" : remote.trim();
            runOnUiThread(() -> {
                releaseRunning = false;
                if (!releaseForeground || web == null || fullscreenView != null) return;
                // Compare the whole V<major>.<minor> release, not just the V97 minor number.
                if (!target.matches("V[0-9]{1,4}\\.[0-9]{1,9}-[A-Za-z0-9._-]{1,85}") ||
                        target.equals(releaseReloaded)) return;
                long remoteNumber;
                try {
                    String[] pair = target.substring(1, target.indexOf('-')).split("\\.");
                    remoteNumber = Long.parseLong(pair[0]) * 1000000000L + Long.parseLong(pair[1]);
                } catch (Exception badVersion) { return; }
                if (remoteNumber <= 0) return;
                // Never reload during draft entry, file selection, recording or posting.
                final String script = """
                    (function() {
                      try {
                        var local = String(document.body && document.body.dataset.build || '');
                        var match = /^V(\\d+)\\.(\\d+)-/.exec(local);
                        var current = match ? (Number(match[1]) * 1000000000 + Number(match[2])) : 0;
                        if (!current || current >= __VERSION__ || document.hidden) return 'skip';
                        var a = document.activeElement;
                        if (a && a.matches('textarea,input,select,[contenteditable]')) return 'busy';
                        if (document.querySelector('.is-recording,.is-uploading,[data-recording="1"],[data-submitting="1"]')) return 'busy';
                        for (var f of document.querySelectorAll('input[type=file]')) {
                          if (f.files && f.files.length) return 'busy';
                        }
                        for (var i of document.querySelectorAll('textarea,input:not([type=hidden]),[contenteditable=true]')) {
                          if (i.isContentEditable && i.textContent.trim()) return 'busy';
                          if ('value' in i && i.type !== 'checkbox' && i.type !== 'radio' &&
                              i.value !== i.defaultValue) return 'busy';
                        }
                        if (typeof hasUnsavedReleaseWork === 'function' && hasUnsavedReleaseWork()) return 'busy';
                        return 'ready';
                      } catch (e) { return 'skip'; }
                    })()
                    """.replace("__VERSION__", Long.toString(remoteNumber));
                web.evaluateJavascript(script, result -> {
                    if (!releaseForeground || web == null || !"\"ready\"".equals(result)) return;
                    String page = web.getUrl();
                    if (page == null || !isTrusted(Uri.parse(page))) return;
                    releaseReloaded = target; // one reload at most per build
                    web.clearCache(true); // never clears cookies or localStorage
                    Uri current = Uri.parse(page);
                    Uri.Builder next = current.buildUpon().clearQuery();
                    for (String key : current.getQueryParameterNames()) {
                        if ("__mc_release".equals(key)) continue;
                        for (String val : current.getQueryParameters(key))
                            next.appendQueryParameter(key, val);
                    }
                    next.appendQueryParameter("__mc_release", target);
                    web.loadUrl(next.build().toString());
                });
            });
        }, "ChhachhReleaseCheck").start();
    }

    // Android 15+ enforces edge-to-edge windows for target SDK 35+. The WebView
    // must start *below* the system status bar and end above system navigation.
    // This is owned by the Android activity, never by website CSS/header patches.
    private void configureWindowInsets() {
        if (Build.VERSION.SDK_INT < 35) return; // Earlier Android fits the decor already.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            if (fullscreenView != null) {
                view.setPadding(0, 0, 0, 0);
            } else {
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return insets;
        });
        root.requestApplyInsets();
    }

    // Restore pull-to-refresh on the Home feed only. The user's finger can
    // scroll a chat or a nested message list without ever reloading the WebView.
    private void enableSafeHomeRefresh() {
        web.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    refreshGestureStartY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    if (refreshGestureStartY >= 0 &&
                            event.getY() - refreshGestureStartY >
                                145 * getResources().getDisplayMetrics().density &&
                            web.getScrollY() <= 1) {
                        // Check the actual SPA route and scroll owner; Android
                        // cannot infer it from WebView.getScrollY alone.
                        web.evaluateJavascript(
                          "(function(){var p=document.querySelector('main#app');" +
                          "return !!(p&&p.querySelector('.home-page-shell')&&" +
                          "!p.querySelector('.message-page-frame,.message-chat-page')&&" +
                          "!document.documentElement.classList.contains('mc-chat-active')&&" +
                          "!(document.scrollingElement&&document.scrollingElement.scrollTop>2)&&" +
                          "!document.querySelector('[role=dialog]:not([hidden]),.mc-media-viewer:not([hidden])'))})()",
                          allowed -> {
                              if ("true".equals(allowed) && web != null) web.reload();
                          });
                    }
                    refreshGestureStartY = -1;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    refreshGestureStartY = -1;
                    break;
            }
            return false; // Keep native scroll, tap, media and gesture handling intact.
        });
    }

    private boolean accepts(String[] types, String category) {
        if (types == null || types.length == 0) return true;
        for (String type : types) {
            if (type == null || type.isEmpty() || type.equals("*/*") || type.contains(category)) return true;
        }
        return false;
    }

    // The file picker has a single entry point for gallery, camera and video capture.
    private void launchFileChooser(WebChromeClient.FileChooserParams params, boolean allowCamera) {
        pendingChooserParams = null;
        try {
            Intent picker = params.createIntent();
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            ArrayList<Intent> cameraChoices = new ArrayList<>();
            if (allowCamera && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                    && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                boolean images = accepts(params.getAcceptTypes(), "image");
                boolean videos = accepts(params.getAcceptTypes(), "video");
                if (images) {
                    Intent capture = captureIntent(false);
                    if (capture != null) cameraChoices.add(capture);
                }
                if (videos) {
                    Intent capture = captureIntent(true);
                    if (capture != null) cameraChoices.add(capture);
                }
            }
            if (params.isCaptureEnabled() && !cameraChoices.isEmpty()) {
                startActivityForResult(cameraChoices.get(0), REQUEST_FILES);
            } else {
                Intent chooser = Intent.createChooser(picker, "Choose media");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        cameraChoices.toArray(new Intent[0]));
                startActivityForResult(chooser, REQUEST_FILES);
            }
            pickerLaunched = true;
        } catch (Exception error) {
            clearUnusedCameraFiles(null);
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = null;
            pickerLaunched = false;
        }
    }

    private Intent captureIntent(boolean video) {
        String mime = video ? "video/mp4" : "image/jpeg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                "chhachh_" + System.currentTimeMillis() + (video ? ".mp4" : ".jpg"));
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        Uri collection = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = getContentResolver().insert(collection, values);
        if (uri == null) return null;
        cameraFiles.add(uri);
        Intent intent = new Intent(video ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.setClipData(ClipData.newUri(getContentResolver(), "Chhachh media capture", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return intent;
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQUEST_FILES || fileCallback == null) return;
        Uri[] output = null;
        if (result == RESULT_OK) {
            if (data != null && data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                output = new Uri[n];
                for (int i = 0; i < n; i++) output[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data != null && data.getData() != null) {
                output = new Uri[] { data.getData() };
            } else {
                Uri captured = findRecordedCameraFile();
                if (captured != null) output = new Uri[] { captured };
            }
        }
        clearUnusedCameraFiles(output);
        fileCallback.onReceiveValue(output);
        fileCallback = null;
        pickerLaunched = false;
    }

    private boolean contains(Uri[] list, Uri item) {
        if (list == null) return false;
        for (Uri uri : list) if (item.equals(uri)) return true;
        return false;
    }

    // Separate photo/video output placeholders; only return a nonempty capture.
    private Uri findRecordedCameraFile() {
        for (Uri uri : cameraFiles) {
            try (android.os.ParcelFileDescriptor fd =
                         getContentResolver().openFileDescriptor(uri, "r")) {
                if (fd != null && fd.getStatSize() > 0) return uri;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private void clearUnusedCameraFiles(Uri[] selected) {
        for (Uri uri : cameraFiles) {
            if (!contains(selected, uri)) {
                try { getContentResolver().delete(uri, null, null); }
                catch (Exception ignored) { }
            }
        }
        cameraFiles.clear();
    }

    // A user-initiated At... action requests device location through the trusted WebView origin.
    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isDeviceLocationOn() {
        try {
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (manager == null) return false;
            if (Build.VERSION.SDK_INT >= 28) return manager.isLocationEnabled();
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                   manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (RuntimeException ex) { return false; }
    }

    private void finishGeolocation(boolean allowed) {
        GeolocationPermissions.Callback cb = pendingGeoCallback;
        String origin = pendingGeoOrigin;
        pendingGeoCallback = null;
        pendingGeoOrigin = null;
        returningFromLocationSettings = false;
        if (cb != null) cb.invoke(origin, allowed, allowed);
    }

    private void afterLocationPermission() {
        if (pendingGeoCallback == null) return;
        if (!hasLocationPermission()) { finishGeolocation(false); return; }
        if (isDeviceLocationOn()) { finishGeolocation(true); return; }
        // System-wide Location can only be enabled by the user.
        new AlertDialog.Builder(this)
            .setTitle("Turn on Location")
            .setMessage("Turn on your phone's Location to add a place to this post.")
            .setPositiveButton("Location settings", (dialog, which) -> {
                try {
                    returningFromLocationSettings = true;
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                } catch (Exception error) { finishGeolocation(false); }
            })
            .setNegativeButton("Cancel", (dialog, which) -> finishGeolocation(false))
            .setOnCancelListener(dialog -> finishGeolocation(false))
            .show();
    }

    private void handleGeolocationRequest(String origin, GeolocationPermissions.Callback callback) {
        if (origin == null || !isTrusted(Uri.parse(origin)) || isFinishing()) {
            callback.invoke(origin, false, false);
            return;
        }
        if (pendingGeoCallback != null) finishGeolocation(false);
        pendingGeoCallback = callback;
        pendingGeoOrigin = origin;
        if (hasLocationPermission()) afterLocationPermission();
        else requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_GEOLOCATION_PERMISSION);
    }

    // Only trusted HTTPS pages may receive Android microphone/camera access.
    private void handleMediaRequest(PermissionRequest req) {
        if (!isTrusted(req.getOrigin())) {
            req.deny();
            return;
        }
        if (pendingPermission != null && pendingPermission != req) {
            pendingPermission.deny();
            pendingPermission = null;
            pendingResources = null;
        }
        ArrayList<String> resources = new ArrayList<>();
        ArrayList<String> permissions = new ArrayList<>();
        for (String resource : req.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                resources.add(resource);
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        && !permissions.contains(Manifest.permission.RECORD_AUDIO))
                    permissions.add(Manifest.permission.RECORD_AUDIO);
            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                resources.add(resource);
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        && !permissions.contains(Manifest.permission.CAMERA))
                    permissions.add(Manifest.permission.CAMERA);
            }
        }
        if (resources.isEmpty()) { req.deny(); return; }
        if (permissions.isEmpty()) {
            req.grant(resources.toArray(new String[0]));
            return;
        }
        pendingPermission = req;
        pendingResources = resources.toArray(new String[0]);
        requestPermissions(permissions.toArray(new String[0]), REQUEST_MEDIA_PERMISSION);
    }

    @Override public void onRequestPermissionsResult(int request, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(request, perms, grants);
        if (request == REQUEST_GEOLOCATION_PERMISSION) {
            afterLocationPermission();
            return;
        }
        if (request == REQUEST_CAMERA_CAPTURE_PERMISSION) {
            WebChromeClient.FileChooserParams queued = pendingChooserParams;
            pendingChooserParams = null;
            if (queued != null)
                launchFileChooser(queued, grants.length > 0 &&
                        grants[0] == PackageManager.PERMISSION_GRANTED);
            return;
        }
        if (request == REQUEST_LEGACY_STORAGE) {
            WebChromeClient.FileChooserParams queued = pendingChooserParams;
            pendingChooserParams = null;
            if (queued != null)
                launchFileChooser(queued, grants.length > 0 &&
                        grants[0] == PackageManager.PERMISSION_GRANTED);
            return;
        }
        if (request != REQUEST_MEDIA_PERMISSION || pendingPermission == null) return;
        PermissionRequest waiting = pendingPermission;
        String[] resources = pendingResources;
        pendingPermission = null;
        pendingResources = null;
        boolean granted = grants.length > 0 && grants.length == perms.length;
        for (int result : grants)
            if (result != PackageManager.PERMISSION_GRANTED) granted = false;
        boolean microphoneDenied = false;
        if (resources != null) {
            for (String resource : resources) {
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        microphoneDenied = true;
                        granted = false;
                    }
                } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) &&
                           checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                }
            }
        } else {
            granted = false;
        }
        if (granted) waiting.grant(resources);
        else {
            waiting.deny();
            showMediaPermissionHelp(microphoneDenied);
        }
    }

    private void showMediaPermissionHelp(boolean microphone) {
        if (isFinishing() || isDestroyed()) return;
        String feature = microphone ? "Microphone" : "Camera";
        new AlertDialog.Builder(this)
            .setTitle(feature + " permission required")
            .setMessage("To use " + (microphone ? "voice recording" : "the camera") +
                " in chhachh, enable " + feature.toLowerCase() +
                " in Android Settings > Apps > chhachh > Permissions. Then return and try again.")
            .setNegativeButton("Not now", null)
            .setPositiveButton("Open settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
                try { startActivity(intent); }
                catch (ActivityNotFoundException ignored) { }
            })
            .show();
    }

    // Open the real Android Sharesheet for a user-initiated Share tap.
    // The navigation token is injected only into the trusted top-level Chhachh page.
    public final class NativeShare {
        @JavascriptInterface public boolean open(String token, String url, String title) {
            if (token == null || !token.equals(nativeDownloadToken) ||
                    url == null || url.length() > 2048) return false;
            Uri uri;
            try { uri = Uri.parse(url); }
            catch (Exception ignored) { return false; }
            if (!isTrusted(uri)) return false;
            final String target = uri.toString();
            final String label = title != null && title.length() <= 100 && !title.trim().isEmpty()
                    ? title.trim() : "Chhachh";
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, target);
                send.putExtra(Intent.EXTRA_SUBJECT, label);
                try { startActivity(Intent.createChooser(send, "Share")); }
                catch (ActivityNotFoundException ignored) { }
            });
            return true;
        }
    }

    // Canonical Android Download handler for completed video blobs from chhachh.com.
    // A per-navigation secret avoids exposing file writes to third-party iframes.
    public final class NativeDownloads {
        private OutputStream output;
        private Uri destination;
        private File legacyFile;
        private long expectedBytes, writtenBytes;
        private int activeId, serial;
        private boolean valid(String token) {
            return token != null && token.equals(nativeDownloadToken);
        }
        private void clear(boolean keep) {
            try { if (output != null) output.close(); } catch (Exception ignored) { }
            output = null;
            if (!keep) {
                if (destination != null) try { getContentResolver().delete(destination, null, null); }
                    catch (Exception ignored) { }
                if (legacyFile != null) legacyFile.delete();
            }
            destination = null; legacyFile = null; expectedBytes = 0;
            writtenBytes = 0; activeId = 0;
        }
        @JavascriptInterface public synchronized int begin(String token, String filename,
                String mime, double size) {
            if (!valid(token) || activeId != 0 || filename == null ||
                    filename.length() > 100 || !filename.matches("[A-Za-z0-9_.-]+") ||
                    !(filename.endsWith(".mp4") || filename.endsWith(".webm")) ||
                    !("video/mp4".equals(mime) || "video/webm".equals(mime)) ||
                    !Double.isFinite(size) || size <= 0 || size > 250L*1024*1024) return 0;
            try {
                expectedBytes = (long)size;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/chhachh");
                    cv.put(MediaStore.Downloads.IS_PENDING, 1);
                    destination = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (destination == null) throw new IOException("No destination");
                    output = getContentResolver().openOutputStream(destination, "w");
                } else {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        runOnUiThread(() -> requestPermissions(new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_LEGACY_STORAGE));
                        clear(false); return 0;
                    }
                    File folder = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), "chhachh");
                    if (!folder.exists() && !folder.mkdirs())
                        throw new IOException("Cannot create Downloads folder");
                    legacyFile = new File(folder, filename);
                    output = new FileOutputStream(legacyFile);
                }
                if (output == null) throw new IOException("No output stream");
                activeId = ++serial; if (activeId == 0) activeId = ++serial;
                writtenBytes = 0;
                return activeId;
            } catch (Exception e) { clear(false); return 0; }
        }
        @JavascriptInterface public synchronized boolean append(String token, int id, String base64) {
            if (!valid(token) || id == 0 || id != activeId || output == null ||
                    base64 == null || base64.length() > 100000) return false;
            try {
                byte[] b = Base64.decode(base64, Base64.DEFAULT);
                if (b.length == 0 || writtenBytes + b.length > expectedBytes) return false;
                output.write(b); writtenBytes += b.length; return true;
            } catch (Exception e) { clear(false); return false; }
        }
        @JavascriptInterface public synchronized boolean finish(String token, int id) {
            if (!valid(token) || id == 0 || id != activeId || output == null ||
                    writtenBytes != expectedBytes) { clear(false); return false; }
            try {
                output.flush(); output.close(); output = null;
                if (destination != null && Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.IS_PENDING, 0);
                    if (getContentResolver().update(destination, cv, null, null) < 1)
                        throw new IOException("Finalizing download failed");
                } else if (legacyFile != null) {
                    MediaScannerConnection.scanFile(MainActivity.this,
                        new String[] { legacyFile.getAbsolutePath() }, null, null);
                }
                clear(true); return true;
            } catch (Exception e) { clear(false); return false; }
        }
        @JavascriptInterface public synchronized void abort(String token, int id) {
            if (valid(token) && id == activeId) clear(false);
        }
    }

    private void download(String url, String userAgent, String disposition, String type) {
        try {
            Uri uri = Uri.parse(url);
            if (!isTrusted(uri)) { openOutside(uri); return; }
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setMimeType(type);
            request.addRequestHeader("User-Agent", userAgent);
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) request.addRequestHeader("Cookie", cookies);
            String filename = URLUtil.guessFileName(url, disposition, type);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            manager.enqueue(request);
        } catch (Exception err) { openOutside(Uri.parse(url)); }
    }

    private void openOutside(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) { }
    }

    private void showOffline() {
        if (offline != null) return;
        offline = new FrameLayout(this);
        offline.setBackgroundColor(Color.WHITE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        int pad = (int)(28 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this);
        title.setText("Connection unavailable");
        title.setTextSize(22);
        title.setTextColor(0xff111827);
        title.setGravity(Gravity.CENTER);
        TextView subtitle = new TextView(this);
        subtitle.setText("Check your internet connection and try again.");
        subtitle.setTextSize(15);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(0xff475569);
        Button retry = new Button(this);
        retry.setText("Try again");
        retry.setOnClickListener(v -> { hideOffline(); web.loadUrl(HOME); });
        content.addView(title);
        content.addView(subtitle);
        content.addView(retry);
        offline.addView(content, new FrameLayout.LayoutParams(-1, -1));
        root.addView(offline, new FrameLayout.LayoutParams(-1, -1));
    }

    private void hideOffline() {
        if (offline == null) return;
        root.removeView(offline);
        offline = null;
    }

    private void exitVideoFullscreen() {
        if (fullscreenView == null) return;
        root.removeView(fullscreenView);
        fullscreenView = null;
        web.setVisibility(View.VISIBLE);
        root.requestApplyInsets();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
    }

    @Override public void onBackPressed() {
        if (fullscreenView != null) { exitVideoFullscreen(); return; }
        if (web == null || !isTrusted(Uri.parse(String.valueOf(web.getUrl())))) {
            super.onBackPressed(); return;
        }
        // In-app Back closes the existing media overlay before navigating.
        web.evaluateJavascript("(function(){try{if(typeof FEED_VIDEO_EXPANDED!=='undefined'"
            + "&&FEED_VIDEO_EXPANDED&&typeof closeExpandedFeedVideo==='function'){"
            + "closeExpandedFeedVideo();return true;}"
            + "if(document.querySelector('.post-photo-expanded-layer')"
            + "&&typeof closeExpandedPostPhoto==='function'){closeExpandedPostPhoto();return true;}"
            + "}catch(e){}return false;})()", result -> {
            if ("true".equals(result)) return;
            if (web != null && web.canGoBack()) web.goBack();
            else MainActivity.super.onBackPressed();
        });
    }
    @Override protected void onPause() {
        releaseForeground = false;
        releaseHandler.removeCallbacks(releaseTimer);
        if (web != null) web.onPause();
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        if (returningFromLocationSettings) {
            returningFromLocationSettings = false;
            if (pendingGeoCallback != null) finishGeolocation(
                    hasLocationPermission() && isDeviceLocationOn());
        }
        releaseForeground = true;
        releaseHandler.removeCallbacks(releaseTimer);
        releaseHandler.postDelayed(releaseTimer, 1500L);
    }
    @Override protected void onDestroy() {
        releaseForeground = false;
        releaseHandler.removeCallbacks(releaseTimer);
        if (pendingGeoCallback != null) finishGeolocation(false);
        if (pendingPermission != null) {
            pendingPermission.deny();
            pendingPermission = null;
            pendingResources = null;
        }
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        clearUnusedCameraFiles(null);
        pendingChooserParams = null;
        if (web != null) {
            root.removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}