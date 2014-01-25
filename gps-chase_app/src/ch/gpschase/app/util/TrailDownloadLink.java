package ch.gpschase.app.util;

import java.util.List;
import java.util.UUID;

import ch.gpschase.app.data.Trail;
import android.net.Uri;


public class TrailDownloadLink {

	public static final String SCHEME = "http";
	public static final String AUTHORITY = "app.gpschase.ch";
	
	/**
	 * Represents the data encoded into a download link
	 */
	public static class DownloadData {
		public UUID trailUuid;		
	}
	
	/**
	 * Creates a download link from the passed data
	 * @param data
	 * @return link
	 */
	public static Uri createDownloadLink(Trail trail) {
		return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath("trail").appendPath(trail.uuid.toString()).build();		
	}
	

	/**
	 * Parses a download link 
	 * @param uri
	 * @return parsed data	
	 * @throws IllegalArgumentException if passed link isn't a valid download link
	 */
	public static DownloadData parseDownloadLink(Uri uri) throws IllegalArgumentException  
	{
		if (uri == null) {
			throw new IllegalArgumentException("Uri is Null");
		}
		if ( !uri.getScheme().equals(SCHEME)) {
			throw new IllegalArgumentException("Uri has wrong scheme");			
		}
		List<String> segments = uri.getPathSegments();
		if (segments.size() < 2) {
			throw new IllegalArgumentException();
		}
		
		DownloadData data = new DownloadData();
		data.trailUuid = UUID.fromString(segments.get(1));
		
		return data;
	}
	
	
}
