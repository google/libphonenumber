package com.google.phonenumbers.demoapp.main;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.phonenumbers.demoapp.R;
import com.google.phonenumbers.demoapp.contacts.ContactsPermissionManagement;
import com.google.phonenumbers.demoapp.contacts.ContactsRead;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberFormatting;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import com.google.phonenumbers.demoapp.result.ResultActivity;
import java.util.ArrayList;

/** Used to handle and process interactions from/with the main page UI of the app. */
public class MainActivity extends AppCompatActivity {

  private CountryDropdown countryDropdown;
  private Button btnCountryDropdownReset;
  private CheckBox cbIgnoreWhitespace;
  private TextView tvError;
  private Button btnError;
  private Button btnStart;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setTitle(R.string.app_name_long);
    }

    countryDropdown = findViewById(R.id.country_dropdown);
    btnCountryDropdownReset = findViewById(R.id.btn_country_dropdown_reset);
    cbIgnoreWhitespace = findViewById(R.id.cb_ignore_whitespace);
    tvError = findViewById(R.id.tv_error);
    btnError = findViewById(R.id.btn_error);
    btnStart = findViewById(R.id.btn_start);

    btnCountryDropdownReset.setOnClickListener(v -> setSimCountryOnCountryDropdown());
    btnStart.setOnClickListener(v -> btnStartClicked());
  }

  @Override
  protected void onStart() {
    super.onStart();
    // Reset all UI elements to default state
    updateUiState(UiState.SELECT_COUNTRY_CODE);
    setSimCountryOnCountryDropdown();
    cbIgnoreWhitespace.setChecked(true);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // Return of the permission result is not about the requested contacts permission
    if (requestCode != ContactsPermissionManagement.CONTACTS_PERMISSION_REQUEST_CODE) {
      return;
    }

    if (grantResults.length == 2
        && grantResults[0] == PackageManager.PERMISSION_GRANTED
        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
      updateUiState(UiState.PROCESSING);
      startProcess();
    } else {
      ContactsPermissionManagement.addOneToNumberOfDenials(this);
      switch (ContactsPermissionManagement.getState(this)) {
          // NEED_REQUEST is specifically to handle the case where the user dismisses the first
          // permission dialog shown since the app's installation.
        case NEEDS_REQUEST:
        case SHOW_RATIONALE:
          updateUiState(UiState.PERMISSION_ERROR_GRANT_IN_APP);
          break;
        case NEEDS_GRANT_IN_SETTINGS:
        default:
          updateUiState(UiState.PERMISSION_ERROR_GRANT_IN_SETTINGS);
          break;
      }
    }
  }

  /**
   * Updates the UI to represent the param {@code uiState}.
   *
   * @param uiState State the UI should be changed to
   */
  private void updateUiState(UiState uiState) {
    // Specifically: countryDropdown, btnCountryDropdownReset, cbIgnoreWhitespace, and btnStart
    boolean mainInteractionsEnabled = false;
    // Specifically: tvError, and btnError
    boolean showError = false;

    switch (uiState) {
      case SELECT_COUNTRY_CODE:
      default:
        mainInteractionsEnabled = true;
        btnStart.setText(getText(R.string.main_activity_start_text_default));
        break;
      case PROCESSING:
        btnStart.setText(getText(R.string.main_activity_start_text_processing));
        break;
      case PERMISSION_ERROR_GRANT_IN_APP:
        showError = true;
        tvError.setText(getText(R.string.main_activity_error_text_grant_in_app));
        btnError.setText(getText(R.string.main_activity_error_cta_grant_in_app));
        btnError.setOnClickListener(v -> ContactsPermissionManagement.request(this));
        btnStart.setText(getText(R.string.main_activity_start_text_processing));
        break;
      case PERMISSION_ERROR_GRANT_IN_SETTINGS:
        showError = true;
        tvError.setText(getText(R.string.main_activity_error_text_grant_in_settings));
        btnError.setText(getText(R.string.main_activity_error_cta_grant_in_settings));
        btnError.setOnClickListener(v -> ContactsPermissionManagement.openSystemSettings(this));
        btnStart.setText(getText(R.string.main_activity_start_text_default));
        break;
    }

    countryDropdown.setEnabled(mainInteractionsEnabled);
    btnCountryDropdownReset.setEnabled(mainInteractionsEnabled);
    cbIgnoreWhitespace.setEnabled(mainInteractionsEnabled);
    tvError.setVisibility(showError ? View.VISIBLE : View.INVISIBLE);
    btnError.setVisibility(showError ? View.VISIBLE : View.INVISIBLE);
    btnStart.setEnabled(mainInteractionsEnabled);
  }

  /** Sets the SIM's country as selected item in the country dropdown. */
  private void setSimCountryOnCountryDropdown() {
    TelephonyManager telephonyManager =
        (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    countryDropdown.setInputForNameCode(telephonyManager.getSimCountryIso());
  }

  /**
   * Called when the start button is clicked. If contacts permissions are granted, starts reading
   * the contacts. If permissions are not granted, handle that appropriately based on the current
   * state in the process.
   */
  private void btnStartClicked() {
    updateUiState(UiState.PROCESSING);

    if (!countryDropdown.validateInput()) {
      updateUiState(UiState.SELECT_COUNTRY_CODE);
      return;
    }

    switch (ContactsPermissionManagement.getState(this)) {
      case ALREADY_GRANTED:
        startProcess();
        break;
      case NEEDS_REQUEST:
        ContactsPermissionManagement.request(this);
        break;
      case SHOW_RATIONALE:
        updateUiState(UiState.PERMISSION_ERROR_GRANT_IN_APP);
        break;
      case NEEDS_GRANT_IN_SETTINGS:
      default:
        updateUiState(UiState.PERMISSION_ERROR_GRANT_IN_SETTINGS);
        break;
    }
  }

  /**
   * Starts the process of reading the contacts, formatting the numbers and starting a {@link
   * ResultActivity} to show the results.
   */
  private void startProcess() {
    ArrayList<PhoneNumberInApp> phoneNumbersSorted = ContactsRead.getAllPhoneNumbersSorted(this);

    if (phoneNumbersSorted.isEmpty()) {
      showNoContactsExistSnackbar();
      updateUiState(UiState.SELECT_COUNTRY_CODE);
      return;
    }

    // Format each phone number.
    for (PhoneNumberInApp phoneNumber : phoneNumbersSorted) {
      PhoneNumberFormatting.formatPhoneNumberInApp(
          phoneNumber, countryDropdown.getNameCodeForInput(), cbIgnoreWhitespace.isChecked());
    }

    // Start new activity to show results.
    Intent intent = new Intent(this, ResultActivity.class);
    intent.putExtra(ResultActivity.PHONE_NUMBERS_SORTED_SERIALIZABLE_EXTRA_KEY, phoneNumbersSorted);
    startActivity(intent);
  }

  /** Shows a Snackbar informing that no contacts exist. */
  private void showNoContactsExistSnackbar() {
    Snackbar.make(
            countryDropdown, R.string.main_activity_no_contacts_exist_text, Snackbar.LENGTH_LONG)
        .show();
  }

  /** Represents the different states the UI of this activity can become. */
  enum UiState {
    /** The user should select a country from the dropdown. */
    SELECT_COUNTRY_CODE,
    /** Used when loading or processing. The UI is disabled for the user during this time. */
    PROCESSING,
    /**
     * Shows a text explaining that the app needs contacts permission to work, and a button to grant
     * the permission directly in the app.
     */
    PERMISSION_ERROR_GRANT_IN_APP,
    /**
     * Shows a text explaining that the app does not have contacts permission, and a button to go to
     * the system settings to grant the permission.
     */
    PERMISSION_ERROR_GRANT_IN_SETTINGS
  }
}
