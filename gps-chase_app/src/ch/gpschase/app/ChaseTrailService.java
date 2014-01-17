package ch.gpschase.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import ch.gpschase.app.data.ChaseInfo;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.Contract.Checkpoints;
import ch.gpschase.app.data.Trail;
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
 * Service which does the real work behind chasing a trail. Acts as a lasting
 * connection to the LocationManager, even when the chasing activity isn't
 * active.
 */
public class ChaseTrailService extends Service {

	/** represents an hit of checkpoint */
	public class Hit {
		public long id;
		public long checkpointId;
		public long time;
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
	private class LocationCallback implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
			GooglePlayServicesClient.OnConnectionFailedListener {

		private static final int HIT_DISTANCE = 5;
		public static final int HIT_DISTANCE_DEBUG = 100;

		@Override
		public void onLocationChanged(Location location) {

			// next checkpoint set?
			if (nextCheckpoint != null && location != null) {

				// update clients with distance still remaining
				float distance = location.distanceTo(nextCheckpoint.location);
				for (Listener l : listeners) {
					l.onDistanceToCheckpointChanged(distance);
				}

				// did we hit a checkpoint?
				if (distance < (App.isDebuggable() ? HIT_DISTANCE_DEBUG : HIT_DISTANCE)) {

					// create a new hit
					Hit hit = new Hit();
					hit.time = System.currentTimeMillis();

					// save hit in DB
					ContentValues values = new ContentValues();
					values.put(Contract.Hits.COLUMN_NAME_CHECKPOINT_ID, nextCheckpoint.id);
					values.put(Contract.Hits.COLUMN_NAME_TIME, hit.time);
					Uri hitsUri = Contract.Hits.getUriDir(chaseInfo.id);
					Uri hitUri = getContentResolver().insert(hitsUri, values);
					hit.id = ContentUris.parseId(hitUri);

					// put it in map
					hits.put(nextCheckpoint, hit);

					// keep checkpoint we've hit
					Checkpoint hitCheckpoint = nextCheckpoint;

					// change to next checkpoint
					int index = trail.checkpoints.indexOf(nextCheckpoint);
					if (index < trail.checkpoints.size() - 1) {
						index++;
						nextCheckpoint = trail.checkpoints.get(index);
					} else {
						nextCheckpoint = null;
					}

					// finished if no more checkpoints left
					if (nextCheckpoint == null) {

						// take time from last hit
						chaseInfo.finished = hit.time;

						// save in DB
						values = new ContentValues();
						values.put(Contract.Chases.COLUMN_NAME_FINISHED, chaseInfo.finished);
						Uri chaseUri = Contract.Chases.getUriId(chaseInfo.id);
						getContentResolver().update(chaseUri, values, null, null);

						// inform clients
						for (Listener l : listeners) {
							l.onFinished();
						}

						// no need to continue service
						stopSelf();

					} else {
						// inform clients
						for (Listener l : listeners) {
							l.onHitCheckpoint(hitCheckpoint);
						}
					}
				}
			} else { // unknown
				for (Listener l : listeners) {
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
			request.setInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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
		ChaseTrailService getService() {
			return ChaseTrailService.this;
		}
	};

	// lists of registered callback listeners
	private final List<Listener> listeners = new ArrayList<Listener>();

	// current chase
	private ChaseInfo chaseInfo;

	// the trail we're chasing
	private Trail trail;

	// the next checkpoint we need to hit
	private Checkpoint nextCheckpoint;

	//
	private final Map<Checkpoint, Hit> hits = new HashMap<Checkpoint, Hit>();

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
		} catch (Exception ex) {
			Log.e("ChaseService", "error while laoding data", ex);
			return START_NOT_STICKY;
		}
	}

	/**
	 * Loads the data from the database
	 * 
	 * @param chaseId
	 * @throws IllegalArgumentException
	 */
	private void load(long chaseId) throws IllegalArgumentException {

		// get info about chase
		chaseInfo = ChaseInfo.fromId(this, chaseId);

		// load trail
		trail = Trail.fromId(this, chaseInfo.trail.id);

		// load hits
		hits.clear();
		Uri hitsUri = Contract.Hits.getUriDir(chaseId);
		Cursor hitCursor = getContentResolver().query(hitsUri, Contract.Hits.READ_PROJECTION, null, null, null);
		while (hitCursor.moveToNext()) {
			Hit hit = new Hit();

			hit.id = hitCursor.getLong(Contract.Hits.READ_PROJECTION_ID_INDEX);
			hit.checkpointId = hitCursor.getLong(Contract.Hits.READ_PROJECTION_CHECKPOINT_ID_INDEX);
			hit.time = hitCursor.getLong(Contract.Hits.READ_PROJECTION_TIME_INDEX);

			// look for checkpoint and add it to the map
			for (Checkpoint checkpoint : trail.checkpoints) {
				if (checkpoint.id == hit.checkpointId) {
					hits.put(checkpoint, hit);
					break;
				}
			}
		}
		hitCursor.close();

		// first checkpoint without a hit is our next one
		for (Checkpoint checkpoint : trail.checkpoints) {
			if (!hits.containsKey(checkpoint)) {
				nextCheckpoint = checkpoint;
				break;
			}
		}
	}

	/**
	 * Reloads the data of the chase
	 */
	public boolean reload() {

		// does a chase exist?
		if (chaseInfo != null) {
			// load data
			try {
				load(chaseInfo.id);
				return true;
			} catch (Exception ex) {
				return false;
			}
		} else {
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
	public ChaseInfo getChaseInfo() {
		return chaseInfo;
	}

	/**
	 * 
	 * @return
	 */
	public Location getLastKnownLocation() {
		if (locationClient != null && locationClient.isConnected()) {
			return locationClient.getLastLocation();
		}
		return null;
	}

	/**
	 * Returns the next checkpoint
	 * 
	 * @return
	 */
	public Checkpoint getNextCheckpoint() {
		return nextCheckpoint;
	}

	/**
	 * Returns the distance to the next checkpoint
	 * 
	 * @return distance in meters or Float.NaN if not available
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

	/**
	 * Return the checkpoints we're chasing
	 * 
	 * @return
	 */
	public Iterable<Checkpoint> getCheckpoints() {
		if (trail != null) {
			return trail.checkpoints;
		} else {
			return null;
		}
	}

	/**
	 * Returns if the specified checkpoint is hit
	 * 
	 * @param checkpoint
	 */
	public boolean isHit(Checkpoint checkpoint) {
		return hits.containsKey(checkpoint);
	}

}