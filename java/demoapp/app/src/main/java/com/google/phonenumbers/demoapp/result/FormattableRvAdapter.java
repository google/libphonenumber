package com.google.phonenumbers.demoapp.result;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.google.phonenumbers.demoapp.R;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import java.util.ArrayList;

/** Adapter for the {@link RecyclerView} used in {@link FormattableFragment}. */
public class FormattableRvAdapter extends RecyclerView.Adapter<FormattableRvAdapter.ViewHolder> {

  private final LayoutInflater layoutInflater;

  /** List of the original version of {@link PhoneNumberInApp}s at the time of object creation. */
  private final ArrayList<PhoneNumberInApp> originalPhoneNumbers;

  /** List of all created {@link ViewHolder}s. */
  private final ArrayList<ViewHolder> viewHolders = new ArrayList<>();

  public FormattableRvAdapter(ArrayList<PhoneNumberInApp> phoneNumbers, Context context) {
    this.originalPhoneNumbers = phoneNumbers;
    this.layoutInflater = LayoutInflater.from(context);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = layoutInflater.inflate(R.layout.formattable_list_item, parent, false);
    ViewHolder viewHolder = new ViewHolder(view);
    viewHolders.add(viewHolder);
    return viewHolder;
  }

  @Override
  public void onViewRecycled(@NonNull ViewHolder holder) {
    super.onViewRecycled(holder);
    viewHolders.remove(holder);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
    if (position >= 0 && position < getItemCount()) {
      viewHolder.setFromPhoneNumberInAppRepresentation(originalPhoneNumbers.get(position));
    }
  }

  @Override
  public int getItemCount() {
    return originalPhoneNumbers.size();
  }

  /**
   * Sets the enabled state for the checkbox of all list items.
   *
   * @param enabled boolean enable state to set
   */
  public void setAllEnabled(boolean enabled) {
    for (ViewHolder viewHolder : viewHolders) {
      viewHolder.setEnabled(enabled);
    }
  }

  /**
   * Returns a list of all list items as {@link PhoneNumberInApp}s in the current state of the UI.
   *
   * @return ArrayList of all list items as {@link PhoneNumberInApp}s in the current state of the UI
   */
  public ArrayList<PhoneNumberInApp> getAllPhoneNumbers() {
    ArrayList<PhoneNumberInApp> phoneNumbers = new ArrayList<>();
    for (ViewHolder viewHolder : viewHolders) {
      phoneNumbers.add(viewHolder.getPhoneNumberInAppRepresentation());
    }
    return phoneNumbers;
  }

  /** {@link RecyclerView.ViewHolder} specifically for a list item of a formattable phone number. */
  public static class ViewHolder extends RecyclerView.ViewHolder {

    /** Representation of the UI as a {@link PhoneNumberInApp}. */
    private PhoneNumberInApp phoneNumberInAppRepresentation;

    private final TextView tvContactName;
    private final TextView tvOriginalPhoneNumber;
    private final TextView tvArrow;
    private final TextView tvFormattedPhoneNumber;

    private final CheckBox checkBox;

    public ViewHolder(View view) {
      super(view);
      ConstraintLayout clListItem = view.findViewById(R.id.cl_list_item);
      clListItem.setOnClickListener(v -> toggleChecked());

      tvContactName = view.findViewById(R.id.tv_contact_name);
      tvOriginalPhoneNumber = view.findViewById(R.id.tv_original_phone_number);
      tvArrow = view.findViewById(R.id.tv_arrow);
      tvFormattedPhoneNumber = view.findViewById(R.id.tv_formatted_phone_number);
      checkBox = view.findViewById(R.id.check_box);

      checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateUiToMatchCheckBox());
    }

    /**
     * Sets the content of the view to the information of param {@code
     * phoneNumberInAppRepresentation}.
     *
     * @param phoneNumberInAppRepresentation PhoneNumberInApp to set content of the view from
     */
    public void setFromPhoneNumberInAppRepresentation(
        PhoneNumberInApp phoneNumberInAppRepresentation) {
      this.phoneNumberInAppRepresentation = phoneNumberInAppRepresentation;
      tvContactName.setText(phoneNumberInAppRepresentation.getContactName());
      tvOriginalPhoneNumber.setText(phoneNumberInAppRepresentation.getOriginalPhoneNumber());
      String formattedPhoneNumber = phoneNumberInAppRepresentation.getFormattedPhoneNumber();
      tvFormattedPhoneNumber.setText(formattedPhoneNumber != null ? formattedPhoneNumber : "");
      checkBox.setChecked(phoneNumberInAppRepresentation.shouldContactBeUpdated());
    }

    /** Toggles the checked state of the {@link ViewHolder#checkBox} if it is enabled. */
    private void toggleChecked() {
      if (checkBox.isEnabled()) {
        checkBox.toggle();
        phoneNumberInAppRepresentation.setShouldContactBeUpdated(checkBox.isChecked());
      }
    }

    /**
     * Update the rest of the UI elements to represent the checked state of {@link
     * ViewHolder#checkBox} correctly.
     */
    private void updateUiToMatchCheckBox() {
      boolean isChecked = checkBox.isChecked();
      tvArrow.setEnabled(isChecked);
      tvFormattedPhoneNumber.setEnabled(isChecked);
    }

    /**
     * Sets the enabled state of the {@link ViewHolder#checkBox}.
     *
     * @param enabled boolean whether the {@link ViewHolder#checkBox} should be enabled
     */
    public void setEnabled(boolean enabled) {
      checkBox.setEnabled(enabled);
    }

    public PhoneNumberInApp getPhoneNumberInAppRepresentation() {
      return phoneNumberInAppRepresentation;
    }
  }
}
