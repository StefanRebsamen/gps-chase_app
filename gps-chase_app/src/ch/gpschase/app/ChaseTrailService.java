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

import ch.gpschase.app.data.Chase;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Hit;
import ch.gpschase.app.data.Image;
import ch.gpschase.app.data.Trail;
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

		private static final int ACCURACY_DEBUG = 100;

		@Override
		public void onLocationChanged(Location location) {

			// next checkpoint set?
			if (currentCheckpoint != null && location != null) {

				// update clients with distance still remaining
				float distance = location.distanceTo(currentCheckpoint.location);
				for (Listener l : listeners) {
					l.onDistanceToCheckpointChanged(distance);
				}

				// did we hit a checkpoint?
				if (distance < (App.isDebuggable() ? ACCURACY_DEBUG : currentCheckpoint.accuracy)) {

					// create a new hit and save it
					Hit hit = chase.addHit(currentCheckpoint);
					hit.save(ChaseTrailService.this);
			
					// keep checkpoint we've hit
					Checkpoint hitCheckpoint = currentCheckpoint;

					// change to next checkpoint
					currentCheckpoint = currentCheckpoint.getNext();

					// finished if no more checkpoints left
					if (currentCheckpoint == null) {

						// take time from last hit
						chase.finished = hit.time;

						// save in DB						
						chase.save(ChaseTrailService.this);

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
			request.setInterval(2000);
			request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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
	private Chase chase;

	// the trail we're chasing
	private Trail trail;

	// the next checkpoint we need to hit
	private Checkpoint currentCheckpoint;

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

		// get chase id from intent extra
		long trailId = intent.getLongExtra(ChaseTrailActivity.INTENT_EXTRA_TRAILID, 0);
		long chaseId = intent.getLongExtra(ChaseTrailActivity.INTENT_EXTRA_CHASEID, 0);

		// load data
		try {
			// load chase
			load(trailId, chaseId);
			
			// mark it as started (if not already done)
			if (chase.started == 0) {
				chase.started = System.currentTimeMillis();
				chase.save(this);
			}
			
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
	private void load(long trailId, long chaseId) throws IllegalArgumentException {

		// load chase
		trail = Trail.load(this, trailId);
		trail.loadChases(this);
		for (Chase ch : trail.getChases()) {
			if (ch.getId() == chaseId) {
				chase = ch;
				break;
			}
		}

		// load checkpoints
		// the images will be loaded by the activity when necessary
		trail.loadCheckpoints(this);

		// determine next checkpoint we need to hit
		// it's the one after the last hit
		chase.loadHits(this);
		currentCheckpoint = null;
		for (Checkpoint checkpoint : trail.getCheckpoints()) {
			if (chase.isHit(checkpoint)) {
				currentCheckpoint = null;
			}
			else if (currentCheckpoint == null) {
				currentCheckpoint = checkpoint;				
			}
		}
	}

	/**
	 * Reloads the data of the chase
	 */
	public boolean reload() {

		// does a chase exist?
		if (chase != null) {
			// load data
			try {
				load(chase.getTrail().getId(),  chase.getId());
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
	public Chase getChase() {
		return chase;
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
	 * Returns the next checkpoint we need to reach
	 * @return
	 */
	public Checkpoint getCurrentCheckpoint() {
		return currentCheckpoint;
	}

	/**
	 * Returns the distance to the next checkpoint
	 * 
	 * @return distance in meters or Float.NaN if not available
	 */
	public float getDistanceToNextCheckpoint() {
		if (currentCheckpoint != null && locationClient.isConnected()) {
			Location lastLocation = locationClient.getLastLocation();
			if (lastLocation != null) {
				return lastLocation.distanceTo(currentCheckpoint.location);
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
			return trail.getCheckpoints();
		} else {
			return null;
		}
	}

}