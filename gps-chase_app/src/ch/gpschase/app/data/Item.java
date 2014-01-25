package ch.gpschase.app.data;

/**
 * Base class for all DTO that have an id
 */
public abstract class  Item {
	
	private long id;

	public long getId() {
		return id;
	}

	protected void setId(long id) {
		this.id = id;
	}

}
