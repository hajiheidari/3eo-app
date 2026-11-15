package ir.threeeo.app;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * The 3eo.ir app: the site inside a WebView, plus what a plain browser tab does not give:
 * pull to refresh, an offline screen, native share, file download and file pick.
 * The user agent ends with "3eoApp/<version>"; the site reads it and removes everything about buying (store rules).
 */
public class MainActivity extends AppCompatActivity {

    private static final String HOME = "https://3eo.ir/";
    private static final String HOST = "3eo.ir";
    private static final String OFFLINE = "file:///android_asset/offline.html";

    private WebView web;
    private SwipeRefreshLayout swipe;
    private String lastUrl = HOME;
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> filePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        swipe = new SwipeRefreshLayout(this);
        swipe.addView(web, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(swipe);

        swipe.setColorSchemeColors(0xFF047857);
        // pull-to-refresh only when the page is scrolled to the top
        swipe.setOnChildScrollUpCallback((parent, child) -> web.getScrollY() > 0);
        swipe.setOnRefreshListener(() -> {
            String u = web.getUrl();
            if (u != null && u.startsWith("http")) {
                web.reload();
            } else {
                web.loadUrl(lastUrl);
            }
        });

        filePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (filePathCallback == null) {
                return;
            }
            Uri[] picked = null;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                picked = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
            }
            filePathCallback.onReceiveValue(picked);
            filePathCallback = null;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportMultipleWindows(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setUserAgentString(s.getUserAgentString() + " 3eoApp/" + versionName() + " (Android)");

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, false);

        web.addJavascriptInterface(new Bridge(), "ThreeEo");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url != null && url.startsWith("http")) {
                    lastUrl = url;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipe.setRefreshing(false);
                CookieManager.getInstance().flush();
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Android 5 and 6 call this older form; newer versions call the one below
                if (Build.VERSION.SDK_INT < 23 && failingUrl != null && failingUrl.startsWith("http")) {
                    swipe.setRefreshing(false);
                    view.loadUrl(OFFLINE);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    swipe.setRefreshing(false);
                    view.loadUrl(OFFLINE);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    filePicker.launch(params.createIntent());
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });

        web.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                String name = URLUtil.guessFileName(url, contentDisposition, mimetype);
                req.setMimeType(mimetype);
                req.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                req.addRequestHeader("User-Agent", userAgent);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(req);
                Toast.makeText(this, "دانلود شروع شد", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "دانلود انجام نشد", Toast.LENGTH_SHORT).show();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                String u = web.getUrl();
                if (u != null && u.startsWith("file:")) {
                    finish();
                } else if (web.canGoBack()) {
                    web.goBack();
                } else {
                    finish();
                }
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            Uri start = getIntent() != null ? getIntent().getData() : null;
            web.loadUrl(start != null && HOST.equals(start.getHost()) ? start.toString() : HOME);
        }
    }

    /** true = the app handled it (do not load it in the page) */
    private boolean handleUrl(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        String host = uri.getHost() == null ? "" : uri.getHost();
        if ((scheme.equals("https") || scheme.equals("http")) && (host.equals(HOST) || host.equals("www." + HOST))) {
            return false; // our own site: stay inside the app
        }
        try {
            // phone, mail, WhatsApp, Telegram and every other site open in the matching app or the browser
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "برنامه‌ی مناسبی برای باز کردن این لینک پیدا نشد", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private String versionName() {
        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return v == null ? "1" : v;
        } catch (PackageManager.NameNotFoundException e) {
            return "1";
        }
    }

    private boolean online() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }

    /** what the site (or the offline page) can ask the app to do */
    private class Bridge {
        @JavascriptInterface
        public void retry() {
            runOnUiThread(() -> {
                if (online()) {
                    web.loadUrl(lastUrl);
                } else {
                    Toast.makeText(MainActivity.this, "هنوز به اینترنت وصل نیستید", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void share(String text, String url) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, (text == null ? "" : text) + (url == null || url.isEmpty() ? "" : "\n" + url));
                startActivity(Intent.createChooser(send, "اشتراک‌گذاری"));
            });
        }
    }
}
