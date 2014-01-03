package ch.gpschase.app;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
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

public class TrailMapFragment extends com.google.android.gms.maps.MapFragment implements OnSharedPreferenceChangeListener {

	private static final int BOUNDS_PADDING = 72;

	private static final int TRAILLINE_COLOR = 0xFF9933CC;

	/**
	 * Default zoom factor for camera
	 */
	public static final int DEFAULT_ZOOM = 17;

	// Container Activity must implement this interface
	public interface MapListener {

		public void onClickedPoint(long pointId);

		public void onClickedMap(LatLng position);

		public void onPositionedPoint(long pointId, LatLng newPosition);

		public void onStartPositioningPoint(long pointId);

	}

	// bitmaps used as custom markers
	private BitmapDescriptor iconNeutralFirstNormal;
	private BitmapDescriptor iconNeutralFirstSelected;
	private BitmapDescriptor iconNeutralOtherNormal;
	private BitmapDescriptor iconNeutralOtherSelected;
	private BitmapDescriptor iconDoneFirstNormal;
	private BitmapDescriptor iconDoneFirstSelected;
	private BitmapDescriptor iconDoneOtherNormal;
	private BitmapDescriptor iconDoneOtherSelected;

	// event listener
	private MapListener listener;

		
	/**
	 * Class for en entry in the markers list
	 */
	private class Point {
		public Marker marker;
		public BitmapDescriptor icon;
		public long id;
		public boolean done;
	}

	// list of points marked on map
	private final LinkedList<Point> pointList = new LinkedList<Point>();

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

		// load images
		iconNeutralFirstNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_first_normal);
		iconNeutralFirstSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_first_selected);
		iconNeutralOtherNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_other_normal);
	    iconNeutralOtherSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_neutral_other_selected);
		iconDoneFirstNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_first_normal);
		iconDoneFirstSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_first_selected);
		iconDoneOtherNormal = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_other_normal);
		iconDoneOtherSelected = BitmapDescriptorFactory.fromResource(R.drawable.ic_cp_done_other_selected);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = super.onCreateView(inflater, container, savedInstanceState);

		map = getMap();

		// enable my location
		map.setMyLocationEnabled(true);
		// set map type
		map.setMapType(getMapType());

		// we need to knwo when the layout is done
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
					listener.onClickedPoint(p.id);
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
					listener.onPositionedPoint(p.id, marker.getPosition());
					refreshTrailLine(false);
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
					listener.onStartPositioningPoint(p.id);
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
					Integer.valueOf(GoogleMap.MAP_TYPE_NORMAL).toString()));
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
				setMapType(Integer.valueOf(sharedPreferences.getString(key, Integer.valueOf(GoogleMap.MAP_TYPE_NORMAL).toString())));
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
		ListPreference pref = new ListPreference(context);
		pref.setTitle(context.getString(R.string.pref_map_type_title));
		pref.setKey(context.getString(R.string.pref_map_type_key));
		pref.setEntries(new String[] {																	// 
				context.getResources().getString(R.string.pref_map_type_normal_display),				//
				context.getResources().getString(R.string.pref_map_type_satellite_display),				//
				context.getResources().getString(R.string.pref_map_type_hybrid_display),				//
				context.getResources().getString(R.string.pref_map_type_terrain_display) });			//
		pref.setEntryValues(new String[] { 																//
				Integer.valueOf(GoogleMap.MAP_TYPE_NORMAL).toString(),									//
				Integer.valueOf(GoogleMap.MAP_TYPE_SATELLITE).toString(), 								//
				Integer.valueOf(GoogleMap.MAP_TYPE_HYBRID).toString(),									//
				Integer.valueOf(GoogleMap.MAP_TYPE_TERRAIN).toString() });								//
		pref.setDefaultValue(Integer.valueOf(GoogleMap.MAP_TYPE_NORMAL).toString());					//
		pref.setSummary("%s");	// TODO update after value is changed
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
		}
		else {			
			// keep it as pending. it will be done as soon as the map is ready
			pendingLatLngBounds = builder;
		}
	}

	/**
	 * Removes the marker for the given point
	 * 
	 * @param checkpoint
	 */
	public void removePoint(long pointId) {
		Point p = getPoint(pointId);
		if (p != null) {
			// remove marker
			p.marker.remove();
			// remove from list
			pointList.remove(p);
			// reset selectedPoint if it was it
			if (selectedPoint == p) {
				selectedPoint = null;
			}
			refreshIcons();
			refreshTrailLine(false);
		}
	}

	/**
	 * 
	 */
	public void setPointIndex(long pointId, int newIndex) {

		Point p = getPoint(pointId);
		pointList.remove(p);
		pointList.add(newIndex, p);

		refreshIcons();
		refreshTrailLine(false);
	}

	/**
	 * Appends a marker for the given point
	 */
	public void addPoint(long pointId, LatLng position, boolean done, boolean refresh) {
		addPoint(pointId, position, pointList.size(), done, refresh);
	}

	/**
	 * Adds a marker for the given point at the given index
	 */
	public void addPoint(long pointId, LatLng position, int index, boolean done, boolean refresh) {
		// add
		Point p = new Point();
		p.id = pointId;

		//
		p.icon = iconNeutralOtherNormal;
		p.marker = map.addMarker(new MarkerOptions().position(position).icon(p.icon));
		p.marker.setDraggable(draggable);
		p.done = done;
		pointList.add(index, p);

		if (refresh) {
			refreshIcons();
			refreshTrailLine(false);
		}
	}

	/**
	 * Removes all points
	 */
	public void clearPoints() {
		for (Point p : pointList) {
			if (p.marker != null) {
				p.marker.remove();
			}
		}
		pointList.clear();

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
	public void selectPoint(long pointId) {
		// get point to select
		Point p = getPoint(pointId);
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
	 * @return Id of the selected point or 0
	 */
	public long getSelectedPoint() {
		return selectedPoint != null ? selectedPoint.id : 0;
	}
	
	/**
	 * Gets the point for a marker
	 * 
	 * @param marker
	 * @return found checkpoint or <code>null</code>
	 */
	private Point getPoint(Marker marker) {
		for (Point p : pointList) {
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
	private Point getPoint(Long checkpointId) {
		for (Point p : pointList) {
			if (p.id == checkpointId) {
				return p;
			}
		}
		return null;
	}

	/**
	 * 
	 */
	private void refreshIcons() {
		int index = 0;
		BitmapDescriptor icon = null;
		for (Point p : pointList) {			
			if (!p.done) {
				if ( index == 0 ) {
					icon = selectedPoint == p ? iconNeutralFirstSelected : iconNeutralFirstNormal;
				}
				else {
					icon = selectedPoint == p ? iconNeutralOtherSelected : iconNeutralOtherNormal;
				}				
			} else {
				if ( index == 0 ) {
					icon = selectedPoint == p ? iconDoneFirstSelected : iconDoneFirstNormal;
				}
				else {
					icon = selectedPoint == p ? iconDoneOtherSelected : iconDoneOtherNormal;
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
	 * 
	 */
	private void refreshTrailLine(boolean recreate) {

		if (recreate && trailLine != null) {
			trailLine.remove();
			trailLine = null;
		}

		if (trailLine == null) {
			trailLine = map.addPolyline(new PolylineOptions().width(5).color(TRAILLINE_COLOR));
		}

		List<LatLng> points = new ArrayList<LatLng>(pointList.size());
		for (Point p : pointList) {			
			points.add(p.marker.getPosition());			
		}
		trailLine.setPoints(points);
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
