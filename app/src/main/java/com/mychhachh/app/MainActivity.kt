package com.mychhachh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingMediaRequest: PermissionRequest? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var documentStartLoaderStyleInstalled = false

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileCallback ?: return@registerForActivityResult
            fileCallback = null
            callback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
        }

    private val mediaPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val request = pendingMediaRequest ?: return@registerForActivityResult
            pendingMediaRequest = null

            val allowed = request.resources.filter { resource ->
                when (resource) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                        hasPermission(Manifest.permission.RECORD_AUDIO)

                    PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                        hasPermission(Manifest.permission.CAMERA)

                    else -> false
                }
            }.toTypedArray()

            if (allowed.isNotEmpty()) {
                request.grant(allowed)
            } else {
                request.deny()
                if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                    !hasPermission(Manifest.permission.RECORD_AUDIO) &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                ) {
                    Toast.makeText(
                        this,
                        "Enable Microphone permission in App settings for voice recording",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private val locationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val origin = pendingGeoOrigin
            val callback = pendingGeoCallback
            pendingGeoOrigin = null
            pendingGeoCallback = null

            if (origin != null && callback != null) {
                val granted =
                    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                callback.invoke(origin, granted, false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.rgb(223, 247, 255)
        window.navigationBarColor = Color.rgb(246, 243, 255)
        WebView.setWebContentsDebuggingEnabled(false)

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(223, 247, 255))
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
        }
        setContentView(webView)

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
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = false
            useWideViewPort = false
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString + " MyChhachhAndroid/2.0"
        }

        installSingleLoaderStyleAtDocumentStart()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = handleUri(request.url)

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                handleUri(Uri.parse(url))

            override fun onPageCommitVisible(view: WebView, url: String) {
                super.onPageCommitVisible(view, url)
                if (!documentStartLoaderStyleInstalled) {
                    view.evaluateJavascript(LOADER_STYLE_JS, null)
                }
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
                    if (!isTrustedOrigin(request.origin)) {
                        request.deny()
                        return@runOnUiThread
                    }

                    val needed = mutableListOf<String>()

                    if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                        !hasPermission(Manifest.permission.RECORD_AUDIO)
                    ) {
                        needed += Manifest.permission.RECORD_AUDIO
                    }

                    if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                        !hasPermission(Manifest.permission.CAMERA)
                    ) {
                        needed += Manifest.permission.CAMERA
                    }

                    if (needed.isEmpty()) {
                        grantAllowedWebResources(request)
                    } else {
                        pendingMediaRequest?.deny()
                        pendingMediaRequest = request
                        mediaPermissions.launch(needed.distinct().toTypedArray())
                    }
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingMediaRequest === request) {
                    pendingMediaRequest = null
                }
                super.onPermissionRequestCanceled(request)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val alreadyGranted =
                    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

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

    private fun installSingleLoaderStyleAtDocumentStart() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                LOADER_STYLE_JS,
                setOf("https://chhachh.pages.dev")
            )
            documentStartLoaderStyleInstalled = true
        }
    }

    private fun grantAllowedWebResources(request: PermissionRequest) {
        val allowed = request.resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    hasPermission(Manifest.permission.RECORD_AUDIO)

                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    hasPermission(Manifest.permission.CAMERA)

                else -> false
            }
        }.toTypedArray()

        if (allowed.isNotEmpty()) request.grant(allowed) else request.deny()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun isTrustedOrigin(origin: Uri): Boolean {
        val scheme = origin.scheme?.lowercase().orEmpty()
        val host = origin.host?.lowercase().orEmpty()
        return scheme == "https" &&
            (host == "chhachh.pages.dev" || host.endsWith(".chhachh.pages.dev"))
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

        private const val LOADER_STYLE_JS = """
            (function () {
              if (document.getElementById('mc-native-loader-style')) return;

              var style = document.createElement('style');
              style.id = 'mc-native-loader-style';
              style.textContent =
                '#mcSmoothRouteBar,#mcSmoothV3Bar,#nprogress,.nprogress,.pace,.pace-progress,' +
                '#loadingBar,#loading-bar,.loading-bar,.top-loading-bar,.top-progress,' +
                '.page-progress,.route-progress,.spa-progress,[data-loader="top"],[data-progress="top"]' +
                '{display:none!important;visibility:hidden!important;opacity:0!important;pointer-events:none!important;}';

              (document.head || document.documentElement).appendChild(style);
            })();
        """
    }
}
