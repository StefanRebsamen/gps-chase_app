package ch.gpschase.app.util;

import android.content.Context;
import ch.gpschase.app.ChaseTrailActivity;
import ch.gpschase.app.data.Chase;
import ch.gpschase.app.data.Trail;

public abstract class TrailActions {

	/**
	 * 
	 * @param trailId
	 */
	public static void chaseTrail(Context context, Trail trail) {
	
		// find a running chase
		Chase runningChase = trail.getFirstRunningChase(context);
		if (runningChase != null) {
			// continue
			ChaseTrailActivity.show(context, runningChase);
		} else {
			// create a new one
			new ChaseCreator(context).show(trail);
		}
	}

	/**
	 * Upload a trail to the server and share it afterwards in an
	 * asynchronous task
	 */
	public static void shareTrail(Context context, Trail trail) {
	
		// execute task
		new UploadTask(context, trail, true).execute();
	}

}
