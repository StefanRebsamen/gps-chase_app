package ch.gpschase.app;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import ch.gpschase.app.ChaseService.Checkpoint;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.ImageManager;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.data.TrailInfo;
import ch.gpschase.app.util.DownloadTask;
import ch.gpschase.app.util.Duration;
import ch.gpschase.app.util.TrailMapFragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

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
				//set title
				getActionBar().setSubtitle(chase.trail.name + " - " + chase.player);
				
				// enable download button only if trail was downloaded
				if (menuDownload != null) {
					menuDownload.setVisible(chase.trail.downloaded != 0);
				}
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
		
		@SuppressLint("DefaultLocale")
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

				//////////////////////////////
				// update duration
				long duration = 0;
				ChaseService.Chase chase = getChase();
				if (chase != null) {
					if (chase.started > 0) {
						if (chase.finished > 0) {
							duration = chase.finished - chase.started;
						} else {
							duration = System.currentTimeMillis() - chase.started;
						}
					}
				}
				// update text view in UI thread
				handler.post(new UpdateUiRunnable(Duration.format(duration)));

				//////////////////////////////
				// Update of trail 
				if (getChase() != null && getChase().trail.downloaded != 0) {
					
					if (updateInterval > 0 &&  System.currentTimeMillis() - lastUpdateCheck >= (updateInterval * 1000)) {
			
						lastUpdateCheck = System.currentTimeMillis();
						
						// TODO: check if there is an update available (HTTP method)
						
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
					loc.setTime(System.currentTimeMillis());
					// locationManager.setTestProviderLocation(mockLocationProviderName, loc);
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
		
		/**
		 * 
		 */
		private class ImageClickListener implements OnClickListener {
			@Override
			public void onClick(View v) {
	
				Long imageId = (Long)v.getTag();
				
		        // create an image view
				ImageView imageView = new ImageView(getActivity());			
	            imageView.setLayoutParams(new LinearLayout.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
	            imageView.setPadding(8, 8, 8, 8);
	            imageView.setAdjustViewBounds(true);
		        imageView.setImageBitmap(App.getImageManager().getFull(imageId));
				
		        // show it in an dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.dialog_show_image_title);
				builder.setView(imageView);
				builder.setPositiveButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.create().show();			
			}		
		}
		
		private final ImageClickListener imageClickListener = new ImageClickListener(); 
		
		// checkpoint we've got to show stuff for
		private long checkpointId;
	
		//number of the checkpoint
		private int checkpointNo;
		
		// distance to next checkpoint
		private float distance; 
		
		// references to UI elements
		private TextView textViewHint;
		private TextView textViewCheckpoint;
		private TextView textViewNotShown;
		private TextView textViewDistance;
		private LinearLayout layoutImages;	
			
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
	
		}
	
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
			// load layout
			View view = inflater.inflate(R.layout.fragment_view_checkpoint, null);
			
			// get references to UI elements
			textViewHint = (TextView)view.findViewById(R.id.textView_hint);
			textViewCheckpoint = (TextView)view.findViewById(R.id.textView_checkpoint);
			textViewNotShown = (TextView)view.findViewById(R.id.textView_not_shown);
			textViewDistance = (TextView)view.findViewById(R.id.textView_distance);
			layoutImages = (LinearLayout)view.findViewById(R.id.layout_images);		
						
			updateHintAndImages();
			updateDistance();		
			
			return view;			
		}
		
		@Override
		public void onStart() {
			super.onStart();
		}	
		
		/**
		 * 
		 * @return
		 */
		public long getCheckpointId() {
			return checkpointId;
		}
	
		
		/**
		 * 
		 * @return
		 */
		public long getCheckpointNo() {
			return checkpointNo;
		}
		
		/**
		 * 
		 * @param checkpointId
		 */
		public void setCheckpoint(long checkpointId, int checkpointNo) {
			if (this.checkpointId != checkpointId || this.checkpointNo != checkpointNo) {
				this.checkpointId = checkpointId;
				this.checkpointNo = checkpointNo;
				updateHintAndImages();
			}
		}
	
		/**
		 * 
		 * @param distance
		 * @return
		 */
		public void setDistance(float distance) {
			this.distance = distance;
			updateDistance();		
		}	
		
		
		/**
		 * 
		 */
		private void updateHintAndImages() {
			
			if (getActivity() != null) {
	
				Cursor cursor;
				
				// load data into UI elements
				Uri checkpointIdUri = Contract.Checkpoints.getUriId(checkpointId);
				cursor = getActivity().getContentResolver().query(checkpointIdUri, Contract.Checkpoints.READ_PROJECTION, null, null, null);
				if (cursor.moveToNext()) {
					String txt;
					txt = cursor.getString(Contract.Checkpoints.READ_PROJECTION_HINT_INDEX);
					textViewHint.setText(txt);
					if (!TextUtils.isEmpty(txt)) {
						textViewHint.setVisibility(View.VISIBLE);
					} else {
						textViewHint.setVisibility(View.GONE);					
					}
					txt = "#" + checkpointNo;
					// tell if itn's not shown on the map
					if (cursor.getInt(Contract.Checkpoints.READ_PROJECTION_LOC_SHOW_INDEX) == 0) {
						textViewNotShown.setVisibility(View.VISIBLE);
					}
					else {
						textViewNotShown.setVisibility(View.INVISIBLE);					
					}
					textViewCheckpoint.setText(txt);
				}
				else {
					textViewHint.setText("");
					textViewCheckpoint.setText("");
					textViewNotShown.setVisibility(View.INVISIBLE);					
				}
				cursor.close();
					
				// load images
				layoutImages.removeAllViews();			
				
				ImageManager imageManager = App.getImageManager();					
				
				Uri imageDirUri = Contract.Images.getUriDir(checkpointId);		
				cursor = getActivity().getContentResolver().query(imageDirUri, Contract.Images.READ_PROJECTION, null, null, Contract.Images.DEFAULT_SORT_ORDER);
				while (cursor.moveToNext()) {
			        long imageId = cursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX);
	
			        // create an image view
					ImageView imageView = new ImageView(getActivity());
					imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					imageView.setPadding(8, 8, 8, 8);
					imageView.setImageBitmap(imageManager.getThumb(imageId));
					imageView.setTag(Long.valueOf(imageId));
			        		        
		            // listen to click event
		            imageView.setOnClickListener(imageClickListener);
					
		            // add to layout
		            layoutImages.addView(imageView);
				}						
			}
		}
	
		
		private void updateDistance() {
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

	
	/**
	 * Open the activity for the specified chase
	 * @param chaseId
	 */
	public static void show(Context context, long chaseId) {
		// switch to chase activity
		Uri chaseIdUri = Contract.Chases.getUriId(chaseId);
		Intent intent = new Intent(Intent.ACTION_DEFAULT, chaseIdUri, context, ChaseTrailActivity.class);
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

		// start service
		Intent intent = new Intent(Intent.ACTION_DEFAULT,
									getIntent().getData(), 
									this, ChaseService.class);
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
		
		// stop service
		Intent intent = new Intent(this, ChaseService.class);
		stopService(intent);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_chase_trail, menu);

		// find an enable download menu
		menuDownload = menu.findItem(R.id.action_download_trail);
		ChaseService.Chase chase = getChase();
		if (chase != null) {
			menuDownload.setVisible(chase.trail.downloaded != 0);
		}
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;

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
					for (ChaseService.Checkpoint cp : chase.checkpoints) {
						// get location
						LatLng location = new LatLng(cp.location
								.getLatitude(), cp.location.getLongitude());						

						if (cp.isHit()) {
							// show on map
							map.addPoint(cp.id, location, true, false);
							
							lastHitLoc = location;
						} else {
							if (cp.showLocation) {
								// show on map
								map.addPoint(cp.id, location, false, false);

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
			for (Checkpoint cp : chase.checkpoints) {
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
				checkpointId = nextCp.id;
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

	/**
	 * 
	 */
	private void downloadTrail(boolean initialProgressDialog) {
				
		final TrailInfo trail = getChase().trail;
		final boolean initialDialog = initialProgressDialog;
		
		class Task extends DownloadTask {
			
			public Task() {
				super(ChaseTrailActivity.this, trail, initialDialog);
			}
			
			@Override
			protected void onComplete(Trail updatedTrail) {
				
				if (updatedTrail != null) {
					// tell chase service to reload
					if (chaseService != null) {
						chaseService.reload();					
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
