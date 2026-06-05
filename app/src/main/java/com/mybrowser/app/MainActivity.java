package com.hrf.aipro;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    // آدرس صفحه اصلی خودت را اینجا بگذار
    private static final String HOME_URL = "https://arena.ai/image/direct";

    private WebView webView;
    private EditText addressBar;
    private Button goButton;
    private ProgressBar progressBar;

    private ValueCallback<Uri[]> filePathCallback;
    private final static int FILE_CHOOSER_REQUEST = 100;
    private final static int STORAGE_PERMISSION_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        addressBar = findViewById(R.id.address_bar);
        goButton = findViewById(R.id.go_button);
        progressBar = findViewById(R.id.progress_bar);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomChromeClient());

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            STORAGE_PERMISSION_REQUEST);
                } else {
                    startDownload(url);
                }
            }
        });

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrlFromBar();
            }
        });

        addressBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER) {
                    loadUrlFromBar();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL);
            addressBar.setText(HOME_URL);
        }
    }

    private void loadUrlFromBar() {
        String input = addressBar.getText().toString().trim();
        if (input.isEmpty()) return;

        if (isValidUrl(input)) {
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                input = "https://" + input;
            }
            webView.loadUrl(input);
        } else {
            webView.loadUrl("https://www.google.com/search?q=" + Uri.encode(input));
        }
    }

    private boolean isValidUrl(String url) {
        if (url.contains(" ") || !url.contains(".")) {
            return false;
        }
        try {
            new URL("https://" + url.replaceFirst("^(https?://)", ""));
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            addressBar.setText(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            addressBar.setText(url);
        }
    }

    private class CustomChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPermissionRequest(android.webkit.PermissionRequest request) {
            request.grant(request.getResources());
        }

        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (MainActivity.this.filePathCallback != null) {
                MainActivity.this.filePathCallback.onReceiveValue(null);
            }
            MainActivity.this.filePathCallback = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            try {
                startActivityForResult(intent, FILE_CHOOSER_REQUEST);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "امکان باز کردن فایل وجود ندارد", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    private void startDownload(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, null, null));
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(request);
        Toast.makeText(this, "دانلود شروع شد", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "دوباره لینک دانلود را لمس کنید", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "بدون دسترسی به حافظه، دانلود ممکن نیست", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}
