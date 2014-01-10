/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.gpschase.app.data;

import java.util.HashMap;
import java.util.UUID;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to a database of trails.
 */
public class Provider extends ContentProvider {

	/**
	 * The database that the provider uses as its underlying data store
	 */
	private static final String DATABASE_NAME = "gps_chase.db";

	/**
	 * The database version
	 */
	private static final int DATABASE_VERSION = 6;

	/**
	 * A projection map used to select columns from trails table
	 */
	private static HashMap<String, String> trailsProjectionMap;
	
	/**
	 * A projection map used to select columns from checkpoints table
	 */
	private static HashMap<String, String> checkpointsProjectionMap;

	/**
	 * A projection map used to select columns from hits table
	 */
	private static HashMap<String, String> imagesProjectionMap;

	/**
	 * A projection map used to select columns from chases table
	 */
	private static HashMap<String, String> chasesProjectionMap;

	/**
	 * A projection map used to select columns from chases table, joined with the trails table
	 */
	private static HashMap<String, String> chasesExProjectionMap;
	
	/**
	 * A projection map used to select columns from hits table
	 */
	private static HashMap<String, String> hitsProjectionMap;
	
	/*
	 * Constants used by the Uri matcher to choose an action based on the
	 * pattern of the incoming URI
	 */

	// the incoming URI matches the trails URI pattern
	private static final int URI_TRAIL_DIR = 10;

	// the incoming URI matches the trail ID URI pattern
	private static final int URI_TRAIL_ID = 11;

	// the incoming URI matches the checkpoints URI pattern
	private static final int URI_CHECKPOINT_DIR = 20;

	// the incoming URI matches the checkpoint ID URI pattern
	private static final int URI_CHECKPOINT_ID = 21;

	// the incoming URI matches the images URI pattern
	private static final int URI_IMAGE_DIR = 30;

	// the incoming URI matches the image ID URI pattern
	private static final int URI_IMAGE_ID = 31;

	// the incoming URI matches the chases URI pattern
	private static final int URI_CHASE_DIR = 100;

	// the incoming URI matches the chase ID URI pattern
	private static final int URI_CHASE_ID = 101;

	// the incoming URI matches the chases URI pattern
	private static final int URI_CHASE_EX_DIR = 102;

	// the incoming URI matches the chase ID URI pattern
	private static final int URI_CHASE_EX_ID = 103;	
	
	// the incoming URI matches the hits URI pattern
	private static final int URI_HIT_DIR = 110;

	// the incoming URI matches the hit ID URI pattern
	private static final int URI_HIT_ID = 111;
	
	// URI matcher instance
	private static final UriMatcher uriMatcher;

	// handle to DatabaseHelper.
	private DatabaseHelper dbHelper;

	/**
	 * A block that instantiates and sets static objects
	 */
	static {

		// Create and initialize URI matcher
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Trails.getUriPatternDir(), URI_TRAIL_DIR);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Trails.getUriPatternId(), URI_TRAIL_ID);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Checkpoints.getUriPatternDir(), URI_CHECKPOINT_DIR);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Checkpoints.getUriPatternId(), URI_CHECKPOINT_ID);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Images.getUriPatternDir(), URI_IMAGE_DIR);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Images.getUriPatternId(), URI_IMAGE_ID);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Chases.getUriPatternDir(), URI_CHASE_DIR);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Chases.getUriPatternId(), URI_CHASE_ID);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Chases.getUriPatternDirEx(), URI_CHASE_EX_DIR);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Chases.getUriPatternIdEx(), URI_CHASE_EX_ID);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Hits.getUriPatternDir(), URI_HIT_DIR);
		uriMatcher.addURI(Contract.AUTHORITY, Contract.Hits.getUriPatternId(), URI_HIT_ID);
		
		// create and initialize projection map for table trails
		trailsProjectionMap = new HashMap<String, String>();
		trailsProjectionMap.put(Contract.Trails._ID, Contract.Trails._ID);
		trailsProjectionMap.put(Contract.Trails.COLUMN_NAME_UUID, Contract.Trails.COLUMN_NAME_UUID);
		trailsProjectionMap.put(Contract.Trails.COLUMN_NAME_UPDATED, Contract.Trails.COLUMN_NAME_UPDATED);
		trailsProjectionMap.put(Contract.Trails.COLUMN_NAME_UPLOADED, Contract.Trails.COLUMN_NAME_UPLOADED);
		trailsProjectionMap.put(Contract.Trails.COLUMN_NAME_DOWNLOADED, Contract.Trails.COLUMN_NAME_DOWNLOADED);
		trailsProjectionMap.put(Contract.Trails.COLUMN_NAME_NAME, Contract.Trails.COLUMN_NAME_NAME);
		trailsProjectionMap.put(Contract.Trails.COLUMN_NAME_DESCRIPTION, Contract.Trails.COLUMN_NAME_DESCRIPTION);
		trailsProjectionMap.put(Contract.Trails.COLUMN_NAME_PASSWORD, Contract.Trails.COLUMN_NAME_PASSWORD);

		// create and initialize projection map for table checkpoints
		checkpointsProjectionMap = new HashMap<String, String>();
		checkpointsProjectionMap.put(Contract.Checkpoints._ID, Contract.Checkpoints._ID);
		checkpointsProjectionMap.put(Contract.Checkpoints.COLUMN_NAME_UUID, Contract.Checkpoints.COLUMN_NAME_UUID);
		checkpointsProjectionMap.put(Contract.Checkpoints.COLUMN_NAME_NO, Contract.Checkpoints.COLUMN_NAME_NO);
		checkpointsProjectionMap.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, Contract.Checkpoints.COLUMN_NAME_LOC_LAT);
		checkpointsProjectionMap.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, Contract.Checkpoints.COLUMN_NAME_LOC_LNG);
		checkpointsProjectionMap.put(Contract.Checkpoints.COLUMN_NAME_LOC_SHOW, Contract.Checkpoints.COLUMN_NAME_LOC_SHOW);
		checkpointsProjectionMap.put(Contract.Checkpoints.COLUMN_NAME_HINT, Contract.Checkpoints.COLUMN_NAME_HINT);
	
		// create and initialize projection map for table images
		imagesProjectionMap = new HashMap<String, String>();
		imagesProjectionMap.put(Contract.Images._ID, Contract.Images._ID);
		imagesProjectionMap.put(Contract.Images.COLUMN_NAME_UUID, Contract.Images.COLUMN_NAME_UUID);
		imagesProjectionMap.put(Contract.Images.COLUMN_NAME_CHECKPOINT_ID, Contract.Images.COLUMN_NAME_CHECKPOINT_ID);
		imagesProjectionMap.put(Contract.Images.COLUMN_NAME_NO, Contract.Images.COLUMN_NAME_NO);
		imagesProjectionMap.put(Contract.Images.COLUMN_NAME_DESCRIPTION, Contract.Images.COLUMN_NAME_DESCRIPTION);

		// create and initialize projection map for table chases
		chasesProjectionMap = new HashMap<String, String>();
		chasesProjectionMap.put(Contract.Chases._ID, Contract.Chases._ID);
		chasesProjectionMap.put(Contract.Chases.COLUMN_NAME_TRAIL_ID, Contract.Chases.COLUMN_NAME_TRAIL_ID);
		chasesProjectionMap.put(Contract.Chases.COLUMN_NAME_PLAYER, Contract.Chases.COLUMN_NAME_PLAYER);
		chasesProjectionMap.put(Contract.Chases.COLUMN_NAME_STARTED, Contract.Chases.COLUMN_NAME_STARTED);
		chasesProjectionMap.put(Contract.Chases.COLUMN_NAME_FINISHED, Contract.Chases.COLUMN_NAME_FINISHED);

		// create and initialize projection map for table chases joined with table trails
		chasesExProjectionMap = new HashMap<String, String>();
		chasesExProjectionMap.put(Contract.Chases._ID, Contract.Chases.TABLE_NAME+"."+Contract.Chases._ID);
		chasesExProjectionMap.put(Contract.Chases.COLUMN_NAME_TRAIL_ID, Contract.Chases.COLUMN_NAME_TRAIL_ID);
		chasesExProjectionMap.put(Contract.Chases.COLUMN_NAME_PLAYER, Contract.Chases.COLUMN_NAME_PLAYER);
		chasesExProjectionMap.put(Contract.Chases.COLUMN_NAME_STARTED, Contract.Chases.COLUMN_NAME_STARTED);
		chasesExProjectionMap.put(Contract.Chases.COLUMN_NAME_FINISHED, Contract.Chases.COLUMN_NAME_FINISHED);
		chasesExProjectionMap.put(Contract.Trails.COLUMN_NAME_UUID, Contract.Trails.COLUMN_NAME_UUID);
		chasesExProjectionMap.put(Contract.Trails.COLUMN_NAME_NAME, Contract.Trails.COLUMN_NAME_NAME);
		chasesExProjectionMap.put(Contract.Trails.COLUMN_NAME_DESCRIPTION, Contract.Trails.COLUMN_NAME_DESCRIPTION);
		chasesExProjectionMap.put(Contract.Trails.COLUMN_NAME_UPDATED, Contract.Trails.COLUMN_NAME_UPDATED);
		chasesExProjectionMap.put(Contract.Trails.COLUMN_NAME_DOWNLOADED, Contract.Trails.COLUMN_NAME_DOWNLOADED);
		
		// create an initialize projection map for table hits
		hitsProjectionMap = new HashMap<String, String>();
		hitsProjectionMap.put(Contract.Hits._ID, Contract.Chases._ID);
		hitsProjectionMap.put(Contract.Hits.COLUMN_NAME_CHASE_ID, Contract.Hits.COLUMN_NAME_CHASE_ID);
		hitsProjectionMap.put(Contract.Hits.COLUMN_NAME_CHECKPOINT_ID, Contract.Hits.COLUMN_NAME_CHECKPOINT_ID);
		hitsProjectionMap.put(Contract.Hits.COLUMN_NAME_TIME, Contract.Hits.COLUMN_NAME_TIME);
	}

	/**
	 * This class helps open, create, and upgrade the database file. Set to
	 * package visibility for testing purposes.
	 */
	static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + Contract.Trails.TABLE_NAME										// 
					+ " (" 																				//
					+ Contract.Trails._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"						//
					+ Contract.Trails.COLUMN_NAME_UUID + " TEXT NOT NULL,"								//
					+ Contract.Trails.COLUMN_NAME_UPDATED + " INTEGER NOT NULL DEFAULT 0,"				//
					+ Contract.Trails.COLUMN_NAME_UPLOADED + " INTEGER NOT NULL DEFAULT 0,"				//
					+ Contract.Trails.COLUMN_NAME_DOWNLOADED + " INTEGER NOT NULL DEFAULT 0,"			//
					+ Contract.Trails.COLUMN_NAME_NAME + " TEXT	NOT NULL,"								//
					+ Contract.Trails.COLUMN_NAME_DESCRIPTION + " TEXT NULL,"							//
					+ Contract.Trails.COLUMN_NAME_PASSWORD + " TEXT	NULL"								//
					+ ");");

			db.execSQL("CREATE TABLE " + Contract.Checkpoints.TABLE_NAME								// 
					+ " (" 																				//
					+ Contract.Checkpoints._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"					//
					+ Contract.Checkpoints.COLUMN_NAME_UUID + " TEXT NULL,"								//
					+ Contract.Checkpoints.COLUMN_NAME_TRAIL_ID + " INTEGER NOT NULL," 					//
					+ Contract.Checkpoints.COLUMN_NAME_NO + " INTEGER NOT NULL,"						//
					+ Contract.Checkpoints.COLUMN_NAME_HINT + " TEXT NULL,"								//
					+ Contract.Checkpoints.COLUMN_NAME_LOC_LNG + " REAL	NOT NULL,"						//
					+ Contract.Checkpoints.COLUMN_NAME_LOC_LAT + " REAL	NOT NULL," 						//
					+ Contract.Checkpoints.COLUMN_NAME_LOC_SHOW + " INTEGER	NOT NULL,"					//
					+ "FOREIGN KEY( " + Contract.Checkpoints.COLUMN_NAME_TRAIL_ID + ") "				//
					+ "  REFERENCES " + Contract.Trails.TABLE_NAME + "(" + Contract.Trails._ID + ") "	//
					+ "  ON DELETE CASCADE ON UPDATE CASCADE"
					+ ");");

			db.execSQL("CREATE TABLE " + Contract.Images.TABLE_NAME										// 
					+ " (" 																				//
					+ Contract.Images._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"						//
					+ Contract.Images.COLUMN_NAME_UUID + " TEXT NULL,"									//
					+ Contract.Images.COLUMN_NAME_CHECKPOINT_ID + " INTEGER	NOT NULL,"					//
					+ Contract.Images.COLUMN_NAME_NO + " INTEGER NOT NULL  DEFAULT 0,"					//
					+ Contract.Images.COLUMN_NAME_DESCRIPTION + " TEXT NULL,"							//
					+ "FOREIGN KEY( " + Contract.Images.COLUMN_NAME_CHECKPOINT_ID + ") "						//
					+ "  REFERENCES " + Contract.Checkpoints.TABLE_NAME + "(" + Contract.Checkpoints._ID + ") "	//
					+ "  ON DELETE CASCADE ON UPDATE CASCADE"
					+ ");");

			db.execSQL("CREATE TABLE " + Contract.Chases.TABLE_NAME										// 
					+ " (" 																				//
					+ Contract.Chases._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"						//
					+ Contract.Chases.COLUMN_NAME_TRAIL_ID + " INTEGER NOT NULL,"						//
					+ Contract.Chases.COLUMN_NAME_PLAYER + " TEXT NULL,"								//
					+ Contract.Chases.COLUMN_NAME_STARTED + " INTEGER NOT NULL,"						//
					+ Contract.Chases.COLUMN_NAME_FINISHED + " INTEGER NULL,"							//
					+ "FOREIGN KEY( " + Contract.Chases.COLUMN_NAME_TRAIL_ID + ") "						//
					+ "  REFERENCES " + Contract.Trails.TABLE_NAME + "(" + Contract.Trails._ID + ") "	//
					+ "  ON DELETE CASCADE ON UPDATE CASCADE"
					+ ");");

			db.execSQL("CREATE TABLE " + Contract.Hits.TABLE_NAME										// 
					+ " (" 																				//
					+ Contract.Hits._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"							//
					+ Contract.Hits.COLUMN_NAME_CHASE_ID + " INTEGER NOT NULL,"							//
					+ Contract.Hits.COLUMN_NAME_CHECKPOINT_ID + " INTEGER NOT NULL," 					//
					+ Contract.Hits.COLUMN_NAME_TIME + " INTEGER NOT NULL,"								//
					+ "FOREIGN KEY( " + Contract.Hits.COLUMN_NAME_CHASE_ID + ") "									//
					+ "  REFERENCES " + Contract.Chases.TABLE_NAME + "(" + Contract.Chases._ID + ") "				//
					+ "  ON DELETE CASCADE ON UPDATE CASCADE,"														//
					+ "FOREIGN KEY( " + Contract.Hits.COLUMN_NAME_CHECKPOINT_ID + ") "								//
					+ "  REFERENCES " + Contract.Checkpoints.TABLE_NAME + "(" + Contract.Checkpoints._ID + ") "		//
					+ "  ON DELETE CASCADE ON UPDATE CASCADE"														//
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//			db.execSQL("ALTER TABLE " + Contract.Trails.TABLE_NAME										// 
//					+ " ADD COLUMN " + Contract.Trails.COLUMN_NAME_SHARED + " INTEGER NULL;");			
		}
				
		@Override
		public void onConfigure(SQLiteDatabase db) {
		 if (!db.isReadOnly()) {
			    // enable foreign key constraints
			    db.execSQL("PRAGMA foreign_keys=ON;");
		  }
		}
	}

	@Override
	public boolean onCreate() {
		// create database helper
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		// constructs a new query builder and sets its table name
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String orderBy;
		
		Log.d("Provider::query", uri.toString());
		
		// choose table name, projection and sort order and adjust the "where" clause based on URI
		switch (uriMatcher.match(uri)) {
		case URI_TRAIL_DIR:
			qb.setTables(Contract.Trails.TABLE_NAME);
			qb.setProjectionMap(trailsProjectionMap);
			orderBy = !TextUtils.isEmpty(sortOrder) ? sortOrder : Contract.Trails.DEFAULT_SORT_ORDER;
			break;

		case URI_TRAIL_ID:
			qb.setTables(Contract.Trails.TABLE_NAME);
			qb.setProjectionMap(trailsProjectionMap);
			qb.appendWhere(Contract.Trails._ID + "=" + uri.getPathSegments().get(1));
			orderBy = null;
			break;

		case URI_CHECKPOINT_DIR:
			qb.setTables(Contract.Checkpoints.TABLE_NAME);
			qb.setProjectionMap(checkpointsProjectionMap);
			qb.appendWhere(Contract.Checkpoints.COLUMN_NAME_TRAIL_ID + "=" + uri.getPathSegments().get(1));
			orderBy = !TextUtils.isEmpty(sortOrder) ? sortOrder : Contract.Checkpoints.DEFAULT_SORT_ORDER;
			break;

		case URI_CHECKPOINT_ID:
			qb.setTables(Contract.Checkpoints.TABLE_NAME);
			qb.setProjectionMap(checkpointsProjectionMap);
			qb.appendWhere(Contract.Checkpoints._ID + "=" + uri.getPathSegments().get(1));
			orderBy = null;
			break;
			
		case URI_IMAGE_DIR:
			qb.setTables(Contract.Images.TABLE_NAME);
			qb.setProjectionMap(imagesProjectionMap);
			qb.appendWhere(Contract.Images.COLUMN_NAME_CHECKPOINT_ID + "=" + uri.getPathSegments().get(1));
			orderBy = !TextUtils.isEmpty(sortOrder) ? sortOrder : Contract.Images.DEFAULT_SORT_ORDER;
			break;

		case URI_IMAGE_ID:
			qb.setTables(Contract.Images.TABLE_NAME);
			qb.setProjectionMap(imagesProjectionMap);
			qb.appendWhere(Contract.Images._ID + "=" + uri.getPathSegments().get(1));
			orderBy = null;
			break;

		case URI_CHASE_DIR:
			qb.setTables(Contract.Chases.TABLE_NAME);
			qb.setProjectionMap(chasesProjectionMap);
			orderBy = !TextUtils.isEmpty(sortOrder) ? sortOrder : Contract.Chases.DEFAULT_SORT_ORDER;
			break;

		case URI_CHASE_ID:
			qb.setTables(Contract.Chases.TABLE_NAME);
			qb.setProjectionMap(chasesProjectionMap);
			qb.appendWhere(Contract.Chases._ID + "=" + uri.getPathSegments().get(1));
			orderBy = null;
			break;

		case URI_CHASE_EX_DIR:
			qb.setTables(Contract.Chases.TABLE_NAME + " JOIN " + Contract.Trails.TABLE_NAME + " ON (" +Contract.Chases.TABLE_NAME +"." + Contract.Chases.COLUMN_NAME_TRAIL_ID + "=" + Contract.Trails.TABLE_NAME + "." + Contract.Trails._ID + ")");
			qb.setProjectionMap(chasesExProjectionMap);
			orderBy = !TextUtils.isEmpty(sortOrder) ? sortOrder : Contract.Chases.DEFAULT_SORT_ORDER;
			break;

		case URI_CHASE_EX_ID:
			qb.setTables(Contract.Chases.TABLE_NAME + " JOIN " + Contract.Trails.TABLE_NAME + " ON (" +Contract.Chases.TABLE_NAME +"." + Contract.Chases.COLUMN_NAME_TRAIL_ID + "=" + Contract.Trails.TABLE_NAME + "." + Contract.Trails._ID + ")");
			qb.setProjectionMap(chasesExProjectionMap);
			qb.appendWhere(Contract.Chases.TABLE_NAME + "." + Contract.Chases._ID + "=" + uri.getPathSegments().get(1));
			orderBy = null;
			break;
			
		case URI_HIT_DIR:
			qb.setTables(Contract.Hits.TABLE_NAME);
			qb.setProjectionMap(hitsProjectionMap);
			qb.appendWhere(Contract.Hits.COLUMN_NAME_CHASE_ID + "=" + uri.getPathSegments().get(1));
			orderBy = !TextUtils.isEmpty(sortOrder) ? sortOrder : Contract.Hits.DEFAULT_SORT_ORDER;
			break;

		case URI_HIT_ID:
			qb.setTables(Contract.Hits.TABLE_NAME);
			qb.setProjectionMap(hitsProjectionMap);
			qb.appendWhere(Contract.Hits._ID + "=" + uri.getPathSegments().get(1));
			orderBy = null;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// perform query
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// tells the cursor what URI to watch
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {

		switch (uriMatcher.match(uri)) {
		case URI_TRAIL_DIR:
			return Contract.Trails.CONTENT_TYPE;

		case URI_TRAIL_ID:
			return Contract.Trails.CONTENT_ITEM_TYPE;

		case URI_CHECKPOINT_DIR:
			return Contract.Checkpoints.CONTENT_TYPE;

		case URI_CHECKPOINT_ID:
			return Contract.Checkpoints.CONTENT_ITEM_TYPE;

		case URI_IMAGE_DIR:
			return Contract.Images.CONTENT_TYPE;

		case URI_IMAGE_ID:
			return Contract.Images.CONTENT_ITEM_TYPE;

		case URI_CHASE_DIR:
			return Contract.Chases.CONTENT_TYPE;

		case URI_CHASE_ID:
			return Contract.Chases.CONTENT_ITEM_TYPE;

		case URI_HIT_DIR:
			return Contract.Hits.CONTENT_TYPE;

		case URI_HIT_ID:
			return Contract.Hits.CONTENT_ITEM_TYPE;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

		Log.d("Provider::insert", uri.toString());
		
		// create map for values to insert
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
			
		String tableName;
		long parentId;
		
		// set tableName, base URI and default values based on URI
		switch (uriMatcher.match(uri)) {		
			case URI_TRAIL_DIR:
				tableName =  Contract.Trails.TABLE_NAME;
				// set name if none is set
				if (values.containsKey(Contract.Trails.COLUMN_NAME_NAME) == false) {
					Resources r = Resources.getSystem();
					values.put(Contract.Trails.COLUMN_NAME_NAME, r.getString(android.R.string.untitled));
				}
				// create a new UUID if none is set
				if (TextUtils.isEmpty(values.getAsString(Contract.Trails.COLUMN_NAME_UUID)) )
				{
					values.put(Contract.Trails.COLUMN_NAME_UUID, UUID.randomUUID().toString());
				}
				break;
				
			case URI_CHECKPOINT_DIR:
				tableName =  Contract.Checkpoints.TABLE_NAME;
				parentId = Long.parseLong(uri.getPathSegments().get(1));				
				values.put(Contract.Checkpoints.COLUMN_NAME_TRAIL_ID, parentId);
				// create a new UUID if none is set
				if (TextUtils.isEmpty(values.getAsString(Contract.Checkpoints.COLUMN_NAME_UUID)) )
				{
					values.put(Contract.Checkpoints.COLUMN_NAME_UUID, UUID.randomUUID().toString());
				}
				break;

			case URI_IMAGE_DIR:
				tableName =  Contract.Images.TABLE_NAME;				
				parentId = Long.parseLong(uri.getPathSegments().get(1));				
				values.put(Contract.Images.COLUMN_NAME_CHECKPOINT_ID, parentId);
				// create a new UUID if none is set
				if (TextUtils.isEmpty(values.getAsString(Contract.Images.COLUMN_NAME_UUID)) )
				{
					values.put(Contract.Images.COLUMN_NAME_UUID, UUID.randomUUID().toString());
				}
				break;

			case URI_CHASE_DIR:
				tableName =  Contract.Chases.TABLE_NAME;
				break;

			case URI_HIT_DIR:
				tableName =  Contract.Hits.TABLE_NAME;				
				parentId = Long.parseLong(uri.getPathSegments().get(1));				
				values.put(Contract.Hits.COLUMN_NAME_CHASE_ID, parentId);
				break;
				
			default: 
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		// perform the insert
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(tableName, null, values);

		if (rowId > 0) {
			// creates a URI for new record
			Uri recordUri;
			switch (uriMatcher.match(uri)) {		
			case URI_TRAIL_DIR:
				recordUri = Contract.Trails.getUriId(rowId);
				break;
				
			case URI_CHECKPOINT_DIR:
				recordUri = Contract.Checkpoints.getUriId(rowId);
				break;

			case URI_IMAGE_DIR:
				recordUri = Contract.Images.getUriId(rowId);
				break;

			case URI_CHASE_DIR:
				recordUri = Contract.Chases.getUriId(rowId);
				break;

			case URI_HIT_DIR:
				recordUri = Contract.Hits.getUriId(rowId);
				break;
				
			default: 
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
			
			// notify about change
			getContext().getContentResolver().notifyChange(uri, null);
			return recordUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {

		Log.d("Provider::delete", uri.toString());
		
		String finalWhere;
		String tableName;
		
		// modify table name and where clause based on URI
		switch (uriMatcher.match(uri)) {
		case URI_TRAIL_ID:
			tableName = Contract.Trails.TABLE_NAME;
			finalWhere = Contract.Trails._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;

		case URI_CHECKPOINT_ID:
			tableName = Contract.Checkpoints.TABLE_NAME;
			finalWhere = Contract.Checkpoints._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;

		case URI_IMAGE_ID:
			tableName = Contract.Images.TABLE_NAME;
			finalWhere = Contract.Images._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;

		case URI_CHASE_ID:
			tableName = Contract.Chases.TABLE_NAME;
			finalWhere = Contract.Chases._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// perform the delete based on the where clause
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = db.delete(tableName, finalWhere, whereArgs);

		// notify about change
		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

		Log.d("Provider::update", uri.toString());
		
		String finalWhere;
		String tableName;
		
		// modify table name and where clause based on URI
		switch (uriMatcher.match(uri)) {

		case URI_TRAIL_ID:
			tableName = Contract.Trails.TABLE_NAME;
			finalWhere = Contract.Trails._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;

		case URI_CHECKPOINT_ID:
			tableName = Contract.Checkpoints.TABLE_NAME;
			finalWhere = Contract.Checkpoints._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;

		case URI_IMAGE_ID:
			tableName = Contract.Images.TABLE_NAME;
			finalWhere = Contract.Images._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;

		case URI_CHASE_ID:
			tableName = Contract.Chases.TABLE_NAME;
			finalWhere = Contract.Chases._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;
			
		case URI_HIT_ID:
			tableName = Contract.Hits.TABLE_NAME;
			finalWhere = Contract.Hits._ID + " = " + uri.getPathSegments().get(1);
			if (where != null) {
				finalWhere = finalWhere + " AND " + where;
			}
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// perform the delete based on the where clause
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = db.update(tableName, values, finalWhere, whereArgs);

		// notify about change
		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	
	
	
}