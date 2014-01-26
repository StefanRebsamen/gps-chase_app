package ch.gpschase.app.data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * DTO for a chase
 */
public class Chase extends Item {
	
	private Trail trail;
	
	public String player;

	public long started;

	public long finished;

	protected Map<Checkpoint, Hit> hits;
	
	/**
	 * Public constructor
	 * @param Trail to chase
	 */
	protected Chase(Trail trail) {
		this.trail = trail;
	}
	

	/**
	 * Creates a new chase
	 * @param Trail to chase
	 */
	public static Chase create(Trail trail) {
		Chase chase = new Chase(trail);
		chase.hits = new HashMap<Checkpoint, Hit>();
		return chase;
	}
	
	/**
	 * Loads a DTO for the specified id
	 * @param context
	 * @param id
	 * @return
	 */
	public static Chase load(Context context, long id) {
		
		Cursor cursor = context.getContentResolver().query(Contract.Chases.getUriId(id), 
															Contract.Chases.READ_PROJECTION, null, null, null);
		if (cursor.moveToNext()) {
			Chase chase = load(context, cursor);
			cursor.close();
			return chase;
		}
		else {
			cursor.close();
			throw new IllegalArgumentException("Chase with id " + id + " not found");
		}
	}
	
	/**
	 * Loads a list of chases
	 * @param context
	 * @return
	 */
	public static List<Chase> list(Context context) {
		
		List<Chase> result = new LinkedList<Chase>();
		
		Cursor cursor = context.getContentResolver().query(Contract.Chases.getUriDir(), 
															Contract.Chases.READ_PROJECTION, 
															null, null, null);		
		while (cursor.moveToNext()) {
			result.add(Chase.load(context, cursor));
		}
		cursor.close();
		
		return result;
	}

	/**
	 * Populates the DTO from the cursor
	 * @param context
	 * @param cursor
	 * @return
	 */
	protected static Chase load(Context context, Cursor cursor) {
		
		long trailId = cursor.getLong(Contract.Chases.READ_PROJECTION_TRAIL_ID_INDEX);		
		Trail trail = Trail.load(context, trailId); // TODO optimize
		Chase chase = new Chase(trail);

		chase.setId(cursor.getLong(Contract.Chases.READ_PROJECTION_ID_INDEX));
		chase.player = cursor.getString(Contract.Chases.READ_PROJECTION_PLAYER_INDEX);
		chase.started = cursor.getLong(Contract.Chases.READ_PROJECTION_STARTED_INDEX);
		chase.finished = cursor.getLong(Contract.Chases.READ_PROJECTION_FINISHED_INDEX);
		
		return chase;
	}

	/**
	 * Saves the DTO to the database
	 * @param context
	 */
	public void save(Context context) {
		ContentValues values = new ContentValues();
		values.put(Contract.Chases.COLUMN_NAME_TRAIL_ID, getTrail().getId());
		values.put(Contract.Chases.COLUMN_NAME_PLAYER, player);
		values.put(Contract.Chases.COLUMN_NAME_STARTED, started);
		values.put(Contract.Chases.COLUMN_NAME_FINISHED, finished);
		
		if (getId() == 0) {
			Uri uri = context.getContentResolver().insert(Contract.Chases.getUriDir(), values);
			setId(ContentUris.parseId(uri));
		}
		else {
			context.getContentResolver().update(Contract.Chases.getUriId(getId()), values, null, null);
		}
	}	
	
	/**
	 * Deletes the chase from the database
	 * @param context
	 */
	public void delete(Context context) {
		context.getContentResolver().delete(Contract.Chases.getUriId(getId()), null, null);
	}

	/**
	 * Return the trail
	 * @return
	 */
	public Trail getTrail() {
		return trail;
	}

	/**
	 * Return an iterable of hits
	 * @return
	 */
	public Iterable<Hit> getHits() {
		if (hits == null) {
			throw new IllegalStateException();
		}
		return hits.values();
	}
	
	/**
	 * Adds a new hit
	 * @param context
	 * @return
	 */
	public Hit addHit(Checkpoint checkpoint) {
		if (hits == null) {
			throw new IllegalStateException();
		}
		return new Hit(this, checkpoint);
	}
	
	/**
	 * Loads the checkpoints (if not already done)
	 * @param context
	 */
	public void loadHits(Context context) {
		if (hits != null) {
			return;
		}
		// we need to access the trails
		trail.loadCheckpoints(context);
		
		hits = new HashMap<Checkpoint, Hit>();
		Hit.load(context, this);
	}

	
	/**
	 * Returns if the specified checkpoint is hit
	 * @param checkpoint
	 */
	public boolean isHit(Checkpoint checkpoint) {
		if (hits == null) {
			throw new IllegalStateException();
		}
		
		for (Map.Entry<Checkpoint, Hit> entry : hits.entrySet()) {
			Log.d("isHit", "Checkpoint " + entry.getKey().getId() + " : " + entry.getValue().getId());
		}
		
		return hits.containsKey(checkpoint);
	}
	
}