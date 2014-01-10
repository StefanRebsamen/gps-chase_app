package ch.gpschase.app.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.location.Location;

/**
 * A checkpoint
 */
public class Checkpoint {
	public UUID uuid;
	public long id;
	public String hint;
	
	public final Location location = new Location("gpschase");
	
	public boolean showLocation;
	public final List<Image> images = new ArrayList<Image>();
}