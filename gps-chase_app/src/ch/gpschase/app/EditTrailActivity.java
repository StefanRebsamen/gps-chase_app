package ch.gpschase.app;

import java.io.File;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Image;
import ch.gpschase.app.data.ImageFileManager;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.TrailMapFragment;
import ch.gpschase.app.util.UploadTask;
import ch.gpschase.app.util.ViewImageDialog;

import com.google.android.gms.maps.model.LatLng;

/**
 * Activity to edit a trail
 */
public class EditTrailActivity extends Activity {

	// fragment tags
	private static final String FRAGMENT_TAG_CPEDIT = "cpedit";
	private static final String FRAGMENT_TAG_MAP = "map";

	// name to identify trail id passed as extra in intent 
	public static final String INTENT_EXTRA_TRAILID = "trailId";
	
	/**
	 * Fragment to edit a single checkpoint
	 */
	public static class EditCheckpointFragment extends Fragment {

		// request codes used to iddentify results from other apps
		private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
		private static final int REQUEST_CODE_IMPORT_IMAGE = 2;

		/**
		 * An extended ImageView with the ability to provide context menu info
		 */		
		private class ImageViewEx extends ImageView {
			ImageViewContextMenuInfo contextMenuInfo = null;

		    public ImageViewEx(Context context) {
		        super(context);
		        contextMenuInfo = new ImageViewContextMenuInfo(this);
		    }

		    public ImageViewEx(Context context, AttributeSet attrs) {
		        super(context, attrs);
		        contextMenuInfo = new ImageViewContextMenuInfo(this);
		    }   

		    protected ContextMenuInfo getContextMenuInfo() {
		        return contextMenuInfo;
		    }

		    public boolean isContextView(ContextMenuInfo menuInfo) {
		        return menuInfo == (ContextMenuInfo)contextMenuInfo;
		    }
		    
		    /**
		     * ContextMenuInfo for ImageView
		     */
			private class ImageViewContextMenuInfo implements ContextMenuInfo {
		        protected ImageView imageView = null;
	
		        protected ImageViewContextMenuInfo(ImageView imageView) {
		        	this.imageView = imageView;
		        }		        
			}
		}
	
		/**
		 * Used for callbacks from fragment
		 */
		public interface Listener {
			
			/** Backward button was clicked */
			public void onMoveBackwardClick();

			/** Forward button was clicked */
			public void onMoveForwardClick();

			/** Delete checkpoint button was clicked */
			public void onDeleteCheckpointClick();
		}

		// listener for callbacks
		private Listener listener;

		private Checkpoint checkpoint;

		// references to UI elements
		private TextView textViewNo;
		private Switch checkBoxShowOnMap;
		private EditText editTextHint;
		private LinearLayout layoutImages;
		private ImageButton buttonNewImage;
		private ImageButton buttonMoveBackward;
		private ImageButton buttonMoveForward;
		private ImageButton buttonDeleteCheckpoint;

		// Temp fir used to capture images
		private File captureTmpFile;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

			// load layout
			View view = inflater.inflate(R.layout.fragment_edit_checkpoint, null);

			// get references to UI elements
			textViewNo = (TextView) view.findViewById(R.id.textView_checkpoint_no);
			checkBoxShowOnMap = (Switch) view.findViewById(R.id.checkBox_show_on_map);
			editTextHint = (EditText) view.findViewById(R.id.editText_hint);
			layoutImages = (LinearLayout) view.findViewById(R.id.layout_images);
			buttonNewImage = (ImageButton) view.findViewById(R.id.button_add_image);
			buttonMoveBackward = (ImageButton) view.findViewById(R.id.button_reorder_checkpoint_backward);
			buttonMoveForward = (ImageButton) view.findViewById(R.id.button_reorder_checkpoint_forward);
			buttonDeleteCheckpoint = (ImageButton) view.findViewById(R.id.button_delete_checkpoint);

			// register handler for buttons
			buttonNewImage.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					addImage();
				}
			});

			buttonMoveBackward.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onMoveBackwardClick();
					}
				}
			});

			buttonMoveForward.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onMoveForwardClick();
					}
				}
			});

			buttonDeleteCheckpoint.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onDeleteCheckpointClick();
					}
				}
			});

			return view;
		}

		@Override
		public void onStop() {
			super.onStart();

			// persist to database
			save();
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);

			// was it really from an image view?
			if (v instanceof ImageViewEx) {

				// inflate menu
				MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.context_menu_image, menu);
				
				// set a title
				menu.setHeaderTitle(R.string.context_menu_image_title);
				menu.setHeaderIcon(R.drawable.ic_image);
			}
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {

			if (item.getMenuInfo() instanceof ImageViewEx.ImageViewContextMenuInfo) {
				ImageViewEx.ImageViewContextMenuInfo info = (ImageViewEx.ImageViewContextMenuInfo)item.getMenuInfo();
				Image image = (Image)(info.imageView.getTag());
				
				switch (item.getItemId()) {
				case R.id.action_delete_image:
					// delete image
					deleteImage(image);					
					return true;
				}
			}
			return super.onContextItemSelected(item);
		}

		/**
		 * 
		 */
		public Checkpoint getCheckpoint() {
			return checkpoint;
		}

		/**
		 * 
		 */
		public void setCheckpoint(Checkpoint checkpoint) {
			
			// did it really change?
			if (this.checkpoint == checkpoint) {
				return;
			}

			// save current checkpoint data
			save();

			// change checkpoint
			this.checkpoint = checkpoint;
			
			// load data into UI elements
			if (checkpoint != null) {
				textViewNo.setText("#" + (checkpoint.getIndex() + 1));
				checkBoxShowOnMap.setChecked(checkpoint.showLocation);
				editTextHint.setText(checkpoint.hint);				
			} else {
				textViewNo.setText("#");
				checkBoxShowOnMap.setChecked(false);
				editTextHint.setText("");
			}
			updateIndex();

			// refresh images
			refreshImages();
		}

		/**
		 * 
		 * @return
		 */
		public Listener getListener() {
			return listener;
		}

		/**
		 * 
		 * @param onButtonListener
		 */
		public void setListener(Listener onButtonListener) {
			this.listener = onButtonListener;
		}

		/**
		 * 
		 * @param forward
		 * @param backward
		 */
		public void enableButtons() {
			boolean backward = checkpoint != null && (!checkpoint.isFirst());
			boolean forward = checkpoint != null && (!checkpoint.isLast());
			boolean delete = checkpoint != null;
			
			if (buttonMoveBackward != null) {
				buttonMoveBackward.setVisibility(backward ? View.VISIBLE : View.INVISIBLE);
			}
			if (buttonMoveForward != null) {
				buttonMoveForward.setVisibility(forward ? View.VISIBLE : View.INVISIBLE);
			}
			if (buttonDeleteCheckpoint != null) {
				buttonDeleteCheckpoint.setVisibility(delete ? View.VISIBLE : View.INVISIBLE);
			}			
		}

		/**
		 * 
		 */
		public void updateIndex() {
			if (checkpoint != null) {
				textViewNo.setText("#" + (checkpoint.getIndex() + 1));
			} else {
				textViewNo.setText("#");
			}			
		}
		
		
		//
		private void refreshImages() {
			
			// nothing to do if view isn't yet created
			if (layoutImages == null) {
				return;
			}
			
			// load images
			layoutImages.removeAllViews();
			if (checkpoint != null) {
				
				ImageFileManager imageManager = App.getImageManager();
				
				// make sure images are loaded
				checkpoint.loadImages(getActivity());			
				
				// ass them to their container
				for (Image image : checkpoint.getImages()) {
					// create an image view
					ImageViewEx imageView = new ImageViewEx(getActivity());
					imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					imageView.setPadding(8, 8, 8, 8);
					imageView.setImageBitmap(imageManager.getThumb(image));
					imageView.setTag(image);

					// listen to click event (show image in dialog)
					imageView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Image image = (Image) v.getTag();
							ViewImageDialog.show(getActivity(), image);
						}
					});

					// we use the context menu
					registerForContextMenu(imageView);

					// add to layout
					layoutImages.addView(imageView);
				}
			}
		}

		
		
		/**
		 * 
		 */
		private void save() {

			if (checkpoint == null) {
				return;
			}

			// read back from UI
			boolean showLocation = checkBoxShowOnMap.isChecked();
			String hint = editTextHint.getText().toString();

			// apply new values (if really changed)
			boolean changed = false;
			if (showLocation != checkpoint.showLocation) {
				checkpoint.showLocation = checkBoxShowOnMap.isChecked();
				changed = true;
			}
			if (!hint.equals(checkpoint.hint)) {
				checkpoint.hint = editTextHint.getText().toString();
				changed = true;
			}
			
			// save if somethin changed
			if (changed) {
				// save values in database
				checkpoint.save(getActivity());
				// mark trail as updated
				checkpoint.getTrail().updated = System.currentTimeMillis();
				checkpoint.getTrail().save(getActivity());
			}
		}
		

		
		/**
		 * 
		 */
		private void addImage() {

			final int CAPTURE = 0;
			final int IMPORT = 1;

			// show a dialog to choose and image source
			String[] sources = { getString(R.string.new_image_capture), getString(R.string.new_image_import) };
			new AlertDialog.Builder(getActivity()) //
					.setTitle(R.string.action_new_image) //
					.setIcon(R.drawable.ic_new_image).setItems(sources, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							// call the appropriate function
							switch (which) {
							case CAPTURE:
								captureImage();
								break;

							case IMPORT:
								importImage();
								break;
							}
						}
					}).create().show();
		}

		/**
		 * Captures an image from the camera
		 */
		private void captureImage() {

			// create a temporary file
			captureTmpFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tmp_"
					+ System.currentTimeMillis() + ".jpg");
			// make sure it's deleted
			try {
				captureTmpFile.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
			// let user take a picture. It will be stored to the passed
			// temporary
			// file
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri tmpUri = Uri.fromFile(captureTmpFile);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, tmpUri);
			startActivityForResult(intent, REQUEST_CODE_CAPTURE_IMAGE);

			// callback will be made to onActivityResult()
		}

		/**
		 * Imports an image from the gallery
		 */
		private void importImage() {

			// issue intent
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent, getString(R.string.new_image_import)), REQUEST_CODE_IMPORT_IMAGE);

			// callback will be made to onActivityResult()
		}

		/**
		 * Notification about an activity
		 */
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			// was it successful and something we were expecting?
			if (resultCode == Activity.RESULT_OK && (requestCode == REQUEST_CODE_CAPTURE_IMAGE || requestCode == REQUEST_CODE_IMPORT_IMAGE)) {

				// create image in database
				Image image = checkpoint.addImage();
				image.save(getActivity());
				
				// add image to ImageManager
				boolean added = false;
				if (requestCode == REQUEST_CODE_CAPTURE_IMAGE) {
					// import from tmp file
					added = App.getImageManager().add(image, captureTmpFile);
				} else if (requestCode == REQUEST_CODE_IMPORT_IMAGE) {
					// determine file path
					Uri uri = data.getData();
					String[] projection = { MediaStore.Images.Media.DATA };
					Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
					int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
					cursor.moveToFirst();
					String path = cursor.getString(column_index);
					cursor.close();
					// import from file
					added = App.getImageManager().add(image, new File(path));
				}

				// succeeded in adding image files?
				if (!added) {
					// delete it from database as it makes no sense to keep a
					// record
					// without associated files
					image.delete(getActivity());

					// show alert dialog
					new AlertDialog.Builder(getActivity()) //
							.setTitle(R.string.dialog_add_image_error_title) //
							.setIcon(android.R.drawable.ic_dialog_alert) //
							.setMessage(R.string.dialog_add_image_error_message) //
							.setPositiveButton(R.string.dialog_ok, null) //
							.create().show();
				}
				else {		
					// refresh images
					refreshImages();
					
					// mark trail as updated
					checkpoint.getTrail().updated = System.currentTimeMillis();
					checkpoint.getTrail().save(getActivity());					
				}
			}

			// delete temp file
			if (captureTmpFile != null) {
				captureTmpFile.delete();
				captureTmpFile = null;
			}
		}

		/**
		 * Deletes the specified image after confirmation from user
		 */
		private void deleteImage(Image image) {

			final Image passedImage = image;

			class DeleteDialogFragment extends DialogFragment {
				@Override
				public Dialog onCreateDialog(Bundle savedInstanceState) {

					// create an image view
					ImageView imageView = new ImageView(getActivity());
					imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT));
					imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					imageView.setPadding(8, 8, 8, 8);
					imageView.setAdjustViewBounds(true);
					imageView.setImageBitmap(App.getImageManager().getFull(passedImage));

					return new AlertDialog.Builder(getActivity())					//						
							.setTitle(R.string.action_delete_image)					//
							.setIcon(R.drawable.ic_delete)							//
							.setMessage(R.string.dialog_delete_image_message)		//
							.setView(imageView).setIcon(R.drawable.ic_delete)		//
							.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									// delete image from database
									passedImage.delete(getActivity());									

									// refresh images
									refreshImages();
									
									// mark trail as updated
									checkpoint.getTrail().updated = System.currentTimeMillis();
									checkpoint.getTrail().save(getActivity());									
								}
							})	//
							.setNegativeButton(R.string.dialog_no, null).create();
				}

			}

			// show dialog to delete checkpoint
			new DeleteDialogFragment().show(getFragmentManager(), null);
		}

	}

	/**
	 * Listens for events from the map
	 */
	private class MapListener implements TrailMapFragment.MapListener {
		@Override
		public void onClickedCheckpoint(Checkpoint checkpoint) {
			// cancel adding checkpoint
			addingCheckpoint = false;
			
			if (checkpoint == selectedCheckpoint) {
				// deselect checkpoint
				selectCheckpoint(null);
			} else {
				// select checkpoint
				selectCheckpoint(checkpoint);
			}
		}

		@Override
		public void onClickedMap(LatLng position) {
			if (addingCheckpoint) {
				addCheckpoint(position);
				// cancel the flag
				addingCheckpoint = false;
			} 
			else {
				selectCheckpoint(null);
			}			
		}

		@Override
		public void onStartPositioningCheckpoint(Checkpoint checkpoint) {
			// cancel adding checkpoint
			addingCheckpoint = false;
		}

		@Override
		public void onPositionedCheckpoint(Checkpoint checkpoint) {
			// persist to database
			checkpoint.save(EditTrailActivity.this);
		}
	}

	/**
	 * Listener for events from the EditCheckpointFragment
	 */
	private class EditListener implements EditCheckpointFragment.Listener {

		@Override
		public void onMoveBackwardClick() {
			moveSelectedCheckpointBackward();
		}

		@Override
		public void onMoveForwardClick() {
			moveSelectedCheckpointForward();
		}

		@Override
		public void onDeleteCheckpointClick() {
			// show dialog to ask if checkpoint should be really deleted
			deleteSelectedCheckpoint();
		}
	}

	// the trail we're editing
	private Trail trail = null;

	// map fragment to be reused
	private TrailMapFragment map;

	// fragment to edit a single checkpoint
	private EditCheckpointFragment checkpointEdit;

	// currently selected checkpoint
	private Checkpoint selectedCheckpoint;
	
	// indicates that we're busy adding a checkpoint
	private boolean addingCheckpoint = false;
	
	/**
	 * Opens the edit activity for the specified trail
	 */
	public static void show(Context context, Trail trail) {
		// switch to activity		
		Intent intent = new Intent(context, EditTrailActivity.class);
		intent.putExtra(INTENT_EXTRA_TRAILID, trail.getId());
		context.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("EditTrailActivit", "onCreate");

		long trailId = getIntent().getLongExtra(INTENT_EXTRA_TRAILID, 0);
		
		// load trail including its checkpoints
		trail = Trail.load(this, trailId);
		trail.loadCheckpoints(this);
		
		// load layout
		setContentView(R.layout.activity_edit_trail);

		// create and add fragments (if not recreated)
		FragmentTransaction ft = getFragmentManager().beginTransaction();

		map = (TrailMapFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
		if (map == null) {
			map = new TrailMapFragment();
			ft.replace(R.id.layout_map, map, FRAGMENT_TAG_MAP);
		}
		map.setMapListener(new MapListener());
		map.setDraggable(true);

		checkpointEdit = (EditCheckpointFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_CPEDIT);
		if (checkpointEdit == null) {
			checkpointEdit = new EditCheckpointFragment();
			checkpointEdit.setListener(new EditListener());
			ft.replace(R.id.layout_checkpoint_edit, checkpointEdit, FRAGMENT_TAG_CPEDIT);
		}
		ft.commit();

		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_edit_trail);
		actionBar.setSubtitle(trail.name);
				
		// make sure nothing is selected
		selectCheckpoint(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate menu
		getMenuInflater().inflate(R.menu.menu_edit_trail, menu);

		// disable upload if trail wasn't already uploaded
		MenuItem menuUpdload = menu.findItem(R.id.action_upload_trail);
		menuUpdload.setVisible(trail.uploaded != 0);

		return true;
	}

	@Override
	public void onStart() {
		super.onStart();

		// TODO check if trail is really editable

		// init map
		map.clearCheckpoints();
		boolean first = true;
		for (Checkpoint checkpoint : trail.getCheckpoints()) {
			LatLng location = new LatLng(checkpoint.location.getLatitude(), checkpoint.location.getLongitude());
			
			// add marker
			map.addCheckpoint(checkpoint, false, false);

			// mark point as selected if necessary
			if (checkpoint == selectedCheckpoint) {
				map.selectCheckpoint(checkpoint);
			}

			// set camera to start
			if (first) {
				map.setCameraTarget(location);
				map.setCameraZoom(TrailMapFragment.DEFAULT_ZOOM);
				first = false;
			}
		}

		// refresh map
		map.refresh();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_new_checkpoint:
			// unselect current checkpoint
			selectCheckpoint(null);
			// set a flag that next click to map adds the new checkpoint
			addingCheckpoint = true;
			// show toast to inform user on what to do
			Toast.makeText(this, R.string.toast_new_checkpoint , Toast.LENGTH_SHORT).show();
			return true;

		case R.id.action_upload_trail:
			// cancel adding checkpoint
			addingCheckpoint = false;
			// upload the trail
			uploadTrail();
			return true;

		case R.id.action_trail_info:
			// cancel adding checkpoint
			addingCheckpoint = false;
			// show the trail info activity
			TrailInfoActivity.show(this, trail);
			return true;
			
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 */
	private void addCheckpoint(LatLng position) {

		Checkpoint checkpoint = trail.addCheckpoint();

		// init its data and save in database
		checkpoint.uuid = UUID.randomUUID();
		checkpoint.location.setLatitude(position.latitude);
		checkpoint.location.setLongitude(position.longitude);
		checkpoint.showLocation = true;
		checkpoint.hint = "";
		checkpoint.save(this);

		// add marker
		map.addCheckpoint(checkpoint, false, true);

		// select added point
		selectCheckpoint(checkpoint);
		
		// mark trail as updated
		trail.updated = System.currentTimeMillis();
		trail.save(this);		
	}

	/**
	 * 
	 * @param pointId
	 */
	private void selectCheckpoint(Checkpoint checkpoint) {

		// make it the new selected point
		selectedCheckpoint = checkpoint;

		// select point
		map.selectCheckpoint(checkpoint);

		// load in checkpoint edit fragment
		checkpointEdit.setCheckpoint(checkpoint);

		// show or hide edit fragment
		if (checkpoint != null && checkpointEdit.isHidden()) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.show(checkpointEdit);
			ft.commit();
		} else if (checkpoint == null && !checkpointEdit.isHidden()) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.hide(checkpointEdit);
			ft.commit();
		}

		// update button on checkpoint edit fragment
		checkpointEdit.enableButtons();
	}

	/**
	 * 
	 */
	private void deleteSelectedCheckpoint() {

		/**
		 * Dialog to ask user about deletion of the checkpoint
		 */
		class DeleteDialogFragment extends DialogFragment {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				return new AlertDialog.Builder(getActivity()) //
						.setTitle(R.string.action_delete_checkpoint) //
						.setIcon(R.drawable.ic_delete) //
						.setMessage(R.string.dialog_delete_checkpoint_message) //
						.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {

								Checkpoint checkpoint = selectedCheckpoint;

								// unselect checkpoint
								selectCheckpoint(null);

								// remove from map
								map.removeCheckpoint(checkpoint);

								// delete checkpoint
								checkpoint.delete(getActivity());

								// mark trail as updated
								trail.updated = System.currentTimeMillis();
								trail.save(EditTrailActivity.this);		
							}
						})
						.setNegativeButton(R.string.dialog_no, null)
						.create();
			}

		}

		// show dialog to delete checkpoint
		new DeleteDialogFragment().show(getFragmentManager(), null);
	}

	/**
	 * 
	 * @param checkpointId
	 */
	private void moveSelectedCheckpointBackward() {
		// move checkpoint
		selectedCheckpoint.moveBackward(this);
			
		// tell the map to refresh itself
		map.reorderCheckpoints();
		
		// update checkpoint edit fragment
		checkpointEdit.enableButtons();
		checkpointEdit.updateIndex();
		
		// mark trail as updated
		trail.updated = System.currentTimeMillis();
		trail.save(this);		
	}

	/**
	 * 
	 * @param checkpointId
	 */
	private void moveSelectedCheckpointForward() {

		// move checkpoint
		selectedCheckpoint.moveForward(this);
	
		// tell the map to refresh itself
		map.reorderCheckpoints();
		
		// update checkpoint edit fragment
		checkpointEdit.enableButtons();
		checkpointEdit.updateIndex();

		// mark trail as updated
		trail.updated = System.currentTimeMillis();
		trail.save(this);		
	}

	/**
	 * Upload a trail to the server in an asynchronous task
	 */
	private void uploadTrail() {

		// deselect checkpoint (saves changes)
		selectCheckpoint(null);

		// execute task
		new UploadTask(this, trail, false).execute();
	}

}
