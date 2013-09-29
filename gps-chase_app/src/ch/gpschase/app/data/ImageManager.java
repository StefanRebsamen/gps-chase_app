package ch.gpschase.app.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.gpschase.app.App;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

/**
 * 
 */
public class ImageManager {
	
	public final static int THUMB_SIZE = 96;
	public final static int FULL_SIZE = 1024;
	
	
	private final String FILE_PREFIX = "img"; 
	private final String FILE_FULL = "full"; 
	private final String FILE_THUMB = "thumb"; 
	private final String FILE_SUFFIX = "jpg"; 

	
	private App app;
	
	public ImageManager(App app) {
		this.app = app;
	}

	
	public Bitmap getFull(long imageId) {
		File file = getFullFile(imageId);
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inPurgeable = true;
		return BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
	}

	
	public Bitmap getThumb(long imageId) {
		File file = getThumbFile(imageId);		
		return (BitmapFactory.decodeFile(file.getAbsolutePath()));
	}

	
	
	
	public boolean add(long imageId, File src) {
		
		try {
			// file has to exist
			if (src == null || !src.exists()) {
				throw new IllegalArgumentException();
			}

			// determine rotation from Exif data
			ExifInterface ei = new ExifInterface(src.getAbsolutePath());
			int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			int rotation = 0;
			switch(orientation) {
			    case ExifInterface.ORIENTATION_ROTATE_90:
			        rotation = 90;
			        break;
			        
			    case ExifInterface.ORIENTATION_ROTATE_180:
			        rotation = 180;
			        break;
			        
			    case ExifInterface.ORIENTATION_ROTATE_270:
			        rotation = 270;
			        break;			        
			}
			
			// get bitmap for full file
			Bitmap fullBitmap = BitmapFactory.decodeFile(src.getAbsolutePath());

			return add(imageId, fullBitmap, rotation);
		}
		catch (Exception ex) {
			Log.e(this.getClass().getSimpleName(), "Error while adding image", ex);
			return false;
		}			
    }
	
	
	public boolean add(long imageId, InputStream src) {
		
		try {
			// file has to exist
			if (src == null) {
				throw new IllegalArgumentException();
			}
			
			// get bitmap for full file
			Bitmap fullBitmap = BitmapFactory.decodeStream(src);

			return add(imageId, fullBitmap, 0);
		}
		catch (Exception ex) {
			Log.e(this.getClass().getSimpleName(), "Error while adding image", ex);
			return false;
		}			
    }	

	
	private boolean add(long imageId, Bitmap fullBitmap, int rotation) {
		
		try {
			Log.v("ImageManager", "original image: width = " + fullBitmap.getWidth() + ", heigth" + fullBitmap.getHeight());
			
			// scale to full image size
			Matrix m = new Matrix();
			m.setRectToRect(new RectF(0, 0, fullBitmap.getWidth(), fullBitmap.getHeight()), new RectF(0, 0, FULL_SIZE, FULL_SIZE), Matrix.ScaleToFit.CENTER);
			fullBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.getWidth(), fullBitmap.getHeight(), m, true);
			
			// rotate picture if necessary					
			if (rotation != 0) {			
		      m = new Matrix();
		      m.postRotate(rotation);
		      fullBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.getWidth(), fullBitmap.getHeight(), m, true);
			}
			
			Log.v("ImageManager", "scaled and rotated image: width = " + fullBitmap.getWidth() + ", heigth" + fullBitmap.getHeight());
			
			// save full image
			FileOutputStream fullOs = new FileOutputStream(getFullFile(imageId));
			fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fullOs);	    
			fullOs.close();
	
			// save thumb
		    Bitmap thumbBitmap = ThumbnailUtils.extractThumbnail(fullBitmap, THUMB_SIZE, THUMB_SIZE);
			FileOutputStream thumbOs = new FileOutputStream(getThumbFile(imageId));
			thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, thumbOs);	    
			thumbOs.close();
		
			return true;
		} 
		catch (Exception ex) {
			Log.e(this.getClass().getSimpleName(), "Error while adding image", ex);
			return false;
		}
	}

	
	public Bitmap delete(long imageId) {				
		getFullFile(imageId).delete();
		getThumbFile(imageId).delete();
		
		return null;		
	}
	
	
	public File getFullFile(long imageId) {
		return new File((app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)), FILE_PREFIX +"_" + imageId + "_" + FILE_FULL + "." + FILE_SUFFIX);
	}


	public File getThumbFile(long imageId) {
		return new File((app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)), FILE_PREFIX +"_" + imageId + "_" + FILE_THUMB + "." + FILE_SUFFIX);
	}
		
	
	/**
	 * Clean
	 */
	public void cleanupFiles() {

		// check for all files in directory if they still belong there
		Pattern pattern = Pattern.compile(FILE_PREFIX + "_([0-9]*)_(" + FILE_FULL + "|"  + FILE_THUMB +")"); 		
		for (File file : app.getExternalFilesDir(Environment.DIRECTORY_PICTURES).listFiles()) {
			Matcher m = pattern.matcher(file.getName());
			if (m.find()) {
				// valid file name, check if record still exists
				long imageId = Long.parseLong(m.group(1));
				Uri imageUri = Contract.Images.getUriId(imageId);
				Cursor cursor = app.getContentResolver().query(imageUri, null, null, null, null);
				if (!cursor.moveToNext()) {
					file.delete();
					Log.v(this.getClass().getSimpleName(), "Deleting image file " + file.getName() + " because record doesn't exist");
				}
			}
			else {
				// strang file name --> delete it
				file.delete();
				Log.v(this.getClass().getSimpleName(), "Deleted file " + file.getName() + " because name doesn't match pattern");
			}
		}		
	}
	
}
