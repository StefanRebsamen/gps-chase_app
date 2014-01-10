package ch.gpschase.app.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class Trail extends TrailInfo {
	
	// list of checkpoints
	public final List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
	
	/**
	 * Returns if a trail is downloaded (not editable) 
	 * @param trailId
	 * @return True if is is downloaded
	 * @throws IllegalArgumentException
	 */
	public static boolean isDownloaded(Context context, long trailId) throws IllegalArgumentException {
		boolean result;
		Uri uri = Contract.Trails.getUriId(trailId);
		Cursor cursor = context.getContentResolver().query(uri, Contract.Trails.READ_PROJECTION, null, null, null);
		if (cursor.moveToNext()) {
			result = cursor.getLong(Contract.Trails.READ_PROJECTION_DOWNLOADED_INDEX) != 0;
			cursor.close();
			return result;
		}
		else {
			cursor.close();
			throw new IllegalArgumentException("Trail with Id " + trailId + " not found");
		}
	}
	
	/**
	 * Returns a trails name 
	 * @param trailId
	 * @return Name of the trails
	 * @throws IllegalArgumentException
	 */
	public static String getName(Context context, long trailId) throws IllegalArgumentException {
		String result;
		Uri uri = Contract.Trails.getUriId(trailId);
		Cursor cursor = context.getContentResolver().query(uri, Contract.Trails.READ_PROJECTION, null, null, null);
		if (cursor.moveToNext()) {
			result = cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX);
			cursor.close();
			return result;
		}
		else {
			cursor.close();
			throw new IllegalArgumentException("Trail with Id " + trailId + " not found");
		}
	}

}
