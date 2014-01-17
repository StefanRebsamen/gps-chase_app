package ch.gpschase.app.data;

import java.util.UUID;

import android.content.Context;
import android.database.Cursor;

/**
 * Represents a trail without it's checkpoints details
 */
public class TrailInfo {
	
	public long id = 0;
	
	public UUID uuid;	
	
	public String name;
	
	public String description;

	public long updated;
	
	public long downloaded;
	
	public long uploaded;
	
	public String password;

	
	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static TrailInfo fromId(Context context, long id) {
		
		Cursor cursor = context.getContentResolver().query(Contract.Trails.getUriId(id), 
															Contract.Trails.READ_PROJECTION, null, null, null);
		if (cursor.moveToNext()) {
			TrailInfo info = fromCursor(cursor);
			cursor.close();
			return info;
		}
		else {
			cursor.close();
			throw new IllegalArgumentException("Trail with id " + id + " not found");
		}
	}

	
	/**
	 * 
	 * @param cursor
	 * @return
	 */
	public static TrailInfo fromCursor(Cursor cursor) {

		TrailInfo info = new TrailInfo();
		
		info.id = cursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX);
		info.uuid = UUID.fromString(cursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX));
		info.name = cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX);
		info.description = cursor.getString(Contract.Trails.READ_PROJECTION_DESCRIPTION_INDEX);
		info.updated = cursor.getLong(Contract.Trails.READ_PROJECTION_UPDATED_INDEX);
		info.downloaded = cursor.getLong(Contract.Trails.READ_PROJECTION_DOWNLOADED_INDEX);		
		info.uploaded = cursor.getLong(Contract.Trails.READ_PROJECTION_UPLOADED_INDEX);		

		return info;
	}
}