package com.chhachh.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String HOME = "https://chhachh.com/";
    private static final int REQUEST_FILES = 401;
    private static final int REQUEST_MEDIA_PERMISSION = 402;
    private static final int REQUEST_LEGACY_STORAGE = 403;
    private WebView web;
    private FrameLayout root, offline;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraFile;
    private boolean pickerLaunched = false;
    private WebChromeClient.FileChooserParams pendingChooserParams;
    private PermissionRequest pendingPermission;
    private String[] pendingResources;

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
        configureWindowInsets();
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
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
            @Override public void onPageFinished(WebView view, String url) {
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
                fileCallback = callback;
                cameraFile = null;
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
        web.setDownloadListener((url, ua, disposition, mime, len) -> download(url, ua, disposition, mime));
        web.loadUrl(HOME);
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
            if (allowCamera && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                boolean images = accepts(params.getAcceptTypes(), "image");
                boolean videos = accepts(params.getAcceptTypes(), "video");
                if (images) {
                    Intent capture = captureIntent(false);
                    if (capture != null) cameraChoices.add(capture);
                } else if (videos) {
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
            if (cameraFile != null) {
                try { getContentResolver().delete(cameraFile, null, null); }
                catch (Exception ignored) { }
            }
            cameraFile = null;
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
        cameraFile = uri;
        Intent intent = new Intent(video ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
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
            } else if (cameraFile != null) {
                output = new Uri[] { cameraFile };
            }
        }
        if (cameraFile != null && (output == null || !contains(output, cameraFile))) {
            try { getContentResolver().delete(cameraFile, null, null); } catch (Exception ignored) {}
        }
        cameraFile = null;
        fileCallback.onReceiveValue(output);
        fileCallback = null;
        pickerLaunched = false;
    }

    private boolean contains(Uri[] list, Uri item) {
        for (Uri uri : list) if (item.equals(uri)) return true;
        return false;
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
        if (fullscreenView != null) exitVideoFullscreen();
        else if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (pendingPermission != null) {
            pendingPermission.deny();
            pendingPermission = null;
            pendingResources = null;
        }
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        pendingChooserParams = null;
        if (web != null) {
            root.removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}