package ch.gpschase.app;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import ch.gpschase.app.data.Chase;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.SelectableListFragment;
import ch.gpschase.app.util.TrailActions;
import ch.gpschase.app.util.TrailMapFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;

/**
 * Activity to show information about a trail
 */
public class TrailInfoActivity extends Activity {
	
	/**
	 * Listener for a tab
	 */
	public class TabListener<T extends Fragment> implements ActionBar.TabListener {

		private final Fragment fragment;
		private final String tag;
		private final int containerViewId;

		public TabListener(Fragment fragment, String tag, int containerViewId) {
			this.fragment = fragment;
			this.tag = tag;
			this.containerViewId = containerViewId;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment.isDetached()) {
				ft.attach(fragment);
			} else if (!fragment.isAdded()) {
				ft.add(containerViewId, fragment, tag);
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
		
		private Trail trail;				
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
			// create view 
			View view = inflater.inflate(R.layout.activity_trail_info_overview, null);
					
			//get references to UI elements
			editTextName = (EditText)view.findViewById(R.id.editText_name);
			editTextDescr = (EditText)view.findViewById(R.id.editText_description);
			TextView textViewName = (TextView)view.findViewById(R.id.textView_name);
			TextView textViewDescr = (TextView)view.findViewById(R.id.textView_description);
			TextView textViewNoCp = (TextView)view.findViewById(R.id.textView_no_checkpoints_value);
			TextView textViewLength = (TextView)view.findViewById(R.id.textView_length_value);
			TextView textViewUpdated = (TextView)view.findViewById(R.id.textView_updated);
			TextView textViewUploaded = (TextView)view.findViewById(R.id.textView_uploaded);
			TextView textViewDownloaded = (TextView)view.findViewById(R.id.textView_downloaded);
			
			// load stuff into UI
			if (trail.isEditable()) {
				editTextName.setText(trail.name);
				editTextDescr.setText(trail.description);
				editTextName.setVisibility(View.VISIBLE);
				editTextDescr.setVisibility(View.VISIBLE);
				textViewName.setVisibility(View.GONE);
				textViewDescr.setVisibility(View.GONE);
			}
			else {
				textViewName.setText(trail.name);
				textViewDescr.setText(trail.description);
				textViewName.setVisibility(View.VISIBLE);
				textViewDescr.setVisibility(View.VISIBLE);				
				editTextName.setVisibility(View.GONE);
				editTextDescr.setVisibility(View.GONE);
			}
			textViewUpdated.setText(App.formatDateTime(trail.updated));
			textViewUploaded.setText(App.formatDateTime(trail.uploaded));
			textViewDownloaded.setText(App.formatDateTime(trail.downloaded));
			
			// lock if it's not editable
			editTextName.setEnabled(trail.isEditable());;
			editTextDescr.setEnabled(trail.isEditable());;
			
			// go through checkpoints and calculate info 
			int cpCount = 0;
			float length = 0.0f;
			Checkpoint previousCp = null;
			for (Checkpoint cp : trail.getCheckpoints()) {
				cpCount++;
				if (previousCp != null) {
					length += cp.location.distanceTo(previousCp.location);
				}
				previousCp = cp;			
			}						
			textViewNoCp.setText(Integer.valueOf(cpCount).toString());		
			textViewLength.setText(Integer.valueOf(Math.round(length)).toString() +  " m");
			
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
	public static class AreaFragment extends TrailMapFragment {
		
		private Trail trail;
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

			View view = super.onCreateView(inflater, container, savedInstanceState);
						
			return view;
		}
		
		@Override
		public void onStart() {
			super.onStart();
			
			// clear everything
			getMap().clear();
									
			// nothing to do without trail
			if (trail == null) {
				return;				
			}			
			
			drawTrailArea();						
		}

		/**
		 * Draws the area of the trail (just the surrounding rectangle)
		 */
		private void drawTrailArea() {
			
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
				// draw a rectangle to show region. Add a bit overhead, so the start marker isn't right at the edge
				LatLngBounds bounds =  boundsBuilder.build();
				PolygonOptions rectOptions = new PolygonOptions();
				double latOverhead = Math.abs(bounds.northeast.latitude - bounds.southwest.latitude) * 0.05; 
				double lngOverhead = Math.abs(bounds.northeast.longitude - bounds.southwest.longitude) * 0.05;							
				rectOptions.add(new LatLng(bounds.northeast.latitude + latOverhead, bounds.northeast.longitude + lngOverhead));
				rectOptions.add(new LatLng(bounds.northeast.latitude + latOverhead, bounds.southwest.longitude - lngOverhead));
				rectOptions.add(new LatLng(bounds.southwest.latitude - latOverhead, bounds.southwest.longitude - lngOverhead));
				rectOptions.add(new LatLng(bounds.southwest.latitude - latOverhead, bounds.northeast.longitude + lngOverhead));
				rectOptions.fillColor((getResources().getColor(R.color.purple_very_light) & 0x00FFFFFF) | 0x44000000);
				rectOptions.strokeColor(getResources().getColor(R.color.purple_light));
				rectOptions.strokeWidth(2);
				getMap().addPolygon(rectOptions).setZIndex(-1);
				// place camera to include the call
				setCamera(locations);				
			}
			// just one?
			else if (locations.size() == 1) {
				// place camera to this one
				setCameraTarget(locations.get(0));
				setCameraZoom(DEFAULT_ZOOM);
			}
			else {
				// nothing to show!
			}
		}
		
		/**
		 * Sets the trail
		 * @param trail
		 */
		public void setTrail(Trail trail) {
			this.trail = trail;
		}
		
	}

	
	/**
	 * Fragment to display a list of chases
	 */
	public static class ChasesFragment extends SelectableListFragment<Chase> {

		private Trail trail;				
		
		/**
		 * 
		 */
		public ChasesFragment() {
			super(0, R.menu.cab_chase);
		}
	
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			// set empty text
			CharSequence emptText = getResources().getText(R.string.empty_text_chases);
			setEmptyText(emptText);
		}

		@Override
		protected List<Chase> loadInBackground() {
			
			trail.loadChases(getActivity());
			
			List<Chase> result = new LinkedList<Chase>();
			for (Chase ch : trail.getChases()) {
				result.add(ch);
			}			
			return result;
		}

		@Override
		protected View getItemView(Chase item, View convertView, ViewGroup parent) {
			// make sure we've got a view
			View view = convertView;
			if (view == null) {
				LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			     view = vi.inflate(R.layout.listrow_chase, null);			
			}
			
			// add item as tag
			view.setTag(item);

			// set texts
			((TextView) view.findViewById(R.id.textView_player)).setText(item.player);
			((TextView) view.findViewById(R.id.textView_started)).setText(App.formatDateTime(item.started));
			if (item.finished != 0) {
				((TextView) view.findViewById(R.id.textView_time)).setText(App.formatDuration(item.finished - item.started));
			} else {
				((TextView) view.findViewById(R.id.textView_time)).setText("");
			}
			
			return view;
		}

		@Override
		protected boolean onActionItemClicked(MenuItem item, int position, long id) {

			View view = getListView().getChildAt(position);
			if (view != null) {
				Chase chase = (Chase) view.getTag();

				switch (item.getItemId()) {

				case R.id.action_continue_chase:
					// continue chase
					ChaseTrailActivity.show(getActivity(), chase);
					return true;

				case R.id.action_delete_chase:
					// delete trail after asking user
					deleteChase(chase);
					return true;
				}
			}

			return false;
		}

		@Override
		public void onListItemClick(int position, long id) {
			View view = getListView().getChildAt(position);
			if (view != null) {
				Chase chase = (Chase) view.getTag();
				if (chase.finished == 0) {
					// continue chase
					ChaseTrailActivity.show(getActivity(), chase);
				} else {
					// inform user that trail is finished already
					Toast.makeText(getActivity(), R.string.toast_chase_already_finished, Toast.LENGTH_SHORT).show();
				}
			}
		}

		@Override
		public void onSelectionChanged(int position, long id) {
			if (actionMode != null) {
				View view = getListView().getChildAt(position);
				if (view != null) {
					Chase chase = (Chase) view.getTag();
					// update title
					actionMode.setTitle(chase.getTrail().name);
					actionMode.setSubtitle(chase.player);
					// modify menu
					MenuItem menuContinue = actionMode.getMenu().findItem(R.id.action_continue_chase);
					if (menuContinue != null) {
						menuContinue.setVisible(chase.finished == 0);
					}
				}
			}
		}

		/**
		 * 
		 * @param chaseId
		 */
		private void deleteChase(Chase chase) {

			final Chase passedChase = chase;

			new DialogFragment() {
				@Override
				public Dialog onCreateDialog(Bundle savedInstanceState) {

					return new AlertDialog.Builder(getActivity()).setTitle(R.string.action_delete_chase)
							.setMessage(R.string.dialog_delete_chase_message).setIcon(R.drawable.ic_delete)
							.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// delete chase in database
									passedChase.delete(getActivity());
									// finish action mode
									finishActionMode();
									// refresh list
									ChasesFragment.this.reload();
								}
							}).setNegativeButton(R.string.dialog_no, null).create();
				}
			}.show(getFragmentManager(), null);
		}

		/**
		 * 
		 * @param trail
		 */
		public void setTrail(Trail trail) {
			this.trail = trail;
		}		
	}
	
	
	// name to identify trail id passed as extra in intent 
	public static final String KEY_TRAILID = "trailId";
	
	// name of the fragments
	private static final String FRAGMENT_TAG_OVERVIEW = "overview";
	private static final String FRAGMENT_TAG_AREA = "area";
	private static final String FRAGMENT_TAG_CHASES = "chases";
	
	// fragments
	private OverviewFragment overviewFragment;
	private AreaFragment areaFragment;
	private ChasesFragment chasesFragment;
	
	FrameLayout frameContent;
	
	// the trail it's all about
	Trail trail = null;
		
	/**
	 * Open the activity for the specified trail
	 * @param trail
	 */
	public static void show(Context context, Trail trail) {
		// switch to chase activity
		Intent intent = new Intent(context, TrailInfoActivity.class);
		intent.putExtra(KEY_TRAILID, trail.getId());
		context.startActivity(intent);
	}
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// find out what trail we need to ashow
		long trailId = getIntent().getLongExtra(KEY_TRAILID, 0);		
		if (trailId == 0 && savedInstanceState != null) {
			trailId = savedInstanceState.getLong(KEY_TRAILID);
		}
		
		// load trail including its checkpoints
		trail = Trail.load(this, trailId);
		trail.loadCheckpoints(this);
		
		// load layout
		setContentView(R.layout.activity_trail_info);

		// get reference to frame where content will be placed
		frameContent = (FrameLayout)findViewById(R.id.content_frame);
		
		// create fragments (if not recreated by the system)	
		overviewFragment = (OverviewFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_OVERVIEW);
		if (overviewFragment == null) {
			overviewFragment = new OverviewFragment();			
		}		
		areaFragment = (AreaFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_AREA);
		if (areaFragment == null) {
			areaFragment = new AreaFragment();
		}
		chasesFragment = (ChasesFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_CHASES);
		if (chasesFragment == null) {
			chasesFragment = new ChasesFragment();
		}
		// pass trail to them
		overviewFragment.setTrail(trail);
		areaFragment.setTrail(trail);
		chasesFragment.setTrail(trail);
		
		// setup action bar
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);
		getActionBar().setTitle(R.string.activity_title_trail_info);
		getActionBar().setSubtitle(trail.name);
		
		// create tabs
		Tab overiviewTab = actionBar.newTab()				//
				.setText(R.string.tab_trail_overview)		//
				.setTag(overviewFragment);					//
		overiviewTab.setTabListener(new TabListener<LocalTrailsFragment>(overviewFragment,FRAGMENT_TAG_OVERVIEW, R.id.content_frame));
		actionBar.addTab(overiviewTab);

		Tab areaTab = actionBar.newTab()					//
				.setText(R.string.tab_trail_area)			//
				.setTag(areaFragment);						//
		areaTab.setTabListener(new TabListener<CloudTrailsFragment>(areaFragment, FRAGMENT_TAG_AREA, R.id.root_frame));
		actionBar.addTab(areaTab);

		Tab chasesTab = actionBar.newTab()					//
				.setText(R.string.tab_trail_chases)			//
				.setTag(chasesFragment);					//
		chasesTab.setTabListener(new TabListener<CloudTrailsFragment>(chasesFragment, FRAGMENT_TAG_CHASES, R.id.content_frame));
		actionBar.addTab(chasesTab);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// save trail we've been displaying 
		outState.putLong(KEY_TRAILID, trail.getId());
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
		
		case android.R.id.home:
			// finish activity
			finish();
			return true;
		
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
