package eu.faircode.netguard;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class StatisticsActivity extends AppCompatActivity {
    private ImageView progress_imageView;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set up webview for amazon reward stuff
        mWebView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        TextView toolbarTextView;
        for (int i = 0; i < toolbar.getChildCount(); ++i) {
            final View toolbarChild = toolbar.getChildAt(i);
            if (toolbarChild instanceof TextView) {
                toolbarTextView = (TextView) toolbarChild;
                toolbarTextView.setText("Progress & Statistics");
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        progress_imageView = (ImageView) findViewById(R.id.progress_imageView);

        progress_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.setVisibility(1);
                mWebView.loadUrl("https://www.amazon.de/Staffel-Folge-Das-Buch-Fremden/dp/B01HNEEECK/ref=sr_1_5?ie=UTF8&qid=1474241580&sr=8-5&keywords=game+of+thrones");

            }
        });

        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mWebView.setVisibility(View.GONE);
                mWebView.removeView(mWebView);
                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP){
                    // lollipop and above versions
                    progress_imageView.setImageDrawable(getDrawable(R.drawable.product_circle));
                } else{
                    // Before lollipop
                    //TODO: To implement for KitKat
                }
                return false;
            }
        });

        //WebViewClient.shouldOverrideUrlLoading

        //Click on progress_image --> user can select his rew
        mWebView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.setVisibility(0);
                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP){
                    // lollipop and above versions
                    progress_imageView.setImageDrawable(getDrawable(R.drawable.product_circle));
                } else{
                    // Before lollipop
                    //TODO: To implement for KitKat
                }
            }
        });

    }
}
