package ch.gpschase.app.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class Trail {
	
	//
	public TrailInfo info; 
	
	// list of checkpoints
	public final List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();

	
	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static Trail fromId(Context context, long id) {
		
		TrailInfo info = TrailInfo.fromId(context, id);
		return fromInfo(context, info);
	}
	
	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static Trail fromInfo(Context context, TrailInfo info) {
		
		Trail trail = new Trail();
		
		// assign the info directly
		trail.info = info;
		
		// load checkpoints
		Uri checkpointsUri = Contract.Checkpoints.getUriDir(info.id);
		Cursor checkpointsCursor = context.getContentResolver().query(checkpointsUri, Contract.Checkpoints.READ_PROJECTION, null, null, Contract.Checkpoints.DEFAULT_SORT_ORDER);
		while (checkpointsCursor.moveToNext()) {
			Checkpoint checkpoint = Checkpoint.fromCursor(context, checkpointsCursor);
			trail.checkpoints.add(checkpoint);
			checkpoint.index = trail.checkpoints.size()-1;
		}	
		checkpointsCursor.close();		
		
		return trail;		
	}	
	
	
}
