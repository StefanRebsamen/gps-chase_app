package ch.gpschase.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import ch.gpschase.app.util.TrailMapFragment;

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

			PreferenceCategory category;
			
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);
			
			// add the preference for the map type to category View
			category = (PreferenceCategory)findPreference(getString(R.string.pref_cat_view_key));
			category.addPreference(TrailMapFragment.getMapTypePreference(getActivity()));

			// add the preference for the update frequency
			category = (PreferenceCategory)findPreference(getString(R.string.pref_cat_chase_key));
			category.addPreference(ChaseTrailActivity.getUpdateFrequencyPreference(getActivity()));
			
			// add a handler for the Cleanup button
			Preference button = findPreference(getString(R.string.pref_cleanup_key));
			button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					// cleanup data
					App.getImageManager().cleanupFiles();
					
					Toast.makeText(getActivity(), R.string.pref_cleanup_done, Toast.LENGTH_SHORT).show();
					
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

	
	/**
	 * Opens the activity
	 */
	public static void show(Context context) {
		Intent intent = new Intent(context, SettingsActivity.class);
		context.startActivity(intent);
	}		
}