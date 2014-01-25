package ch.gpschase.app;

import java.util.ArrayList;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import ch.gpschase.app.data.Chase;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Image;
import ch.gpschase.app.data.ImageFileManager;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.DownloadTask;
import ch.gpschase.app.util.TrailMapFragment;
import ch.gpschase.app.util.ViewImageDialog;

import com.google.android.gms.maps.model.LatLng;

public class ChaseTrailActivity extends Activity {

	// tags fro the fragments
	private static final String FRAGMENT_TAG_CPVIEW = "cpview";
	private static final String FRAGMENT_TAG_MAP = "map";
	
	// name to identify chase id passed as extra in intent 
	public static final String INTENT_EXTRA_CHASEID = "chaseId";
	
	/**
	 * 
	 */
	private class ChaseServiceConnection implements ServiceConnection {
		
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d("ChaseTrailActivity", "ChaseService connected");

			service = ((ChaseTrailService.LocalBinder) binder).getService();
			service.registerListener(serviceListener);

			// checkpoint view also needs the service
			checkpointView.setService(service);
			
			// set action bar title
			Chase chase = service.getChase();
			if (chase != null) {
				//set title
				getActionBar().setSubtitle(chase.getTrail().name + " - " + chase.player);
				
				// enable download button only if trail was downloaded
				if (menuDownload != null) {
					menuDownload.setVisible(chase.getTrail().downloaded != 0);
				}
			}
									
			// update complete UI
			update();	
			
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d("ChaseTrailActivity", "ChaseService disconnected");
			service = null;
		}
	};

	/**
	 * 
	 */
	private class ChaseServiceListener implements ChaseTrailService.Listener {

		@Override
		public void onStarted() {
			
		}		

		@Override
		public void onHitCheckpoint(Checkpoint checkpoint) {
			
			// vibrate for 600 milliseconds
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(600);

			// play a notification sound
			try {
				Uri notification = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone r = RingtoneManager.getRingtone(
						getApplicationContext(), notification);
				r.play();
			} catch (Exception e) {
				// just let it be ...
			}

			// update UI
			update();
		}

		@Override
		public void onDistanceToCheckpointChanged(float distance) {
			// update UI
			updateDistance(distance);
		}

		@Override
		public void onFinished() {
			// show toast
			Toast.makeText(ChaseTrailActivity.this, R.string.toast_finished_chase, Toast.LENGTH_LONG)
					.show();
			// vibrate for 1000 milliseconds
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(1000);

			// play a notification sound
			try {
				Uri notification = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				Ringtone r = RingtoneManager.getRingtone(
						getApplicationContext(), notification);
				r.play();
				r.play();
			} catch (Exception e) {
				// just let it be ...
			}

			// update UI
			update();
		}

	};

	/**
	 * 
	 */
	private class MapListener implements TrailMapFragment.MapListener {

		@Override
		public void onClickedCheckpoint(Checkpoint checkpoint) {
			// do nothing
		}

		@Override
		public void onClickedMap(LatLng position) {
			if (App.isDebuggable()) {

				// set mock location
				locationManager.setTestProviderStatus(mockLocationProviderName,
						LocationProvider.AVAILABLE, null,
						System.currentTimeMillis());
				Location loc = new Location(mockLocationProviderName);
				loc.setLongitude(position.longitude);
				loc.setLatitude(position.latitude);
				loc.setAltitude(0);
				loc.setAccuracy(5);
				loc.setBearing(0);
				loc.setSpeed(0);
				loc.setTime(System.currentTimeMillis());

				mockLocation = loc;
			}
		}

		@Override
		public void onPositionedCheckpoint(Checkpoint checkpoint) {
			// do nothing
		}

		@Override
		public void onStartPositioningCheckpoint(Checkpoint checkpoint) {
			// do nothing
		}
	};



	/**
	 * Timer to execute things periodically
	 */
	private class TimerRunnable implements Runnable {

		// create a handle for UI thread
		final Handler handler = new Handler();

		/**
		 * 
		 */
		private class UpdateUiRunnable implements Runnable {

			private String duration;

			UpdateUiRunnable(String duration) {
				this.duration = duration;
			}

			public void run() {
				textViewTime.setText(duration);
			}
		}

		/**
		 * 
		 */
		private class DownloadTrailRunnable implements Runnable {
			
			public void run() {
				downloadTrail(false);
			}
		}
		
		@SuppressLint({ "DefaultLocale", "NewApi" })
		public void run() {
			// new randon generator instance
			final Random rnd = new Random();

			// timestamp of last check for an update
			long lastUpdateCheck = System.currentTimeMillis();
			
			// update frequency in seconds (default is 2 minutes)			
			int updateInterval = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ChaseTrailActivity.this)
					.getString(getUpdateFrequencyPreference(ChaseTrailActivity.this).getKey(), "2")) * 60;
			
			// while thread object still exists
			while (timerThread != null) {

				if (service != null) {
					
					Chase chase = service.getChase();
					if (chase != null) {
					
						//////////////////////////////
						// update duration
						long duration = 0;
						if (chase.started > 0) {
							if (chase.finished > 0) {
								duration = chase.finished - chase.started;
							} else {
								duration = System.currentTimeMillis() - chase.started;
							}
						}
						
						// update text view in UI thread
						handler.post(new UpdateUiRunnable(App.formatDuration(duration)));
		
						//////////////////////////////
						// Update of trail 
						if (chase.getTrail().downloaded != 0) {
							
							if (updateInterval > 0 &&  System.currentTimeMillis() - lastUpdateCheck >= (updateInterval * 1000)) {
								
								lastUpdateCheck = System.currentTimeMillis();						
								
								// do update in handler
								handler.post(new DownloadTrailRunnable());												
							}
						}
						
						//////////////////////////////
						// Mock location 
										
						// feed mock location if one is set. To make it more realistic
						// for the location manager,
						// we wobble a bit around the set mock location
						if (mockLocation != null) {
							double offset = rnd.nextDouble() * 1 / 11111; // approx 1 m
							Location loc = new Location(mockLocation);
							loc.setLatitude(loc.getLatitude() + offset);
							loc.setLongitude(loc.getLongitude() + offset);
							loc.setTime(System.currentTimeMillis());
							loc.setAccuracy(2);
							loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
							locationManager.setTestProviderLocation(mockLocationProviderName, loc);
	
						}
					}
				}
				
				// wait a second
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// nothing to worry about
				}
			}

		}
	};

	/**
	 * 
	 */
	public static class ViewCheckpointFragment extends Fragment {
		
		// reference to the service
		ChaseTrailService service;
		
		
		/**
		 * 	
		 * @param service
		 */
		public void setService(ChaseTrailService service) {
			if (service == null)
				throw new  IllegalArgumentException();
			
			this.service = service;
		}
			
		// references to UI elements
		private TextView textViewHint;
		private TextView textViewNo;
		private TextView textViewNotShown;
		private TextView textViewDistance;
		private LinearLayout layoutImages;				
	
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
			// load layout
			View view = inflater.inflate(R.layout.fragment_view_checkpoint, null);
			
			// get references to UI elements
			textViewHint = (TextView)view.findViewById(R.id.textView_checkpoint_hint);
			textViewNo = (TextView)view.findViewById(R.id.textView_checkpoint_no);
			textViewNotShown = (TextView)view.findViewById(R.id.textView_not_shown);
			textViewDistance = (TextView)view.findViewById(R.id.textView_distance);
			layoutImages = (LinearLayout)view.findViewById(R.id.layout_images);		
						
			updateHintAndImages();
			updateDistance(Float.NaN);		
			
			return view;			
		}
		
		/**
		 * 
		 */
		private void updateHintAndImages() {
						
			Checkpoint checkpoint = null; 
		    if (service != null) {
		    	checkpoint = service.getCurrentCheckpoint();
		    }			
			
			if (checkpoint != null) {
				// load images
				checkpoint.loadImages(getActivity());
				
				// load data into UI elements
				textViewHint.setText(checkpoint.hint);
				if (!TextUtils.isEmpty(checkpoint.hint)) {
					textViewHint.setVisibility(View.VISIBLE);
				} else {
					textViewHint.setVisibility(View.GONE);					
				}
				
				// tell if itn's not shown on the map
				if (!checkpoint.showLocation) {
					textViewNotShown.setVisibility(View.VISIBLE);
				}
				else {
					textViewNotShown.setVisibility(View.INVISIBLE);					
				}
				textViewNo.setText("# " + (checkpoint.getIndex() + 1));

				// load images
				ImageFileManager imageManager = App.getImageManager();
				
				layoutImages.removeAllViews();			
				for (Image image : checkpoint.getImages()) {
			        // create an image view
					ImageView imageView = new ImageView(getActivity());
					imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					imageView.setPadding(8, 8, 8, 8);
					imageView.setImageBitmap(imageManager.getThumb(image));
					imageView.setTag(image);
			        		        
					// listen to click event
					imageView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Image image = (Image) v.getTag();
							ViewImageDialog.show(getActivity(), image);
						}
					});
		            
		            // add to layout
		            layoutImages.addView(imageView);					
				}
			
			}
			else {
				textViewHint.setText("");
				textViewNo.setText("");
				textViewNotShown.setVisibility(View.INVISIBLE);					
				layoutImages.removeAllViews();			
			}				
		}
			
		private void updateDistance(float distance) {
			if (textViewDistance != null) {
				// format distance
				if (!Float.valueOf(distance).equals(Float.NaN)) {
					textViewDistance.setText(String.format("%.0f m", distance));
				} else {
					textViewDistance.setText("? m");
				}
			}
		}
		
		
	
		
	}

	// map fragment to be reused
	private TrailMapFragment map;

	// references to UI elements
	private TextView textViewTime;
	private TextView textViewProgress;
	private ProgressBar progressBar;
	private MenuItem menuDownload;
		
	// reference to case service
	private ChaseTrailService service;

	private LocationManager locationManager;

	// mock location provider and resource (just for debugging purposes)
	private String mockLocationProviderName;
	private Location mockLocation;

	// chase service connection instance
	private final ChaseServiceConnection chaseServiceConnection = new ChaseServiceConnection();

	// chase service listener instance
	private final ChaseServiceListener serviceListener = new ChaseServiceListener();

	// map listener instance
	private final MapListener mapListener = new MapListener();

	// timer thread, which runs once a second
	private Thread timerThread;

	// 
	private ViewCheckpointFragment checkpointView;	


	/**
	 * Open the activity for the specified chase
	 * @param chaseId
	 */
	public static void show(Context context, Chase chase) {
		// switch to chase activity
		Intent intent = new Intent(context, ChaseTrailActivity.class);
		intent.putExtra(INTENT_EXTRA_CHASEID, chase.getId());
		context.startActivity(intent);
	}	
	
	/**
	 * 
	 * @param context
	 * @return
	 */
	public static ListPreference getUpdateFrequencyPreference(Context context) {

		// define preference and return it
		final ListPreference pref = new ListPreference(context);
		pref.setTitle(context.getString(R.string.pref_update_frequency_title));
		pref.setKey(context.getString(R.string.pref_update_frequency_key));
		pref.setEntries(new String[] {																	// 
				context.getResources().getString(R.string.pref_update_frequency_2),						//
				context.getResources().getString(R.string.pref_update_frequency_5),						//
				context.getResources().getString(R.string.pref_update_frequency_10),					//
				context.getResources().getString(R.string.pref_update_frequency_never)} );				//
		pref.setEntryValues(new String[] {	//
				"2",						//
				"5", 						//
				"10",						//
				"0" });						//
		pref.setDefaultValue("2");			//
		pref.setSummary("%s");
		
		pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// update summary
				int index = pref.findIndexOfValue((String)newValue);
				pref.setSummary(pref.getEntries()[index]);
				return true;
			}
		});
		return pref;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("ChaseTrailActivity", "onCreate");



		// add mock location provider if in debug mode
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (App.isDebuggable()) {
			mockLocationProviderName = LocationManager.GPS_PROVIDER;
			locationManager.addTestProvider(mockLocationProviderName, false,
					false, false, false, true, true, true, Criteria.POWER_LOW,
					Criteria.ACCURACY_FINE);
			locationManager.setTestProviderEnabled(mockLocationProviderName,
					true);
		}

		// load layout and get references to UI elements
		setContentView(R.layout.activity_chase_trail);
		textViewTime = (TextView) findViewById(R.id.textView_Time);
		textViewProgress = (TextView) findViewById(R.id.textView_checkpoint_progress);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		// create and add fragments (if not recreated)
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		
		map = (TrailMapFragment)getFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
		if (map == null) {
			map = new TrailMapFragment();
			ft.replace(R.id.layout_map, map, FRAGMENT_TAG_MAP);
		}		
		map.setMapListener(mapListener);
		map.setDraggable(false);
		
		checkpointView = (ViewCheckpointFragment)getFragmentManager().findFragmentByTag(FRAGMENT_TAG_CPVIEW);
		if (checkpointView == null) {
			checkpointView = new ViewCheckpointFragment();
			ft.replace(R.id.layout_checkpoint_view, checkpointView, FRAGMENT_TAG_CPVIEW);
		}		
		ft.commit();
	
		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_chase_trail);

		// keep screen alive 
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_keep_awake_key), false)) {		
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		// register and bind service
		bindService(new Intent(this, ChaseTrailService.class),
				chaseServiceConnection, Context.BIND_AUTO_CREATE);		
		// start service. Pass id of trail as intent extra
		Intent intent = new Intent(Intent.ACTION_DEFAULT, getIntent().getData(), this,  ChaseTrailService.class);
		intent.putExtra(INTENT_EXTRA_CHASEID, getIntent().getLongExtra(INTENT_EXTRA_CHASEID, 0));
		startService(intent);		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		Log.d("ChaseTrailActivity", "onDestroy");

		// remove mock location provider if in debug mode
		if (App.isDebuggable()) {
			if (locationManager.getProvider(mockLocationProviderName) != null) {
				locationManager.removeTestProvider(mockLocationProviderName);
			}
		}

		// unregister and unbind chase servive
		if (service != null) {
			service.unregisterListener(serviceListener);
		}
		unbindService(chaseServiceConnection);
		
		// stop service
		Intent intent = new Intent(this, ChaseTrailService.class);
		stopService(intent);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_chase_trail, menu);

		// find an enable download menu
		menuDownload = menu.findItem(R.id.action_download_trail);
		if (service != null) {
			Chase chase = service.getChase();
			if (chase != null) {
				menuDownload.setVisible(chase.getTrail().downloaded != 0);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_download_trail:
			downloadTrail(true);
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStart() {
		super.onStart();

		Log.d("ChaseTrailActivity", "onStart");

		// start timer
		if (timerThread == null) {
			timerThread = new Thread(new TimerRunnable());
			timerThread.start();
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		Log.d("ChaseTrailActivity", "onStop");
				
		// stop timer
		if (timerThread != null) {
			Thread thread = timerThread;
			// reset time thread reference to singal thread to exit
			timerThread = null;
			// wait for it to exit
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				// nothing to worry about
			}
		}
	}

	
	/**
	 * Updates the map
	 */
	private void updateMap() {

		// must be done on UI thread
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// start from scratch
				map.clearCheckpoints();

				Iterable<Checkpoint> checkpoints = null;
				if (service != null )
				{
					checkpoints = service.getCheckpoints();
				}

				if (checkpoints != null) {
						
					LatLng lastHitLoc = null;
					LatLng nextCpLoc = null;
					for (Checkpoint cp : checkpoints) {
						// get location
						LatLng location = new LatLng(cp.location
								.getLatitude(), cp.location.getLongitude());						
	
						if (service.getChase().isHit(cp) ) {
							// show on map
							map.addCheckpoint(cp, true, false);
							
							lastHitLoc = location;
						} else {
							if (cp.showLocation) {
								// show on map
								map.addCheckpoint(cp, false, false);
	
								nextCpLoc = location;
							}
							break;
						}
					}
					map.refresh();
	
					// set zoom depending on what makes most sense to include
					ArrayList<LatLng> boundLocs = new ArrayList<LatLng>(2);
					if (lastHitLoc != null) {
						boundLocs.add(lastHitLoc);
					}
					if (nextCpLoc != null) {
						boundLocs.add(nextCpLoc);
					}
					// set camera
					if (boundLocs.size() > 1 ) {
						map.setCamera(boundLocs);
					}
					else if (boundLocs.size() > 0) {
						map.setCameraTarget(boundLocs.get(0));
						map.setCameraZoom(TrailMapFragment.DEFAULT_ZOOM);
					} else if (service != null) {						
						Location here = service.getLastKnownLocation();
						if (here != null) {
							map.setCameraTarget(new LatLng(here.getLatitude(),
									here.getLongitude()));
							map.setCameraZoom(TrailMapFragment.DEFAULT_ZOOM);
						}
					}
				}
			}
		});
	}

	/**
	 * 
	 */
	private void updateDistance() {
		float distance = Float.NaN;
		if (service != null) {			
			distance = service.getDistanceToNextCheckpoint();
		}
		updateDistance(distance);
	}

	/**
	 * 
	 * @param distance
	 */
	@SuppressLint("DefaultLocale")
	private void updateDistance(float distance) {
		// update UI in main thread
		class UiRunnable implements Runnable {
			float distance;

			public UiRunnable(float distance) {
				this.distance = distance;
			}

			@Override
			public void run() {
				checkpointView.updateDistance(distance);
			}
		}
		runOnUiThread(new UiRunnable(distance));
	}

	/**
	 * 
	 */
	private void updateProgress() {
		// count checkpoints
		int totalCheckpoints = 0;
		int hitCheckpoints = 0;
		Iterable<Checkpoint> checkpoints = null;
		if (service != null )
		{
			checkpoints = service.getCheckpoints();
		}
		
		if (checkpoints != null) {
			for (Checkpoint cp : service.getCheckpoints()) {
				totalCheckpoints++;
				if (service.getChase().isHit(cp)) {
					hitCheckpoints++;
				}
			}
		}
		
		final int total = totalCheckpoints; 
		final int hit = hitCheckpoints; 
		// update UI in main thread
		class UiRunnable implements Runnable {
			@Override
			public void run() {
				textViewProgress.setText(hit + "/" + total);
				progressBar.setMax(total);
				progressBar.setProgress(hit);
			}
		}
		runOnUiThread(new UiRunnable());
	}
	
	/**
	 * 
	 */
	private void updateCheckpointView() {
				
		// update UI in main thread
		class UiRunnable implements Runnable {

			@Override
			public void run() {
				// get current checkpoint
				Checkpoint checkpoint = null;;
				if (service != null) {
					checkpoint = service.getCurrentCheckpoint();
				}

				// current checkpoint available?
				if (checkpoint != null) {
					// show if necessary
					if (checkpointView.isHidden()) {
						FragmentTransaction ft = getFragmentManager().beginTransaction();
						ft.show(checkpointView);
						ft.commit();
					}				
				}
				else {
					// hide if necessary
					if (!checkpointView.isHidden()) {
						FragmentTransaction ft = getFragmentManager().beginTransaction();
						ft.hide(checkpointView);
						ft.commit();
					}
				}
				// update
				checkpointView.updateHintAndImages();
			}
		}		
		runOnUiThread(new UiRunnable());
	}
	
	/**
	 * 
	 */
	private void update() {
		updateProgress();
		updateCheckpointView(); 
		updateMap();
		updateDistance();
	}


	/**
	 * 
	 */
	private void downloadTrail(boolean initialProgressDialog) {
				
		final Trail trail = service.getChase().getTrail();
		final boolean initialDialog = initialProgressDialog;
		
		class Task extends DownloadTask {
			
			public Task() {
				super(ChaseTrailActivity.this, trail, initialDialog);
			}
			
			@Override
			protected void onDownloaded(Trail downloadedTrail) {
				
				if (downloadedTrail != null) {
					// tell chase service to reload
					if (service != null) {
						service.reload();					
					}				
					//update UI
					update();
				}	
			}
		}
		
		// start task
		new Task().execute();
	}

}
