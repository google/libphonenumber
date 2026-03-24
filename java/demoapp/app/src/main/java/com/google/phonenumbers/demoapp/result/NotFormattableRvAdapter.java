package com.google.phonenumbers.demoapp.result;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.phonenumbers.demoapp.R;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import java.util.ArrayList;

/** Adapter for the {@link RecyclerView} used in {@link NotFormattableFragment}. */
public class NotFormattableRvAdapter
    extends RecyclerView.Adapter<NotFormattableRvAdapter.ViewHolder> {

  private final LayoutInflater layoutInflater;

  /** List of the original version of {@link PhoneNumberInApp}s at the time of object creation. */
  private final ArrayList<PhoneNumberInApp> originalPhoneNumbers;

  public NotFormattableRvAdapter(ArrayList<PhoneNumberInApp> phoneNumbers, Context context) {
    this.originalPhoneNumbers = phoneNumbers;
    this.layoutInflater = LayoutInflater.from(context);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = layoutInflater.inflate(R.layout.not_formattable_list_item, parent, false);
    return new ViewHolder(view);
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
   * {@link RecyclerView.ViewHolder} specifically for a list item of a not formattable phone number.
   */
  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final TextView tvContactName;
    private final TextView tvReason;
    private final TextView tvOriginalPhoneNumber;

    public ViewHolder(View view) {
      super(view);
      tvContactName = view.findViewById(R.id.tv_contact_name);
      tvReason = view.findViewById(R.id.tv_reason);
      tvOriginalPhoneNumber = view.findViewById(R.id.tv_original_phone_number);
    }

    /**
     * Sets the content of the view to the information of param {@code
     * phoneNumberInAppRepresentation}.
     *
     * @param phoneNumberInAppRepresentation PhoneNumberInApp to set content of the view from
     */
    public void setFromPhoneNumberInAppRepresentation(
        PhoneNumberInApp phoneNumberInAppRepresentation) {
      tvContactName.setText(phoneNumberInAppRepresentation.getContactName());

      switch (phoneNumberInAppRepresentation.getFormattingState()) {
        case PARSING_ERROR:
          tvReason.setText(R.string.not_formattable_parsing_error_text);
          break;
        case NUMBER_IS_SHORT_NUMBER:
          tvReason.setText(R.string.not_formattable_short_number_text);
          break;
        case NUMBER_IS_ALREADY_IN_E164:
          tvReason.setText(R.string.not_formattable_already_e164_text);
          break;
        case NUMBER_IS_NOT_VALID:
          tvReason.setText(R.string.not_formattable_invalid_number_text);
          break;
        default:
          tvReason.setText(R.string.not_formattable_unknown_error_text);
          break;
      }

      tvOriginalPhoneNumber.setText(phoneNumberInAppRepresentation.getOriginalPhoneNumber());
    }
  }
}
