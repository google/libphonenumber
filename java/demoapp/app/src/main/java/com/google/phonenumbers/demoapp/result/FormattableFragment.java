package com.google.phonenumbers.demoapp.result;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.phonenumbers.demoapp.R;
import com.google.phonenumbers.demoapp.contacts.ContactsWrite;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import java.util.ArrayList;

/**
 * Used to handle and process interactions from/with the "Formattable" results section in the result
 * page UI of the app.
 */
public class FormattableFragment extends Fragment {

  /** The fragment root view. */
  private View root;
  /** The RecyclerView containing the list. */
  private RecyclerView recyclerView;

  private Button btnUpdateSelected;

  /** The sorted phone numbers the list currently contains. */
  private ArrayList<PhoneNumberInApp> phoneNumbers;

  public FormattableFragment(ArrayList<PhoneNumberInApp> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    root = inflater.inflate(R.layout.fragment_formattable, container, false);
    recyclerView = root.findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));

    btnUpdateSelected = root.findViewById(R.id.btn_update_selected);
    btnUpdateSelected.setOnClickListener(v -> btnUpdateSelectedClicked());

    reloadList();
    return root;
  }

  /**
   * Attempts to update the selected contacts and shows success or error based on the outcome.
   * Called when the update selected button is clicked.
   */
  private void btnUpdateSelectedClicked() {
    updateUiState(UiState.PROCESSING);

    // Get the most up to date list of phone numbers from the RecyclerView adapter.
    if (recyclerView.getAdapter() == null) {
      showErrorSnackbar();
      updateUiState(UiState.SELECT_PHONE_NUMBERS);
      return;
    }
    phoneNumbers = ((FormattableRvAdapter) recyclerView.getAdapter()).getAllPhoneNumbers();

    // Create a sublist with all phone numbers that have the checkbox checked.
    ArrayList<PhoneNumberInApp> phoneNumbersToUpdate = new ArrayList<>();
    for (PhoneNumberInApp phoneNumber : phoneNumbers) {
      if (phoneNumber.shouldContactBeUpdated()) {
        phoneNumbersToUpdate.add(phoneNumber);
      }
    }

    if (phoneNumbersToUpdate.isEmpty()) {
      showNoNumbersSelectedSnackbar();
      updateUiState(UiState.SELECT_PHONE_NUMBERS);
      return;
    }

    boolean errorWhileUpdatingPhoneNumbers =
        !ContactsWrite.updatePhoneNumbers(phoneNumbersToUpdate, root.getContext());
    if (errorWhileUpdatingPhoneNumbers) {
      showErrorSnackbar();
      updateUiState(UiState.SELECT_PHONE_NUMBERS);
    } else {
      showContactsWriteSuccessSnackbar();
      phoneNumbers.removeAll(phoneNumbersToUpdate);
      reloadList();
    }
  }

  /** Shows a Snackbar informing that no numbers are selected. */
  private void showNoNumbersSelectedSnackbar() {
    Snackbar.make(root, R.string.formattable_no_numbers_selected_text, Snackbar.LENGTH_LONG).show();
  }

  /** Shows a Snackbar informing that the selected contacts were successfully written. */
  private void showContactsWriteSuccessSnackbar() {
    Snackbar.make(root, R.string.formattable_contacts_write_success_text, Snackbar.LENGTH_LONG)
        .show();
  }

  /** Shows a Snackbar informing that there was an error (and the user should try again). */
  private void showErrorSnackbar() {
    Snackbar.make(root, R.string.formattable_error_text, Snackbar.LENGTH_LONG).show();
  }

  /**
   * Reloads the UI so the list contains the phone numbers currently in {@link
   * FormattableFragment#phoneNumbers}.
   */
  private void reloadList() {
    FormattableRvAdapter adapter = new FormattableRvAdapter(phoneNumbers, root.getContext());
    recyclerView.setAdapter(adapter);
    updateUiState(
        phoneNumbers.isEmpty() ? UiState.NO_PHONE_NUMBERS_IN_LIST : UiState.SELECT_PHONE_NUMBERS);
  }

  /**
   * Updates the UI to represent the param {@code uiState}.
   *
   * @param uiState State the UI should be changed to
   */
  private void updateUiState(UiState uiState) {
    // Specifically: btnUpdateSelected, and all CheckBoxes (of the list items)
    boolean mainInteractionsEnabled = false;

    switch (uiState) {
      case SELECT_PHONE_NUMBERS:
      default:
        mainInteractionsEnabled = true;
        btnUpdateSelected.setText(R.string.formattable_update_selected_text_default);
        break;
      case PROCESSING:
        btnUpdateSelected.setText(R.string.formattable_update_selected_text_processing);
        break;
      case NO_PHONE_NUMBERS_IN_LIST:
        btnUpdateSelected.setText(R.string.formattable_update_selected_text_default);
        break;
    }

    btnUpdateSelected.setEnabled(mainInteractionsEnabled);

    if (recyclerView.getAdapter() != null) {
      ((FormattableRvAdapter) recyclerView.getAdapter()).setAllEnabled(mainInteractionsEnabled);
    }
  }

  /** Represents the different states the UI of this fragment can become. */
  enum UiState {
    /** The user should select the phone numbers to update. */
    SELECT_PHONE_NUMBERS,
    /** Used when loading or processing. The UI is disabled for the user during this time. */
    PROCESSING,
    /**
     * There are no phone number sin the list (the list is empty). Therefore the update selected
     * button is disabled.
     */
    NO_PHONE_NUMBERS_IN_LIST
  }
}
