package ch.gpschase.app.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ProtocolException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import ch.gpschase.app.App;

/**
 * 
 */
public class Client {

	/** Field names for JSON **/
	private class Json {
		private static final String TRAIL_UUID = "uuid";
		private static final String TRAIL_UPDATED = "updated";
		private static final String TRAIL_UPLOADED = "uploaded";
		private static final String TRAIL_NAME = "name";
		private static final String TRAIL_DESCRIPTION = "descr";
		private static final String TRAIL_PASSWORD = "pwd";
		private static final String TRAIL_CHECKPOINTS = "chkpts";

		private static final String CHECKPOINT_UUID = "uuid";
		private static final String CHECKPOINT_LOCATION = "loc";
		private static final String CHECKPOINT_SHOW_ON_MAP = "loc_show";
		private static final String CHECKPOINT_HINT = "hint";
		private static final String CHECKPOINT_IMAGES = "images";

		private static final String IMAGE_UUID = "uuid";
		private static final String IMAGE_DESCRIPTION = "descr";
	}

	private final static Uri BASE_URI = Uri.parse("http://192.168.0.20:5000");

	/**
	 * 
	 */
	public class ClientException extends Exception {

		public ClientException(String message) {
			super(message);
		}

		public ClientException(String message, Exception ex) {
			super(message, ex);
		}
	}

	/**
	 * 
	 */
	public class TrailInfo {
		public UUID uuid;
		public long id = 0; // 0 if not locally existant
		public long updated;
		public long uploaded;
		public long downloaded;
		public String name;
		public String description;
	}
	
	/**
	 * 
	 */
	private class Image {
		long id;
		UUID uuid;
		String description;
	}	
	
	/**
	 * 
	 */
	private class Checkpoint {
		UUID uuid;
		long id;
		String hint;
		boolean showLocation;
		double locLng;
		double locLat;
		final List<Image> images = new ArrayList<Image>();
	}

	/**
	 * 
	 */
	private class Trail extends TrailInfo {
		final List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		public String password;		
	}	
		
	/**
	 * 
	 */
	private class UploadData {
		// id and UUID of the trail
		long id;
		UUID uuid;
		// trail as JSON
		public String json;
		// list of images
		public final List<Image> images = new ArrayList<Image>();
	}
	
	/**
	 * 
	 * @param trailUuid
	 * @return
	 */
	private Uri getTrailUri(UUID trailUuid) {
		return BASE_URI.buildUpon().appendPath("trail").appendPath(trailUuid.toString()).build();
	}

	/**
	 * 
	 * @param trailUuid
	 * @param imageUuid
	 * @return
	 */
	private Uri getImageUri(UUID trailUuid, UUID imageUuid) {
		return getTrailUri(trailUuid).buildUpon().appendPath("image").appendPath(imageUuid.toString()).build();
	}

	/**
	 * 
	 * @param trailUuid
	 * @return
	 */
	private Uri getTrailImagesUri(UUID trailUuid) {
		return BASE_URI.buildUpon().appendPath("trail").appendPath(trailUuid.toString()).appendPath("images") .build();
	}

	// context to use
	private Context context;

	/**
	 * Constructor
	 * 
	 * @param context
	 */
	public Client(Context context) {
		this.context = context;
	}

	/**
	 * @deprecated	Was used during development
	 * @param context
	 * @param trailId
	 */
	private void assignUUIDS(long trailId) {

		// update trail
		Uri trailUri = Contract.Trails.getUriId(trailId);
		Cursor trailCursor = context.getContentResolver().query(trailUri, Contract.Trails.READ_PROJECTION, null, null, null);
		if (!trailCursor.moveToFirst()) {
			throw new InvalidParameterException("Trail " + trailId + " not found");
		}
		if (TextUtils.isEmpty(trailCursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX))) {
			ContentValues values = new ContentValues();
			values.put(Contract.Trails.COLUMN_NAME_UUID, UUID.randomUUID().toString());
			context.getContentResolver().update(Contract.Trails.getUriId(trailId), values, null, null);
		}
		
		// update checkpoints
		Uri checkpointsUri = Contract.Checkpoints.getUriDir(trailId);
		Cursor checkpointsCursor = context.getContentResolver().query(checkpointsUri, Contract.Checkpoints.READ_PROJECTION, null, null,
				Contract.Checkpoints.DEFAULT_SORT_ORDER);
		while (checkpointsCursor.moveToNext()) {
			if (TextUtils.isEmpty(checkpointsCursor.getString(Contract.Checkpoints.READ_PROJECTION_UUID_INDEX))) {
				ContentValues values = new ContentValues();
				values.put(Contract.Checkpoints.COLUMN_NAME_UUID, UUID.randomUUID().toString());
				Uri checkpointIdUri = Contract.Checkpoints.getUriId(checkpointsCursor
						.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX));
				context.getContentResolver().update(checkpointIdUri, values, null, null);
			}
			
			// update images
			Uri imageDirUri = Contract.Images.getUriDir(checkpointsCursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX));
			Cursor imagesCursor = context.getContentResolver().query(imageDirUri, Contract.Images.READ_PROJECTION, null, null,
					Contract.Images.DEFAULT_SORT_ORDER);
			while (imagesCursor.moveToNext()) {
				if (TextUtils.isEmpty(imagesCursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX))) {
					ContentValues values = new ContentValues();
					values.put(Contract.Images.COLUMN_NAME_UUID, UUID.randomUUID().toString());
					Uri imageIdUri = Contract.Images.getUriId(imagesCursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX));
					context.getContentResolver().update(imageIdUri, values, null, null);
				}
			}
		}
		checkpointsCursor.close();
	}

	/**
	 * Goes through specified trail and collects the data to upload
	 * 
	 * @param context
	 * @param trailId
	 * @return
	 */
	private UploadData getUploadData(long trailId) throws IOException, InvalidParameterException {

		// make sure trail exists
		Uri trailUri = Contract.Trails.getUriId(trailId);
		Cursor trailCursor = context.getContentResolver().query(trailUri, Contract.Trails.READ_PROJECTION, null, null, null);
		if (!trailCursor.moveToFirst()) {
			throw new InvalidParameterException("Trail " + trailId + " not found");
		}

		UploadData data = new UploadData();

		StringWriter json = new StringWriter();
		JsonWriter writer = new JsonWriter(json);

		// trail details
		writer.beginObject();

		if (TextUtils.isEmpty(trailCursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX))) {
			ContentValues values = new ContentValues();
			values.put(Contract.Trails.COLUMN_NAME_UUID, UUID.randomUUID().toString());
			context.getContentResolver().update(Contract.Trails.getUriId(trailId), values, null, null);
		}

		writer.name(Json.TRAIL_UUID).value(trailCursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX));
		writer.name(Json.TRAIL_UPDATED).value(trailCursor.getLong(Contract.Trails.READ_PROJECTION_UPDATED_INDEX));
		writer.name(Json.TRAIL_UPLOADED).value(trailCursor.getLong(Contract.Trails.READ_PROJECTION_UPLOADED_INDEX));
		writer.name(Json.TRAIL_NAME).value(trailCursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX));
		writer.name(Json.TRAIL_DESCRIPTION).value(trailCursor.getString(Contract.Trails.READ_PROJECTION_DESCRIPTION_INDEX));
		writer.name(Json.TRAIL_PASSWORD).value(trailCursor.getString(Contract.Trails.READ_PROJECTION_PASSWORD_INDEX));

		data.id = trailCursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX);
		data.uuid = UUID.fromString(trailCursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX));

		trailCursor.close();

		// checkpoints
		Uri checkpointsUri = Contract.Checkpoints.getUriDir(trailId);
		Cursor checkpointsCursor = context.getContentResolver().query(checkpointsUri, Contract.Checkpoints.READ_PROJECTION, null, null,
				Contract.Checkpoints.DEFAULT_SORT_ORDER);
		if (checkpointsCursor.moveToFirst()) {
			writer.name(Json.TRAIL_CHECKPOINTS).beginArray();
			do {
				writer.beginObject();
				writer.name(Json.CHECKPOINT_UUID).value(checkpointsCursor.getString(Contract.Checkpoints.READ_PROJECTION_UUID_INDEX));
				writer.name(Json.CHECKPOINT_LOCATION).beginArray()	// lng, lat
						.value(checkpointsCursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LNG_INDEX))
						.value(checkpointsCursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LAT_INDEX)).endArray();
				writer.name(Json.CHECKPOINT_SHOW_ON_MAP).value(
						checkpointsCursor.getInt(Contract.Checkpoints.READ_PROJECTION_LOC_SHOW_INDEX) != 0);
				writer.name(Json.CHECKPOINT_HINT).value(checkpointsCursor.getString(Contract.Checkpoints.READ_PROJECTION_HINT_INDEX));

				// images
				Uri imageDirUri = Contract.Images.getUriDir(checkpointsCursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX));
				Cursor imagesCursor = context.getContentResolver().query(imageDirUri, Contract.Images.READ_PROJECTION, null, null,
						Contract.Images.DEFAULT_SORT_ORDER);
				if (imagesCursor.moveToFirst()) {
					writer.name(Json.CHECKPOINT_IMAGES).beginArray();
					do {
						writer.beginObject();
						writer.name(Json.IMAGE_UUID).value(imagesCursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX));
						writer.name(Json.IMAGE_DESCRIPTION)
								.value(imagesCursor.getString(Contract.Images.READ_PROJECTION_DESCRIPTION_INDEX));
						writer.endObject();

						// add it to the image list
						Image image = new Image();
						image.id = imagesCursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX);
						image.uuid = UUID.fromString(imagesCursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX));
						data.images.add(image);

					} while (imagesCursor.moveToNext());
					writer.endArray();
				}
				imagesCursor.close();
				writer.endObject();

			} while (checkpointsCursor.moveToNext());
			writer.endArray();

		}
		checkpointsCursor.close();
		writer.endObject();

		// create JSON string
		writer.close();
		data.json = json.toString();

		return data;

	}

	/**
	 * Uploads a trail to the server
	 * @param trailId
	 * @throws ClientException
	 */
	public UUID uploadTrail(long trailId) throws ClientException {

		try {
			HttpResponse response;
			int statusCode;
			
			// make sure everything has an UUID
			//assignUUIDS(trailId);

			// get data to publish
			UploadData data = getUploadData(trailId);

			// create HTTP client
			HttpClient http = new DefaultHttpClient();
						
			// retrieve a list of images on server
			String json = getJson(http, getTrailImagesUri(data.uuid));
			List<UUID> imagesOnServer = new ArrayList<UUID>();
			JsonReader reader = new JsonReader(new StringReader(json));
			reader.beginArray();
			while (reader.hasNext()) {
				reader.beginObject();
				TrailInfo trail = new TrailInfo();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals(Json.IMAGE_UUID)) {
						UUID imageUuid = UUID.fromString(reader.nextString());
						imagesOnServer.add(imageUuid);
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			}
			reader.endArray();
			reader.close();
						
			// upload images
			for (Image image : data.images) {
				boolean skip = false;
				// skip if already on server				
				for (UUID imageUuid : imagesOnServer)
				{
					if (imageUuid.equals(image.uuid)) {
						skip = true;
						imagesOnServer.remove(imageUuid);
						break;
					}
				}				
				if (!skip) {					
					HttpPut put = new HttpPut(getImageUri(data.uuid, image.uuid).toString());
					// upload full file
					File fullFile = App.getImageManager().getFullFile(image.id);
					if (fullFile.exists()) {
						put.setEntity(new FileEntity(fullFile, "image/jpeg"));
						response = http.execute(put);
						statusCode = response.getStatusLine().getStatusCode();
						if (statusCode != 200) {
							throw new ProtocolException("Server returned status code " + statusCode + " after uploading image");
						}
					}
				}								
			}

			// upload trail as JSON
			HttpPut put = new HttpPut(getTrailUri(data.uuid).toString());
			put.addHeader("Content-Type", "application/json");
			put.addHeader("Accept", "application/json");
			put.setEntity(new StringEntity(data.json, HTTP.UTF_8));
			response = http.execute(put);
			statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				throw new ProtocolException("Server returned status code " + statusCode + " after uploading trail JSON");
			}
			
			// delete remaining images on server
			for (UUID imageUuid : imagesOnServer) {
				HttpDelete delete = new HttpDelete(getImageUri(data.uuid, imageUuid).toString());
				response = http.execute(delete);
				statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
					throw new ProtocolException("Server returned status code " + statusCode + " after deleting image");
				}				
			}
			
			// write to DB that it was uploaded
			ContentValues values = new ContentValues();
			values.put(Contract.Trails.COLUMN_NAME_UPLOADED, System.currentTimeMillis());
			context.getContentResolver().update(Contract.Trails.getUriId(trailId), values, null, null);
			
			return data.uuid;
			
		} catch (Exception ex) {
			throw new ClientException("Error while exporting trail to server", ex);
		}
	}

	/**
	 * @param trailUuid
	 * @throws ClientException
	 */
	public long downloadTrail(UUID trailUuid) throws ClientException {
	
		try {
			// create HTTP client
			HttpClient http = new DefaultHttpClient();
			
			// get response from server as JSON
			String json = getJson(http, getTrailUri(trailUuid));
			
			/////////////////////////// 
			// parse JSON and create object tree of complete trail
			JsonReader reader = new JsonReader(new StringReader(json));
			reader.beginObject();
			Trail trail = new Trail();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals(Json.TRAIL_UUID)) {
					trail.uuid = UUID.fromString(reader.nextString());
				} else if (name.equals(Json.TRAIL_UPDATED)) {
					trail.updated = reader.nextLong();
				} else if (name.equals(Json.TRAIL_UPLOADED)) {
					trail.uploaded = reader.nextLong();
				} else if (name.equals(Json.TRAIL_NAME)) {
					trail.name = reader.nextString();
				} else if (name.equals(Json.TRAIL_DESCRIPTION)) {
					if (reader.peek() == JsonToken.STRING)
						trail.description = reader.nextString();
					else
						reader.skipValue();
				} else if (name.equals(Json.TRAIL_PASSWORD)) {
					if (reader.peek() == JsonToken.STRING)
						trail.password = reader.nextString();
					else
						reader.skipValue();
				} else if (name.equals(Json.TRAIL_CHECKPOINTS)) {
					// checkpoints
					reader.beginArray();
					while (reader.hasNext()) {
						reader.beginObject();
						Checkpoint checkpoint = new Checkpoint();
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals(Json.CHECKPOINT_UUID)) {
								checkpoint.uuid = UUID.fromString(reader.nextString());
							} else if (name.equals(Json.CHECKPOINT_SHOW_ON_MAP)) {
								checkpoint.showLocation = reader.nextBoolean();
							} else if (name.equals(Json.CHECKPOINT_HINT)) {
								if (reader.peek() == JsonToken.STRING)
									checkpoint.hint = reader.nextString();
								else
									reader.skipValue();
							} else if (name.equals(Json.CHECKPOINT_LOCATION)) {
								// location comes as an array (lng,lat)
								reader.beginArray();
								checkpoint.locLng = reader.nextDouble();
								checkpoint.locLat = reader.nextDouble();
								reader.endArray();
							} else if (name.equals(Json.CHECKPOINT_IMAGES)) {
								// images
								reader.beginArray();
								while (reader.hasNext()) {
									reader.beginObject();
									Image image = new Image();
									while (reader.hasNext()) {
										name = reader.nextName();
										if (name.equals(Json.IMAGE_UUID)) {
											image.uuid = UUID.fromString(reader.nextString());
										} else if (name.equals(Json.IMAGE_DESCRIPTION)) {
											if (reader.peek() == JsonToken.STRING)
												image.description = reader.nextString();
											else
												reader.skipValue();
										} else {
											reader.skipValue();
										}
									}
									reader.endObject();
									checkpoint.images.add(image);
								}
								reader.endArray();
							} else {
								reader.skipValue();
							}
						}
						reader.endObject();
						trail.checkpoints.add(checkpoint);
					}
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
			reader.close();

			/////////////////////////// 
			// save object tree to database
			ContentValues values = new ContentValues();
		
			/////////////////////////// 
			// update or insert trail
			values.put(Contract.Trails.COLUMN_NAME_UUID, trail.uuid.toString());
			values.put(Contract.Trails.COLUMN_NAME_UPDATED, trail.updated);
			values.put(Contract.Trails.COLUMN_NAME_UPLOADED, trail.uploaded);
			values.put(Contract.Trails.COLUMN_NAME_DOWNLOADED, System.currentTimeMillis());
			values.put(Contract.Trails.COLUMN_NAME_NAME, trail.name);
			values.put(Contract.Trails.COLUMN_NAME_DESCRIPTION, trail.description);
			values.put(Contract.Trails.COLUMN_NAME_PASSWORD, trail.password);

			Uri trailDirUri = Contract.Trails.getUriDir();
			Cursor trailCursor = context.getContentResolver().query(trailDirUri, Contract.Trails.READ_PROJECTION, 
																	Contract.Trails.COLUMN_NAME_UUID+"=?", 
																	new String[]{trailUuid.toString()}, 
																	null);
			if (trailCursor.moveToFirst()) {
				trail.id = trailCursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX);
				Uri trailUri = Contract.Trails.getUriId(trail.id); 
				context.getContentResolver().update(trailUri, values, null, null);
			}
			else {
				Uri trailUri = context.getContentResolver().insert(Contract.Trails.getUriDir(), values);
				trail.id = ContentUris.parseId(trailUri);
			}
			// close cursor
			trailCursor.close();			
			
			/////////////////////////// 
			// update or insert checkpoints			
			Uri checkpointDirUri = Contract.Checkpoints.getUriDir(trail.id);
			Cursor checkpointCursor = context.getContentResolver().query(checkpointDirUri, Contract.Checkpoints.READ_PROJECTION,																			null, null,	null);			
			for (Checkpoint checkpoint : trail.checkpoints) {

				// fill in values
				values.clear();
				values.put(Contract.Checkpoints.COLUMN_NAME_UUID, checkpoint.uuid.toString());
				values.put(Contract.Checkpoints.COLUMN_NAME_NO, trail.checkpoints.indexOf(checkpoint) + 1);
				values.put(Contract.Checkpoints.COLUMN_NAME_LOC_SHOW, checkpoint.showLocation ? 1 : 0);
				values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, checkpoint.locLng);
				values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, checkpoint.locLat);
				values.put(Contract.Checkpoints.COLUMN_NAME_HINT, checkpoint.hint);

				// see if checkpoints already exists
				if (checkpointCursor.moveToFirst()) {
					do {
						UUID uuid =  UUID.fromString(checkpointCursor.getString(Contract.Checkpoints.READ_PROJECTION_UUID_INDEX));
						if (checkpoint.uuid.equals(uuid)) {
							checkpoint.id = checkpointCursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX);
							break;
						}						
					} while (checkpointCursor.moveToNext());
				}
				if (checkpoint.id != 0) {
					// update
					Uri checkpointUri =  Contract.Checkpoints.getUriId(checkpoint.id);
					context.getContentResolver().update(checkpointUri, values, null, null);
				}
				else {
					// insert
					Uri checkpointUri = context.getContentResolver().insert(checkpointDirUri, values);
					checkpoint.id = ContentUris.parseId(checkpointUri);					
				}

				/////////////////////////// 
				// update or insert images for current checkpoint			
				Uri imageDirUri = Contract.Images.getUriDir(checkpoint.id);
				Cursor imageCursor = context.getContentResolver().query(imageDirUri, Contract.Images.READ_PROJECTION,																			null, null,	null);			
				for (Image image : checkpoint.images) {

					// fill in values
					values.clear();
					values.put(Contract.Images.COLUMN_NAME_UUID, image.uuid.toString());
					values.put(Contract.Images.COLUMN_NAME_NO, checkpoint.images.indexOf(image) + 1);
					values.put(Contract.Images.COLUMN_NAME_DESCRIPTION, image.description);

					// see if checkpoints already exists
					if (checkpointCursor.moveToFirst()) {
						do {
							UUID uuid =  UUID.fromString(checkpointCursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX));
							if (image.uuid.equals(uuid)) {
								image.id = imageCursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX);
								break;
							}						
						} while (imageCursor.moveToNext());
					}
					if (image.id != 0) {
						// update
						Uri imageUri =  Contract.Images.getUriId(image.id);
						context.getContentResolver().update(imageUri, values, null, null);
					}
					else {
						// insert
						Uri imageUri = context.getContentResolver().insert(imageDirUri, values);
						image.id = ContentUris.parseId(imageUri);					
					}											
				}			
				
				/////////////////////////// 
				// delete images for current checkpoint
				if (imageCursor.moveToFirst()) {
					do {
						UUID uuid =  UUID.fromString(imageCursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX));
						boolean found = false;
						for (Image image : checkpoint.images) {
							if (image.uuid.equals(uuid)) {
								found = true;
								break;
							}
						}
						if (!found) {
							long id =  imageCursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX);
							Uri imageUri =  Contract.Images.getUriId(id);
							context.getContentResolver().delete(imageUri, null, null);
						}
					} while (imageCursor.moveToNext());
				}
				
				// close cursor
				imageCursor.close();
			}			
			
			/////////////////////////// 
			// delete checkpoints
			if (checkpointCursor.moveToFirst()) {
				do {
					UUID uuid =  UUID.fromString(checkpointCursor.getString(Contract.Checkpoints.READ_PROJECTION_UUID_INDEX));
					boolean found = false;
					for (Checkpoint checkpoint : trail.checkpoints) {
						if (checkpoint.uuid.equals(uuid)) {
							found = true;
							break;
						}
					}
					if (!found) {
						long id =  checkpointCursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX);
						Uri checkpointUri =  Contract.Checkpoints.getUriId(id);
						context.getContentResolver().delete(checkpointUri, null, null);
					}
				} while (checkpointCursor.moveToNext());
			}
			
			// close cursor
			checkpointCursor.close();
			
			/////////////////////////// 
			// download image files			
			for (Checkpoint checkpoint : trail.checkpoints) {
				for (Image image : checkpoint.images) {
					
					// skip if files already exist
					if (App.getImageManager().exists(image.id)) {
						continue;
					}
					
					// query server
					HttpGet get = new HttpGet(getImageUri(trail.uuid, image.uuid).toString());
					HttpResponse response = http.execute(get);
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode != 200) {
						throw new ProtocolException("Server returned status code " + statusCode);
					}
					// get image from response and add it the the image manager
					InputStream inputStream = response.getEntity().getContent();
					App.getImageManager().add(image.id, inputStream);
				}
			}						
						
			/////////////////////////// 
			// return the id of the trail as result
			return trail.id;
			
		} catch (Exception ex) {						
			throw new ClientException("Error while downloading trail from server", ex);
		}
	}
	
//	/**
//	 * @throws ClientException
//	 */
//	public List<TrailInfo> findTrails() throws ClientException {
//
//		try {
//			// get response from server as JSON
//			String json = getJson(getTrailsDirUri());
//
//			/////////////////////////// 
//			// parse JSON and fill list
//			ArrayList<TrailInfo> result = new ArrayList<TrailInfo>();
//			JsonReader reader = new JsonReader(new StringReader(json));
//			reader.beginArray();
//			while (reader.hasNext()) {
//				reader.beginObject();
//				TrailInfo trail = new TrailInfo();
//				while (reader.hasNext()) {
//					String name = reader.nextName();
//					if (name.equals(Json.TRAIL_UUID)) {
//						trail.uuid = UUID.fromString(reader.nextString());
//					} else if (name.equals(Json.TRAIL_NAME)) {
//						trail.name = reader.nextString();
//					} else if (name.equals(Json.TRAIL_DESCRIPTION)) {
//						if (reader.peek() == JsonToken.STRING)
//							trail.description = reader.nextString();
//						else
//							reader.skipValue();
//					} else {
//						reader.skipValue();
//					}
//				}
//				result.add(trail);
//				reader.endObject();
//			}
//			reader.endArray();
//			reader.close();
//
//			/////////////////////////// 
//			// complete result with local id (if existent)
//			Uri trailsUri = Contract.Trails.getUriDir();
//			Cursor trailsCursor = context.getContentResolver().query(trailsUri, Contract.Trails.READ_PROJECTION, null, null, null);
//			while (trailsCursor.moveToNext()) {
//				if (!TextUtils.isEmpty(trailsCursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX))) {
//					UUID uuid = UUID.fromString(trailsCursor.getString(Contract.Trails.READ_PROJECTION_UUID_INDEX));
//					for (TrailInfo trail : result) {
//						if (trail.uuid.equals(uuid)) {
//							trail.id = trailsCursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX);
//							break;
//						}
//					}
//				}
//
//			}
//			trailsCursor.close();
//
//			return result;
//
//		} catch (Exception ex) {
//			throw new ClientException("Error while getting trails from server", ex);
//		}
//	}


	/**
	 * Retrieves a JSON document from the specified URI
	 * @param http
	 * @param uri
	 * @return returned JSON document 
	 * @throws ClientException
	 */
	private String getJson( HttpClient http, Uri uri) throws ClientException {
		try {
			// query server
			HttpGet get = new HttpGet(uri.toString());
			get.addHeader("Accept", "application/json");
			HttpResponse response = http.execute(get);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				throw new ProtocolException("Server returned status code " + statusCode);
			}
			// get JSON from response
			InputStream inputStream = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, HTTP.UTF_8), 8);
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
			inputStream.close();
			return result.toString();
		} catch (Exception ex) {
			throw new ClientException("Error while getting JSON from server", ex);
		}
	}

}
