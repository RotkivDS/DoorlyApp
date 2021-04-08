package de.viktor.doorlyapp.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.viktor.doorlyapp.ApplicationProvider
import de.viktor.doorlyapp.R
import de.viktor.doorlyapp.ui.main.pages.DataListViewFragment
import de.viktor.doorlyapp.ui.main.pages.MainControlFragment
import de.viktor.doorlyapp.ui.main.pages.SettingsFragment

private val TAB_TITLES = arrayOf(
    R.string.tab_text_1,
    R.string.tab_text_2
)

private val TAB_TO_FRAGMENTS = mapOf(
    0 to { MainControlFragment.newInstance() },
    1 to { DataListViewFragment.newInstance() }
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return TAB_TO_FRAGMENTS[position]?.let { it() } ?: error("Ups")
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return TAB_TITLES.size
    }
}