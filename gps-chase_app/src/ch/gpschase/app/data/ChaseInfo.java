package ch.gpschase.app.data;

import java.util.UUID;

import android.content.Context;
import android.database.Cursor;

/**
 * Info about a trail
 */
public class ChaseInfo {
	
	public long id = 0;
	
	public TrailInfo trail;
	
	public String player;

	public long started;

	public long finished;


	
	public static ChaseInfo fromId(Context context, long id) {
		
		Cursor cursor = context.getContentResolver().query(Contract.Chases.getUriIdEx(id), 
															Contract.Chases.READ_PROJECTION_EX, null, null, null);
		if (cursor.moveToNext()) {
			ChaseInfo info = fromCursor(context, cursor);
			cursor.close();
			return info;
		}
		else {
			cursor.close();
			throw new IllegalArgumentException("Chase with id " + id + " not found");
		}
	}
	
	
	public static ChaseInfo fromCursor(Context context, Cursor cursor) {

		ChaseInfo info = new ChaseInfo();
		
		info.id = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_ID_INDEX);
		info.player = cursor.getString(Contract.Chases.READ_PROJECTION_EX_PLAYER_INDEX);
		info.started = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_STARTED_INDEX);
		info.finished = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_FINISHED_INDEX);		

		long trailId = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_TRAIL_ID_INDEX);		
		info.trail = TrailInfo.fromId(context, trailId);
				
		return info;
	}
}