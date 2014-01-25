package ch.gpschase.app.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
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
 * Manage the image.
 * 
 * Is create through the app
 */
public class ImageFileManager {
	
	public final static int THUMB_SIZE = 96;
	public final static int FULL_SIZE = 1024;
	
	
	private final String FILE_PREFIX = "img"; 
	private final String FILE_FULL = "full"; 
	private final String FILE_THUMB = "thumb"; 
	private final String FILE_SUFFIX = "jpg"; 

	// reference to the app
	private App app;
	
	/**
	 * Constructor
	 * @param app
	 */
	public ImageFileManager(App app) {
		this.app = app;
	}

	/**
	 * Returns the full size image as bitmap
	 * @param image
	 * @return
	 */
	public Bitmap getFull(Image image) {
		File file = getFullFile(image);
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inPurgeable = true;
		return BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
	}

	/**
	 * Returns the thumb nail as bitmap
	 * @param image
	 * @return
	 */
	public Bitmap getThumb(Image image) {
		File file = getThumbFile(image);		
		return (BitmapFactory.decodeFile(file.getAbsolutePath()));
	}
	
	/**
	 * Adds the image file
	 * @param image
	 * @param src
	 * @return
	 */
	public boolean add(Image image, File src) {
		
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

			return add(image, fullBitmap, rotation);
		}
		catch (Exception ex) {
			Log.e(this.getClass().getSimpleName(), "Error while adding image", ex);
			return false;
		}			
    }
	
	/**
	 * 
	 * @param image
	 * @param src
	 * @return
	 */
	public boolean add(Image image, InputStream src) {
		
		try {
			// file has to exist
			if (src == null) {
				throw new IllegalArgumentException();
			}
			
			// get bitmap for full file
			Bitmap fullBitmap = BitmapFactory.decodeStream(src);

			return add(image, fullBitmap, 0);
		}
		catch (Exception ex) {
			Log.e(this.getClass().getSimpleName(), "Error while adding image", ex);
			return false;
		}			
    }	

	/**
	 * 
	 * @param image
	 * @param fullBitmap
	 * @param rotation
	 * @return
	 */
	private boolean add(Image image, Bitmap fullBitmap, int rotation) {
		
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
			
			// delete old files
			delete(image);
			
			// save full image
			FileOutputStream fullOs = new FileOutputStream(getFullFile(image));
			fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fullOs);	    
			fullOs.close();
	
			// save thumb
		    Bitmap thumbBitmap = ThumbnailUtils.extractThumbnail(fullBitmap, THUMB_SIZE, THUMB_SIZE);
			FileOutputStream thumbOs = new FileOutputStream(getThumbFile(image));
			thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, thumbOs);	    
			thumbOs.close();
		
			return true;
		} 
		catch (Exception ex) {
			Log.e(this.getClass().getSimpleName(), "Error while adding image", ex);
			return false;
		}
	}

	/**
	 * 
	 * @param image
	 * @return
	 */
	public Bitmap delete(Image image) {				
		getFullFile(image).delete();
		getThumbFile(image).delete();
		
		return null;		
	}

	/**
	 * Returns if the file for the given image exist 
	 * @param image
	 * @return
	 */
	public boolean exists(Image image) {				
		return getFullFile(image).exists() && getThumbFile(image).exists();
	}
	
	/**
	 * 
	 * @param image
	 * @return
	 */
	public File getFullFile(Image image) {
		return new File((app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)), FILE_PREFIX +"_" + image.uuid.toString() + "_" + FILE_FULL + "." + FILE_SUFFIX);
	}

	/**
	 * 
	 * @param image
	 * @return
	 */
	public File getThumbFile(Image image) {
		return new File((app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)), FILE_PREFIX +"_" + image.uuid.toString() + "_" + FILE_THUMB + "." + FILE_SUFFIX);
	}
		
	/**
	 * Clean
	 */
	public void cleanupFiles() {

		// check for all files in directory if they still belong there
		Pattern pattern = Pattern.compile(FILE_PREFIX + "_([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})_(" + FILE_FULL + "|"  + FILE_THUMB +")");	
		for (File file : app.getExternalFilesDir(Environment.DIRECTORY_PICTURES).listFiles()) {
			Matcher m = pattern.matcher(file.getName());
			if (m.find()) {
				// valid file name, check if record still exists
				UUID uuid  = UUID.fromString((m.group(1)));
				
				long imageId = Image.exists(app, uuid);
				if (imageId == 0) {
					file.delete();
					Log.v(this.getClass().getSimpleName(), "Deleting image file " + file.getName() + " because record doesn't exist");
				}
			}
			else {
				// strange file name --> delete it
				file.delete();
				Log.v(this.getClass().getSimpleName(), "Deleted file " + file.getName() + " because name doesn't match pattern");
			}
		}		
	}
	
}
