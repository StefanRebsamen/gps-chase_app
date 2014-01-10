package ch.gpschase.app.data;

import java.util.UUID;

/**
 * Info about a trail
 */
public class TrailInfo {
	public UUID uuid;
	public long id = 0; // 0 if not locally existant
	public long updated;
	public String name;
	public String description;
}