package ch.gpschase.app;

import java.util.ArrayList;
import java.util.Random;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import ch.gpschase.app.ChaseService.Chase;
import ch.gpschase.app.ChaseService.Checkpoint;
import ch.gpschase.app.data.Contract;

public class ChaseTrailActivity extends Activity {

	private static final String FRAGMENT_TAG_CPVIEW = "cpview";
	private static final String FRAGMENT_TAG_MAP = "map";

	/**
	 * 
	 */
	private class ChaseServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d("ChaseTrailActivity", "ChaseService connected");

			chaseService = ((ChaseService.LocalBinder) service).getService();
			chaseService.registerListener(chaseServiceListener);

			// set action bar title
			ChaseService.Chase chase = chaseService.getChase();
			if (chase != null) {
				//
				getActionBar().setSubtitle(chase.getTrailName() + " - " + chase.getPlayer());				
			}
			
			// update UI
			update();			
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d("ChaseTrailActivity", "ChaseService disconnected");
			chaseService = null;
		}
	};

	/**
	 * 
	 */
	private class ChaseServiceListener implements ChaseService.Listener {

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
		public void onClickedPoint(long pointId) {
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
		public void onPositionedPoint(long pointId, LatLng newPosition) {
			// do nothing
		}

		@Override
		public void onStartPositioningPoint(long pointId) {
			// do nothing
		}
	};



	/**
	 * 
	 */
	private class TimerRunnable implements Runnable {

		// create a handle for UI thread
		final Handler handler = new Handler();

		private class UiRunnable implements Runnable {

			private String duration;

			UiRunnable(String duration) {
				this.duration = duration;
			}

			public void run() {
				textViewTime.setText(duration);
			}
		}

		@SuppressLint("DefaultLocale")
		public void run() {
			// new randon generator instance
			final Random rnd = new Random();

			// while thread object still exists
			while (timerThread != null) {

				long duration = 0;
				ChaseService.Chase chase = getChase();
				if (chase != null) {
					long started = chase.getStarted();
					long finished = chase.getFinished();

					if (started > 0) {
						if (finished > 0) {
							duration = finished - started;
						} else {
							duration = System.currentTimeMillis() - started;
						}

					}
				}

				// update text view in UI thread
				handler.post(new UiRunnable(Utils.formatDuration(duration)));

				// feed mock location if one is set. To make it more realistic
				// for the location manager,
				// we weobble a bit arounf the set mock location
				if (mockLocation != null) {
					double offset = rnd.nextDouble() * 1 / 11111; // approx 1 m
					Location loc = new Location(mockLocation);
					loc.setLatitude(loc.getLatitude() + offset);
					locationManager.setTestProviderLocation(
							mockLocationProviderName, loc);
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

	// map fragment to be reused
	private TrailMapFragment map;

	// references to UI elements
	private TextView textViewTime;
	private TextView textViewProgress;
	private ProgressBar progressBar;
	
	// reference to case service
	private ChaseService chaseService;

	private LocationManager locationManager;

	// mock location provider and resource (just for debugging purposes)
	private String mockLocationProviderName;
	private Location mockLocation;

	// chase service connection instance
	private final ChaseServiceConnection chaseServiceConnection = new ChaseServiceConnection();

	// chase service listener instance
	private final ChaseServiceListener chaseServiceListener = new ChaseServiceListener();

	// map listener instance
	private final MapListener mapListener = new MapListener();

	// timer thread, which runs once a second
	private Thread timerThread;

	// 
	private ViewCheckpointFragment checkpointView;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("ChaseTrailActivity", "onCreate");

		// start service
		Intent intent = new Intent(Intent.ACTION_DEFAULT,
				getIntent().getData(), this, ChaseService.class);
		startService(intent);

		// register and bind service
		bindService(new Intent(this, ChaseService.class),
				chaseServiceConnection, Context.BIND_AUTO_CREATE);

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
		if (chaseService != null) {
			chaseService.unregisterListener(chaseServiceListener);
		}
		unbindService(chaseServiceConnection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_chase_trail, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
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
	 * 
	 */
	private void updateMap() {

		// must be done on UI thread
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// start from scratch
				map.clearPoints();

				ChaseService.Chase chase = chaseService.getChase();
				if (chase != null) {
					LatLng lastHitLoc = null;
					LatLng nextCpLoc = null;
					for (ChaseService.Checkpoint cp : chase.getCheckpoints()) {
						// get location
						LatLng location = new LatLng(cp.getLocation()
								.getLatitude(), cp.getLocation().getLongitude());						

						if (cp.isHit()) {
							// show on map
							map.addPoint(cp.getId(), location, true, false);
							
							lastHitLoc = location;
						} else {
							if (cp.isShowLocation()) {
								// show on map
								map.addPoint(cp.getId(), location, false, false);

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
					} else if (chaseService != null) {						
						Location here = chaseService.getLastKnwonLocation();
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
		ChaseService.Chase chase = chaseService.getChase();
		if (chase != null) {
			distance = chase.getDistanceToNextCheckpoint();
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
				checkpointView.setDistance(distance);
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
		ChaseService.Chase chase = chaseService.getChase();
		if (chase != null) {
			for (Checkpoint cp : chase.getCheckpoints()) {
				totalCheckpoints++;
				if (cp.isHit()) {
					hitCheckpoints++;
				}
			}
		}
		// update UI in main thread
		class UiRunnable implements Runnable {
			int total, hit;

			public UiRunnable(int total, int hit) {
				this.total = total;
				this.hit = hit;
			}

			@Override
			public void run() {
				textViewProgress.setText(hit + "/" + total);
				progressBar.setMax(total);
				progressBar.setProgress(hit);
			}
		}
		runOnUiThread(new UiRunnable(totalCheckpoints, hitCheckpoints));
	}
	
	/**
	 * 
	 */
	private void updateCheckpointView() {
		
		ChaseService.Chase chase = chaseService.getChase();
		long checkpointId = 0;
		int checkpointNo = 0;
		if (chase != null) {
			ChaseService.Checkpoint nextCp = chase.getNextCheckpoint();
			if (nextCp != null) {
				checkpointId = nextCp.getId();
				checkpointNo = nextCp.getNo();
			}
		}
		
		// update UI in main thread
		class UiRunnable implements Runnable {
			long checkpointId;
			int checkpointNo;

			public UiRunnable(long checkpointId, int checkpointNo) {
				this.checkpointId = checkpointId;
				this.checkpointNo = checkpointNo;
			}

			@Override
			public void run() {
				// next checkpoint available?
				if (this.checkpointId != 0) {
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
				checkpointView.setCheckpoint(checkpointId, checkpointNo);
			}
		}		
		runOnUiThread(new UiRunnable(checkpointId, checkpointNo));
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
	 * @return
	 */
	private ChaseService.Chase getChase() {
		if (chaseService != null) {
			return chaseService.getChase();
		}
		return null;
	}


}
