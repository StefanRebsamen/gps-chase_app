package ch.gpschase.app.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * DTO for a trail
 */
public class Trail extends Item {
		
	public UUID uuid;	
	
	public String name = "";
	
	public String description = "";

	public long updated = 0;
	
	public long downloaded = 0;
	
	public long uploaded = 0;
	
	public String password;
	
	public UUID token;
	
	/**
	 * List of checkpoints
	 * ATTENTION: It's the user responsibility to populate!
	 */
	protected List<Checkpoint> checkpoints = null;

	/**
	 * Protected constructor
	 */
	protected Trail() {
		
	}
	
	/**
	 * Creates a completely new trail
	 * @param Trail to chase
	 */
	public static Trail create() {
		
		Trail trail = new Trail();
		
		// already assign an empty checkpoint list
		trail.checkpoints = new LinkedList<Checkpoint>();
		
		// create an UUID and a token
		trail.uuid = UUID.randomUUID();
		trail.token = UUID.randomUUID();
		
		return trail;
	}

	/**
	 * Creates a new trail based on a passed UUID
	 * @param Trail to chase
	 */
	public static Trail loadOrCreate(Context context, UUID uuid) {
				
		Cursor cursor = context.getContentResolver().query(Contract.Trails.getUriDir(), 
				Contract.Trails.READ_PROJECTION, Contract.Trails.COLUMN_NAME_UUID + " = '" + uuid.toString() + "'",
				null, null);
		
		if (cursor.moveToNext()) {
			Trail trail = load(cursor);
			cursor.close();
			return trail;

		} else {
			cursor.close();
			Trail trail = new Trail();

			// already assign an empty checkpoint list
			trail.checkpoints = new LinkedList<Checkpoint>();

			// assign passed uuid
			trail.uuid = uuid;

			return trail;
		}
	}
	
	/**
	 * Load a list of trails from the database
	 * @param context
	 * @return
	 */
	public static List<Trail> list(Context context) {
	
		List<Trail> result = new LinkedList<Trail>();
		
		Cursor cursor = context.getContentResolver().query(Contract.Trails.getUriDir(), 
															Contract.Trails.READ_PROJECTION, 
															null, null, null);		
		while (cursor.moveToNext()) {
			result.add(Trail.load(cursor));
		}
		cursor.close();
		
		return result;
	}
	
	
	/**
	 * Loads a single DTO from the database
	 * @param context
	 * @param id
	 * @return
	 */
	public static Trail load(Context context, long id) {
		
		Cursor cursor = context.getContentResolver().query(Contract.Trails.getUriId(id), 
															Contract.Trails.READ_PROJECTION, null, null, null);
		if (cursor.moveToNext()) {
			Trail trail = load(cursor);
			cursor.close();
			return trail;
		}
		else {
			cursor.close();
			throw new IllegalArgumentException("Trail with id " + id + " not found");
		}
	}

	/**
	 * Populates the DTO from the cursor
	 * @param cursor
	 * @return
	 */
	protected static Trail load(Cursor cursor) {

		Trail trail = new Trail();
		
		trail.setId(cursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX));
		trail.uuid = UUID.fromString(cursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX));
		trail.name = cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX);
		trail.description = cursor.getString(Contract.Trails.READ_PROJECTION_DESCRIPTION_INDEX);
		trail.updated = cursor.getLong(Contract.Trails.READ_PROJECTION_UPDATED_INDEX);
		trail.downloaded = cursor.getLong(Contract.Trails.READ_PROJECTION_DOWNLOADED_INDEX);		
		trail.uploaded = cursor.getLong(Contract.Trails.READ_PROJECTION_UPLOADED_INDEX);
		if (!cursor.isNull(Contract.Trails.READ_PROJECTION_TOKEN_INDEX)) {
			trail.token = UUID.fromString(cursor.getString(Contract.Trails.READ_PROJECTION_TOKEN_INDEX));
		}
		else {
			trail.token = null;
		}

		return trail;
	}
	
	/**
	 * Populates the trail and all its children
	 */
	public void loadAll(Context context) {
		loadCheckpoints(context);
		for (Checkpoint checkpoint : checkpoints) {
			checkpoint.loadImages(context);
		}
	}
	
	/**
	 * Saves the DTO to the database
	 * @param context
	 */
	public void save(Context context) {
		
		ContentValues values = new ContentValues();
		values.put(Contract.Trails.COLUMN_NAME_UUID, uuid.toString());
		values.put(Contract.Trails.COLUMN_NAME_NAME, name);
		values.put(Contract.Trails.COLUMN_NAME_DESCRIPTION, description);
		values.put(Contract.Trails.COLUMN_NAME_UPDATED, updated);
		values.put(Contract.Trails.COLUMN_NAME_DOWNLOADED, downloaded);
		values.put(Contract.Trails.COLUMN_NAME_UPLOADED, uploaded);
		values.put(Contract.Trails.COLUMN_NAME_PASSWORD, password);
		values.put(Contract.Trails.COLUMN_NAME_TOKEN, token != null ? token.toString() : null);
		
		if (getId() == 0) {
			Uri uri = context.getContentResolver().insert(Contract.Trails.getUriDir(), values);
			setId(ContentUris.parseId(uri));
		}
		else {
			context.getContentResolver().update(Contract.Trails.getUriId(getId()), values, null, null);
		}
	}

	/**
	 * Deletes the DTO from the database
	 * @param context
	 */
	public void delete(Context context) {
		
		// delete images to remove them from the image manager
		loadCheckpoints(context);
		List<Checkpoint> clonedList = new ArrayList<Checkpoint>(checkpoints);
		for (Checkpoint checkpoint: clonedList) {
			checkpoint.delete(context);
		}
		
		context.getContentResolver().delete(Contract.Trails.getUriId(getId()), null, null);
	}

	/**
	 * Gets the first running chase for this trail
	 * @param context
	 * @return
	 */
	public Chase getFirstRunningChase(Context context) {
		long chaseId = 0;
		String selection = Contract.Chases.COLUMN_NAME_TRAIL_ID + "=" + getId()
							+ " AND " 											//
							+ Contract.Chases.COLUMN_NAME_FINISHED + "=0"; 		//
		Cursor chasesCursor =  context.getContentResolver().query(							//
													Contract.Chases.getUriDir(), 					//
													Contract.Chases.READ_PROJECTION,				//
													selection, 										//
													null, 											//
													null);											//
		if (chasesCursor.moveToNext()) {
			chaseId = chasesCursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX);
		}
		chasesCursor.close();						
		
		if (chaseId != 0) {
			return Chase.load(context, chaseId);
		}
		return null;
	}
	
	/**
	 * Return an iterable of checkpoints
	 * @return
	 */
	public Iterable<Checkpoint> getCheckpoints() {
		if (checkpoints == null) {
			throw new IllegalStateException();
		}
		return checkpoints;
	}
	
	/**
	 * Adds a new checkpoint
	 * @param context
	 * @return
	 */
	public Checkpoint addCheckpoint() {
		if (checkpoints == null) {
			throw new IllegalStateException();
		}
		return new Checkpoint(this);
	}
	
	/**
	 * Loads the checkpoints (if not already done)
	 * @param context
	 */
	public void loadCheckpoints(Context context) {
		if (checkpoints != null) {
			return;
		}
		
		checkpoints = new LinkedList<Checkpoint>();
		Checkpoint.load(context, this);
	}
	
	/**
	 * Returns if the specified trail is editable
	 */
	public boolean isEditable() {
		return this.token != null;
	}

	/**
	 * Returns if the specified trail was downloaded
	 */
	public boolean isDownloaded() {
		return this.downloaded != 0;
	}
	
	
}