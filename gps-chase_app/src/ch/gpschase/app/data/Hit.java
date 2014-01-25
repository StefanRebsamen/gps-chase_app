package ch.gpschase.app.data;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * DTO for a hit
 */
public class Hit extends Item {
	
	private Chase chase;
	
	private Checkpoint checkpoint;
	
	public long time;

		
	/**
	 * Protected constructor
	 */
	public Hit(Chase chase, Checkpoint checkpoint) {
		this.chase = chase;
		this.checkpoint = checkpoint;
		
		time = System.currentTimeMillis();
		
		chase.hits.put(checkpoint, this);
	}
	
	/**
	 * Loads the hits for the specified chase
	 * @param context
	 * @param chase
	 * @return
	 */
	protected static void load(Context context, Chase chase) {
		
		List<Hit> result = new LinkedList<Hit>();
		Uri uri = Contract.Hits.getUriDir(chase.getId());
		Cursor cursor = context.getContentResolver().query(uri, Contract.Hits.READ_PROJECTION, null, null, null);
		while (cursor.moveToNext()) {
			long checkpointId = cursor.getLong(Contract.Hits.READ_PROJECTION_CHECKPOINT_ID_INDEX);
			// find a checkpoint
			for (Checkpoint checkpoint : chase.getTrail().getCheckpoints() ) {
				if (checkpoint.getId() == checkpointId) {
					// add new instance to chasses hit map
					Hit hit = new Hit(chase, checkpoint);
					hit.setId(cursor.getLong(Contract.Hits.READ_PROJECTION_ID_INDEX));
					hit.time = cursor.getLong(Contract.Hits.READ_PROJECTION_TIME_INDEX);			
					result.add(hit);
					// next checkpoint
					break;
				}
			}			
		}
		cursor.close();
	}

	/**
	 * Saves the DTO to the database
	 * @param context
	 */
	public void save(Context context) {
		ContentValues values = new ContentValues();
		values.put(Contract.Hits.COLUMN_NAME_TIME, time);
		values.put(Contract.Hits.COLUMN_NAME_CHECKPOINT_ID, checkpoint.getId());
		if (getId() == 0) {
			Uri uri = context.getContentResolver().insert(Contract.Hits.getUriDir(chase.getId()), values);
			setId(ContentUris.parseId(uri));
		}
		else {
			context.getContentResolver().update(Contract.Hits.getUriId(getId()), values, null, null);
		}
	}
	
}