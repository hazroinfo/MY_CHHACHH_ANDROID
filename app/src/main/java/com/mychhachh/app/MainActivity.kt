package com.mychhachh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingMediaRequest: PermissionRequest? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = fileCallback ?: return@registerForActivityResult
        fileCallback = null
        callback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
    }

    private val mediaPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val request = pendingMediaRequest ?: return@registerForActivityResult
        pendingMediaRequest = null
        val allowed = request.resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                else -> false
            }
        }.toTypedArray()
        if (allowed.isNotEmpty()) request.grant(allowed) else request.deny()
    }

    private val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val origin = pendingGeoOrigin
        val callback = pendingGeoCallback
        pendingGeoOrigin = null
        pendingGeoCallback = null
        if (origin != null && callback != null) {
            val granted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            callback.invoke(origin, granted, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WebView.setWebContentsDebuggingEnabled(false)

        val chromeBlue = Color.rgb(223, 247, 255)
        window.statusBarColor = chromeBlue
        window.navigationBarColor = Color.rgb(246, 243, 255)

        val density = resources.displayMetrics.density
        val headerGap = (4f * density).toInt()
        val topLineMaskHeight = maxOf(2, (3f * density).toInt())

        webView = WebView(this).apply {
            setBackgroundColor(chromeBlue)
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(chromeBlue)

            addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            addView(
                View(this@MainActivity).apply {
                    setBackgroundColor(chromeBlue)
                    isClickable = false
                    isFocusable = false
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    topLineMaskHeight,
                    Gravity.TOP
                )
            )
        }
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, statusBars.top + headerGap, 0, navigationBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)

        configureWebView()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(HOME_URL)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun configureWebView() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            allowContentAccess = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            loadWithOverviewMode = false
            useWideViewPort = false
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString + " MyChhachhAndroid/2.0"
        }

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        installAudioCaptureCompatAtDocumentStart()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUri(request.url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUri(Uri.parse(url))
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    val intent = fileChooserParams?.createIntent()
                        ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                    filePicker.launch(intent)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    callback?.onReceiveValue(null)
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val needed = mutableListOf<String>()
                    if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    ) {
                        needed += Manifest.permission.RECORD_AUDIO
                    }
                    if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    ) {
                        needed += Manifest.permission.CAMERA
                    }

                    if (needed.isEmpty()) {
                        request.grant(request.resources)
                    } else {
                        pendingMediaRequest?.deny()
                        pendingMediaRequest = request
                        mediaPermissions.launch(needed.distinct().toTypedArray())
                    }
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val alreadyGranted =
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (alreadyGranted) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    locationPermissions.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
    }

    private fun installAudioCaptureCompatAtDocumentStart() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                AUDIO_CAPTURE_COMPAT_JS,
                setOf("https://chhachh.pages.dev")
            )
        }
    }

    private fun handleUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()

        if ((scheme == "https" || scheme == "http") &&
            (host == "chhachh.pages.dev" || host.endsWith(".chhachh.pages.dev"))
        ) {
            return false
        }

        return try {
            when (scheme) {
                "http", "https", "tel", "mailto", "geo", "sms", "market", "intent" -> {
                    val intent = if (scheme == "intent") {
                        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }
                    startActivity(intent)
                    true
                }
                else -> {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onPause() {
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        pendingMediaRequest?.deny()
        pendingMediaRequest = null
        webView.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val HOME_URL = "https://chhachh.pages.dev/"

        private const val AUDIO_CAPTURE_COMPAT_JS = """
            (function () {
              if (window.__mcAndroidCompatInstalled) return;
              window.__mcAndroidCompatInstalled = true;

              var media = navigator.mediaDevices;
              if (media && media.getUserMedia && !media.__mcAndroidAudioCompat) {
                var originalGetUserMedia = media.getUserMedia.bind(media);
                media.__mcAndroidAudioCompat = true;

                media.getUserMedia = function (constraints) {
                  var requested = constraints || {};
                  var safe = requested;

                  if (requested.audio && typeof requested.audio === 'object') {
                    safe = { audio: true };
                    if (Object.prototype.hasOwnProperty.call(requested, 'video')) {
                      safe.video = requested.video;
                    }
                  }

                  return originalGetUserMedia(safe).catch(function (firstError) {
                    var retryable = safe.audio && firstError &&
                      (firstError.name === 'NotReadableError' ||
                       firstError.name === 'AbortError' ||
                       firstError.name === 'OverconstrainedError');

                    if (!retryable) throw firstError;

                    return new Promise(function (resolve) {
                      setTimeout(resolve, 300);
                    }).then(function () {
                      var retry = { audio: true };
                      if (Object.prototype.hasOwnProperty.call(safe, 'video')) {
                        retry.video = safe.video;
                      }
                      return originalGetUserMedia(retry);
                    });
                  });
                };
              }

              if (window.FormData && !FormData.prototype.__mcAudioMimeCompat) {
                var originalAppend = FormData.prototype.append;
                try {
                  Object.defineProperty(FormData.prototype, '__mcAudioMimeCompat', {
                    value: true,
                    configurable: false,
                    enumerable: false
                  });
                } catch (_) {
                  FormData.prototype.__mcAudioMimeCompat = true;
                }

                FormData.prototype.append = function (name, value, fileName) {
                  try {
                    if (value && typeof Blob !== 'undefined' && value instanceof Blob) {
                      var originalType = String(value.type || '');
                      if (/^audio\//i.test(originalType) && originalType.indexOf(';') !== -1) {
                        var normalizedType = originalType.split(';')[0].trim().toLowerCase();
                        if (typeof File !== 'undefined' && value instanceof File) {
                          value = new File(
                            [value],
                            value.name || ('voice-' + Date.now()),
                            {
                              type: normalizedType,
                              lastModified: value.lastModified || Date.now()
                            }
                          );
                        } else {
                          value = new Blob([value], { type: normalizedType });
                        }
                      }
                    }
                  } catch (_) {}

                  if (arguments.length >= 3 && fileName !== undefined) {
                    return originalAppend.call(this, name, value, fileName);
                  }
                  return originalAppend.call(this, name, value);
                };
              }

              var doc = document;
              var root = doc.documentElement;
              var scrollClass = 'mc-android-scroll-active';
              var clearTimer = 0;

              function installScrollStyle() {
                if (doc.getElementById('mc-android-scroll-style')) return;
                var style = doc.createElement('style');
                style.id = 'mc-android-scroll-style';
                style.textContent =
                  'html.' + scrollClass + ' :is(.card,.page-heading,.profile-pro-card,.shop-pro-card,.settings-group,.admin-section,.notification-card,.announcement-card,.search-panel,.conversation-list,.chat-panel,.vote-card,.vote-create-card,.top,.community-footer,.side-menu,.faux-search,.global-notice,.reaction-picker,.post-more-menu,.vote-opponent-results,.mc-live-weather-page)' +
                  '{-webkit-backdrop-filter:none!important;backdrop-filter:none!important;filter:none!important;}' +
                  'html.' + scrollClass + ' :is(.winner-balloons i,.vote-pulse-orb,#mcLiveWeatherBg,.mc-live-weather-hero-symbol)' +
                  '{animation-play-state:paused!important;}';
                (doc.head || root).appendChild(style);
              }

              function clearScrollModeSoon(delay) {
                clearTimeout(clearTimer);
                clearTimer = setTimeout(function () {
                  root.classList.remove(scrollClass);
                }, delay || 140);
              }

              function markScrolling() {
                installScrollStyle();
                if (!root.classList.contains(scrollClass)) {
                  root.classList.add(scrollClass);
                }
                clearScrollModeSoon(160);
              }

              addEventListener('touchstart', markScrolling, { passive: true, capture: true });
              addEventListener('touchmove', markScrolling, { passive: true, capture: true });
              addEventListener('scroll', markScrolling, { passive: true, capture: true });
              addEventListener('touchend', function () { clearScrollModeSoon(140); }, { passive: true, capture: true });
              addEventListener('touchcancel', function () { clearScrollModeSoon(100); }, { passive: true, capture: true });
            })();
        """
    }
}
