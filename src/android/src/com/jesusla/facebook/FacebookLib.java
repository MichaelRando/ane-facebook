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


import java.lang.reflect.Field;

public class FacebookLib extends Context {
  static public FacebookLib staticReference;
  public String applicationId;
  public AccessToken oldAccessToken;
  public Session.StatusCallback sessionStatusCallback;
  
  public FacebookLib() {
    FacebookLib.staticReference = this;
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
		if (CustomActivity.staticReference.getIntent().getBooleanExtra("allowLoginUI", false)) {
	      if (state == SessionState.CLOSED_LOGIN_FAILED) {
	        dispatchStatusEventAsync("LOGIN_FAILED", "SESSION");
	      }
	      if ((state == SessionState.OPENED)||(state == SessionState.OPENED_TOKEN_UPDATED)) {
	        dispatchStatusEventAsync("LOGIN", "SESSION");
	      }
		}
	  }
    };

	
	
    // use our fbAppID instead of com.facebook.sdk.ApplicationId
    applicationId = getProperty("FacebookAppID");
	Extension.debug("FacebookAppID = "+ applicationId);

    Settings.setShouldAutoPublishInstall(true); 
	
	// readOldAccessToken() {
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
	if ((isSessionValid() == false) || (CustomActivity.staticReference == null)) {
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
	// Extension.debug("FacebookLib::customActivity = " + customActivity);
	
	// don't bother with the login unless necessary
	if ((isSessionValid() == false) || (CustomActivity.staticReference == null)) {
	  startLoginActivity(true);
	}
	else {
	  dispatchStatusEventAsync("LOGIN", "SESSION");
	}
  }

  private void startLoginActivity(boolean allowLoginUI) {
	Intent intent = new Intent(getActivity(), CustomActivity.class);
	intent.putExtra("allowLoginUI", allowLoginUI);
    getActivity().startActivity(intent);
  }

  public void logout() {
	if (isSessionValid()) {
	  Session.getActiveSession().closeAndClearTokenInformation();
	}
	if (CustomActivity.staticReference != null) {
	  CustomActivity.staticReference.finish();
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

  public void showDialog(String action, Bundle params) {
    Session session = Session.getActiveSession();
	  Assert.assertTrue(session.isOpened());
	  Assert.assertNotNull(CustomActivity.staticReference);
	  
    WebDialog.OnCompleteListener onComplete = new WebDialog.OnCompleteListener() {
      @Override
      public void onComplete(Bundle values, FacebookException error) {
        if (error == null) {
          String url = encodeBundle(values);
          asyncFlashCall(null, null, "dialogDidComplete", url);
        } else if (error instanceof FacebookOperationCanceledException) {
          // User clicked the "x" button
          String url = encodeBundle(values);
          asyncFlashCall(null, null, "dialogDidNotComplete", url);
        } else {
          // Generic, ex: network error
          String url = encodeBundle(values);
          asyncFlashCall(null, null, "dialogDidFailWithError", url);
        }
      }
    };

    String method = params.getString("method");
    if (method.equalsIgnoreCase("feed")) {
      WebDialog feedDialog =
        new WebDialog.FeedDialogBuilder(CustomActivity.staticReference, Session.getActiveSession(), params)
      .setOnCompleteListener(onComplete)
      .build();
      feedDialog.show();
    }
    else if (method.equalsIgnoreCase("apprequests")) {
      WebDialog requestDialog =
        new WebDialog.RequestsDialogBuilder(CustomActivity.staticReference, Session.getActiveSession(), params)
      .setOnCompleteListener(onComplete)
      .build();
      requestDialog.show();
    }
  }

  public String graph(String graphPath, Bundle params, String httpMethodString) {
    final String uuid = UUID.randomUUID().toString();
    Session session = Session.getActiveSession();
	Assert.assertTrue(session.isOpened());

    Request.Callback callback = new Request.Callback() {
      @Override
      public void onCompleted(Response response) {
		if (response.getGraphObject() != null) {
		  try {
            JSONObject data = new JSONObject(response.getGraphObject().asMap());
            asyncFlashCall(null, null, "requestDidLoad", uuid, data);
          } 
		  catch (NullPointerException e) {
            Extension.debug("Extension.fail Parsing '%s'", response.getGraphObject().asMap());
		    asyncFlashCall(null, null, "requestDidFailWithError", uuid, e);
          }
		}
		else {
		  asyncFlashCall(null, null, "requestDidFailWithError", uuid, response.getError());
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
	    if (!url.endsWith("?"))
  		  url += '&';
	    url = url + key + '=' + val;
	  }
	}
    return url;
  }
}
