/**
 * 
 */
package ch.gpschase.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

/**
 * Activity to allow the user to change settings
 */
public class SettingsActivity extends Activity {

	// tag for preferences fragment
	private static final String FRAGMENT_TAG = "prefs";

	/**
	 * 
	 */
	public static class SettingsFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);
			
			// add the preference for the map type to category View
			Context context = getActivity();
			PreferenceCategory category = (PreferenceCategory)findPreference(getString(R.string.pref_cat_view_key));
			category.addPreference(TrailMapFragment.getMapTypePreference(context));
			
			// add a handler for the Cleanup button
			Preference button = findPreference(getString(R.string.pref_cleanup_key));
			button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					// cleanup data
					App.getImageManager().cleanupFiles();
					return true;
				}
			});
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load layout
		setContentView(R.layout.activity_settings);
		
		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_settings);
		
		// add fragment (if not already recreated)
		if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
			getFragmentManager().beginTransaction().replace(R.id.layout_container, new SettingsFragment(), FRAGMENT_TAG).commit();
		}
	}

}