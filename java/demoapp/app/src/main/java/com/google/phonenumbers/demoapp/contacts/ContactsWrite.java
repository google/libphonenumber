package com.google.phonenumbers.demoapp.contacts;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import java.util.ArrayList;

/** Handles everything related to writing the device contacts. */
public class ContactsWrite {

  private ContactsWrite() {}

  /**
   * Attempts to update all phone numbers in param {@code phoneNumbers} in the device's contacts.
   * {@link PhoneNumberInApp#shouldContactBeUpdated()} is not called in this method and should be
   * checked while creating the param {@code phoneNumbers}.
   *
   * @param phoneNumbers ArrayList of all phone numbers to update
   * @param context Context of the app
   * @return boolean whether operation was successful
   */
  public static boolean updatePhoneNumbers(
      ArrayList<PhoneNumberInApp> phoneNumbers, Context context) {
    if (!ContactsPermissionManagement.isGranted(context)) {
      return false;
    }

    // Create a list of operations to only have to apply one batch.
    ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();

    for (PhoneNumberInApp phoneNumber : phoneNumbers) {
      // Identify the exact phone number entry to update.
      String whereConditionBase = Phone._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
      String[] whereConditionParams =
          new String[] {
            phoneNumber.getId(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
          };

      contentProviderOperations.add(
          ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
              .withSelection(whereConditionBase, whereConditionParams)
              .withValue(Phone.NUMBER, phoneNumber.getFormattedPhoneNumber())
              .build());
    }

    try {
      context
          .getContentResolver()
          .applyBatch(ContactsContract.AUTHORITY, contentProviderOperations);
    } catch (OperationApplicationException | RemoteException e) {
      return false;
    }
    return true;
  }
}
