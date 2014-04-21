package ch.gpschase.app.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import ch.gpschase.app.R;
import ch.gpschase.app.data.Checkpoint;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Fragment to display a trail (or parts of it) on a GoogleMaps map
 */
public class TrailMapFragment extends com.google.android.gms.maps.MapFragment implements OnSharedPreferenceChangeListener {

	private static final int BOUNDS_PADDING = 72;

	/**
	 * Color used to draw the trail line
	 */
	public int traiLineColor = 0;

	/**
	 * Default zoom factor for camera
	 */
	public static final int DEFAULT_ZOOM = 17;

	// Container Activity must implement this interface
	public interface MapListener {

		public void onClickedCheckpoint(Checkpoint checkpoint);

		public void onClickedMap(LatLng position);

		public void onPositionedCheckpoint(Checkpoint checkpoint);

		public void onStartPositioningCheckpoint(Checkpoint checkpoint);

	}

	// bitmaps used as custom markers
	private BitmapDescriptor iconNeutralFirstNormal;
	private BitmapDescriptor iconNeutralFirstSelected;
	private BitmapDescriptor iconNeutralHiddenNormal;
	private BitmapDescriptor iconNeutralHiddenSelected;
	private BitmapDescriptor iconNeutralOtherNormal;
	private BitmapDescriptor iconNeutralOtherSelected;
	private BitmapDescriptor iconHitFirstNormal;
	private BitmapDescriptor iconHitFirstSelected;
	private BitmapDescriptor iconHitOtherNormal;
	private BitmapDescriptor iconHitOtherSelected;

	// event listener
	private MapListener listener;

	/**
	 * An entry in the markers list
	 */
	private class Point {
		public Checkpoint checkpoint;

		public Marker marker;
		public BitmapDescriptor icon;
		public boolean hit;
	}

	// list of points marked on map
	private final LinkedList<Point> points = new LinkedList<Point>();

	// currently selected point
	private Point selectedPoint;

	// polyline used to draw trail
	private Polyline trailLine;

	//
	private GoogleMap map;

	// signals that the map is initialized (size != 0)
	private boolean mapInitialized = false;

	//
	private LatLngBounds.Builder pendingLatLngBounds = null;

	//
	private boolean draggable = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// we provide an option menu
		this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = super.onCreateView(inflater, container, savedInstanceState);

		try {
			MapsInitializer.initialize(getActivity());
		}
		catch (Exception ex) {
			// ignore
			Log.e("TrailMapFragment", "Error while initializing map", ex);
		}
		
		// load images
		iconNeutralFirstNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_first_normal);
		iconNeutralFirstSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_first_selected);
		iconNeutralHiddenNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_hidden_normal);
		iconNeutralHiddenSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_hidden_selected);
		iconNeutralOtherNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_other_normal);
		iconNeutralOtherSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_other_selected);
		iconHitFirstNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_first_normal);
		iconHitFirstSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_first_selected);
		iconHitOtherNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_other_normal);
		iconHitOtherSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_other_selected);
				
		// set background
		view.setBackgroundResource(android.R.color.white);
		
		// get reference to the map object
		map = getMap();

		// enable my location
		map.setMyLocationEnabled(true);
		// set map type
		map.setMapType(getMapType());

		// we need to know when the layout is done
		map.setOnCameraChangeListener(new OnCameraChangeListener() {

			@Override
			public void onCameraChange(CameraPosition arg0) {
				// we' not interested in the event any more
				map.setOnCameraChangeListener(null);

				// set flag
				mapInitialized = true;

				// do update if there is one pending
				if (pendingLatLngBounds != null) {
					map.moveCamera(CameraUpdateFactory.newLatLngBounds(pendingLatLngBounds.build(), BOUNDS_PADDING));
					pendingLatLngBounds = null;
				}

			}
		});

		UiSettings settings = map.getUiSettings();
		settings.setMyLocationButtonEnabled(true);
		settings.setZoomControlsEnabled(false);
		settings.setCompassEnabled(true);

		// set click event for markers
		map.setOnMarkerClickListener(new OnMarkerClickListener() {
			public boolean onMarkerClick(Marker marker) {
				Point p = getPoint(marker);
				if (p != null && listener != null) {
					listener.onClickedCheckpoint(p.checkpoint);
				}
				return true;
			}
		});

		// set click event for markers
		map.setOnMarkerDragListener(new OnMarkerDragListener() {
			@Override
			public void onMarkerDragEnd(Marker marker) {
				Point p = getPoint(marker);
				if (p != null && listener != null) {

					p.checkpoint.location.setLongitude(marker.getPosition().longitude);
					p.checkpoint.location.setLatitude(marker.getPosition().latitude);

					refreshTrailLine(false);

					listener.onPositionedCheckpoint(p.checkpoint);
				}
			}

			@Override
			public void onMarkerDrag(Marker arg0) {
				// not of interest
			}

			@Override
			public void onMarkerDragStart(Marker marker) {
				Point p = getPoint(marker);
				if (p != null && listener != null) {
					listener.onStartPositioningCheckpoint(p.checkpoint);
				}
			}
		});

		// set click event for map
		map.setOnMapClickListener(new OnMapClickListener() {
			@Override
			public void onMapClick(LatLng position) {
				if (listener != null) {
					listener.onClickedMap(position);
				}
			}
		});

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
	
		refreshTrailLine(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// read color from resource
		traiLineColor = getResources().getColor(R.color.purple_dark);
	}

	@Override
	public void onDestroyView() {
		
		// clear everything
		clear();
		
		super.onDestroyView();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// inflate menu
		inflater.inflate(R.menu.menu_trail_map, menu);

		// check the choice currently set
		switch (getMapType()) {
		case GoogleMap.MAP_TYPE_NORMAL:
			menu.findItem(R.id.action_map_type_normal).setChecked(true);
			break;

		case GoogleMap.MAP_TYPE_SATELLITE:
			menu.findItem(R.id.action_map_type_satellite).setChecked(true);
			break;

		case GoogleMap.MAP_TYPE_HYBRID:
			menu.findItem(R.id.action_map_type_hybrid).setChecked(true);
			break;

		case GoogleMap.MAP_TYPE_TERRAIN:
			menu.findItem(R.id.action_map_type_terrain).setChecked(true);
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_map_type_normal:
			setMapType(GoogleMap.MAP_TYPE_NORMAL);
			item.setChecked(true);
			return true;

		case R.id.action_map_type_satellite:
			setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			item.setChecked(true);
			return true;

		case R.id.action_map_type_hybrid:
			setMapType(GoogleMap.MAP_TYPE_HYBRID);
			item.setChecked(true);
			return true;

		case R.id.action_map_type_terrain:
			setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			item.setChecked(true);
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 * @return
	 */
	public int getMapType() {
		// try to save as preference
		Context context = getActivity();
		if (context != null) {
			return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(getMapTypePreference(context).getKey(),
					Integer.valueOf(GoogleMap.MAP_TYPE_HYBRID).toString()));
		} else {
			return GoogleMap.MAP_TYPE_NORMAL;
		}
	}

	/**
	 * Sets the map type
	 * 
	 * @param mapType
	 *            {@link com.google.android.gms.maps.GoogleMap.setMapType}
	 */
	public void setMapType(int mapType) {
		// try to save as preference
		Context context = getActivity();
		if (context != null) {
			PreferenceManager.getDefaultSharedPreferences(context).edit()
					.putString(getMapTypePreference(context).getKey(), Integer.valueOf(mapType).toString()).commit();
		}
		// set to map (if already created)
		if (map != null) {
			map.setMapType(mapType);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		// try to save as preference
		Context context = getActivity();
		if (context != null) {
			if (key.equals(getMapTypePreference(context).getKey())) {
				setMapType(Integer.valueOf(sharedPreferences.getString(key, Integer.valueOf(GoogleMap.MAP_TYPE_HYBRID).toString())));
			}
		}
	}

	/**
	 * 
	 * @param context
	 * @return
	 */
	public static ListPreference getMapTypePreference(Context context) {

		// define preference and return it
		final ListPreference pref = new ListPreference(context);
		pref.setTitle(context.getString(R.string.pref_map_type_title));
		pref.setKey(context.getString(R.string.pref_map_type_key));
		pref.setEntries(new String[] { //
		context.getResources().getString(R.string.pref_map_type_normal), //
				context.getResources().getString(R.string.pref_map_type_satellite), //
				context.getResources().getString(R.string.pref_map_type_hybrid), //
				context.getResources().getString(R.string.pref_map_type_terrain) }); //
		pref.setEntryValues(new String[] { //
		Integer.valueOf(GoogleMap.MAP_TYPE_NORMAL).toString(), //
				Integer.valueOf(GoogleMap.MAP_TYPE_SATELLITE).toString(), //
				Integer.valueOf(GoogleMap.MAP_TYPE_HYBRID).toString(), //
				Integer.valueOf(GoogleMap.MAP_TYPE_TERRAIN).toString() }); //
		pref.setDefaultValue(Integer.valueOf(GoogleMap.MAP_TYPE_HYBRID).toString()); //
		pref.setSummary("%s");

		pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// update summary
				int index = pref.findIndexOfValue((String) newValue);
				pref.setSummary(pref.getEntries()[index]);
				return true;
			}
		});
		return pref;
	}

	/**
	 * 
	 * @return
	 */
	public MapListener getMapListener() {
		return listener;
	}

	/**
	 * 
	 * @param listener
	 */
	public void setMapListener(MapListener listener) {
		this.listener = listener;
	}

	/**
	 * Returns the camera target
	 * 
	 * @return
	 */
	public LatLng getCameraTarget() {
		return map.getCameraPosition().target;
	}

	/**
	 * Returns the camera zoom
	 * 
	 * @return
	 */
	public float getCameraZoom() {
		return map.getCameraPosition().zoom;
	}

	/**
	 * 
	 */
	public void setCameraTarget(LatLng target) {
		// do it immediately
		map.moveCamera(CameraUpdateFactory.newLatLng(target));
		pendingLatLngBounds = null;
	}

	/**
	 * 
	 */
	public void setCameraZoom(float zoom) {
		// do it immediately
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(map.getCameraPosition().target, zoom));
		pendingLatLngBounds = null;
	}

	/**
	 * 
	 */
	public void setCamera(Iterable<LatLng> locations) {

		if (locations == null) {
			throw new IllegalArgumentException();
		}
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		for (LatLng loc : locations) {
			builder.include(loc);
		}
		// is map initialized
		if (mapInitialized) {
			// do update immediately if possible
			CameraUpdate update = CameraUpdateFactory.newLatLngBounds(builder.build(), BOUNDS_PADDING);
			map.moveCamera(update);
		} else {
			// keep it as pending. it will be done as soon as the map is ready
			pendingLatLngBounds = builder;
		}
	}

	/**
	 * Removes the marker for the given point
	 * @param checkpoint
	 */
	public void removeCheckpoint(Checkpoint checkpoint) {
		Point p = getPoint(checkpoint);
		if (p != null) {
			// remove marker
			p.marker.remove();
			// remove from list
			points.remove(p);
			// reset selectedPoint if it was it
			if (selectedPoint == p) {
				selectedPoint = null;
			}
			refreshIcons();
			refreshTrailLine(false);
		}
	}

	/**
	 * Tells the map to reorder it's points accodrign to the checkpoint indices
	 */
	public void reorderCheckpoints() {

		// reorders points according to the indices of their checkpoints
		Collections.sort(points, new Comparator<Point>() {
			@Override
			public int compare(Point o1, Point o2) {
				return (o1.checkpoint.getIndex() > o2.checkpoint.getIndex() ? -1 : (o1.checkpoint.getIndex() == o2.checkpoint.getIndex() ? 0 : 1));
			}
		});

		// refresh
		refreshIcons();
		refreshTrailLine(false);
	}

	/**
	 * Appends a marker for the given point
	 */
	public void addCheckpoint(Checkpoint checkpoint, boolean hit, boolean refresh) {
		// add
		Point p = new Point();
		p.checkpoint = checkpoint;

		//
		p.icon = iconNeutralOtherNormal;
		LatLng pos = new LatLng(checkpoint.location.getLatitude(), checkpoint.location.getLongitude());
		p.marker = map.addMarker(new MarkerOptions().position(pos).icon(p.icon));
		p.marker.setDraggable(draggable);
		p.hit = hit;
		points.add(p);

		if (refresh) {
			refreshIcons();
			refreshTrailLine(false);
		}
	}

	/**
	 * Removes all points
	 */
	public void clear() {
		for (Point p : points) {
			if (p.marker != null) {
				p.marker.remove();
			}
		}
		points.clear();

		// remove polyline
		if (trailLine != null) {
			trailLine.remove();
		}
	}

	/**
	 * Forces a refresh of markers icons and trail line
	 */
	public void refresh() {
		refreshIcons();
		refreshTrailLine(true);
	}

	/**
	 * 
	 * @param pointId
	 */
	public void selectCheckpoint(Checkpoint checkpoint) {
		// get point to select
		Point p = getPoint(checkpoint);
		if (p != null) {
			selectedPoint = p;
		} else {
			selectedPoint = null;
		}
		//
		refreshIcons();
	}

	/**
	 * Returns the selected point
	 * 
	 * @return Id of the selected point or 0
	 */
	public Checkpoint getSelectedCheckPoint() {
		if (selectedPoint != null)
			return selectedPoint.checkpoint;
		else
			return null;
	}

	/**
	 * Gets the point for a marker
	 * 
	 * @param marker
	 * @return found checkpoint or <code>null</code>
	 */
	private Point getPoint(Marker marker) {
		for (Point p : points) {
			if (p.marker != null && p.marker.equals(marker)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Gets the Point for an id
	 * 
	 * @param checkpoint
	 * @return found marker or <code>null</code>
	 */
	private Point getPoint(Checkpoint checkpoint) {
		for (Point p : points) {
			if (p.checkpoint == checkpoint) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Refreshes all the icons
	 */
	private void refreshIcons() {
		int index = 0;
		BitmapDescriptor icon = null;
		for (Point p : points) {
			if (!p.hit) {
				if (index == 0) {
					icon = selectedPoint == p ? iconNeutralFirstSelected : iconNeutralFirstNormal;
				} else if (!p.checkpoint.showLocation){
					icon = selectedPoint == p ? iconNeutralHiddenSelected : iconNeutralHiddenNormal;
				} else {
					icon = selectedPoint == p ? iconNeutralOtherSelected : iconNeutralOtherNormal;
				}
			} else {
				if (index == 0) {
					icon = selectedPoint == p ? iconHitFirstSelected : iconHitFirstNormal;
				} else {
					icon = selectedPoint == p ? iconHitOtherSelected : iconHitOtherNormal;
				}
			}
			// set icon only if it really changed
			if (p.icon != icon) {
				p.icon = icon;
				p.marker.setIcon(p.icon);
			}
			index++;
		}
	}

	/**
	 * Refreshes the trail line
	 */
	private void refreshTrailLine(boolean recreate) {

		if (recreate && trailLine != null) {
			trailLine.remove();
			trailLine = null;
		}

		if (trailLine == null) {
			trailLine = map.addPolyline(new PolylineOptions().width(5).color(traiLineColor));
		}

		List<LatLng> linePoints = new ArrayList<LatLng>(points.size());
		for (Point p : points) {
			linePoints.add(p.marker.getPosition());
		}
		trailLine.setPoints(linePoints);
	}

	/**
	 * 
	 * @return
	 */
	public boolean isDraggable() {
		return draggable;
	}

	/**
	 * 
	 * @param draggable
	 */
	public void setDraggable(boolean draggable) {
		this.draggable = draggable;
	}
	
}
