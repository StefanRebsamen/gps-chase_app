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
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import ch.gpschase.app.App;

/**
 * API to use against backend
 */
public class BackendClient {

	/** Field names for JSON **/
	private static class Json {
		private static final String TRAIL_UUID = "uuid";
		private static final String TRAIL_UPDATED = "updated";
		private static final String TRAIL_NAME = "name";
		private static final String TRAIL_DESCRIPTION = "descr";
		private static final String TRAIL_CHECKPOINTS = "chkpts";

		private static final String CHECKPOINT_UUID = "uuid";
		private static final String CHECKPOINT_LOCATION = "loc";
		private static final String CHECKPOINT_SHOW_ON_MAP = "loc_show";
		private static final String CHECKPOINT_HINT = "hint";
		private static final String CHECKPOINT_IMAGES = "images";

		private static final String IMAGE_UUID = "uuid";
		private static final String IMAGE_DESCRIPTION = "descr";
		
				
		public static String write(Trail trail) throws IOException {
			
			StringWriter json = new StringWriter();
			JsonWriter writer = new JsonWriter(json);

			// trail info
			writer.beginObject();

			writer.name(Json.TRAIL_UUID).value(trail.info.uuid.toString());
			writer.name(Json.TRAIL_NAME).value(trail.info.name);
			writer.name(Json.TRAIL_DESCRIPTION).value(trail.info.description);
			writer.name(Json.TRAIL_UPDATED).value(trail.info.updated);

			// checkpoints
			if (!trail.checkpoints.isEmpty()) {
				writer.name(Json.TRAIL_CHECKPOINTS).beginArray();
				for (Checkpoint checkpoint : trail.checkpoints) {
					writer.beginObject();
					writer.name(Json.CHECKPOINT_UUID).value(checkpoint.uuid.toString());
					writer.name(Json.CHECKPOINT_LOCATION).beginArray()	// lng, lat
							.value(checkpoint.location.getLongitude() )
							.value(checkpoint.location.getLatitude()).endArray();
					writer.name(Json.CHECKPOINT_SHOW_ON_MAP).value(checkpoint.showLocation);
					writer.name(Json.CHECKPOINT_HINT).value(checkpoint.hint);
			
					if (!checkpoint.images.isEmpty()) {
						writer.name(Json.CHECKPOINT_IMAGES).beginArray();
						for (Image image : checkpoint.images) {
							writer.beginObject();
							writer.name(Json.IMAGE_UUID).value(image.uuid.toString());
							writer.name(Json.IMAGE_DESCRIPTION).value(image.description);
							writer.endObject();							
						}
						writer.endArray();
					}
					writer.endObject();
				}
				writer.endArray();				
			}		
			writer.endObject();
			
			// create JSON string
			writer.close();
			return json.toString();			
		}
		
	
		
		public static Trail parse(String json) throws IOException {
			
			JsonReader reader = new JsonReader(new StringReader(json));
			reader.beginObject();
			Trail trail = new Trail();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals(Json.TRAIL_UUID)) {
					trail.info.uuid = UUID.fromString(reader.nextString());
				} else if (name.equals(Json.TRAIL_UPDATED)) {
					trail.info.updated = reader.nextLong();
				} else if (name.equals(Json.TRAIL_NAME)) {
					trail.info.name = reader.nextString();
				} else if (name.equals(Json.TRAIL_DESCRIPTION)) {
					if (reader.peek() == JsonToken.STRING)
						trail.info.description = reader.nextString();
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
								checkpoint.location.setLongitude(reader.nextDouble());
								checkpoint.location.setLatitude(reader.nextDouble());
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
			
			return trail;
		}
	}

	private final static Uri BASE_URI = Uri.parse("http://192.168.0.20:5000");

	/**
	 * Exception thrown to signal an error within the client 
	 */
	public class ClientException extends Exception {

		private static final long serialVersionUID = 1L;

		public ClientException(String message) {
			super(message);
		}

		public ClientException(String message, Exception ex) {
			super(message, ex);
		}
	}
		
	// context to use
	private Context context;

	
	/**
	 * Constructor
	 * @param context
	 */
	public BackendClient(Context context) {
		this.context = context;
	}
	
	/**
	 * Build the URI from the trail
	 * @param trailUuid
	 * @return
	 */
	private Uri getTrailUri(UUID trailUuid) {
		return BASE_URI.buildUpon().appendPath("trail").appendPath(trailUuid.toString()).build();
	}

	/**
	 * Returns the backend URI for the given image
	 * @param trailUuid
	 * @param imageUuid
	 * @return
	 */
	private Uri getImageUri(UUID trailUuid, UUID imageUuid) {
		return getTrailUri(trailUuid).buildUpon().appendPath("image").appendPath(imageUuid.toString()).build();
	}

	/**
	 * Returns the backend URI for image list of the given trail 
	 * @param trailUuid
	 * @return
	 */
	private Uri getTrailImagesUri(UUID trailUuid) {
		return getTrailUri(trailUuid).buildUpon().appendPath("images") .build();
	}


	/**
	 * Uploads a trail to the server
	 * @param trailInfo
	 * @throws ClientException
	 */
	public void uploadTrail(TrailInfo trailInfo) throws ClientException {

		try {
			HttpResponse response;
			int statusCode;
					
			// create json to upload
			Trail trail = Trail.fromInfo(context, trailInfo);
			String trailJson = Json.write(trail);

			// create HTTP client
			HttpClient http = createHttpClient();
						
			// retrieve a list of images on server
			String imageJson = getJson(http, getTrailImagesUri(trailInfo.uuid));
			List<UUID> imagesOnServer = new ArrayList<UUID>();
			JsonReader reader = new JsonReader(new StringReader(imageJson));
			reader.beginArray();
			while (reader.hasNext()) {
				reader.beginObject();
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
			for (Checkpoint checkpoint : trail.checkpoints) {
				for (Image image : checkpoint.images) {
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
						HttpPut put = new HttpPut(getImageUri(trailInfo.uuid, image.uuid).toString());
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
			}

			// upload trail as JSON
			HttpPut put = new HttpPut(getTrailUri(trailInfo.uuid).toString());
			put.addHeader("Content-Type", "application/json");
			put.addHeader("Accept", "application/json");
			put.setEntity(new StringEntity(trailJson, HTTP.UTF_8));
			response = http.execute(put);
			statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				throw new ProtocolException("Server returned status code " + statusCode + " after uploading trail JSON");
			}
			
			// delete remaining images on server
			for (UUID imageUuid : imagesOnServer) {
				HttpDelete delete = new HttpDelete(getImageUri(trailInfo.uuid, imageUuid).toString());
				response = http.execute(delete);
				statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
					throw new ProtocolException("Server returned status code " + statusCode + " after deleting image");
				}				
			}
								
			// update uploaded timestamp
			trailInfo.uploaded = System.currentTimeMillis();
			
			// write to DB that it was uploaded
			ContentValues values = new ContentValues();
			values.put(Contract.Trails.COLUMN_NAME_UPLOADED, trailInfo.uploaded); 
			context.getContentResolver().update(Contract.Trails.getUriId(trailInfo.id), values, null, null);
									
		} catch (Exception ex) {
			throw new ClientException("Error while exporting trail to server", ex);
		}
	}
		
	/**
	 * @param trailUuid
	 * @throws ClientException
	 * @return downloaded trail or null if it wasn't modified
	 */
	public boolean downloadTrail(TrailInfo trailInfo) throws ClientException {
	
		try {
			// create HTTP client
			HttpClient http = createHttpClient();
			
			/////////////////////////// 
			// query server
			Uri uri = getTrailUri(trailInfo.uuid);
			uri = uri.buildUpon().appendQueryParameter("updated", Long.valueOf(trailInfo.updated).toString()).build();
			HttpGet get = new HttpGet(uri.toString());
			get.addHeader("Accept", "application/json");
			
			HttpResponse response = http.execute(get);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 304) {
				// not modified
				return false;
			}
			else if (statusCode != 200) {
				throw new ProtocolException("Server returned status code " + statusCode);
			}
			
			/////////////////////////// 
			// get JSON from response
			InputStream inputStream = response.getEntity().getContent();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, HTTP.UTF_8), 8);
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				result.append(line);
			}
			bufferedReader.close();
			inputStream.close();
			String json = result.toString();
			
			// parse JSON and create object tree of complete trail
			Trail trail = Json.parse(json);

			/////////////////////////// 
			// save object tree to database
			ContentValues values = new ContentValues();
		
			/////////////////////////// 
			// update or insert trail
			values.put(Contract.Trails.COLUMN_NAME_UUID, trail.info.uuid.toString());
			values.put(Contract.Trails.COLUMN_NAME_UPDATED, trail.info.updated);
			values.put(Contract.Trails.COLUMN_NAME_DOWNLOADED, System.currentTimeMillis());
			values.put(Contract.Trails.COLUMN_NAME_NAME, trail.info.name);
			values.put(Contract.Trails.COLUMN_NAME_DESCRIPTION, trail.info.description);

			Uri trailDirUri = Contract.Trails.getUriDir();
			Cursor trailCursor = context.getContentResolver().query(trailDirUri, Contract.Trails.READ_PROJECTION, 
																	Contract.Trails.COLUMN_NAME_UUID+"=?", 
																	new String[]{ trail.info.uuid.toString()}, 
																	null);
			if (trailCursor.moveToFirst()) {
				trail.info.id = trailCursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX);
				Uri trailUri = Contract.Trails.getUriId(trail.info.id); 
				context.getContentResolver().update(trailUri, values, null, null);
			}
			else {
				Uri trailUri = context.getContentResolver().insert(Contract.Trails.getUriDir(), values);
				trail.info.id = ContentUris.parseId(trailUri);
			}
			// close cursor
			trailCursor.close();			
			
			/////////////////////////// 
			// update or insert checkpoints			
			Uri checkpointDirUri = Contract.Checkpoints.getUriDir(trail.info.id);
			Cursor checkpointCursor = context.getContentResolver().query(checkpointDirUri, Contract.Checkpoints.READ_PROJECTION,																			null, null,	null);			
			for (Checkpoint checkpoint : trail.checkpoints) {

				// fill in values
				values.clear();
				values.put(Contract.Checkpoints.COLUMN_NAME_UUID, checkpoint.uuid.toString());
				values.put(Contract.Checkpoints.COLUMN_NAME_NO, trail.checkpoints.indexOf(checkpoint) + 1);
				values.put(Contract.Checkpoints.COLUMN_NAME_LOC_SHOW, checkpoint.showLocation ? 1 : 0);
				values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, checkpoint.location.getLongitude());
				values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, checkpoint.location.getLatitude());
				values.put(Contract.Checkpoints.COLUMN_NAME_HINT, checkpoint.hint);

				// see if checkpoint already exists
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

					// see if image already exists
					if (imageCursor.moveToFirst()) {
						do {
							UUID uuid =  UUID.fromString(imageCursor.getString(Contract.Images.READ_PROJECTION_UUID_INDEX));
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
					get = new HttpGet(getImageUri(trail.info.uuid, image.uuid).toString());
					response = http.execute(get);
					statusCode = response.getStatusLine().getStatusCode();
					if (statusCode != 200) {
						throw new ProtocolException("Server returned status code " + statusCode);
					}
					// get image from response and add it the the image manager
					inputStream = response.getEntity().getContent();
					App.getImageManager().add(image.id, inputStream);
					inputStream.close();
				}
			}						
						
			/////////////////////////// 
			// return the trail as result
			return true;
			
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
	 * Factory method to create  HTTP client with the necessary parameters set 
	 * @return create HTTP client
	 */
	private HttpClient createHttpClient() {		
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, 5000);
		HttpConnectionParams.setSoTimeout(params, 10000);
		return new DefaultHttpClient(params);
	}
	
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
