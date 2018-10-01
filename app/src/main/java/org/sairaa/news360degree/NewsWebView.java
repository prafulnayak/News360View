package org.sairaa.news360degree;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.sairaa.news360degree.utils.DialogAction;

public class NewsWebView extends AppCompatActivity {
    private WebView webView;
    private DialogAction dialogAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_web_view);
        dialogAction = new DialogAction(this);

        Toolbar toolbar = findViewById(R.id.web_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if(getSupportActionBar() != null){
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(getString(R.string.detail_news));

        }
        Intent intent = getIntent();
        String url = intent.getStringExtra(getString(R.string.url));
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //show dialog while loading url
        dialogAction.showDialog(getString(R.string.app_name),getString(R.string.loaging));
        webView.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            public void onPageFinished(WebView view, String url) {
                dialogAction.hideDialog();
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                dialogAction.hideDialog();
            }

        });
        webView.loadUrl(url);
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
