package com.mumian.ytpos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final int ALIPAY_WECHAT = 556;
    private static final int CARD = 283;
    private WebView webView;
    private WebSettings webSettings;
    private static final String packageName = "com.ccb.smartpos.bankpay";
    private static final String className = "com.ccb.smartpos.bankpay.ui.MainActivity";
    private Intent targetIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        targetIntent = new Intent(Intent.ACTION_VIEW);
        targetIntent.setClassName(packageName,className);
        webView = (WebView)findViewById(R.id.webView);
        String appName = getString(R.string.transAppName);
        webSettings = webView.getSettings();
        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);//支持通过JS打开新窗口
        // 若加载的 html 里有JS 在执行动画等操作，会造成资源浪费（CPU、电量）
        // 在 onStop 和 onResume 里分别把 setJavaScriptEnabled() 给设置成 false 和 true 即可

        //设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(false); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(false); // 缩放至屏幕的大小

        //缩放操作
        webSettings.setSupportZoom(false); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(false); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件

        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式

        webView.addJavascriptInterface(MainActivity.this,"test");
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view,String url)
            {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载了");
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                System.out.println("结束加载了");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {


            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d("webview", message + " -- From line " + lineNumber + " of " + sourceID);
            }
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                System.out.println("标题在这里");
                //mtitle.setText(title);
            }


            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                view.requestFocus();
                if (newProgress < 100) {
                    String progress = newProgress + "%";
                    //loading.setText(progress);
                } else if (newProgress == 100) {
                    String progress = newProgress + "%";
                    //loading.setText(progress);
                }
            }
        });

        webView.loadUrl("http://k3cloud.yatai360.cn:8001/account/login");
    }



    @Override
    public boolean onKeyUp(int keyCode,KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack())
        {
            webView.goBack();
            return true;
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.print("onStop");
        //webSettings.setJavaScriptEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("onResume");
        //webSettings.setJavaScriptEnabled(true);
    }

    //销毁Webview
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();

            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        System.out.println("onActivityResult");
        String returnCode = data.getStringExtra("resultCode");
        String resultMsg = data.getStringExtra("resultMsg");
        String returnData = data.getStringExtra("transData");
        switch (requestCode)
        {
            case CARD:
                if (returnCode.equals("0")) {
                    //交易成功
                    Toast.makeText(this, "信用卡交易成功", Toast.LENGTH_LONG).show();
                }else
                {
                    Toast.makeText(this, "信用卡交易失败", Toast.LENGTH_LONG).show();
                    //交易失败
                }
                webView.loadUrl("javascript:callPayByCardCallback('" + returnCode + "','" + resultMsg + "','"+ returnData +"')");
                break;
            case ALIPAY_WECHAT:
                if (returnCode.equals("0")) {
                    //交易成功
                    Toast.makeText(this, "微信/支付包交易成功", Toast.LENGTH_LONG).show();
                }else
                {
                    //交易失败
                    Toast.makeText(this, "微信/支付宝交易失败", Toast.LENGTH_LONG).show();
                }
                webView.loadUrl("javascript:callPayByAlipayWechatCallback('" + returnCode + "','" + resultMsg + "','"+ returnData +"')");
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @JavascriptInterface
    public void test(String msg)
    {
        System.out.println(msg);
        Log.e("Log","Hello World");
    }

    @JavascriptInterface
    public void PayByAlipayWechat(String msg)
    {
        msg = parseAmount(msg);
        Bundle bundle = new Bundle();
        bundle.putString("appName","建行收单应用");
        bundle.putString("transId","微信/支付宝消费");
        bundle.putString("transData",msg);
        targetIntent.putExtras(bundle);
        startActivityForResult(targetIntent,ALIPAY_WECHAT);
    }

    @JavascriptInterface
    public void PayByCard(String msg)
    {
        msg = parseAmount(msg);
        Bundle bundle = new Bundle();
        bundle.putString("appName","建行收单应用");
        bundle.putString("transId","消费");
        bundle.putString("transData",msg);
        targetIntent.putExtras(bundle);
        startActivityForResult(targetIntent,CARD);
    }

    public String parseAmount(String msg) {
        String strTransData = msg;
        String amount;
        try {
            JSONObject inJsonObject = new JSONObject(
                    strTransData == null ? "" : strTransData);

            if (inJsonObject.has("amt")){
                amount = inJsonObject.getString("amt");
                inJsonObject.put("amt", makeFieldAmount(amount));
                strTransData = inJsonObject.toString();
            }
            if (inJsonObject.has("disAmt")){
                amount = inJsonObject.getString("disAmt");
                inJsonObject.put("disAmt", makeFieldAmount(amount));
                strTransData = inJsonObject.toString();
            }
            if (inJsonObject.has("orgAmt")){
                amount = inJsonObject.getString("orgAmt");
                inJsonObject.put("orgAmt", makeFieldAmount(amount));
                strTransData = inJsonObject.toString();
            }
            if (inJsonObject.has("outAmt")){
                amount = inJsonObject.getString("outAmt");
                inJsonObject.put("outAmt", makeFieldAmount(amount));
                strTransData = inJsonObject.toString();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return strTransData;
    }

    public String makeFieldAmount(String amount) {
        String[] part = amount.split("\\.");
        switch (part.length) {
            case 1:
                return ("0000000000" + amount + "00").substring(amount.length());
            case 2:
                String digit = part[1].length() >= 2 ? part[1].substring(0, 2)
                        : (part[1] + "00").substring(0, 2);
                amount = "000000000000" + part[0] + digit;
                return amount.substring(amount.length() - 12);
        }
        System.out.println(part.length);
        return null;
    }
}
