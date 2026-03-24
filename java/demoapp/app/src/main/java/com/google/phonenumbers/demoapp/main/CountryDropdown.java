package com.google.phonenumbers.demoapp.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.phonenumbers.demoapp.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A component containing a searchable dropdown input populated with all regions {@link
 * PhoneNumberUtil} supports. Dropdown items are of format {@code [countryName] ([nameCode]) -
 * +[callingCode]} (e.g. {@code Switzerland (CH) - +41}). Method provides access to the name code
 * (e.g. {@code CH}) of the current input. Name code: <a
 * href="https://www.iso.org/glossary-for-iso-3166.html">ISO 3166-1 alpha-2 country code</a> (e.g.
 * {@code CH}). Calling code: <a
 * href="https://www.itu.int/dms_pub/itu-t/opb/sp/T-SP-E.164D-2016-PDF-E.pdf">ITU-T E.164 assigned
 * country code</a> (e.g. {@code 41}).
 */
public class CountryDropdown extends LinearLayout {

  /**
   * Map containing keys of format {@code [countryName] ([nameCode]) - +[callingCode]} (e.g. {@code
   * Switzerland (CH) - +41}), and name codes (e.g. {@code CH}) as values.
   */
  private static final Map<String, String> countryLabelMapNameCode = new HashMap<>();
  /** Ascending sorted list of the keys in {@link CountryDropdown#countryLabelMapNameCode}. */
  private static final List<String> countryLabelSorted = new ArrayList<>();

  private final TextInputLayout input;
  private final AutoCompleteTextView inputEditText;

  /** The name code of the current input. */
  private String nameCode;

  public CountryDropdown(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    inflate(getContext(), R.layout.country_dropdown, this);
    input = findViewById(R.id.country_dropdown_input);
    inputEditText = findViewById(R.id.country_dropdown_input_edit_text);

    inputEditText.setOnKeyListener(
        (v, keyCode, event) -> {
          // If the DEL key is used and the input was a valid dropdown option, clear the input
          // completely
          if (keyCode == KeyEvent.KEYCODE_DEL && setNameCodeForInput()) {
            inputEditText.setText("");
          }
          // Disable the error state when editing the input after the validation revealed an error
          if (input.isErrorEnabled()) {
            disableInputError();
          }
          return false;
        });

    populateCountryLabelMapNameCode();
    setAdapter();
  }

  /**
   * Populates {@link CountryDropdown#countryLabelMapNameCode} with all regions {@link
   * PhoneNumberUtil} supports if not populated yet.
   */
  private void populateCountryLabelMapNameCode() {
    if (!countryLabelMapNameCode.isEmpty()) {
      return;
    }

    Set<String> supportedNameCodes = PhoneNumberUtil.getInstance().getSupportedRegions();
    for (String nameCode : supportedNameCodes) {
      String countryLabel = getCountryLabelForNameCode(nameCode);
      countryLabelMapNameCode.put(countryLabel, nameCode);
    }
  }

  /**
   * Returns the label of format {@code [countryName] ([nameCode]) - +[callingCode]} (e.g. {@code
   * Switzerland (CH) - +41}) for the param {@code nameCode}.
   *
   * @param nameCode String in format of a name code (e.g. {@code CH})
   * @return String label of format {@code [countryName] ([nameCode]) - +[callingCode]} (e.g. {@code
   *     Switzerland (CH) - +41})
   */
  private String getCountryLabelForNameCode(String nameCode) {
    Locale locale = new Locale("en", nameCode);
    String countryName = locale.getDisplayCountry();
    int callingCode =
        PhoneNumberUtil.getInstance().getCountryCodeForRegion(nameCode.toUpperCase(Locale.ROOT));

    return countryName + " (" + nameCode.toUpperCase(Locale.ROOT) + ") - +" + callingCode;
  }

  /**
   * Populates {@link CountryDropdown#countryLabelSorted} with the ascending sorted keys of {@link
   * CountryDropdown#countryLabelMapNameCode} if not populated yet. Then sets an {@link
   * ArrayAdapter} with {@link CountryDropdown#countryLabelSorted} for the dropdown to show the
   * list.
   */
  private void setAdapter() {
    if (countryLabelSorted.isEmpty()) {
      countryLabelSorted.addAll(countryLabelMapNameCode.keySet());
      Collections.sort(countryLabelSorted);
    }

    ArrayAdapter<String> arrayAdapter =
        new ArrayAdapter<>(getContext(), R.layout.country_dropdown_item, countryLabelSorted);
    inputEditText.setAdapter(arrayAdapter);
  }

  /**
   * Returns whether the current input is a valid dropdown option. Also updates the input error
   * accordingly.
   *
   * @return boolean whether the current input is a valid dropdown option
   */
  public boolean validateInput() {
    if (!setNameCodeForInput()) {
      enableInputError();
      return false;
    }

    disableInputError();
    return true;
  }

  /**
   * Sets the {@link CountryDropdown#nameCode} to the name code of the current input if that's a
   * valid dropdown option. Else set's it to an empty String.
   *
   * @return boolean whether the current input is a valid dropdown option
   */
  private boolean setNameCodeForInput() {
    String nameCodeForInput = countryLabelMapNameCode.get(getInput());
    if (nameCodeForInput == null) {
      nameCode = "";
      return false;
    }

    nameCode = nameCodeForInput;
    return true;
  }

  /** Shows the error message on the input component. */
  private void enableInputError() {
    input.setErrorEnabled(true);
    input.setError(getResources().getString(R.string.main_activity_country_dropdown_error));
  }

  /** Hides the error message on the input component. */
  private void disableInputError() {
    input.setError(null);
    input.setErrorEnabled(false);
  }

  private String getInput() {
    return inputEditText.getText().toString();
  }

  /**
   * Returns the name code of the current input if it's a valid dropdown option, else returns an
   * empty String.
   *
   * @return String name code of the current input if it's a valid dropdown option, else returns an
   *     empty String
   */
  public String getNameCodeForInput() {
    setNameCodeForInput();
    return nameCode;
  }

  /**
   * Sets the label of the country with the name code param {@code nameCode} on the input if it's
   * valid. Else the input is not changed.
   *
   * @param nameCode String in format of a name code (e.g. {@code CH})
   */
  public void setInputForNameCode(String nameCode) {
    String countryLabel = getCountryLabelForNameCode(nameCode);
    if (!countryLabelSorted.contains(countryLabel)) {
      return;
    }

    inputEditText.setText(countryLabel);
    validateInput();
  }

  @Override
  public void setEnabled(boolean enabled) {
    input.setEnabled(enabled);
  }
}
