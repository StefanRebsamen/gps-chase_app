package ch.gpschase.app.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;

/**
 * A checkpoint
 */
public class Checkpoint {
	public long id;
	
	public UUID uuid;

	public int index;	// starting at 0
	
	public final Location location = new Location("gpschase");
	
	public boolean showLocation;
	
	public String hint;
	
	public final List<Image> images = new ArrayList<Image>();
	
	/**
	 * 
	 * @param cursor
	 * @return
	 */
	public static Checkpoint fromCursor(Context context, Cursor cursor) {

		Checkpoint checkpoint = new Checkpoint();
		checkpoint.id = cursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX);
		checkpoint.uuid = UUID.fromString(cursor.getString(Contract.Checkpoints.READ_PROJECTION_UUID_INDEX));
		checkpoint.location.setLatitude(cursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LAT_INDEX));
		checkpoint.location.setLongitude(cursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LNG_INDEX));
		checkpoint.showLocation = cursor.getInt(Contract.Checkpoints.READ_PROJECTION_LOC_SHOW_INDEX) != 0;
		checkpoint.hint = cursor.getString(Contract.Checkpoints.READ_PROJECTION_HINT_INDEX);

		// load images
		Uri imageDirUri = Contract.Images.getUriDir(checkpoint.id);		
		Cursor imageCursor = context.getContentResolver().query(imageDirUri, Contract.Images.READ_PROJECTION, null, null,
				Contract.Images.DEFAULT_SORT_ORDER);
		while (imageCursor.moveToNext()) {

			Image image = Image.fromCursor(imageCursor);
			checkpoint.images.add(image);
		}		
		
		return checkpoint;
	}
	
}