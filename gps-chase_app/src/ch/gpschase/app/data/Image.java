package ch.gpschase.app.data;

import java.util.UUID;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import ch.gpschase.app.App;

/**
 * DTO for an image
 */
public class Image extends Item {
	
	private Checkpoint checkpoint;
	
	private int index = 0;
	
	public UUID uuid;
	
	public String description;

	/**
	 * Protected constructor
	 */
	protected Image(Checkpoint checkpoint) {
		
		uuid = UUID.randomUUID();
		
		this.checkpoint = checkpoint;
		index = checkpoint.images.size();
		checkpoint.images.add(this);
	}
	
	/**
	 * Load the images for the specified checkpoint
	 * @param cursor
	 * @return
	 */
	public static void load(Context context, Checkpoint checkpoint) {
					
		Uri uri = Contract.Images.getUriDir(checkpoint.getId());		
		Cursor cursor = context.getContentResolver().query(uri, 
														Contract.Images.READ_PROJECTION, 
														null, null,
														Contract.Images.DEFAULT_SORT_ORDER);
		
		while (cursor.moveToNext()) {

			Image image = new Image(checkpoint);
			image.setId(cursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX));
			image.checkpoint = checkpoint;
			image.uuid = UUID.fromString(cursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX));
			image.description = cursor.getString(Contract.Images.READ_PROJECTION_DESCRIPTION_INDEX);
		}		
		cursor.close();
	}
	
	/**
	 * Save the DTO to the database
	 * @param context
	 */
	public void save(Context context) {
		ContentValues values = new ContentValues();
		values.put(Contract.Images.COLUMN_NAME_UUID, uuid.toString());
		values.put(Contract.Images.COLUMN_NAME_INDEX, index);
		values.put(Contract.Images.COLUMN_NAME_DESCRIPTION, description);
		if (getId() == 0) {
			Uri uri = context.getContentResolver().insert(Contract.Images.getUriDir(checkpoint.getId()), values);
			setId(ContentUris.parseId(uri));
		}
		else {
			context.getContentResolver().update(Contract.Images.getUriId(getId()), values, null, null);
		}
	}


	/**
	 * Deletes the checkpoint from the database
	 * @param context
	 */
	public void delete(Context context) {
		
		// delete in database
		context.getContentResolver().delete(Contract.Images.getUriId(getId()), null, null);
		
		// delete in checkpoint
		checkpoint.images.remove(this);

		// adjust indices in db
		for (Image image : checkpoint.images) {
			if (image.index != checkpoint.images.indexOf(image)) {
				image.index = checkpoint.images.indexOf(image);
				image.save(context);
			}
		}		

		// delete from image manager
		App.getImageManager().delete(this);
	}

	/**
	 * Returns if checkpoint is the first
	 * @return
	 */
	public boolean isFirst() {
		return checkpoint.images.indexOf(this) == 0;
	}
		
	/**
	 * Returns if checkpoint is the last
	 * @return
	 */
	public boolean isLast() {
		return checkpoint.images.indexOf(this) == checkpoint.images.size() - 1;		
	}
	
	/**
	 * Return the index of the trail
	 * @return
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the Checkpoint the image belongs to
	 * @return
	 */
	public Checkpoint getCheckpoint() {
		return checkpoint;
	}
		
	/**
	 * 
	 * @param context
	 */
	public boolean moveForward(Context context) {
	
		// calculate and check new index
		int newIndex = checkpoint.images.indexOf(this) + 1;
		if ( newIndex >= checkpoint.images.size()) {
			return false;
		}
		
		// move in list
		checkpoint.images.remove(this);
		checkpoint.images.add(newIndex, this);
				
		// adjust indices in db
		for (Image image : checkpoint.images) {
			if (image.index != checkpoint.images.indexOf(image)) {
				image.index = checkpoint.images.indexOf(image);
				image.save(context);
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
		int newIndex = checkpoint.images.indexOf(this) - 1;
		if ( newIndex < 0) {
			return false;
		}
		
		// move in list
		checkpoint.images.remove(this);
		checkpoint.images.add(newIndex, this);
				
		// adjust indices in db
		for (Image image : checkpoint.images) {
			if (image.index != checkpoint.images.indexOf(image)) {
				image.index = checkpoint.images.indexOf(image);
				image.save(context);
			}
		}		
		
		return true;
	}
		

	/**
	 * Return the id if an image with the given UUID exist
	 * @param cursor
	 * @return
	 */
	public static long exists(Context context, UUID imageUUID) {	
					
//		Uri uri = Contract.Images.getUriDir(checkpoint.getId());	// todo		
//		Cursor cursor = context.getContentResolver().query(uri, 
//														Contract.Images.READ_PROJECTION, 
//														null, null,
//														Contract.Images.DEFAULT_SORT_ORDER);
//		
//		long imageId = 0;
//		
//		if (cursor.moveToNext()) {
//			imageId = cursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX);
//		}		
//		cursor.close();
//		
//		return imageId;
		return 0;
	}
	
}