package ch.gpschase.app.data;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the GPSchase content provider and its clients.
 */
public final class Contract {

	/**
	 * Authority for this provider
	 */
	public static final String AUTHORITY = "ch.gpschase.app.data.Trails";

	// the scheme part for this provider's URI
	private static final String SCHEME = "content";
	
		
	/**
	 * Constants for table trails
	 */
	public static final class Trails implements BaseColumns {
		
		/**
		 * The table name offered by this provider
		 */
		public static final String TABLE_NAME = "trails";

		private static final String PATH_DIR = "trails";		
		private static final String PATH_ID = "trail";
		
		/**
		 * @return Uri for listening records
		 */
		public static String getUriPatternDir() {
			return PATH_DIR;
		}
	
		/**
		 * @return	Uri for a acessing single record
		 */
		public static String getUriPatternId() {
			return PATH_ID + "/#";
		}
		
		/**
		 * @return	UriMatcher pattern for listening record
		 */
		public static Uri getUriDir() {
			return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_DIR).build();
		}
				
		/**
		 * @return	UriMatcher pattern for accessing a single record
		 */
		public static Uri getUriId(long Id) {
			return ContentUris.withAppendedId(new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_ID).build(), Id) ;
		}
		
		/*
		 * MIME type definitions
		 */

		/**
		 * The MIME type of {@link #URI_DIR} providing a directory of trails
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.gpschase.trail";

		/**
		 * The MIME type of a {@link #URI_DIR} sub-directory of a single trail.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.gpschase.trail";


		/*
		 * Column definitions
		 */

		/**
		 * Column name for the UUID of the trail
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_UUID = "uuid";
		
		/**
		 * Timestamp when the trail was last updated
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String COLUMN_NAME_UPDATED = "updated";

		/**
		 * Timestamp when the trail was last uploaded
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String COLUMN_NAME_UPLOADED = "uploaded";
		
		/**
		 * Timestamp when the trail was last downloaded
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String COLUMN_NAME_DOWNLOADED = "downloaded";
		
		/**
		 * Column name for the name of the trail
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_NAME = "name";

		/**
		 * Column name of the trail description
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_DESCRIPTION = "description";

		/**
		 * Password to edit the trail (MD5-Hash)
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_PASSWORD = "password";
				
		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = _ID + " DESC";

		
		/**
		 * Standard projection
		 */
		public static final String[] READ_PROJECTION = new String[] { 
			_ID,						//
			COLUMN_NAME_UUID,			//
			COLUMN_NAME_UPDATED,		//
			COLUMN_NAME_UPLOADED,		//
			COLUMN_NAME_DOWNLOADED,		//
			COLUMN_NAME_NAME,			//
			COLUMN_NAME_DESCRIPTION,	// 
			COLUMN_NAME_PASSWORD		//
		};

		public static final int READ_PROJECTION_ID_INDEX = 0;
		public static final int READ_PROJECTION_UUID_INDEX = 1;
		public static final int READ_PROJECTION_UPDATED_INDEX = 2;
		public static final int READ_PROJECTION_UPLOADED_INDEX = 3;
		public static final int READ_PROJECTION_DOWNLOADED_INDEX = 4;
		public static final int READ_PROJECTION_NAME_INDEX = 5;
		public static final int READ_PROJECTION_DESCRIPTION_INDEX = 6;
		public static final int READ_PROJECTION_PASSWORD_INDEX = 7;				

	}	

	/**
	 * Constants for table checkpoints
	 */
	public static final class Checkpoints implements BaseColumns {

		/**
		 * The table name offered by this provider
		 */
		public static final String TABLE_NAME = "checkpoints";

		private static final String PATH_DIR = "checkpoints";		
		private static final String PATH_ID = "checkpoint";

		
		/**
		 * @return Uri for listening records
		 */
		public static String getUriPatternDir() {
			return Trails.getUriPatternId() + "/" + PATH_DIR;
		}
	
		/**
		 * @return	Uri for a acessing single record
		 */
		public static String getUriPatternId() {
			return PATH_ID + "/#";
		}
		
		/**
		 * @return	UriMatcher pattern for listening record
		 */
		public static Uri getUriDir(long trailId) {
			return Trails.getUriId(trailId).buildUpon().appendPath(PATH_DIR).build();
		}
				
		/**
		 * @return	UriMatcher pattern for accessing a single record
		 */
		public static Uri getUriId(long Id) {
			return ContentUris.withAppendedId(new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_ID).build(), Id) ;
		}
		
		/*
		 * MIME type definitions
		 */

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of checkpoints
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.gpschase.checkpoint";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single checkpoint.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.gpschase.checkpoint";


		/*
		 * Column definitions
		 */

		/**
		 * Column name for the UUID of the checkpoint
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_UUID = "uuid";
		
		/**
		 * Column name for reference to the trail the checkpoint belongs to
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_TRAIL_ID = "trail_id";
		
		/**
		 * Column name for the number of the checkpoint
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_NO = "no";

		/**
		 * Column name of the checkpoint hint
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_HINT = "hint";

		
		/**
		 * Column name of the checkpoint location longitude
		 * <P>
		 * Type: REAL
		 * </P>
		 */
		public static final String COLUMN_NAME_LOC_LNG = "loc_lng";

		/**
		 * Column name of the checkpoint location latitude
		 * <P>
		 * Type: REAL
		 * </P>
		 */
		public static final String COLUMN_NAME_LOC_LAT = "loc_lat";

		/**
		 * Column name of the checkpoint location latitude
		 * <P>
		 * Type: REAL
		 * </P>
		 */
		public static final String COLUMN_NAME_LOC_SHOW = "loc_show";
		
		
		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = COLUMN_NAME_NO + " ASC";
		
		
		/**
		 * Standard projection
		 */
		public static final String[] READ_PROJECTION = new String[] { 
			_ID,					//
			COLUMN_NAME_UUID,		//
			COLUMN_NAME_NO,			//
			COLUMN_NAME_LOC_LNG,	// 
			COLUMN_NAME_LOC_LAT,	// 
			COLUMN_NAME_LOC_SHOW,	// 
			COLUMN_NAME_HINT		//
		};

		public static final int READ_PROJECTION_ID_INDEX = 0;
		public static final int READ_PROJECTION_UUID_INDEX = 1;
		public static final int READ_PROJECTION_NO_INDEX = 2;
		public static final int READ_PROJECTION_LOC_LNG_INDEX = 3;
		public static final int READ_PROJECTION_LOC_LAT_INDEX = 4;
		public static final int READ_PROJECTION_LOC_SHOW_INDEX = 5;
		public static final int READ_PROJECTION_HINT_INDEX = 6;
		
	}
	
	
	/**
	 * Constants for table trails
	 */
	public static final class Images implements BaseColumns {

		/**
		 * The table name offered by this provider
		 */
		public static final String TABLE_NAME = "images";

		private static final String PATH_DIR = "images";		
		private static final String PATH_ID = "image";
		
		/**
		 * @return Uri for listening records
		 */
		public static String getUriPatternDir() {
			return Checkpoints.getUriPatternId() + "/" + PATH_DIR;
		}
	
		/**
		 * @return	Uri for a acessing single record
		 */
		public static String getUriPatternId() {
			return PATH_ID + "/#";
		}
		
		/**
		 * @return	UriMatcher pattern for listening record
		 */
		public static Uri getUriDir(long checkpoinId) {
			return Checkpoints.getUriId(checkpoinId).buildUpon().appendPath(PATH_DIR).build();
		}
				
		/**
		 * @return	UriMatcher pattern for accessing a single record
		 */
		public static Uri getUriId(long Id) {
			return ContentUris.withAppendedId(new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_ID).build(), Id) ;
		}
		
		/*
		 * MIME type definitions
		 */

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of images
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.gpschase.image";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single image.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.gpschase.image";


		/*
		 * Column definitions
		 */

		/**
		 * Column name for the UUID of the image
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_UUID = "uuid";
		
		/**
		 * Column name for reference to the checkpoint the image belongs to
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_CHECKPOINT_ID = "checkpoint_id";
		
		/**
		 * Column name for the number of the image within its container
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_NO = "no";

		/**
		 * Column name of the image description
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_DESCRIPTION = "description";

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = _ID + " ASC," +  COLUMN_NAME_NO + " ASC";
		
		
		/**
		 * Standard projection
		 */
		public static final String[] READ_PROJECTION = new String[] { 
			_ID,							//
			COLUMN_NAME_UUID,				//
			COLUMN_NAME_CHECKPOINT_ID,		//
			COLUMN_NAME_NO,					//
			COLUMN_NAME_DESCRIPTION,		//
		};

		public static final int READ_PROJECTION_ID_INDEX = 0;
		public static final int READ_PROJECTION_UUID_INDEX = 1;
		public static final int READ_PROJECTION_NO_INDEX = 2;
		public static final int READ_PROJECTION_DESCRIPTION_INDEX = 3;
			
	}
	
	
	/**
	 * Constants for table chases
	 */
	public static final class Chases implements BaseColumns {

		/**
		 * The table name offered by this provider
		 */
		public static final String TABLE_NAME = "chases";

		private static final String PATH_DIR = "chases";		
		private static final String PATH_ID = "chase";
		private static final String PATH_DIR_EX = "chasesEx";		
		private static final String PATH_ID_EX = "chaseEx";
		
		/**
		 * @return Uri for listening records
		 */
		public static String getUriPatternDir() {
			return PATH_DIR;
		}
	
		/**
		 * @return	Uri for a acessing single record
		 */
		public static String getUriPatternId() {
			return PATH_ID + "/#";
		}
		
		/**
		 * @return	UriMatcher pattern for listening record
		 */
		public static Uri getUriDir() {
			return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_DIR).build();
		}
				
		/**
		 * @return	UriMatcher pattern for accessing a single record
		 */
		public static Uri getUriId(long Id) {
			return ContentUris.withAppendedId(new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_ID).build(), Id);
		}

		/**
		 * @return Uri for listening records, extended with columns from the trails table
		 */
		public static String getUriPatternDirEx() {
			return PATH_DIR_EX;
		}
	
		/**
		 * @return	Uri for a acessing single record, extended with columns from the trails table
		 */
		public static String getUriPatternIdEx() {
			return PATH_ID_EX + "/#";
		}
		
		/**
		 * @return	UriMatcher pattern for listening record, extended with columns from the trails table
		 */
		public static Uri getUriDirEx() {
			return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_DIR_EX).build();
		}
				
		/**
		 * @return	UriMatcher pattern for accessing a single record, extended with columns from the trails table
		 */
		public static Uri getUriIdEx(long Id) {
			return ContentUris.withAppendedId(new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_ID_EX).build(), Id);
		}
		
		/*
		 * MIME type definitions
		 */

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of chases
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.gpschase.chase";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single chase.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.gpschase.chase";


		/*
		 * Column definitions
		 */
		
		/**
		 * Column name for reference to the trail the chase is made for
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_TRAIL_ID = "trail_id";
		
		/**
		 * Column name for the player who's doing the chase
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_PLAYER = "player";

		/**
		 * Column name for the time the chase was started
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_STARTED = "started";

		/**
		 * Column name for the time the chase was finished
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_FINISHED = "finished";
				
		
		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = TABLE_NAME+"."+_ID + " DESC";
		
				
		/**
		 * Standard projection for extended query
		 */
		public static final String[] READ_PROJECTION_EX = new String[] { 
			_ID,								// 
			COLUMN_NAME_TRAIL_ID,				//
			COLUMN_NAME_PLAYER,					//	
			COLUMN_NAME_STARTED,				// 
			COLUMN_NAME_FINISHED,				// 
			Trails.COLUMN_NAME_UUID,			//
			Trails.COLUMN_NAME_NAME,			//
			Trails.COLUMN_NAME_DESCRIPTION,		// 
			Trails.COLUMN_NAME_UPDATED,			// 
			Trails.COLUMN_NAME_DOWNLOADED		// 
		};

		public static final int READ_PROJECTION_EX_ID_INDEX = 0;
		public static final int READ_PROJECTION_EX_TRAIL_ID_INDEX = 1;
		public static final int READ_PROJECTION_EX_PLAYER_INDEX = 2;
		public static final int READ_PROJECTION_EX_STARTED_INDEX = 3;
		public static final int READ_PROJECTION_EX_FINISHED_INDEX = 4;
		public static final int READ_PROJECTION_EX_TRAIL_UUID_INDEX = 5;
		public static final int READ_PROJECTION_EX_TRAIL_NAME_INDEX = 6;
		public static final int READ_PROJECTION_EX_TRAIL_DESCRIPTION_INDEX = 7;		
		public static final int READ_PROJECTION_EX_TRAIL_UPDATED_INDEX = 8;		
		public static final int READ_PROJECTION_EX_TRAIL_DOWNLOADED_INDEX = 9;		
	}	

	
	/**
	 * Constants for table hits
	 */
	public static final class Hits implements BaseColumns {

		/**
		 * The table name offered by this provider
		 */
		public static final String TABLE_NAME = "hits";

		private static final String PATH_DIR = "hits";		
		private static final String PATH_ID = "hit";
		
		/**
		 * @return Uri for listening records
		 */
		public static String getUriPatternDir() {
			return Chases.getUriPatternId() + "/" + PATH_DIR;
		}
	
		/**
		 * @return	Uri for a acessing single record
		 */
		public static String getUriPatternId() {
			return PATH_ID + "/#";
		}
		
		/**
		 * @return	UriMatcher pattern for listening record
		 */
		public static Uri getUriDir(long chaseId) {
			return Chases.getUriId(chaseId).buildUpon().appendPath(PATH_DIR).build();
		}
				
		/**
		 * @return	UriMatcher pattern for accessing a single record
		 */
		public static Uri getUriId(long Id) {
			return ContentUris.withAppendedId(new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(PATH_ID).build(), Id) ;
		}
		
		/*
		 * MIME type definitions
		 */

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of chases
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.gpschase.hit";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single chase.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.gpschase.hit";


		/*
		 * Column definitions
		 */
		
		/**
		 * Column name for reference to the chase the checkpoint was reached for
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_CHASE_ID = "chase_id";
		

		/**
		 * Column name for reference to the checkpoint that was reached
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_CHECKPOINT_ID = "checkpoint_id";
		
		/**
		 * Column name for the time the checkpoint was reached
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COLUMN_NAME_TIME = "time";

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = _ID + " ASC";
		
		
		/**
		 * Standard projection
		 */
		public static final String[] READ_PROJECTION = new String[] { 
			_ID,								// 
			COLUMN_NAME_CHASE_ID,				//
			COLUMN_NAME_CHECKPOINT_ID,			// 
			COLUMN_NAME_TIME					// 
		};

		public static final int READ_PROJECTION_ID_INDEX = 0;
		public static final int READ_PROJECTION_CHASE_ID_INDEX = 1;
		public static final int READ_PROJECTION_CHECKPOINT_ID_INDEX = 2;
		public static final int READ_PROJECTION_TIME_INDEX = 3;		
	}
	
}
