package com.google.phonenumbers.demoapp.contacts;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Handles everything related to the contacts permissions ({@link permission#READ_CONTACTS} and
 * {@link permission#WRITE_CONTACTS}) and the requesting process to grant the permissions.
 */
public class ContactsPermissionManagement {

  public static final int CONTACTS_PERMISSION_REQUEST_CODE = 0;

  private static final String SHARED_PREFS_NAME = "contacts-permission-management";
  private static final String NUMBER_OF_CONTACTS_PERMISSION_DENIALS_KEY =
      "NUMBER_OF_CONTACTS_PERMISSION_DENIALS";

  private ContactsPermissionManagement() {}

  /**
   * Returns the current state of the permissions granting as {@link PermissionState}.
   *
   * @param activity Activity of the app
   * @return {@link PermissionState} of the permissions granting
   */
  public static PermissionState getState(Activity activity) {
    if (isGranted(activity.getApplicationContext())) {
      return PermissionState.ALREADY_GRANTED;
    }
    if (!shouldPermissionBeRequestedInApp(activity.getApplicationContext())) {
      return PermissionState.NEEDS_GRANT_IN_SETTINGS;
    }
    if (shouldShowRationale(activity)) {
      return PermissionState.SHOW_RATIONALE;
    }
    return PermissionState.NEEDS_REQUEST;
  }

  /**
   * Returns whether the contacts permissions ({@link permission#READ_CONTACTS} and {@link
   * permission#WRITE_CONTACTS}) are granted for the param {@code context}.
   *
   * @param context Context of the app
   * @return boolean whether contacts permissions are granted
   */
  public static boolean isGranted(Context context) {
    if (ContextCompat.checkSelfPermission(context, permission.READ_CONTACTS)
        == PackageManager.PERMISSION_DENIED) {
      return false;
    }
    return ContextCompat.checkSelfPermission(context, permission.WRITE_CONTACTS)
        != PackageManager.PERMISSION_DENIED;
  }

  /**
   * Returns whether the permissions should be requested directly in the app or not. Specifically
   * returns true if less than 2 denials happened since the app installation.
   *
   * @param context Context of the app
   * @return boolean whether the permissions should be requested directly in the app
   */
  private static boolean shouldPermissionBeRequestedInApp(Context context) {
    return getNumberOfDenials(context) < 2;
  }

  /**
   * Returns the number of times the permission dialog has been denied since the app installation.
   * Dismissing the permission dialog instead of answering is considered a denial.
   *
   * @param context Context of the app
   * @return int number of times the permission has been denied
   */
  private static int getNumberOfDenials(Context context) {
    SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
    return preferences.getInt(NUMBER_OF_CONTACTS_PERMISSION_DENIALS_KEY, 0);
  }

  /**
   * Adds 1 to the number of denials since the app installation. Should be called every time the
   * user denies the permission (in the dialog). Dismissing the permission dialog instead of
   * answering is considered a denial.
   *
   * @param context Context of the app
   */
  public static void addOneToNumberOfDenials(Context context) {
    SharedPreferences.Editor editor =
        context.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE).edit();
    editor.putInt(NUMBER_OF_CONTACTS_PERMISSION_DENIALS_KEY, getNumberOfDenials(context) + 1);
    editor.apply();
  }

  /**
   * Returns whether a rational should be shown explaining why the app requests these permissions
   * (before requesting them).
   *
   * @param activity Activity of the app
   * @return boolean whether a rational should be shown
   */
  private static boolean shouldShowRationale(Activity activity) {
    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.READ_CONTACTS)) {
      return true;
    }
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.WRITE_CONTACTS);
  }

  /**
   * Requests the contact permissions ({@link permission#READ_CONTACTS} and {@link
   * permission#WRITE_CONTACTS}) in the param {@code activity} with the request code {@link
   * ContactsPermissionManagement#CONTACTS_PERMISSION_REQUEST_CODE}.
   *
   * @param activity Activity of the app
   */
  public static void request(Activity activity) {
    activity.requestPermissions(
        new String[] {permission.READ_CONTACTS, permission.WRITE_CONTACTS},
        CONTACTS_PERMISSION_REQUEST_CODE);
  }

  /**
   * Opens the system settings (app details page) if the app can. Special cases that can not open
   * the system settings are for example emulators without Play Store installed.
   *
   * @param activity Activity of the app
   */
  public static void openSystemSettings(Activity activity) {
    Intent intent =
        new Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + activity.getPackageName()));
    activity.startActivity(intent);
  }

  /** Represents the different states the permissions granting process can be at. */
  public enum PermissionState {
    /** The permissions are already granted. The action requiring the permissions can be started. */
    ALREADY_GRANTED,
    /**
     * The permissions are not granted, but can be requested directly (without showing a rationale).
     */
    NEEDS_REQUEST,
    /**
     * The permissions are not granted and a rationale should be shown explaining why the app
     * requests the permissions before requesting them (directly in the app).
     */
    SHOW_RATIONALE,
    /**
     * The permissions are not granted and can not be granted directly in the app. The user has to
     * grant permissions in the system settings instead.
     */
    NEEDS_GRANT_IN_SETTINGS
  }
}
