package com.mychhachh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
        if (allowed.isNotEmpty()) request.grant(request.resources) else request.deny()
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
        window.statusBarColor = Color.rgb(223, 247, 255)
        WebView.setWebContentsDebuggingEnabled(false)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(223, 247, 255))
        }
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(223, 247, 255))
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        var initialInsetsApplied = false
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            if (!initialInsetsApplied) {
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                initialInsetsApplied = true
                ViewCompat.setOnApplyWindowInsetsListener(view, null)
            }
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

        installDocumentStartLoaderGuard()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUri(request.url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUri(Uri.parse(url))
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.i(WEBVIEW_LOG_TAG, "PAGE_STARTED $url")
            }

            override fun onPageCommitVisible(view: WebView, url: String) {
                super.onPageCommitVisible(view, url)
                Log.i(WEBVIEW_LOG_TAG, "PAGE_COMMIT_VISIBLE $url")
                applyNativePageFixes(view)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.i(WEBVIEW_LOG_TAG, "PAGE_FINISHED $url")
                applyNativePageFixes(view)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    Log.e(
                        WEBVIEW_LOG_TAG,
                        "MAIN_FRAME_ERROR code=${error.errorCode} description=${error.description} url=${request.url}"
                    )
                }
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

    private fun installDocumentStartLoaderGuard() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Log.w(WEBVIEW_LOG_TAG, "DOCUMENT_START_SCRIPT unavailable; using page-commit fallback")
            return
        }

        WebViewCompat.addDocumentStartJavaScript(
            webView,
            LOADER_GUARD_JS,
            setOf("https://chhachh.pages.dev")
        )
    }

    private fun applyNativePageFixes(view: WebView) {
        view.evaluateJavascript(LOADER_GUARD_JS, null)
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
        private const val WEBVIEW_LOG_TAG = "MyChhachhWebView"
        private const val LOADER_GUARD_JS = """
            (function(){
              try {
                if(!window.__mcAndroidDragClickGuard){
                  window.__mcAndroidDragClickGuard=1;

                  var dragStartX=0;
                  var dragStartY=0;
                  var dragTracking=false;
                  var dragMoved=false;
                  var suppressClickUntil=0;

                  function clock(){
                    return (window.performance && typeof performance.now==='function')
                      ? performance.now()
                      : Date.now();
                  }

                  window.addEventListener('pointerdown',function(ev){
                    if(ev.isPrimary===false) return;
                    dragStartX=Number(ev.clientX)||0;
                    dragStartY=Number(ev.clientY)||0;
                    dragMoved=false;
                    dragTracking=true;
                  },{capture:true,passive:true});

                  window.addEventListener('pointermove',function(ev){
                    if(!dragTracking || dragMoved || ev.isPrimary===false) return;
                    var dx=(Number(ev.clientX)||0)-dragStartX;
                    var dy=(Number(ev.clientY)||0)-dragStartY;
                    if((dx*dx)+(dy*dy)>324) dragMoved=true;
                  },{capture:true,passive:true});

                  window.addEventListener('pointerup',function(ev){
                    if(ev.isPrimary===false) return;
                    if(dragTracking && dragMoved) suppressClickUntil=clock()+450;
                    dragTracking=false;
                  },{capture:true,passive:true});

                  window.addEventListener('pointercancel',function(){
                    dragTracking=false;
                    dragMoved=false;
                  },{capture:true,passive:true});

                  window.addEventListener('click',function(ev){
                    if(clock()>=suppressClickUntil) return;
                    ev.preventDefault();
                    ev.stopImmediatePropagation();
                    ev.stopPropagation();
                  },true);
                }

                var STYLE_ID='__mc_android_loader_guard';
                if(document.getElementById(STYLE_ID)) return;
                var s=document.createElement('style');
                s.id=STYLE_ID;
                s.textContent=
                  '#mcSmoothRouteBar,#mcSmoothV3Bar,#nprogress,.nprogress,.pace,.pace-progress,#loadingBar,.loading-bar,#loading-bar,.top-loading-bar,.top-progress,.page-progress,.route-progress,.spa-progress,.progress-line,.loader-line,[data-loader="top"],[data-progress="top"]{display:none!important;opacity:0!important;visibility:hidden!important;height:0!important;max-height:0!important;border:0!important;box-shadow:none!important;pointer-events:none!important}';
                (document.head||document.documentElement).appendChild(s);
              } catch (_) {}
            })();
        """

    }
}
