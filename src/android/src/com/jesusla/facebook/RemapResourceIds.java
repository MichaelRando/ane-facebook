package com.jesusla.facebook;

import java.lang.reflect.Field;
import android.content.Context;
import com.facebook.android.R;

public class RemapResourceIds {
  
  // reference: http://techiepulkit.blogspot.in/2013/01/air-android-native-extensions-speeding.html
  public int getRemappedId(Context context, String resourceString) {
    String packageName = context.getPackageName()+".R$";
	String[] arr = new String[2];
	arr = resourceString.split("\\.");
	try {
	  Class someObject = Class.forName(packageName+arr[0]);
	  Field someField = someObject.getField(arr[1]);
	  return someField.getInt(new Integer(0));
	}
	catch (Exception e) {
	  return 0;
	}
  }
  
  static int singleton = 0;
  public RemapResourceIds(Context context) {
	if (singleton == 0) {
	  patchFacebookResourceIdsAtRuntime(context);
	  singleton = 1;
	}
  }
  
  void patchFacebookResourceIdsAtRuntime(Context context) {
    // the sdk ids get remapped when ADT process Android ANEs
    R.id.com_facebook_login_activity_progress_bar = getRemappedId(context,"id.com_facebook_login_activity_progress_bar"); 
    R.id.com_facebook_picker_activity_circle = getRemappedId(context,"id.com_facebook_picker_activity_circle"); 
    R.id.com_facebook_picker_checkbox = getRemappedId(context,"id.com_facebook_picker_checkbox"); 
    R.id.com_facebook_picker_checkbox_stub = getRemappedId(context,"id.com_facebook_picker_checkbox_stub"); 
    R.id.com_facebook_picker_divider = getRemappedId(context,"id.com_facebook_picker_divider"); 
    R.id.com_facebook_picker_done_button = getRemappedId(context,"id.com_facebook_picker_done_button"); 
    R.id.com_facebook_picker_image = getRemappedId(context,"id.com_facebook_picker_image"); 
    R.id.com_facebook_picker_list_section_header = getRemappedId(context,"id.com_facebook_picker_list_section_header"); 
    R.id.com_facebook_picker_list_view = getRemappedId(context,"id.com_facebook_picker_list_view"); 
    R.id.com_facebook_picker_profile_pic_stub = getRemappedId(context,"id.com_facebook_picker_profile_pic_stub"); 
    R.id.com_facebook_picker_row_activity_circle = getRemappedId(context,"id.com_facebook_picker_row_activity_circle"); 
    R.id.com_facebook_picker_title = getRemappedId(context,"id.com_facebook_picker_title"); 
    R.id.com_facebook_picker_title_bar = getRemappedId(context,"id.com_facebook_picker_title_bar"); 
    R.id.com_facebook_picker_title_bar_stub = getRemappedId(context,"id.com_facebook_picker_title_bar_stub"); 
    R.id.com_facebook_picker_top_bar = getRemappedId(context,"id.com_facebook_picker_top_bar"); 
    R.id.com_facebook_placepickerfragment_search_box_stub = getRemappedId(context,"id.com_facebook_placepickerfragment_search_box_stub"); 
    R.id.com_facebook_usersettingsfragment_login_button = getRemappedId(context,"id.com_facebook_usersettingsfragment_login_button"); 
    R.id.com_facebook_usersettingsfragment_logo_image = getRemappedId(context,"id.com_facebook_usersettingsfragment_logo_image"); 
    R.id.com_facebook_usersettingsfragment_profile_name = getRemappedId(context,"id.com_facebook_usersettingsfragment_profile_name"); 
    R.id.large = getRemappedId(context,"id.large"); 
    R.id.normal = getRemappedId(context,"id.normal"); 
    R.id.picker_subtitle = getRemappedId(context,"id.picker_subtitle"); 
    R.id.search_box = getRemappedId(context,"id.search_box"); 
    R.id.small = getRemappedId(context,"id.small"); 
	R.string.com_facebook_dialogloginactivity_ok_button = getRemappedId(context,"string.com_facebook_dialogloginactivity_ok_button");
    R.string.com_facebook_loginview_log_out_button = getRemappedId(context,"string.com_facebook_loginview_log_out_button");
    R.string.com_facebook_loginview_log_in_button = getRemappedId(context,"string.com_facebook_loginview_log_in_button");
    R.string.com_facebook_loginview_logged_in_as = getRemappedId(context,"string.com_facebook_loginview_logged_in_as");
    R.string.com_facebook_loginview_logged_in_using_facebook = getRemappedId(context,"string.com_facebook_loginview_logged_in_using_facebook");
    R.string.com_facebook_loginview_log_out_action = getRemappedId(context,"string.com_facebook_loginview_log_out_action");
    R.string.com_facebook_loginview_cancel_action = getRemappedId(context,"string.com_facebook_loginview_cancel_action");
    R.string.com_facebook_logo_content_description = getRemappedId(context,"string.com_facebook_logo_content_description");
    R.string.com_facebook_usersettingsfragment_log_in_button = getRemappedId(context,"string.com_facebook_usersettingsfragment_log_in_button");
    R.string.com_facebook_usersettingsfragment_logged_in = getRemappedId(context,"string.com_facebook_usersettingsfragment_logged_in");
    R.string.com_facebook_usersettingsfragment_not_logged_in = getRemappedId(context,"string.com_facebook_usersettingsfragment_not_logged_in");
    R.string.com_facebook_placepicker_subtitle_format = getRemappedId(context,"string.com_facebook_placepicker_subtitle_format");
    R.string.com_facebook_placepicker_subtitle_catetory_only_format = getRemappedId(context,"string.com_facebook_placepicker_subtitle_catetory_only_format");
    R.string.com_facebook_placepicker_subtitle_were_here_only_format = getRemappedId(context,"string.com_facebook_placepicker_subtitle_were_here_only_format");
    R.string.com_facebook_picker_done_button_text = getRemappedId(context,"string.com_facebook_picker_done_button_text");
    R.string.com_facebook_choose_friends = getRemappedId(context,"string.com_facebook_choose_friends");
    R.string.com_facebook_nearby = getRemappedId(context,"string.com_facebook_nearby");
    R.string.com_facebook_loading = getRemappedId(context,"string.com_facebook_loading");
    R.string.com_facebook_internet_permission_error_title = getRemappedId(context,"string.com_facebook_internet_permission_error_title");
    R.string.com_facebook_internet_permission_error_message = getRemappedId(context,"string.com_facebook_internet_permission_error_message");
    R.string.com_facebook_requesterror_web_login = getRemappedId(context,"string.com_facebook_requesterror_web_login");
    R.string.com_facebook_requesterror_relogin = getRemappedId(context,"string.com_facebook_requesterror_relogin");
    R.string.com_facebook_requesterror_password_changed = getRemappedId(context,"string.com_facebook_requesterror_password_changed");
    R.string.com_facebook_requesterror_reconnect = getRemappedId(context,"string.com_facebook_requesterror_reconnect");
    R.string.com_facebook_requesterror_permissions = getRemappedId(context,"string.com_facebook_requesterror_permissions");
  }
}