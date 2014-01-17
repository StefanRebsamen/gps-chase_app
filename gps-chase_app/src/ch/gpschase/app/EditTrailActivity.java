package ch.gpschase.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.ImageManager;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.data.TrailInfo;
import ch.gpschase.app.util.TrailDownloadLink;
import ch.gpschase.app.util.TrailMapFragment;
import ch.gpschase.app.util.UploadTask;

import com.google.android.gms.maps.model.LatLng;

public class EditTrailActivity extends Activity {

	private static final String FRAGMENT_TAG_CPEDIT = "cpedit";
	private static final String FRAGMENT_TAG_MAP = "map";

	/**
	 * 
	 */
	public static class EditCheckpointFragment extends Fragment {

		// request codes used to iddentify results from other apps
		private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
		private static final int REQUEST_CODE_IMPORT_IMAGE = 2;

		/**
		 * 
		 */
		public interface OnButtonListener {
			public void onReorderBackwardClicked();

			public void onReorderForwardClicked();

			public void onDeleteCheckpointClicked();
		}

		/**
		 * 
		 */
		private class ImageClickListener implements OnClickListener {
			@Override
			public void onClick(View v) {

				Long imageId = (Long) v.getTag();

				// create an image view
				ImageView imageView = new ImageView(getActivity());
				imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));
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

		/**
		 * 
		 */
		private class ButtonListener implements OnClickListener {

			@Override
			public void onClick(View v) {

				switch (v.getId()) {
				case R.id.button_add_image:
					addImage();
					break;

				case R.id.button_reorder_checkpoint_backward:
					// forward to listener
					if (onButtonListener != null) {
						onButtonListener.onReorderBackwardClicked();
					}
					break;

				case R.id.button_reorder_checkpoint_forward:
					// forward to listener
					if (onButtonListener != null) {
						onButtonListener.onReorderForwardClicked();
					}
					break;

				case R.id.button_delete_checkpoint:
					// forward to listener
					if (onButtonListener != null) {
						onButtonListener.onDeleteCheckpointClicked();
					}
					break;
				}
			}
		}

		private final ImageClickListener imageClickListener = new ImageClickListener();
		private final ButtonListener buttonListener = new ButtonListener();

		// listener for callbacks
		private OnButtonListener onButtonListener;

		// trail and checkpoint we've got to show stuff for
		private long trailId;
		private long checkpointId;

		// references to UI elements
		private CheckBox checkBoxShowOnMap;
		private EditText editTextHint;
		private LinearLayout layoutImages;
		private ImageButton buttonNewImage;
		private ImageButton buttonReorderBackward;
		private ImageButton buttonReorderForward;
		private ImageButton buttonDeleteCheckpoint;

		// Temp fir used to capture images
		private File captureTmpFile;

		private long imageIdtoDelete;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// we want our own menu
			setHasOptionsMenu(true);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

			// load layout
			View view = inflater.inflate(R.layout.fragment_edit_checkpoint, null);

			// get references to UI elements
			checkBoxShowOnMap = (CheckBox) view.findViewById(R.id.checkBox_show_on_map);
			editTextHint = (EditText) view.findViewById(R.id.editText_hint);
			layoutImages = (LinearLayout) view.findViewById(R.id.layout_images);
			buttonNewImage = (ImageButton) view.findViewById(R.id.button_add_image);
			buttonReorderBackward = (ImageButton) view.findViewById(R.id.button_reorder_checkpoint_backward);
			buttonReorderForward = (ImageButton) view.findViewById(R.id.button_reorder_checkpoint_forward);
			buttonDeleteCheckpoint = (ImageButton) view.findViewById(R.id.button_delete_checkpoint);

			// register handler
			buttonNewImage.setOnClickListener(buttonListener);
			buttonReorderBackward.setOnClickListener(buttonListener);
			buttonReorderForward.setOnClickListener(buttonListener);
			buttonDeleteCheckpoint.setOnClickListener(buttonListener);

			return view;
		}

		@Override
		public void onStart() {
			super.onStart();
			
			
		}

		@Override
		public void onStop() {
			super.onStart();

			// persist to database
			saveData();
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);

			// was it really from an image view?
			if (v instanceof ImageView) {
				Long imageId = (Long) v.getTag();
				imageIdtoDelete = imageId;

				// inflate menu
				MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.context_menu_image, menu);

				// set a title
				menu.setHeaderTitle(R.string.context_menu_image_title);
			}
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.action_delete_image:
				// delete image (id was set when context menu was created)
				deleteImage(imageIdtoDelete);
				return true;

			default:
				return super.onContextItemSelected(item);
			}
		}

		/**
		 * 
		 */
		public long getTrailId() {
			return trailId;
		}

		/**
		 * 
		 * @param trailId
		 */
		public void setTrail(long trailId) {
			this.trailId = trailId;
		}

		/**
		 * 
		 */
		public long getCheckpointId() {
			return checkpointId;
		}

		/**
		 * 
		 */
		public void setCheckpoint(long checkpointId) {
			// did it really change?
			if (this.checkpointId != checkpointId) {
				// save current checkpoint data
				saveData();
				// change checkpoint
				this.checkpointId = checkpointId;
				// load new data
				loadData();
			}
		}

		/**
		 * 
		 * @return
		 */
		public OnButtonListener getOnButtonListener() {
			return onButtonListener;
		}

		/**
		 * 
		 * @param onButtonListener
		 */
		public void setOnButtonListener(OnButtonListener onButtonListener) {
			this.onButtonListener = onButtonListener;
		}

		/**
		 * 
		 * @param forward
		 * @param backward
		 */
		public void enableReorderButtons(boolean backward, boolean forward) {
			if (buttonReorderBackward != null) {
				buttonReorderBackward.setEnabled(backward);
			}
			if (buttonReorderForward != null) {
				buttonReorderForward.setEnabled(forward);
			}
		}

		/**
		 * 
		 */
		private void loadData() {
			Cursor cursor;

			// load data into UI elements
			Uri checkpointIdUri = Contract.Checkpoints.getUriId(checkpointId);
			cursor = getActivity().getContentResolver().query(checkpointIdUri, Contract.Checkpoints.READ_PROJECTION, null, null, null);
			if (cursor.moveToNext()) {
				checkBoxShowOnMap.setChecked(cursor.getInt(Contract.Checkpoints.READ_PROJECTION_LOC_SHOW_INDEX) != 0);
				editTextHint.setText(cursor.getString(Contract.Checkpoints.READ_PROJECTION_HINT_INDEX));
			} else {
				checkBoxShowOnMap.setChecked(false);
				editTextHint.setText("");
			}
			cursor.close();

			// load images
			layoutImages.removeAllViews();

			ImageManager imageManager = App.getImageManager();

			Uri imageDirUri = Contract.Images.getUriDir(checkpointId);
			cursor = getActivity().getContentResolver().query(imageDirUri, Contract.Images.READ_PROJECTION, null, null,
					Contract.Images.DEFAULT_SORT_ORDER);
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
				registerForContextMenu(imageView);

				// add to layout
				layoutImages.addView(imageView);
			}
			cursor.close();
		}

		/**
		 * 
		 */
		private void saveData() {

			if (checkpointId == 0) {
				return;
			}

			// save values in database
			ContentValues values = new ContentValues();
			values.put(Contract.Checkpoints.COLUMN_NAME_LOC_SHOW, checkBoxShowOnMap.isChecked() ? 1 : 0);
			values.put(Contract.Checkpoints.COLUMN_NAME_HINT, editTextHint.getText().toString());
			Uri checkpointIdUri = Contract.Checkpoints.getUriId(checkpointId);
			getActivity().getContentResolver().update(checkpointIdUri, values, null, null);
			// update updated timestamp for trail
			values.clear();
			values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
			Uri trailIdUri = Contract.Trails.getUriId(trailId);
			getActivity().getContentResolver().update(trailIdUri, values, null, null);
		}

		/**
		 * 
		 */
		private void addImage() {

			final int CAPTURE = 0;
			final int IMPORT = 1;
			// show a dialog to choose and image source
			String[] sources = { getString(R.string.new_image_capture), getString(R.string.new_image_import) };
			new AlertDialog.Builder(getActivity()).setTitle(R.string.action_new_image)
					.setItems(sources, new DialogInterface.OnClickListener() {
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
		 * 
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
		 * 
		 */
		private void importImage() {

			// issue intent
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent, getString(R.string.new_image_import)), REQUEST_CODE_IMPORT_IMAGE);

			// callback will be made to onActivityResult()
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			// was it successful and something we were expecting?
			if (resultCode == Activity.RESULT_OK && (requestCode == REQUEST_CODE_CAPTURE_IMAGE || requestCode == REQUEST_CODE_IMPORT_IMAGE)) {

				// insert image record to database
				Uri imagesUri = Contract.Images.getUriDir(checkpointId);
				ContentValues values = new ContentValues();
				Uri imageUri = getActivity().getContentResolver().insert(imagesUri, values);
				long imageId = Long.parseLong(imageUri.getLastPathSegment());

				// add image to ImageManager
				boolean added = false;
				if (requestCode == REQUEST_CODE_CAPTURE_IMAGE) {
					added = App.getImageManager().add(imageId, captureTmpFile);
				} else if (requestCode == REQUEST_CODE_IMPORT_IMAGE) {
					Uri uri = data.getData();
					String[] projection = { MediaStore.Images.Media.DATA };
					Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
					int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
					cursor.moveToFirst();
					String path = cursor.getString(column_index);
					cursor.close();
					added = App.getImageManager().add(imageId, new File(path));
				}

				// succeeded in adding image files?
				if (!added) {
					// delete it from database as it makes no sense to keep a
					// record
					// without associated files
					getActivity().getContentResolver().delete(imageUri, null, null);

					// show alert dialog
					new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_add_image_error_title)
							.setMessage(R.string.dialog_add_image_error_message).setPositiveButton(R.string.dialog_ok, null).create()
							.show();
				}

				// update updated timestamp for trail
				values.clear();
				values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
				Uri trailIdUri = Contract.Trails.getUriId(trailId);
				getActivity().getContentResolver().update(trailIdUri, values, null, null);

				// update data to see pictures
				loadData();
			}

			// delete temp file
			if (captureTmpFile != null) {
				captureTmpFile.delete();
				captureTmpFile = null;
			}
		}

		/**
		 * 
		 */
		private void deleteImage(long imageId) {

			final long imageIdFinal = imageId;

			/**
			 * Dialog to ask user about deletion of the checkpoint
			 */
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
					imageView.setImageBitmap(App.getImageManager().getFull(imageIdFinal));

					return new AlertDialog.Builder(getActivity()).setTitle(R.string.action_delete_image)
							.setMessage(R.string.dialog_delete_image_message).setView(imageView).setIcon(R.drawable.ic_delete)
							.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									// delete checkpoint from database
									Uri imageUri = Contract.Images.getUriId(imageIdFinal);
									getActivity().getContentResolver().delete(imageUri, null, null);

									// remove files
									App.getImageManager().delete(imageIdFinal);

									// update updated timestamp for trail
									ContentValues values = new ContentValues();
									values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
									Uri trailIdUri = Contract.Trails.getUriId(trailId);
									getActivity().getContentResolver().update(trailIdUri, values, null, null);

									// load listView
									loadData();
								}
							}).setNegativeButton(R.string.dialog_no, null).create();
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
			if (checkpoint == selectedCheckpoint) {
				// deselect checkpoint
				selectCheckpoint(null);				
			}
			else {	
				// select checkpoint
				selectCheckpoint(checkpoint);
			}
		}

		@Override
		public void onClickedMap(LatLng position) {			
			addCheckpoint(position);				
		}

		@Override
		public void onStartPositioningCheckpoint(Checkpoint checkpoint) {
			// nothing to do
		}

		@Override
		public void onPositionedCheckpoint(Checkpoint checkpoint) {
			// persist to database
			ContentValues values = new ContentValues();
			values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, checkpoint.location.getLatitude());
			values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, checkpoint.location.getLongitude());
			Uri checkpointUri = Contract.Checkpoints.getUriId(checkpoint.id);
			getContentResolver().update(checkpointUri, values, null, null);
		}
	}

	/**
	 * Listener for events from the EditCheckpointFragment
	 */
	private class EditButtonListener implements EditCheckpointFragment.OnButtonListener {

		@Override
		public void onReorderBackwardClicked() {
			reorderSelectedCheckpointBackward();
		}

		@Override
		public void onReorderForwardClicked() {
			reorderSelectedCheckpointForward();
		}

		@Override
		public void onDeleteCheckpointClicked() {
			// show dialog to ask if checkpoint should be really deleted
			reorderSelectedCheckpointForward();
		}
	}

	// the trail we're editing
	private Trail trail = null;

	// map fragment to be reused
	private TrailMapFragment map;

	// fragment to edit a single checkpoint
	private EditCheckpointFragment checkpointEdit;

	// currently selected checkpoint
	Checkpoint selectedCheckpoint;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("EditTrailActivit", "onCreate");

		long trailId = ContentUris.parseId(getIntent().getData());		
		trail = Trail.fromId(this, trailId);
				
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
			checkpointEdit.setTrail(trail.info.id);
			checkpointEdit.setOnButtonListener(new EditButtonListener());
			ft.replace(R.id.layout_checkpoint_edit, checkpointEdit, FRAGMENT_TAG_CPEDIT);
		}
		ft.commit();

		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_edit_trail);

		// show trails name as subtitle
		actionBar.setSubtitle(trail.info.name);

		// make sure nothing is selected
		selectCheckpoint(null);
						
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate menu
		getMenuInflater().inflate(R.menu.menu_edit_trail, menu);

		// disable upload if trail wasn't already uploaded
		MenuItem menuUpdload = menu.findItem(R.id.action_upload_trail);
		menuUpdload.setVisible(trail.info.uploaded != 0);
		
		return true;
	}
	
	
	@Override
	public void onStart() {
		super.onStart();

		Log.d("EditTrailActivit", "onStart");

		// TODO check if trail is not downloaded
		
		// clear old checkpoints
		map.clearCheckpoints();

		// load all checkpoints
		boolean first = true;
		for (Checkpoint checkpoint : trail.checkpoints) {
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
		case android.R.id.home:
			finish();
			return true;

		case R.id.action_upload_trail:
			// upload the trail 
			uploadTrail();
			return true;
			
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 */
	private void addCheckpoint(LatLng position) {

		Checkpoint checkpoint = new Checkpoint();
		
		// init its data
		checkpoint.index = trail.checkpoints.size();
		checkpoint.uuid = UUID.randomUUID();
		checkpoint.location.setLatitude(position.latitude);
		checkpoint.location.setLongitude(position.longitude);
		checkpoint.showLocation = true;
		checkpoint.hint = "";		

		// create new checkpoint in database
		ContentValues values = new ContentValues();
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, checkpoint.location.getLatitude());
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, checkpoint.location.getLongitude());
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_SHOW, checkpoint.showLocation ? 1 : 0);
		values.put(Contract.Checkpoints.COLUMN_NAME_NO, checkpoint.index + 1);
		Uri checkpointsDir = Contract.Checkpoints.getUriDir(trail.info.id);
		Uri checkpointUri = getContentResolver().insert(checkpointsDir, values);
		checkpoint.id = ContentUris.parseId(checkpointUri);

		// update updated timestamp for trail
		values.clear();
		values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
		Uri trailIdUri = Contract.Trails.getUriId(trail.info.id);
		getContentResolver().update(trailIdUri, values, null, null);

		// append to our list
		trail.checkpoints.add(checkpoint);

		// add marker
		map.addCheckpoint(checkpoint, false, true);

		// select added point
		selectCheckpoint(checkpoint);
	}

	/**
	 * 
	 * @param pointId
	 */
	private void selectCheckpoint(Checkpoint checkpoint) {
		// do nothing if the same point was checked before
		if (selectedCheckpoint == checkpoint) {
			return;
		}

		// make it the new selected point
		selectedCheckpoint = checkpoint;

		// select point
		map.selectCheckpoint(checkpoint);

		// load in checkpoint edit fragment
		if (checkpoint != null)
			checkpointEdit.setCheckpoint(checkpoint.id);
		else
			checkpointEdit.setCheckpoint(0);
			

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
		updateEditButtons();
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
				return new AlertDialog.Builder(getActivity()).setTitle(R.string.action_delete_checkpoint)
						.setMessage(R.string.dialog_delete_checkpoint_message).setIcon(R.drawable.ic_delete)
						.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {

								Checkpoint checkpoint = selectedCheckpoint; 
								
								// unselect checkpoint
								selectCheckpoint(null);

								// remove from map
								map.removeCheckpoint(checkpoint);

								// delete checkpoint from database
								Uri checkpointUri = Contract.Checkpoints.getUriId(checkpoint.id);
								getContentResolver().delete(checkpointUri, null, null);

								// update updated timestamp for trail
								ContentValues values = new ContentValues();
								values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
								Uri trailIdUri = Contract.Trails.getUriId(trail.info.id);
								getContentResolver().update(trailIdUri, values, null, null);

								// remove from our list
								trail.checkpoints.remove(checkpoint);

								// update button on checkpoint edit fragment
								updateEditButtons();

								// renumber in database
								reindexCheckpoints();
							}
						}).setNegativeButton(R.string.dialog_no, null).create();
			}

		}

		// show dialog to delete checkpoint
		new DeleteDialogFragment().show(getFragmentManager(), null);
	}

	/**
	 * 
	 * @param checkpointId
	 */
	private void reorderSelectedCheckpointBackward() {
		// get index of checkpoint
		int index = trail.checkpoints.indexOf(selectedCheckpoint);
		if (index > 0) {
			// move it in list
			int newIndex = index - 1;
			trail.checkpoints.remove(selectedCheckpoint);
			trail.checkpoints.add(newIndex, selectedCheckpoint);

			// do it on the map
			map.setCheckpointIndex(selectedCheckpoint, newIndex);

			// update button on checkpoint edit fragment
			updateEditButtons();

			// renumber in database
			reindexCheckpoints();

			// update updated timestamp for trail
			ContentValues values = new ContentValues();
			values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
			Uri trailIdUri = Contract.Trails.getUriId(trail.info.id);
			getContentResolver().update(trailIdUri, values, null, null);

		}
	}

	/**
	 * 
	 * @param checkpointId
	 */
	private void reorderSelectedCheckpointForward() {
		// get index of checkpoint
		int index = trail.checkpoints.indexOf(selectedCheckpoint);
		if (index < trail.checkpoints.size() - 1) {
			// move it in list
			int newIndex = index + 1;
			trail.checkpoints.remove(index);
			trail.checkpoints.add(newIndex, selectedCheckpoint);

			// do it on the map
			map.setCheckpointIndex(selectedCheckpoint, newIndex);

			// update button on checkpoint edit fragment
			updateEditButtons();

			// update updated timestamp for trail
			ContentValues values = new ContentValues();
			values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
			Uri trailIdUri = Contract.Trails.getUriId(trail.info.id);
			getContentResolver().update(trailIdUri, values, null, null);

			// reindex in database
			reindexCheckpoints();
		}
	}

	/**
	 * Makes sure the index of the checkpoints in the database match
	 * @param index
	 */
	private void reindexCheckpoints() {
		
		for (Checkpoint checkpoint : trail.checkpoints) {
			if (checkpoint.index != trail.checkpoints.indexOf(checkpoint)) {
				checkpoint.index = trail.checkpoints.indexOf(checkpoint);
				ContentValues values = new ContentValues();
				values.put(Contract.Checkpoints.COLUMN_NAME_NO, checkpoint.index+1);
				Uri checkpointUri = Contract.Checkpoints.getUriId(checkpoint.id);
				getContentResolver().update(checkpointUri, values, null, null);				
			}
		}
		
		updateEditButtons();
	}

	/**
	 * 
	 */
	private void updateEditButtons() {

		boolean backward = selectedCheckpoint != null &&  (selectedCheckpoint.index > 0);
		boolean forward = selectedCheckpoint != null && (selectedCheckpoint.index <  trail.checkpoints.size() -1 );
		checkpointEdit.enableReorderButtons(backward, forward);
	}

	
	/**
	 * Upload a trail to the server in an asynchronous task
	 */
	private void uploadTrail() {
						
		// deselect checkpoint (saves changes)
		selectCheckpoint(null);
		
		// execute task
		new UploadTask(this, trail.info, false).execute();
	}
		
}
