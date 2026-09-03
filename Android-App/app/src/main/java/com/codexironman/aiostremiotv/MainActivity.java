package com.codexironman.aiostremiotv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String CONFIG_URL = "http://127.0.0.1:3000/stremio/configure";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WebView webView;
    private ProgressBar progress;
    private LinearLayout statusPanel;
    private TextView statusText;
    private long deadline;
    private boolean pageReady;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        applyTvSystemUi();
        buildUi();
        configureWebView();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        startServerAndWait();
    }

    private void startServerAndWait() {
        Intent service = new Intent(this, AioServerService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        deadline = System.currentTimeMillis() + 180_000L;
        pageReady = false;
        webView.setVisibility(View.GONE);
        statusPanel.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        progress.setVisibility(View.VISIBLE);
        statusText.setText("TV local server start ho raha hai…\nPehli baar 1–2 minute lag sakte hain.");
        probeServer();
    }

    private void probeServer() {
        executor.execute(() -> {
            boolean ready = false;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(CONFIG_URL).openConnection();
                connection.setConnectTimeout(1200); connection.setReadTimeout(1200);
                int code = connection.getResponseCode(); ready = code >= 200 && code < 400;
            } catch (Exception ignored) {} finally { if (connection != null) connection.disconnect(); }
            final boolean serverReady = ready;
            handler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (serverReady) showConfiguration();
                else if (System.currentTimeMillis() < deadline) handler.postDelayed(this::probeServer, 750);
                else { progress.setVisibility(View.GONE); statusText.setText("Server start nahi hua. Retry karein.\n\n" + AioServerService.readLogTail(MainActivity.this, 14)); }
            });
        });
    }

    private void showConfiguration() {
        pageReady = true; statusPanel.setVisibility(View.GONE); webView.setVisibility(View.VISIBLE); webView.requestFocus(); webView.loadUrl(CONFIG_URL);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, float size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setGravity(Gravity.CENTER); return v; }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(0xFF0B0D14);
        root.setOnApplyWindowInsetsListener((view, insets) -> { view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets; });
        webView = new WebView(this); webView.setVisibility(View.GONE); root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progress.setIndeterminate(true); root.addView(progress, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        statusPanel = new LinearLayout(this); statusPanel.setOrientation(LinearLayout.VERTICAL); statusPanel.setGravity(Gravity.CENTER); statusPanel.setPadding(dp(28), dp(28), dp(28), dp(28)); statusPanel.setBackgroundColor(0xFF0B0D14);
        TextView title = text("AIO Stream X TV", 28, 0xFFFFFFFF); statusText = text("TV local server start ho raha hai…", 18, 0xFFB8BBC7); statusText.setPadding(0, dp(14), 0, dp(24));
        Button retry = new Button(this); retry.setText("Retry"); retry.setAllCaps(false); retry.setTextSize(18); retry.setFocusable(true); retry.setFocusableInTouchMode(true); retry.setOnClickListener(view -> startServerAndWait());
        statusPanel.addView(title, new LinearLayout.LayoutParams(-1, -2)); statusPanel.addView(statusText, new LinearLayout.LayoutParams(-1, -2)); statusPanel.addView(retry, new LinearLayout.LayoutParams(dp(260), dp(64))); root.addView(statusPanel, new FrameLayout.LayoutParams(-1, -1)); setContentView(root);
    }

    @SuppressLint("SetJavaScriptEnabled") private void configureWebView() {
        WebSettings settings = webView.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true); settings.setDatabaseEnabled(true); settings.setLoadsImagesAutomatically(true); settings.setMediaPlaybackRequiresUserGesture(false); settings.setBuiltInZoomControls(false); settings.setDisplayZoomControls(false); settings.setTextZoom(110); settings.setDefaultFontSize(18); settings.setDefaultFixedFontSize(16); settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        webView.setFocusable(true); webView.setFocusableInTouchMode(true); webView.setLayerType(View.LAYER_TYPE_HARDWARE, null); webView.setVerticalScrollBarEnabled(false); webView.setHorizontalScrollBarEnabled(false); webView.addJavascriptInterface(new CloudflareBridge(), "AIOStreamX");
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient() { @Override public void onProgressChanged(WebView view, int value) { if (pageReady) { progress.setIndeterminate(false); progress.setProgress(value); progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE); } } });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap icon) { progress.setVisibility(View.VISIBLE); }
            @Override public void onPageFinished(WebView view, String url) { progress.setVisibility(View.GONE); injectTvNavigation(view); view.requestFocus(); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return handleLink(request.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return handleLink(url); }
        });
    }

    private void applyTvSystemUi() { getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE); }
    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) applyTvSystemUi(); }
    private void injectTvNavigation(WebView view) { try { BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("tv-navigation.js"))); StringBuilder script = new StringBuilder(); String line; while ((line = reader.readLine()) != null) script.append(line).append('\n'); reader.close(); view.evaluateJavascript(script.toString(), null); } catch (Exception ignored) {} }

    private final class CloudflareBridge {
        @JavascriptInterface public String getTunnelStatus() { JSONObject result = new JSONObject(); try { String hostname = CloudflareTunnelStore.getHostname(MainActivity.this); result.put("supported", true); result.put("hostname", hostname); result.put("tokenSaved", CloudflareTunnelStore.hasToken(MainActivity.this)); result.put("needsTokenReplacement", CloudflareTunnelStore.needsTokenReplacement(MainActivity.this)); result.put("running", AioServerService.isTunnelRunning()); result.put("state", AioServerService.getTunnelState()); result.put("detail", AioServerService.getTunnelDetail()); result.put("publicBaseUrl", hostname.isEmpty() ? "" : "https://" + hostname); } catch (Exception error) { try { result.put("error", error.getMessage()); } catch (Exception ignored) {} } return result.toString(); }
        @JavascriptInterface public String saveTunnel(String hostname, String token) { JSONObject result = new JSONObject(); try { CloudflareTunnelStore.save(MainActivity.this, hostname, token); result.put("ok", true); AioServerService.reloadTunnel(MainActivity.this); } catch (Exception error) { try { result.put("ok", false); result.put("error", error.getMessage()); } catch (Exception ignored) {} } return result.toString(); }
        @JavascriptInterface public String removeTunnel() { CloudflareTunnelStore.remove(MainActivity.this); AioServerService.reloadTunnel(MainActivity.this); JSONObject result = new JSONObject(); try { result.put("ok", true); } catch (Exception ignored) {} return result.toString(); }
    }

    private boolean handleLink(String url) { Uri uri = Uri.parse(url); String scheme = uri.getScheme(); if (scheme == null) return false; if (scheme.equals("http") || scheme.equals("https")) { String host = uri.getHost(); if ("127.0.0.1".equals(host) || "localhost".equals(host)) return false; try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception error) { Toast.makeText(this, "External link open nahi hua", Toast.LENGTH_SHORT).show(); } return true; } try { Intent intent = scheme.equals("intent") ? Intent.parseUri(url, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, uri); startActivity(intent); } catch (ActivityNotFoundException error) { Toast.makeText(this, "Is link ke liye app nahi mila", Toast.LENGTH_LONG).show(); } catch (Exception error) { Toast.makeText(this, "Link open nahi hua", Toast.LENGTH_SHORT).show(); } return true; }
    @Override public void onBackPressed() { if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); executor.shutdownNow(); if (webView != null) webView.destroy(); super.onDestroy(); }
}
