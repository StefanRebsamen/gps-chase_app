package ch.gpschase.app;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.TrailActions;
import ch.gpschase.app.util.TrailMapFragment;

/**
 * Activity to show information about a trail
 */
public class TrailInfoActivity extends Activity {
	
	/**
	 * Listener for a tab
	 */
	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {

		private final Fragment fragment;
		private final String tag;

		public TabListener(Fragment fragment, String tag) {
			this.fragment = fragment;
			this.tag = tag;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment.isDetached()) {
				ft.attach(fragment);
			} else if (!fragment.isAdded()) {
				ft.add(R.id.content_frame, fragment, tag);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.detach(fragment);
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

		
	/**
	 * 
	 */
	public static class OverviewFragment extends Fragment {
		
		// UI elements
		private EditText editTextName;
		private EditText editTextDescr;
		private TextView textViewNoCp;
		private TextView textViewDistance;
		private TextView textViewUpdated;
		private TextView textViewUploaded;
		private TextView textViewDownloaded;
		
		private Trail trail;
				
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
			// create view and get references to UI elements
			View view = inflater.inflate(R.layout.activity_trail_info_overview, null);
			editTextName = (EditText)view.findViewById(R.id.editText_name);
			editTextDescr = (EditText)view.findViewById(R.id.editText_description);
			textViewNoCp = (TextView)view.findViewById(R.id.textView_no_checkpoints_value);
			textViewDistance = (TextView)view.findViewById(R.id.textView_distance_value);
			textViewUpdated = (TextView)view.findViewById(R.id.textView_updated);
			textViewUploaded = (TextView)view.findViewById(R.id.textView_uploaded);
			textViewDownloaded = (TextView)view.findViewById(R.id.textView_downloaded);
			
			// load stuff into UI
			editTextName.setText(trail.name);
			editTextDescr.setText(trail.description);
			textViewUpdated.setText(App.formatDateTime(trail.updated));
			textViewUploaded.setText(App.formatDateTime(trail.uploaded));
			textViewDownloaded.setText(App.formatDateTime(trail.downloaded));
			
			// lock if it's not editable
			editTextName.setEnabled(trail.isEditable());;
			editTextDescr.setEnabled(trail.isEditable());;
			
			// go through checkpoints and calculate info 
			int cpCount = 0;
			float distance = 0.0f;
			Checkpoint previousCp = null;
			for (Checkpoint cp : trail.getCheckpoints()) {
				cpCount++;
				if (previousCp != null) {
					distance += cp.location.distanceTo(previousCp.location);
				}
				previousCp = cp;			
			}						
			textViewNoCp.setText(Integer.valueOf(cpCount).toString());		
			textViewDistance.setText(Integer.valueOf(Math.round(distance)).toString() +  "m");
			
			return view;
		}
		
		@Override
		public void onPause() {
			super.onPause();
			
			// save if something really changed
			if (trail != null && trail.isEditable()) {			
				String name = editTextName.getText().toString();
				String descr = editTextDescr.getText().toString();
				
				boolean changed = false;
				if (!name.equals(trail.name)) {
					changed = true;
					trail.name = name;
				}
				if (!descr.equals(trail.description)) {
					changed = true;
					trail.description = descr;
				}
				if (changed) {
					trail.save(getActivity());
				}			
			}
		}
		
		public void setTrail(Trail trail) {
			this.trail = trail;
		}
	}
	
	
	/**
	 * 
	 */
	public static class MapFragment extends TrailMapFragment {
		
		private Trail trail;
		
		@Override
		public void onStart() {
			super.onStart();
			
			// clear everything
			getMap().clear();
			
			// nothing to do without trail
			if (trail == null) {
				return;				
			}			
			
			drawTrail();						
		}

		/**
		 * 
		 */
		private void drawTrail() {
			
			// show region on map
			LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
			List<LatLng> locations = new ArrayList<LatLng>(); 
			for (Checkpoint checkpoint : trail.getCheckpoints()) {
				// show start
				if (locations.size() == 0) {
					addCheckpoint(checkpoint, false, true);
				}
				LatLng location = new LatLng(checkpoint.location.getLatitude(), checkpoint.location.getLongitude());
				locations.add(location);
				boundsBuilder.include(location);				
			}

			// several checkpoints?
			if (locations.size() > 1) {
				// draw a rectangle to show region
				LatLngBounds bounds =  boundsBuilder.build();
				PolygonOptions rectOptions = new PolygonOptions();
				rectOptions.add(bounds.northeast);
				rectOptions.add(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude));
				rectOptions.add(bounds.southwest);		
				rectOptions.add(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude));
				rectOptions.fillColor((getResources().getColor(R.color.green_light) & 0x00FFFFFF) | 0x44000000);
				rectOptions.strokeColor(Color.TRANSPARENT);
				getMap().addPolygon(rectOptions).setZIndex(-1);
				// place camera to include the call
				setCamera(locations);				
			}
			// just one?
			else if (locations.size() == 1) {
				// place camera to this one
				setCameraTarget(locations.get(1));
				setCameraZoom(DEFAULT_ZOOM);
			}
			else {
				// nothing to show!
			}
		}
		
		public void setTrail(Trail trail) {
			this.trail = trail;
		}
		
	}
	
	
	// name to identify trail id passed as extra in intent 
	public static final String INTENT_EXTRA_TRAILID = "trailId";
	
	// name of the fragments
	private static final String FRAGMENT_TAG_OVERVIEW = "overview";
	private static final String FRAGMENT_TAG_MAP = "map";
	
	// fragments
	private OverviewFragment overviewFragment;
	private MapFragment mapFragment;
	
	// the trail it's all about
	Trail trail = null;
		
	/**
	 * Open the activity for the specified trail
	 * @param trail
	 */
	public static void show(Context context, Trail trail) {
		// switch to chase activity
		Intent intent = new Intent(context, TrailInfoActivity.class);
		intent.putExtra(INTENT_EXTRA_TRAILID, trail.getId());
		context.startActivity(intent);
	}
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load trail including its checkpoints
		long trailId = getIntent().getLongExtra(INTENT_EXTRA_TRAILID, 0);		
		trail = Trail.load(this, trailId);
		trail.loadCheckpoints(this);
		
		// load layout
		setContentView(R.layout.activity_trail_info);

		// create fragments (if not recreated by the system)	
		overviewFragment = (OverviewFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_OVERVIEW);
		if (overviewFragment == null) {
			overviewFragment = new OverviewFragment();			
		}		
		mapFragment = (MapFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
		if (mapFragment == null) {
			mapFragment = new MapFragment();
		}
		// pass trail to them
		overviewFragment.setTrail(trail);
		mapFragment.setTrail(trail);
		
		// setup action bar
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);
		getActionBar().setTitle(R.string.activity_title_trail_info);
		getActionBar().setSubtitle(trail.name);
		
		// create tabs
		Tab overiviewTab = actionBar.newTab()		//
				.setText(R.string.tab_overview)	//
				.setTag(overviewFragment);				//
		overiviewTab.setTabListener(new TabListener<MyTrailsFragment>(overviewFragment, FRAGMENT_TAG_OVERVIEW));
		actionBar.addTab(overiviewTab);

		Tab mapTab = actionBar.newTab()				//
				.setText(R.string.tab_map)	//
				.setTag(mapFragment);						//
		mapTab.setTabListener(new TabListener<CloudTrailsFragment>(mapFragment, FRAGMENT_TAG_MAP));
		actionBar.addTab(mapTab);
			
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_trail_info, menu);
		return true;
	}	
		
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// modify menu
		MenuItem menuEdit = menu.findItem(R.id.action_edit_trail);
		if (menuEdit != null) {
			menuEdit.setVisible(trail.isEditable());
		}
		MenuItem menuShare = menu.findItem(R.id.action_share_trail);
		if (menuShare != null) {
			menuShare.setVisible(!trail.isDownloaded());
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_chase_trail:
			// chase trail
			TrailActions.chaseTrail(this, trail);
			return true;

		case R.id.action_edit_trail:
			// edit trail
			EditTrailActivity.show(this, trail);
			return true;

		case R.id.action_share_trail:
			// upload and share trail
			TrailActions.shareTrail(this, trail);
			return true;

		}
		return false;
	}		
	
}
