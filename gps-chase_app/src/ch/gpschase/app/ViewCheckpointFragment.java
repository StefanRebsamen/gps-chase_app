package ch.gpschase.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.ImageManager;


/**
 * 
 */
public class ViewCheckpointFragment extends Fragment {
	
	/**
	 * 
	 */
	private class ImageClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {

			Long imageId = (Long)v.getTag();
			
	        // create an image view
			ImageView imageView = new ImageView(getActivity());			
            imageView.setLayoutParams(new LinearLayout.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setPadding(8, 8, 8, 8);
            imageView.setAdjustViewBounds(true);
	        imageView.setImageBitmap(App.getImageManager().getFull(imageId));
			
	        // show it in an dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.dialog_show_image_title);
			builder.setView(imageView);
			builder.setPositiveButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();			
		}		
	}
	
	private final ImageClickListener imageClickListener = new ImageClickListener(); 
	
	// checkpoint we've got to show stuff for
	private long checkpointId;

	//number of the checkpoint
	private int checkpointNo;
	
	// distance to next checkpoint
	private float distance; 
	
	// references to UI elements
	private TextView textViewHint;
	private TextView textViewCheckpoint;
	private TextView textViewNotShown;
	private TextView textViewDistance;
	private LinearLayout layoutImages;	
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// load layout
		View view = inflater.inflate(R.layout.fragment_view_checkpoint, null);
		
		// get references to UI elements
		textViewHint = (TextView)view.findViewById(R.id.textView_hint);
		textViewCheckpoint = (TextView)view.findViewById(R.id.textView_checkpoint);
		textViewNotShown = (TextView)view.findViewById(R.id.textView_not_shown);
		textViewDistance = (TextView)view.findViewById(R.id.textView_distance);
		layoutImages = (LinearLayout)view.findViewById(R.id.layout_images);		
					
		updateHintAndImages();
		updateDistance();		
		
		return view;			
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}	
	
	/**
	 * 
	 * @return
	 */
	public long getCheckpointId() {
		return checkpointId;
	}

	
	/**
	 * 
	 * @return
	 */
	public long getCheckpointNo() {
		return checkpointNo;
	}
	
	/**
	 * 
	 * @param checkpointId
	 */
	public void setCheckpoint(long checkpointId, int checkpointNo) {
		if (this.checkpointId != checkpointId || this.checkpointNo != checkpointNo) {
			this.checkpointId = checkpointId;
			this.checkpointNo = checkpointNo;
			updateHintAndImages();
		}
	}

	/**
	 * 
	 * @param distance
	 * @return
	 */
	public void setDistance(float distance) {
		this.distance = distance;
		updateDistance();		
	}	
	
	
	/**
	 * 
	 */
	private void updateHintAndImages() {
		
		if (getActivity() != null) {

			Cursor cursor;
			
			// load data into UI elements
			Uri checkpointIdUri = Contract.Checkpoints.getUriId(checkpointId);
			cursor = getActivity().getContentResolver().query(checkpointIdUri, Contract.Checkpoints.READ_PROJECTION, null, null, null);
			if (cursor.moveToNext()) {
				String txt;
				txt = cursor.getString(Contract.Checkpoints.READ_PROJECTION_HINT_INDEX);
				textViewHint.setText(txt);
				if (!TextUtils.isEmpty(txt)) {
					textViewHint.setVisibility(View.VISIBLE);
				} else {
					textViewHint.setVisibility(View.GONE);					
				}
				txt = "#" + checkpointNo;
				// tell if itn's not shown on the map
				if (cursor.getInt(Contract.Checkpoints.READ_PROJECTION_LOC_SHOW_INDEX) == 0) {
					textViewNotShown.setVisibility(View.VISIBLE);
				}
				else {
					textViewNotShown.setVisibility(View.INVISIBLE);					
				}
				textViewCheckpoint.setText(txt);
			}
			else {
				textViewHint.setText("");
				textViewCheckpoint.setText("");
				textViewNotShown.setVisibility(View.INVISIBLE);					
			}
			cursor.close();
				
			// load images
			layoutImages.removeAllViews();			
			
			ImageManager imageManager = App.getImageManager();					
			
			Uri imageDirUri = Contract.Images.getUriDir(checkpointId);		
			cursor = getActivity().getContentResolver().query(imageDirUri, Contract.Images.READ_PROJECTION, null, null, Contract.Images.DEFAULT_SORT_ORDER);
			while (cursor.moveToNext()) {
		        long imageId = cursor.getLong(Contract.Images.READ_PROJECTION_ID_INDEX);

		        // create an image view
				ImageView imageView = new ImageView(getActivity());
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setPadding(8, 8, 8, 8);
				imageView.setImageBitmap(imageManager.getThumb(imageId));
				imageView.setTag(Long.valueOf(imageId));
		        		        
	            // listen to click event
	            imageView.setOnClickListener(imageClickListener);
				
	            // add to layout
	            layoutImages.addView(imageView);
			}						
		}
	}

	
	private void updateDistance() {
		if (textViewDistance != null) {
			// format distance
			if (!Float.valueOf(distance).equals(Float.NaN)) {
				textViewDistance.setText(String.format("%.0f m", distance));
			} else {
				textViewDistance.setText("? m");
			}
		}
	}
	
	

	
}
