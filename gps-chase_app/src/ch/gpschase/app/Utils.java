package ch.gpschase.app;

/**
 * 
 */
public class Utils {

	
	public static String formatDuration(long duration) {
		
		if (duration < 0) {
			return "";
		}

		// format into into seconds, minutes, hours
		duration /= 1000;
		long seconds = duration % 60;
		long minutes = duration / 60 % 60;
		long hours = duration / 3600;
		String str = "";
		if (hours > 0) {
			str = String.format("%02d:%02d:%02d", hours,
					minutes, seconds);
		} else {
			str = String.format("%02d:%02d", minutes,
					seconds);
		}

		return str;
	}
	
}
