package com.google.phonenumbers.demoapp.result;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.phonenumbers.demoapp.R;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp.FormattingState;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Used to handle and process interactions from/with the "Not formattable" results section in the
 * result page UI of the app.
 */
public class NotFormattableFragment extends Fragment {

  /** The fragment root view. */
  private View root;
  /** The RecyclerView containing the list. */
  private RecyclerView recyclerView;

  /**
   * The sorted phone numbers the list contains (some might not be visible in the UI due to the
   * {@link NotFormattableFragment#appliedFilters}).
   */
  private final ArrayList<PhoneNumberInApp> phoneNumbers;

  /** The filters that are currently applied to the list. */
  private final ArrayList<FormattingState> appliedFilters = new ArrayList<>();

  public NotFormattableFragment(ArrayList<PhoneNumberInApp> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    root = inflater.inflate(R.layout.fragment_not_formattable, container, false);
    recyclerView = root.findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));

    Chip chipParsingError = root.findViewById(R.id.chip_parsing_error);
    connectChipToFormattingState(chipParsingError, FormattingState.PARSING_ERROR);
    Chip chipShortNumber = root.findViewById(R.id.chip_short_number);
    connectChipToFormattingState(chipShortNumber, FormattingState.NUMBER_IS_SHORT_NUMBER);
    Chip chipAlreadyE164 = root.findViewById(R.id.chip_already_e164);
    connectChipToFormattingState(chipAlreadyE164, FormattingState.NUMBER_IS_ALREADY_IN_E164);
    Chip chipInvalidNumber = root.findViewById(R.id.chip_invalid_number);
    connectChipToFormattingState(chipInvalidNumber, FormattingState.NUMBER_IS_NOT_VALID);

    // Add add filters as they are all preselected in the UI
    appliedFilters.addAll(
        Arrays.asList(
            FormattingState.PARSING_ERROR,
            FormattingState.NUMBER_IS_SHORT_NUMBER,
            FormattingState.NUMBER_IS_ALREADY_IN_E164,
            FormattingState.NUMBER_IS_NOT_VALID));
    // List only needs to be loaded if there are phone numbers.
    if (!phoneNumbers.isEmpty()) {
      reloadListWithFilters();
    }
    return root;
  }

  /**
   * Sets up the param {@code chip} to add/remove the param {@code formattingState} from the {@link
   * NotFormattableFragment#appliedFilters} list when it is checked/unchecked, and then reloads the
   * phone number list.
   *
   * @param chip Chip of which to handle check/uncheck action
   * @param formattingState FormattingState the param {@code chip} represents
   */
  private void connectChipToFormattingState(Chip chip, FormattingState formattingState) {
    chip.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            appliedFilters.add(formattingState);
          } else {
            appliedFilters.remove(formattingState);
          }
          reloadListWithFilters();
        });
  }

  /**
   * Reloads the UI so the list contains the phone numbers matching the currently {@link
   * NotFormattableFragment#appliedFilters}.
   */
  private void reloadListWithFilters() {
    ArrayList<PhoneNumberInApp> phoneNumbersToShow = new ArrayList<>();
    for (PhoneNumberInApp phoneNumber : phoneNumbers) {
      if (appliedFilters.contains(phoneNumber.getFormattingState())) {
        phoneNumbersToShow.add(phoneNumber);
      }
    }

    if (phoneNumbersToShow.isEmpty()) {
      showNoNumbersMatchFiltersSnackbar();
    }

    NotFormattableRvAdapter adapter =
        new NotFormattableRvAdapter(phoneNumbersToShow, root.getContext());
    recyclerView.setAdapter(adapter);
  }

  /** Shows a Snackbar informing that no numbers match the selected filters. */
  private void showNoNumbersMatchFiltersSnackbar() {
    Snackbar.make(
            root, R.string.not_formattable_no_numbers_match_filters_text, Snackbar.LENGTH_LONG)
        .show();
  }
}
