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

  private static int activityCount = 0;
  private int activityId = 0;

  // loginActivity is persistent, except the OS kills it off whenever it feels like
  // - turns out that when instrumented, this is happening often
  private static CustomActivity loginActivity;

  // dialogActivity closes and finishs every time it runs - should not return on resume 
  // - unless loginActivity was previous nuked by OS, then it should migrate
  private static CustomActivity dialogActivity;

  private UiLifecycleHelper uiHelper;
  private Session.StatusCallback statusCallback;
  private WebDialog.OnCompleteListener dialogCallback;
  
  public CustomActivity() {
    activityId = ++activityCount;
    Extension.debug("CustomActivity(): %d", activityId);
  }

  public static CustomActivity getLoginActivity() {
    return loginActivity;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Extension.debug("CustomActivity %d:onCreate", activityId);
    new RemapResourceIds(this);
    super.onCreate(savedInstanceState);

    statusCallback = new Session.StatusCallback() {
      @Override
      public void call(Session session, SessionState state, Exception exception) {
        Extension.debug("CustomActivity:onSessionStateChange(%s) [e:%s]", state, exception);
        // forward this back to the caller for asynch dispatch
        FacebookLib.sessionStatusCallback.call(session,state,exception);
        if (state == SessionState.CLOSED_LOGIN_FAILED) {
          if (loginActivity != null) {
            //Extension.debug("loginActivity.finish() called");
            loginActivity.finish();  
          }
        }
        if ((state == SessionState.OPENED)||(state == SessionState.OPENED_TOKEN_UPDATED)) {
        }
      }
    };
    
    dialogCallback = new WebDialog.OnCompleteListener() {
      @Override
      public void onComplete(Bundle values, FacebookException error) {
        // forward this back to the caller for asynch dispatch
        FacebookLib.dialogCompleteCallback.onComplete(values,error);
        
        //  Extension.debug("dialogCompleteCallback loginActivity %d dialogActivity %d", 
        //      loginActivity != null ? loginActivity.activityId : -1, 
        //      dialogActivity != null ? dialogActivity.activityId : -1);
        
        // if these activities ended up the same, keep the activity alive as the loginActivity
        if (loginActivity != dialogActivity) {
          //Extension.debug("dialogActivity.finish() called");
          dialogActivity.finish();  
        }
        else {
          // Extension.debug("dialogActivity.finish() NOT called");
        }
        // but the dialogActivity is done
        dialogActivity = null;
      }
    };

	  // the bulk of the fb boilerplate is now provided
	  uiHelper = new UiLifecycleHelper(this, statusCallback);
    uiHelper.onCreate(savedInstanceState);
		
	  Session session = Session.getActiveSession();
	  if (FacebookLib.isSessionValid() == false) {
	    // if this player has an old access token, try to migrate it forward
	    AccessToken oldAccessToken = FacebookLib.oldAccessToken;
      if (oldAccessToken != null) {
        session = Session.openActiveSessionWithAccessToken(this, oldAccessToken, null);
        Assert.assertEquals(session, Session.getActiveSession());
      }
	  }
	
	  Intent intent = getIntent();
    boolean allowLoginUI = intent.getBooleanExtra("allowLoginUI", false);

	  // if no active open session, create one with the ui-enabled helper
    if (FacebookLib.isSessionValid() == false) {
	    session = Session.openActiveSession(this, allowLoginUI, null);
      
      // if login didn't get at least a session, close this activity
      if (session == null) {
        //Extension.debug("CustomActivity:onCreate called finish()");
        finish();
        return;
      }
    } 
    if (loginActivity == null) {
      loginActivity = this;
    }

    // handle startActivity vis a vis showDialog
    String method = intent.getStringExtra("method");
    Bundle params = intent.getBundleExtra("params");
    // don't restore these values on save/load - pause/resume
    intent.putExtra("method", (String)null);
    intent.putExtra("params", (Bundle)null);

    if (("feed").equalsIgnoreCase(method)) {
      Assert.assertTrue(session.isOpened());
      Assert.assertNull(dialogActivity);
      dialogActivity = this;
      WebDialog feedDialog =
        new WebDialog.FeedDialogBuilder(dialogActivity, Session.getActiveSession(), params)
      .setOnCompleteListener(dialogCallback)
      .build();
      feedDialog.show();
    }
    else if (("apprequests").equalsIgnoreCase(method)) {
      Assert.assertTrue(session.isOpened());
      Assert.assertNull(dialogActivity);
      dialogActivity = this;
      WebDialog requestDialog =
        new WebDialog.RequestsDialogBuilder(dialogActivity, Session.getActiveSession(), params)
      .setOnCompleteListener(dialogCallback)
      .build();
      requestDialog.show();
    }
  }

  @Override
  public void onResume() {
    Extension.debug("CustomActivity %d:onResume", activityId);
    super.onResume();
    uiHelper.onResume();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Extension.debug("CustomActivity %d:onActivityResult %d, %d", activityId, requestCode, resultCode);
    super.onActivityResult(requestCode, resultCode, data);
    uiHelper.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onPause() {
    Extension.debug("CustomActivity %d:onPause", activityId);
    super.onPause();
    uiHelper.onPause();
  }

  @Override
  public void onDestroy() {
    Extension.debug("CustomActivity %d:onDestroy", activityId);
    // Extension.debug("onDestroy loginActivity %d dialogActivity %d", 
    //      loginActivity != null ? loginActivity.activityId : -1, 
    //      dialogActivity != null ? dialogActivity.activityId : -1);

    super.onDestroy();
    uiHelper.onDestroy();
    if (this == loginActivity) {
      //Extension.debug("loginActivity nulled");
      loginActivity = null;
    }
    if (this == dialogActivity) {
      //Extension.debug("dialogActivity nulled");
      dialogActivity = null;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Extension.debug("CustomActivity %d:onSaveInstanceState", activityId);
    super.onSaveInstanceState(outState);
    uiHelper.onSaveInstanceState(outState);
  }
}
