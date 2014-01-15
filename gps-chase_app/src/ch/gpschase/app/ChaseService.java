package ch.gpschase.app;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.Contract.Checkpoints;
import ch.gpschase.app.data.TrailInfo;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;

import com.google.android.gms.location.LocationListener;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Service which does the real work behind chasing a trail. Acts as a lasting connection to the LocationManager,
 * even when the chasing activity isn't active.
 */
public class ChaseService extends Service  {

	/** represents an hit of checkpoint */
	public class Hit {
		public  long id;
		public long time;			
	}

	/** represents a checkpoint */
	public class Checkpoint  extends ch.gpschase.app.data.Checkpoint{
		
		private Hit hit;
						
		public boolean isHit() {
			return hit != null;
		}
		
		public int getNo() {
			return chase.checkpoints.indexOf(this) + 1;
		}
	}

	/** represents the chase */
	public class Chase {
		
		public long id;
		public long started; 
		public long finished;
		public final List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();

		public TrailInfo trail;
		
		public String player;
		
		// the next checkpoint we need to hit
		private Checkpoint nextCheckpoint; 

		public Checkpoint getNextCheckpoint() {
			return nextCheckpoint;
		}

		/**
		 * 
		 * @return
		 */
		public float getDistanceToNextCheckpoint() {
			if (nextCheckpoint != null && locationClient.isConnected()) {
				Location lastLocation = locationClient.getLastLocation();
				if (lastLocation != null) {
					return lastLocation.distanceTo(nextCheckpoint.location);
				} else {
					return Float.NaN;
				}
			} else {
				return Float.NaN;
			}
		}
	
	}
		
	
	/**
	 * Interface used for callbacks
	 */
	public interface Listener {
		public void onStarted();
		public void onHitCheckpoint(Checkpoint checkpoint);
		public void onDistanceToCheckpointChanged(float distance);
		public void onFinished();
	}
	


	/**
	 * 
	 */
	private class LocationCallback implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

		private static final int HIT_DISTANCE = 5;
		public static final int HIT_DISTANCE_DEBUG = 100;

		@Override
		public void onLocationChanged(Location location) {
								
			// next checkpoint set?
			if (chase.nextCheckpoint != null && location != null) {
				
				// update clients with distance still remaining
				float distance = location.distanceTo(chase.nextCheckpoint.location);
				for ( Listener l : listeners) {
					l.onDistanceToCheckpointChanged(distance);
				}

				// did we hit a checkpoint?
				if (distance < (App.isDebuggable() ? HIT_DISTANCE_DEBUG : HIT_DISTANCE)) {
					
					// create a new hit
					Hit hit = new Hit();
					hit.time = System.currentTimeMillis();
					chase.nextCheckpoint.hit = hit;

					// save in DB
					ContentValues values = new ContentValues();
					values.put(Contract.Hits.COLUMN_NAME_CHECKPOINT_ID, chase.nextCheckpoint.id);
					values.put(Contract.Hits.COLUMN_NAME_TIME, hit.time);
					Uri hitsUri = Contract.Hits.getUriDir(chase.id);
					Uri hitUri = getContentResolver().insert(hitsUri, values);
					hit.id = ContentUris.parseId(hitUri);

					// keep checkpoint we've hit
					Checkpoint hitCheckpoint = chase.nextCheckpoint;
					
					// change to next checkpoint
					int index = chase.checkpoints.indexOf(chase.nextCheckpoint);
					if (index < chase.checkpoints.size()-1 ) {
						index++;
						chase.nextCheckpoint = chase.checkpoints.get(index);
					}
					else {
						chase.nextCheckpoint = null;
					}					

					// finished if no more checkpoints left
					if (chase.nextCheckpoint == null) {
						
						// take time from last hit
						chase.finished = hit.time;
						
						// save in DB
						values = new ContentValues();
						values.put(Contract.Chases.COLUMN_NAME_FINISHED, chase.finished);
						Uri chaseUri = Contract.Chases.getUriId(chase.id);
						getContentResolver().update(chaseUri, values, null, null);

						// inform clients
						for ( Listener l : listeners) {
							l.onFinished();
						}
						
						// no need to continue service
						stopSelf();
						
					} else {
						// inform clients
						for ( Listener l : listeners) {
							l.onHitCheckpoint(hitCheckpoint);
						}						
					}
				}
			}			
			else {	// unknown
				for ( Listener l : listeners) {
					l.onDistanceToCheckpointChanged(Float.NaN);
				}				
			}
		}

		@Override
		public void onConnectionFailed(ConnectionResult arg0) {
			// we don't care yet
		}

		@Override
		public void onConnected(Bundle arg0) {
			// request location updates each second
			LocationRequest request = LocationRequest.create();
			request.setInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY );
			locationClient.requestLocationUpdates(request, locationCallback);
		}

		@Override
		public void onDisconnected() {
			// we don't care yet
		}
	}

	/**
	 * 
	 */
	public class LocalBinder extends Binder {
		ChaseService getService() {
			return ChaseService.this;
		}
	};

	
	// lists of registered callback listeners
	private final List<Listener> listeners = new ArrayList<Listener>();

	// current chase
	private Chase chase;
	
	// callback handler instance cor location client
	private final LocationCallback locationCallback = new LocationCallback();
	
	// location client (used to receive updates about current position)
	private LocationClient locationClient;	
	
	
	@Override
	public void onCreate() {
		Log.d("ChaseService", "onCreate");		
		// create and connect location client
		locationClient = new LocationClient(this, locationCallback, locationCallback);
		locationClient.connect();
	}	

	@Override
	public void onDestroy() {
		Log.d("ChaseService", "onDestroy");		
		// disconnect and destroy location client
		locationClient.removeLocationUpdates(locationCallback);
		locationClient.disconnect();
		locationClient = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("ChaseService", "onStartCommand");		

		// load some information about the chase into an object tree
		long chaseId = ContentUris.parseId(intent.getData());
		
		// load data
		try {
			load(chaseId);
			return START_STICKY;
		}
		catch (Exception ex)
		{
			return START_NOT_STICKY;			
		}		
	}

	/**
	 * Loads the data from the database
	 * @param chaseId
	 * @throws IllegalArgumentException
	 */
	private void load(long chaseId) throws IllegalArgumentException {
		Uri chaseUri = Contract.Chases.getUriId(chaseId);
		Cursor cursorChase;
		cursorChase = getContentResolver().query(chaseUri, Contract.Chases.READ_PROJECTION, null, null, null);
		if (!cursorChase.moveToNext()) {
			cursorChase.close();
			throw new IllegalArgumentException("Chase not found");
		}
		
		chase = new Chase();
		chase.trail = new TrailInfo();
		chase.id = cursorChase.getLong(Contract.Chases.READ_PROJECTION_ID_INDEX);
		chase.trail.id = cursorChase.getLong(Contract.Chases.READ_PROJECTION_TRAIL_ID_INDEX);
		chase.player = cursorChase.getString(Contract.Chases.READ_PROJECTION_PLAYER_INDEX);
		chase.started = cursorChase.getLong(Contract.Chases.READ_PROJECTION_STARTED_INDEX);
		chase.finished = cursorChase.getLong(Contract.Chases.READ_PROJECTION_FINISHED_INDEX);
		cursorChase.close();

		// load trail details
		Uri trailUri = Contract.Trails.getUriId(chase.trail.id);
		Cursor trailCursor = getContentResolver().query(trailUri, Contract.Trails.READ_PROJECTION, null, null, null);
		if (trailCursor.moveToNext()) {
			chase.trail.uuid = UUID.fromString(trailCursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX));			
			chase.trail.name = trailCursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX);			
			chase.trail.downloaded = trailCursor.getLong(Contract.Trails.READ_PROJECTION_DOWNLOADED_INDEX);			
			chase.trail.updated = trailCursor.getLong(Contract.Trails.READ_PROJECTION_UPDATED_INDEX);			
		}
		trailCursor.close();	
			
		// load checkpoints
		Uri checkpointsUri = Contract.Checkpoints.getUriDir(chase.trail.id);
		Cursor checkpointsCursor = getContentResolver().query(checkpointsUri, Contract.Checkpoints.READ_PROJECTION, null, null, Contract.Checkpoints.DEFAULT_SORT_ORDER);
		while (checkpointsCursor.moveToNext()) {			
			Checkpoint checkpoint = new Checkpoint();
			checkpoint.id = checkpointsCursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX);
			checkpoint.location.setLatitude(checkpointsCursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LAT_INDEX));
			checkpoint.location.setLongitude(checkpointsCursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LNG_INDEX));
			checkpoint.showLocation = checkpointsCursor.getInt(Contract.Checkpoints.READ_PROJECTION_LOC_SHOW_INDEX) != 0;
			chase.checkpoints.add(checkpoint);
		}
		checkpointsCursor.close();

		// load hits
		Uri hitsUri = Contract.Hits.getUriDir(chaseId);
		Cursor hitsCursor = getContentResolver().query(hitsUri, Contract.Hits.READ_PROJECTION, null, null, null);
		while (hitsCursor.moveToNext()) {
			long hitCheckpointId = hitsCursor.getLong(Contract.Hits.READ_PROJECTION_CHECKPOINT_ID_INDEX);						
			// look for checkpoint
			for (Checkpoint checkpoint : chase.checkpoints ) {
				if (checkpoint.id == hitCheckpointId) {
					checkpoint.hit = new Hit();
					checkpoint.hit.id = hitsCursor.getLong(Contract.Hits.READ_PROJECTION_ID_INDEX);
					checkpoint.hit.time = hitsCursor.getLong(Contract.Hits.READ_PROJECTION_TIME_INDEX);
					continue;	// next hit
				}
			}			
		}
		hitsCursor.close();
		
		// first checkpoint without a hit is our next one
		for (Checkpoint checkpoint : chase.checkpoints ) {
			if (checkpoint.hit == null) {
				chase.nextCheckpoint = checkpoint;
				break;
			}
		}
	}

	/**
	 * Reloas the data of the chase
	 */
	public boolean reload() {

		// does a chase exist?
		if (chase != null) {			
			// load data
			try {
				load(chase.id);
				return true;
			}
			catch (Exception ex)
			{
				return false;
			}
		}
		else {
			return false;
		}
	}

	private final LocalBinder localBinder = new LocalBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		return localBinder;
	}

	/**
	 * 
	 * @param listener
	 */
	public void registerListener(Listener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/**
	 * 
	 * @param listener
	 */
	public void unregisterListener(Listener listener) {
		listeners.remove(listener);
	}

	/**
	 * 
	 * @return
	 */
	public Chase getChase() {
		return chase;
	}

	/**
	 * 
	 * @return
	 */
	public Location getLastKnwonLocation() {
		if (locationClient != null && locationClient.isConnected()) {
			return locationClient.getLastLocation();
		}
		return null;
	}
	
}