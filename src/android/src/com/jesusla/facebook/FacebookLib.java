package com.jesusla.facebook;

import java.net.URLEncoder;
import java.util.Date;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import junit.framework.Assert;

import com.facebook.*;
import com.facebook.widget.*;

import com.jesusla.ane.Context;
import com.jesusla.ane.Extension;

// legacy includes {showDialog}
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.DialogError;

public class FacebookLib extends Context {
  static public String applicationId;
  static public AccessToken oldAccessToken;
  static public Session.StatusCallback sessionStatusCallback;
  static public WebDialog.OnCompleteListener dialogCompleteCallback;
  private Boolean m_allowLoginUI = false;
  private Facebook facebook = null;
  
  public FacebookLib() {
    Extension.debug("FacebookLib::FacebookLib");
    registerFunction("applicationId", "getApplicationId");
    registerFunction("accessToken", "getAccessToken");
    registerFunction("expirationDate", "getExpirationDate");
    registerFunction("login");
    registerFunction("logout");
    registerFunction("isSessionValid");
    registerFunction("showDialog");
    registerFunction("graph");
  }

  @Override
  protected void initContext() {
    Extension.debug("FacebookLib::initContext");
    new RemapResourceIds(getActivity());
	
	  sessionStatusCallback = new Session.StatusCallback() {
      @Override
	    public void call(Session session, SessionState state, Exception exception) {
	      // dispatch the login response for user initiated sessions but not my autosession
		    if (m_allowLoginUI) {
	        if (state == SessionState.CLOSED_LOGIN_FAILED) {
	          dispatchStatusEventAsync("LOGIN_FAILED", "SESSION");
	        }
	        if ((state == SessionState.OPENED)||(state == SessionState.OPENED_TOKEN_UPDATED)) {
	          dispatchStatusEventAsync("LOGIN", "SESSION");
	        }
		    }
	    }
    };

    dialogCompleteCallback = new WebDialog.OnCompleteListener() {
      @Override
      public void onComplete(Bundle values, FacebookException error) {
        if (error == null) {
          String url = encodeBundle(values);
          asyncFlashCall(null, null, "dialogDidComplete", url);
        } else if (error instanceof FacebookOperationCanceledException) {
          // User clicked the "x" button
          String url = encodeBundle(values);
          asyncFlashCall(null, null, "dialogDidNotComplete", url);
        } else if (error instanceof FacebookDialogException) {
          FacebookDialogException de = (FacebookDialogException)error;
          Extension.debug("FacebookDialogException (%s, %d, %s) ", de.getMessage(), de.getErrorCode(), de.getFailingUrl());      
          String url = encodeBundle(values);
          Extension.debug("FacebookDialogException values " + url);
          asyncFlashCall(null, null, "dialogDidNotComplete", url);
        } else {
          // Generic, ex: network error
          Extension.debug("FacebookException error = "+ error.toString());      
          String url = encodeBundle(values);
          asyncFlashCall(null, null, "dialogDidFailWithError", url);
        }
      }
    };

    // use our fbAppID instead of com.facebook.sdk.applicationId
    applicationId = getProperty("FacebookAppID");
	  Extension.debug("FacebookAppID = "+ applicationId);

    Settings.setShouldAutoPublishInstall(true); 
	
	  // readdOldAccessToken() {
    SharedPreferences preferences = getActivity().getPreferences(Activity.MODE_PRIVATE);
    String accessToken = preferences.getString("FBAccessTokenKey", null);
    long accessExpires = preferences.getLong("FBExpirationDateKey", 0);
    long lastAccessUpdate = preferences.getLong("FBLastAccessUpdate", 0);
    if (accessToken != null) {
      oldAccessToken = AccessToken.createFromExistingAccessToken(accessToken, new Date(accessExpires), new Date(lastAccessUpdate), null, null);
	    // removeOldAccessToken() {
      SharedPreferences.Editor editor = preferences.edit();
      editor.remove("FBAccessTokenKey");
      editor.remove("FBExpirationDateKey");
      editor.remove("FBLastAccessUpdate");
      editor.commit();
    }
	
	  // Previously, the sessionValid flag was used and extended to say: we know we've got credentials, just reuse them
	  // To emulate and improve that behavior, I attempt a silent login at launch, which takes the place of extending credentials from before
	  if (isSessionValid() == false) {
	    startLoginActivity(false);
	  }
  }

  public String getApplicationId() {
    return applicationId;
  }

  public String getAccessToken() {
    if (isSessionValid()) {
	    return Session.getActiveSession().getAccessToken();
	  }
    return null;
  }

  public Date getExpirationDate() {
	  if (isSessionValid()) {
	    Date expires = Session.getActiveSession().getExpirationDate();
      return expires;
	  }
	  return null;
  }
  
  public void login() {
	  Extension.debug("FacebookLib::login");
	  // Extension.debug("FacebookLib::isSessionValid = " + isSessionValid());
	
	  // don't bother with the login unless necessary
	  if (isSessionValid() == false) {
	    startLoginActivity(true);
	  }
	  else {
	    dispatchStatusEventAsync("LOGIN", "SESSION");
	  }
  }

  private void startLoginActivity(boolean allowLoginUI) {
    m_allowLoginUI = allowLoginUI;
	  Intent intent = new Intent(getActivity(), CustomActivity.class);
	  intent.putExtra("allowLoginUI", m_allowLoginUI);
    getActivity().startActivity(intent);
  }

  public void logout() {
	  if (isSessionValid()) {
	    Session.getActiveSession().closeAndClearTokenInformation();
	  }
    dispatchStatusEventAsync("LOGOUT", "SESSION");
  }

  static public boolean isSessionValid() {
    Session session = Session.getActiveSession();
    if (session != null) {
      return session.isOpened();
    }
    return false;
  }
  private final DialogListener dialogListener = new DialogListener() {
    @Override public void onFacebookError(FacebookError e) { asyncFlashCall(null, null, "dialogDidFailWithError", ""); }
    @Override public void onError(DialogError e) { asyncFlashCall(null, null, "dialogDidFailWithError", ""); }
    @Override public void onCancel() { asyncFlashCall(null, null, "dialogDidNotComplete", ""); }
    @Override public void onComplete(Bundle values) {
      String url = encodeBundle(values);
      asyncFlashCall(null, null, "dialogDidComplete", url);
    }
  };
  
  public void showDialog(String action, Bundle params) {
    String method = params.getString("method");
    Intent intent = new Intent(getActivity(), CustomActivity.class);
    intent.putExtra("method", method);
    intent.putExtra("params", params);

    getActivity().startActivity(intent);
  }
  /*
  public void showDialog(String action, Bundle params) {
    if (facebook == null) {
      facebook = new Facebook(applicationId);
      Session session = Session.getActiveSession();
      Assert.assertTrue(session.isOpened());
      facebook.setAccessToken(session.getAccessToken());
      facebook.setAccessExpires(session.getExpirationDate().getTime());
    }
    facebook.dialog(getActivity(), action, params, dialogListener);
  }*/

  public String graph(String graphPath, Bundle params, String httpMethodString) {
    final String uuid = UUID.randomUUID().toString();
    Session session = Session.getActiveSession();
	  Assert.assertTrue(session.isOpened());

    Request.Callback callback = new Request.Callback() {
      @Override
      public void onCompleted(Response response) {
		    if (response.getGraphObject() != null) {
          JSONObject data = response.getGraphObject().getInnerJSONObject();
          asyncFlashCall(null, null, "requestDidLoad", uuid, data);
		    }
		    else {
		      asyncFlashCall(null, null, "requestDidFailWithError", uuid, response.getError().getRequestResultBody());
          // I'm worried when requests fail but we thought the session is valid and open
          // so I'm clearing the active session, you will have to login fresh.
          Session.setActiveSession(null);
		    }
      }
    };
    HttpMethod httpMethod = HttpMethod.GET;
    if (httpMethodString.equalsIgnoreCase("delete")) {
      httpMethod = HttpMethod.DELETE;
    }
    if (httpMethodString.equalsIgnoreCase("post")) {
      httpMethod = HttpMethod.POST;
    }
    Request request = new Request(session, graphPath, params, httpMethod, callback);
    request.executeAsync();
    return uuid;
  }

  private String encodeBundle(Bundle bundle) {
    String url = "fbconnect://success";
	  if (bundle != null ) {
	    url += "?";
	    for (String key : bundle.keySet()) {
	      String val = bundle.getString(key);
	      key = URLEncoder.encode(key);
	      val = URLEncoder.encode(val);
	      if (!url.endsWith("?")) {
          url += '&';  
        }
  		  url = url + key + '=' + val;
	    }
	  }
    return url;
  }
}
