package com.jesusla.facebook;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.*;
import com.facebook.*;
import com.facebook.widget.*;
import junit.framework.Assert;
import com.jesusla.ane.Extension;

public class CustomActivity extends Activity {
  static public CustomActivity staticReference;
  private UiLifecycleHelper uiHelper;
  
  public CustomActivity() {
  	// the fb dialog calls have to execute in this context; leaving the activity running and passing this reference back allows it
	  staticReference = this;
    new RemapResourceIds(this);
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Extension.debug("CustomActivity:onCreate");
    super.onCreate(savedInstanceState);

	  Session.StatusCallback statusCallback = new Session.StatusCallback() {
      @Override
	    public void call(Session session, SessionState state, Exception exception) {
	      Extension.debug("CustomActivity:onSessionStateChange(%s) [e:%s]", state, exception);
		    // forward this back to the caller for asynch dispatch
		    FacebookLib.staticReference.sessionStatusCallback.call(session,state,exception);
	  	  if (state == SessionState.CLOSED_LOGIN_FAILED) {
          Extension.debug("CustomActivity:onCreateCustomActivity.staticReference.finish()");
	        CustomActivity.staticReference.finish();
	      }
	      if ((state == SessionState.OPENED)||(state == SessionState.OPENED_TOKEN_UPDATED)) {
	      }
	    }
	  };
	
	  // the bulk of the fb boilerplate is now provided
	  uiHelper = new UiLifecycleHelper(this, statusCallback);
    uiHelper.onCreate(savedInstanceState);
		
	  Session session = Session.getActiveSession();
	  if (FacebookLib.isSessionValid() == false) {
	    // if this player has an old access token, try to migrate it forward
	    AccessToken oldAccessToken = FacebookLib.staticReference.oldAccessToken;
        if (oldAccessToken != null) {
          session = Session.openActiveSessionWithAccessToken(this, oldAccessToken, null);
          Assert.assertEquals(session, Session.getActiveSession());
        }
	    }
	
	  boolean allowLoginUI = getIntent().getBooleanExtra("allowLoginUI", false);
	  // if no active open session, create one with the ui-enabled helper
    if (FacebookLib.isSessionValid() == false) {
	    session = Session.openActiveSession(this, allowLoginUI, null);
    } 

	  // if login didn't get at least a session, close this activity
	  if (session == null) {
      Extension.debug("CustomActivity:onCreate called finish()");
	    finish();
	  }
  }

  @Override
  public void onResume() {
    Extension.debug("CustomActivity:onResume");
    super.onResume();
    uiHelper.onResume();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Extension.debug("CustomActivity:onActivityResult %d, %d",requestCode, resultCode);
    super.onActivityResult(requestCode, resultCode, data);
    uiHelper.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onPause() {
    Extension.debug("CustomActivity:onPause");
    super.onPause();
    uiHelper.onPause();
  }

  @Override
  public void onDestroy() {
    Extension.debug("CustomActivity:onDestroy");
    super.onDestroy();
    uiHelper.onDestroy();
	uiHelper = null;
	staticReference = null;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Extension.debug("CustomActivity:onSaveInstanceState");
    super.onSaveInstanceState(outState);
    uiHelper.onSaveInstanceState(outState);
  }
}
