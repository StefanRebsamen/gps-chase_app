package ch.gpschase.app;

import java.util.Date;

import ch.gpschase.app.data.ImageFileManager;
import android.app.Application;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Debug;
import android.util.TypedValue;

/**
 * 
 */
public class App extends Application {

	// sensible place to declare a log tag for the application
	public static final String LOG_TAG = "myapp";

	// instance
	private static App instance = null;

	// keep references to our global resources
	private static ImageFileManager imageManager = null;

	// used to format data and time
	private static java.text.DateFormat dateFormat;
	private static java.text.DateFormat timeFormat;

	/**
	 * Returns the ImageManager instance
	 */
	public static ImageFileManager getImageManager() {
		if (imageManager == null) {
			if (instance == null)
				throw new IllegalStateException("Application not created yet!");
			imageManager = new ImageFileManager(instance);
		}
		return imageManager;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// provide an instance for our static accessors
		instance = this;

		dateFormat = android.text.format.DateFormat.getDateFormat(this);
		timeFormat = android.text.format.DateFormat.getTimeFormat(this);

	}

	/**
	 * Return if the app runs in the debugger
	 * 
	 * @return True if is is debuggable
	 */
	public static boolean isDebuggable() {
		if (instance == null)
			throw new IllegalStateException("Application not created yet!");
		return Debug.isDebuggerConnected();
	}

	public static String formatDate(long timestamp) {
		if (timestamp > 0) {
			Date dateTime = new Date(timestamp);
			return dateFormat.format(dateTime);
		} else {
			return "";
		}
	}

	public static String formatTime(long timestamp) {
		if (timestamp > 0) {
			Date dateTime = new Date(timestamp);
			return timeFormat.format(dateTime);
		} else {
			return "";
		}
	}

	public static String formatDateTime(long timestamp) {
		if (timestamp > 0) {
			Date dateTime = new Date(timestamp);
			return dateFormat.format(dateTime) + " " + timeFormat.format(dateTime);
		} else {
			return "";
		}
	}

	public static String formatDuration(long duration) {
		if (duration > 0) {
			// format into into seconds, minutes, hours
			duration /= 1000;
			long seconds = duration % 60;
			long minutes = duration / 60 % 60;
			long hours = duration / 3600;
			String str = "";
			if (hours > 0) {
				str = String.format("%02d:%02d:%02d", hours, minutes, seconds);
			} else {
				str = String.format("%02d:%02d", minutes, seconds);
			}
			return str;
		} else {
			return "";
		}
	}

	/**
	 * Converts device independent pixels to pixels
	 */
	public static float convertToPx(int dp) {
		Resources r = instance.getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
		return px;
	}
	
	/**
	 * Website URL of the application
	 */
	public final static Uri URL = Uri.parse("http://www.gpschase.ch");  
}