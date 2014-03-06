package ch.gpschase.app;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import ch.gpschase.app.util.TrailMapFragment;

/**
 * 
 */
public class SettingsFragment extends PreferenceFragment {

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
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		
	    View view = super.onCreateView(inflater, container, savedInstanceState);
	    // we want no padding
	    if(view != null) {
	        ListView lv = (ListView) view.findViewById(android.R.id.list);
	        lv.setPadding(0, 0, 0, 0);
	    }
	    // set background
		view.setBackgroundResource(R.drawable.bg_item);
		return view;
	}	
}