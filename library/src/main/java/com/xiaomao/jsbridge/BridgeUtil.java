package com.xiaomao.jsbridge;

import android.content.Context;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BridgeUtil {
    public static final String toLoadJs = "jsbridge";
    final static String GL_SCHEMA = "gl://";
    final static String GL_RESPONSE = GL_SCHEMA + "response/";
    final static String GL_REQUEST = GL_SCHEMA + "request/";
    final static String GL_PAGE_LOADED = GL_SCHEMA + "pageLoaded/";
    final static String UNDERLINE_STR = "_";

    final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";
    final static String JS_HANDLE_REQUEST_FROM_JAVA = "javascript:WebViewJavascriptBridge._handleRequestFromNative('%s');";
    final static String JS_HANDLE_RESPONSE_FROM_JAVA = "javascript:WebViewJavascriptBridge._handleResponseFromNative('%s');";


    public static void webViewLoadLocalJs(WebView view, String path) {
        String jsContent = assetFile2Str(view.getContext(), path);
        view.loadUrl("javascript:" + jsContent);
    }

    public static String assetFile2Str(Context c, String urlStr) {
        InputStream in = null;
        try {
            in = c.getAssets().open(urlStr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null && !line.matches("^\\s*//.*")) {
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
