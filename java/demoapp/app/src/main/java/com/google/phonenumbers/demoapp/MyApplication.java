package com.google.phonenumbers.demoapp;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

/**
 * Used instead of default {@link Application} instance. Only difference is that this implementation
 * enabled Dynamic Colors for the app.
 */
public class MyApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    DynamicColors.applyToActivitiesIfAvailable(this);
  }
}
