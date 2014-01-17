package ch.gpschase.app.data;

import java.util.UUID;

import android.database.Cursor;
import android.net.Uri;

/**
 * An image
 */
public class Image {
	
	public long id;
	
	public UUID uuid;
	
	public String description;

	
	/**
	 * 
	 * @param cursor
	 * @return
	 */
	public static Image fromCursor(Cursor cursor) {

		Image image = new Image();
		image.id = cursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX);
		image.uuid = UUID.fromString(cursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX));
		image.description = cursor.getString(Contract.Images.READ_PROJECTION_DESCRIPTION_INDEX);

		return image;
	}	
}