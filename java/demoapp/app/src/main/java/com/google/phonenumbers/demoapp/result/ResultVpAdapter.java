package com.google.phonenumbers.demoapp.result;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;

/** Adapter for the {@link androidx.viewpager2.widget.ViewPager2} used in {@link ResultActivity}. */
class ResultVpAdapter extends FragmentStateAdapter {

  private final ArrayList<Fragment> fragments;
  private final ArrayList<String> titles;

  /**
   * Constructor to set predefined Fragments and their titles.
   *
   * @param fragmentManager of {@link ViewPager2}'s host
   * @param lifecycle of {@link ViewPager2}'s host
   * @param fragments ArrayList of predefined Fragments (in correct order)
   * @param titles ArrayList of titles of the predefined Fragments in param {@code fragments}
   *     (respectively for the Fragment at the same position in param {@code fragments}
   */
  public ResultVpAdapter(
      @NonNull FragmentManager fragmentManager,
      @NonNull Lifecycle lifecycle,
      ArrayList<Fragment> fragments,
      ArrayList<String> titles) {
    super(fragmentManager, lifecycle);
    this.fragments = fragments;
    this.titles = titles;
  }

  /**
   * Returns the predefined Fragment (set with constructor) at position param {@code position}.
   * Returns a new Fragment if no predefined Fragment exists at position.
   *
   * @param position int position of the predefined Fragment
   * @return Fragment at position param {@code position} or new Fragment if no predefined Fragment
   *     exists at position
   */
  @NonNull
  @Override
  public Fragment createFragment(int position) {
    if (position >= 0 && position < getItemCount()) {
      return fragments.get(position);
    }
    return new Fragment();
  }

  @Override
  public int getItemCount() {
    return fragments.size();
  }

  /**
   * Returns the predefined title (set with constructor) at position param {@code position}. Returns
   * an empty String if no predefined Fragment exists at position.
   *
   * @param position int position of the predefined title
   * @return String title at position param {@code position} or empty String if no predefined title
   *     exists at position
   */
  public String getTitle(int position) {
    if (position >= 0 && position < titles.size()) {
      return titles.get(position);
    }
    return "";
  }
}
