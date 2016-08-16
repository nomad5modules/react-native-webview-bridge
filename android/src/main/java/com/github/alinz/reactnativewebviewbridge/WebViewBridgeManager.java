package com.github.alinz.reactnativewebviewbridge;

import android.webkit.WebView;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.webview.ReactWebViewManager;
import com.facebook.react.views.webview.WebViewConfig;

import android.content.Intent;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.net.Uri;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.util.Log;

import java.util.Map;

import javax.annotation.Nullable;

public class WebViewBridgeManager extends ReactWebViewManager {
  private static final String REACT_CLASS = "RCTWebViewBridge";

  public static final int COMMAND_INJECT_BRIDGE_SCRIPT = 100;
  public static final int COMMAND_SEND_TO_BRIDGE = 101;

  private ReactApplicationContext reactApplicationContext = null;
  private boolean initializedBridge;

  public WebViewBridgeManager(ReactApplicationContext context) {
    super();
    initializedBridge = false;
    reactApplicationContext = context;
  }

  public WebViewBridgeManager(WebViewConfig webViewConfig) {
    super(webViewConfig);
    initializedBridge = false;
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public @Nullable Map<String, Integer> getCommandsMap() {
    Map<String, Integer> commandsMap = super.getCommandsMap();

    commandsMap.put("injectBridgeScript", COMMAND_INJECT_BRIDGE_SCRIPT);
    commandsMap.put("sendToBridge", COMMAND_SEND_TO_BRIDGE);

    return commandsMap;
  }

  @Override
  public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
    super.receiveCommand(root, commandId, args);

    switch (commandId) {
      case COMMAND_INJECT_BRIDGE_SCRIPT:
        injectBridgeScript(root);
        break;
      case COMMAND_SEND_TO_BRIDGE:
        sendToBridge(root, args.getString(0));
        break;
      default:
        //do nothing!!!!
    }
  }

  private void sendToBridge(WebView root, String message) {
    //root.loadUrl("javascript:(function() {\n" + script + ";\n})();");
    String script = "WebViewBridge.onMessage('" + message + "');";
    WebViewBridgeManager.evaluateJavascript(root, script);
  }

  private void injectBridgeScript(WebView root) {
    //this code needs to be called once per context
    if (!initializedBridge) {
      root.addJavascriptInterface(new JavascriptBridge((ReactContext) root.getContext()), "WebViewBridgeAndroid");
      initializedBridge = true;
      root.reload();
    }

    // this code needs to be executed everytime a url changes.
    WebViewBridgeManager.evaluateJavascript(root, ""
            + "(function() {"
            + "if (window.WebViewBridge) return;"
            + "var customEvent = document.createEvent('Event');"
            + "var WebViewBridge = {"
              + "send: function(message) { WebViewBridgeAndroid.send(message); },"
              + "onMessage: function() {}"
            + "};"
            + "window.WebViewBridge = WebViewBridge;"
            + "customEvent.initEvent('WebViewBridge', true, true);"
            + "document.dispatchEvent(customEvent);"
            + "}());");
  }

  static private void evaluateJavascript(WebView root, String javascript) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
      root.evaluateJavascript(javascript, null);
    } else {
      root.loadUrl("javascript:" + javascript);
    }
  }
  @ReactProp(name = "uploadEnabledAndroid")
  public void uploadEnabledAndroid(WebView view, boolean enabled) {
    Log.d("error", "==================== ENABLED ANDEOID UPLOAD");
    if(enabled) {
      view.setWebChromeClient(new WebChromeClient(){

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
          /* ((MainActivity)mActivity).setUploadMessage(uploadMsg); */
          openFileChooserView();

        }

        public boolean onJsConfirm (WebView view, String url, String message, JsResult result){
          return true;
        }

        public boolean onJsPrompt (WebView view, String url, String message, String defaultValue, JsPromptResult result){
          return true;
        }

        // For Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
          /* ((MainActivity)mActivity).setUploadMessage(uploadMsg); */
          openFileChooserView();
        }

        // For Android  > 4.1.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
          /* ((MainActivity)mActivity).setUploadMessage(uploadMsg); */
          openFileChooserView();
        }

        // For Android > 5.0
        public boolean onShowFileChooser (WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
          /* ((MainActivity)mActivity).setmUploadCallbackAboveL(filePathCallback); */
          openFileChooserView();
          return true;
        }

        private void openFileChooserView(){
          Log.d("error", "OPEN FILE CHOOSER VIEW");
          try {
            final Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setType("image/*");
            final Intent chooserIntent = Intent.createChooser(galleryIntent, "choose file");

            Log.d("error", "acti:" + reactApplicationContext.hasCurrentActivity());
            reactApplicationContext.getCurrentActivity().startActivityForResult(chooserIntent, 1);
          } catch (Exception e) {
            Log.d("error", e.toString());
          }
        }
      });
    }
  }
}
