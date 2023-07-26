package com.google.phonenumbers.demoapp.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import java.util.ArrayList;
import java.util.Collections;

/** Handles everything related to reading the device contacts. */
public class ContactsRead {

  private ContactsRead() {}

  /**
   * Reads all phone numbers in the device's contacts and return them as a list of {@link
   * PhoneNumberInApp}s ascending sorted by the contact name. An empty list is also returned if the
   * app has no permission to read contacts or an error occurred while doing so
   *
   * @param context Context of the app
   * @return ArrayList of all phone numbers in the device's contacts, also empty if the app has no
   *     permission to read contacts or an error occurred while doing so
   */
  public static ArrayList<PhoneNumberInApp> getAllPhoneNumbersSorted(Context context) {
    ArrayList<PhoneNumberInApp> phoneNumbers = new ArrayList<>();

    if (!ContactsPermissionManagement.isGranted(context)) {
      return phoneNumbers;
    }

    ContentResolver cr = context.getContentResolver();
    // Only query for contacts with phone number(s).
    Cursor cursor =
        cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
    // If query doesn't work as intended.
    if (cursor == null) {
      return phoneNumbers;
    }

    while (cursor.moveToNext()) {
      // ID to identify the phone number entry in the contacts (can be used to update in contacts).
      int idIndex = cursor.getColumnIndex(Phone._ID);
      String id = idIndex != -1 ? cursor.getString(idIndex) : "";

      int contactNameIndex = cursor.getColumnIndex(Phone.DISPLAY_NAME);
      String contactName = contactNameIndex != -1 ? cursor.getString(contactNameIndex) : "";

      int originalPhoneNumberIndex = cursor.getColumnIndex(Phone.NUMBER);
      String originalPhoneNumber =
          originalPhoneNumberIndex != -1 ? cursor.getString(originalPhoneNumberIndex) : "";

      PhoneNumberInApp phoneNumberInApp =
          new PhoneNumberInApp(id, contactName, originalPhoneNumber);
      phoneNumbers.add(phoneNumberInApp);
    }
    cursor.close();
    Collections.sort(phoneNumbers);
    return phoneNumbers;
  }
}
