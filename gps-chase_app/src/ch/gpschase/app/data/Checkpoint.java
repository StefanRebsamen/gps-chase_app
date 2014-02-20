package ch.gpschase.app.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;

/**
 * DTO for a checkpoint
 */
public class Checkpoint extends Item {
	
	public UUID uuid;
	
	private Trail trail;

	private int index;	// starting at 0
	
	public final Location location = new Location("gpschase");
	
	public boolean showLocation;
	
	public String hint;
	
	public int accuracy = 5;
	
	/**
	 * List of images
	 * ATTENTION: It's the user responsibility to populate!
	 */

	protected List<Image> images = null;

	/**
	 * Protected constructor
	 */
	protected Checkpoint(Trail trail) {

		uuid = UUID.randomUUID();
		
		this.trail = trail;		
		index = trail.checkpoints.size();
		trail.checkpoints.add(this);
	}
	
	/**
	 * Populates the DTO from the cursor
	 * @param context
	 * @param trail
	 */
	protected static void load(Context context, Trail trail) {
	
		Uri uri = Contract.Checkpoints.getUriDir(trail.getId());
		Cursor cursor = context.getContentResolver().query(uri, Contract.Checkpoints.READ_PROJECTION, 
															null, null, Contract.Checkpoints.DEFAULT_SORT_ORDER);
		while (cursor.moveToNext()) {		
			Checkpoint checkpoint = new Checkpoint(trail);
			checkpoint.setId(cursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX));
			checkpoint.uuid = UUID.fromString(cursor.getString(Contract.Checkpoints.READ_PROJECTION_UUID_INDEX));
			checkpoint.location.setLatitude(cursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LAT_INDEX));
			checkpoint.location.setLongitude(cursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LNG_INDEX));
			checkpoint.showLocation = cursor.getInt(Contract.Checkpoints.READ_PROJECTION_LOC_SHOW_INDEX) != 0;
			checkpoint.hint = cursor.getString(Contract.Checkpoints.READ_PROJECTION_HINT_INDEX);			
			checkpoint.accuracy = cursor.getInt(Contract.Checkpoints.READ_PROJECTION_ACCURACY_INDEX);
		}
		cursor.close();
	}
	
	/**
	 * Saves the DTO to the database
	 * @param context
	 */
	public void save(Context context) {
		ContentValues values = new ContentValues();
		values.put(Contract.Checkpoints.COLUMN_NAME_UUID, uuid.toString());
		values.put(Contract.Checkpoints.COLUMN_NAME_INDEX, index);
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_SHOW, showLocation ? 1 : 0);
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, location.getLongitude());
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, location.getLatitude());
		values.put(Contract.Checkpoints.COLUMN_NAME_HINT, hint);
		values.put(Contract.Checkpoints.COLUMN_NAME_ACCURACY, accuracy);
		if (getId() == 0) {
			Uri uri = context.getContentResolver().insert(Contract.Checkpoints.getUriDir(trail.getId()), values);
			setId(ContentUris.parseId(uri));
		}
		else {
			context.getContentResolver().update(Contract.Checkpoints.getUriId(getId()), values, null, null);
		}
	}

	/**
	 * Deletes the checkpoint from the database
	 * @param context
	 */
	public void delete(Context context) {
		
		// delete images to remove them from the image manager
		loadImages(context);
		List<Image> clonedList = new ArrayList<Image>(images);
		for (Image image: clonedList) {
			image.delete(context);
		}

		// delete in database
		context.getContentResolver().delete(Contract.Checkpoints.getUriId(getId()), null, null);
		
		// delete in trail
		trail.checkpoints.remove(this);

		// adjust indices in db
		for (Checkpoint checkpoint : trail.checkpoints) {
			if (checkpoint.index != trail.checkpoints.indexOf(checkpoint)) {
				checkpoint.index = trail.checkpoints.indexOf(checkpoint);
				checkpoint.save(context);
			}
		}				
	}

	/**
	 * Returns if checkpoint is the first
	 * @return
	 */
	public boolean isFirst() {
		return trail.checkpoints.indexOf(this) == 0;
	}
		
	/**
	 * Returns if checkpoint is the last
	 * @return
	 */
	public boolean isLast() {
		return trail.checkpoints.indexOf(this) == trail.checkpoints.size() - 1;		
	}
	
	/**
	 * Returns the index of the trail
	 * @return
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the trail the checkpoint belongs to
	 * @return
	 */
	public Trail getTrail() {
		return trail;
	}
	
	/**
	 * Returns the next checkpoint (if available)
	 * @return next checkpoint, otherwise 0
	 */
	public Checkpoint getNext() {
		int index = trail.checkpoints.indexOf(this);
		if (index < trail.checkpoints.size() - 1) {
			return trail.checkpoints.get(index+1);
		}
		else {
			return null;
		}
	}

	
	/**
	 * 
	 * @param context
	 */
	public boolean moveForward(Context context) {
	
		// calculate and check new index
		int newIndex = trail.checkpoints.indexOf(this) + 1;
		if ( newIndex >= trail.checkpoints.size()) {
			return false;
		}
		
		// move in list
		trail.checkpoints.remove(this);
		trail.checkpoints.add(newIndex, this);
				
		// adjust indices in db
		for (Checkpoint checkpoint : trail.checkpoints) {
			if (checkpoint.index != trail.checkpoints.indexOf(checkpoint)) {
				checkpoint.index = trail.checkpoints.indexOf(checkpoint);
				checkpoint.save(context);
			}
		}
		
		return true;
	}

	
	/**
	 * 
	 * @param context
	 */
	public boolean moveBackward(Context context) {
		// calculate and check new index
		int newIndex = trail.checkpoints.indexOf(this) - 1;
		if ( newIndex < 0) {
			return false;
		}
		
		// move in list
		trail.checkpoints.remove(this);
		trail.checkpoints.add(newIndex, this);
				
		// adjust indices in db
		for (Checkpoint checkpoint : trail.checkpoints) {
			if (checkpoint.index != trail.checkpoints.indexOf(checkpoint)) {
				checkpoint.index = trail.checkpoints.indexOf(checkpoint);
				checkpoint.save(context);
			}
		}
		
		return true;
	}

	/**
	 * Return an iterable of images
	 * @return
	 */
	public Iterable<Image> getImages() {
		if (images == null) {
			throw new IllegalStateException();
		}
		return images;
	}
	
	/**
	 * Adds a new image
	 * @param context
	 * @return
	 */
	public Image addImage() {
		if (images == null) {
			throw new IllegalStateException();
		}
		Image img = new Image(this);
		return img;
	}
	
	/**
	 * Loads the images (if not already done)
	 * @param context
	 */
	public void loadImages(Context context) {
		if (images != null) {
			return;
		}
		images = new LinkedList<Image>();
		Image.load(context, this);
	}
	
}