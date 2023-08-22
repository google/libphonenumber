package com.google.phonenumbers.demoapp.result;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.phonenumbers.demoapp.R;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp.FormattingState;
import java.util.ArrayList;

/** Used to handle and process interactions from/with the result page UI of the app. */
public class ResultActivity extends AppCompatActivity {

  public static final String PHONE_NUMBERS_SORTED_SERIALIZABLE_EXTRA_KEY =
      "PHONE_NUMBERS_SORTED_SERIALIZABLE_EXTRA";

  @Override
  @SuppressWarnings("unchecked")
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_result);

    // Setup ActionBar (title, and home button).
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setTitle(R.string.app_name_long);
      actionBar.setHomeAsUpIndicator(R.drawable.ic_outline_home_30);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    ArrayList<PhoneNumberInApp> phoneNumbersFormattableSorted = new ArrayList<>();
    ArrayList<PhoneNumberInApp> phoneNumbersNotFormattableSorted = new ArrayList<>();
    try {
      ArrayList<PhoneNumberInApp> phoneNumbersSorted =
          (ArrayList<PhoneNumberInApp>)
              getIntent().getSerializableExtra(PHONE_NUMBERS_SORTED_SERIALIZABLE_EXTRA_KEY);
      // Split phoneNumbersSorted into two separate lists.
      for (PhoneNumberInApp phoneNumber : phoneNumbersSorted) {
        if (phoneNumber.getFormattingState() == FormattingState.COMPLETED) {
          phoneNumbersFormattableSorted.add(phoneNumber);
        } else if (phoneNumber.getFormattingState() != FormattingState.PENDING) {
          phoneNumbersNotFormattableSorted.add(phoneNumber);
        }
      }
    } catch (ClassCastException exception) {
      this.finish();
    }

    // Create two Fragments with each one of the split lists.
    FormattableFragment formattableFragment =
        new FormattableFragment(phoneNumbersFormattableSorted);
    NotFormattableFragment notFormattableFragment =
        new NotFormattableFragment(phoneNumbersNotFormattableSorted);
    setUpTapLayout(formattableFragment, notFormattableFragment);
  }

  /**
   * Sets up the {@link TabLayout} with the two param fragments.
   *
   * @param formattableFragment FormattableFragment for first tap
   * @param notFormattableFragment NotFormattableFragment for second tab
   */
  private void setUpTapLayout(
      FormattableFragment formattableFragment, NotFormattableFragment notFormattableFragment) {
    // The Fragments for the taps in correct order.
    ArrayList<Fragment> fragments = new ArrayList<>();
    // The titles for the tabs (respectively for the Fragment at the same position in fragments).
    ArrayList<String> fragmentTitles = new ArrayList<>();
    fragments.add(formattableFragment);
    fragmentTitles.add(getString(R.string.formattable_formattable_text));
    fragments.add(notFormattableFragment);
    fragmentTitles.add(getString(R.string.not_formattable_not_formattable_text));

    ResultVpAdapter vpAdapter =
        new ResultVpAdapter(getSupportFragmentManager(), getLifecycle(), fragments, fragmentTitles);
    ViewPager2 viewPager = findViewById(R.id.view_pager);
    viewPager.setAdapter(vpAdapter);

    new TabLayoutMediator(
            findViewById(R.id.tab_layout),
            viewPager,
            (tab, position) -> tab.setText(vpAdapter.getTitle(position)))
        .attach();
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    // If home button (house icon) in the ActionBar
    if (item.getItemId() == android.R.id.home) {
      this.finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
