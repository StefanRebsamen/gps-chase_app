package ch.gpschase.app;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.ImageManager;

/**
 * 
 */
public class EditCheckpointFragment extends Fragment {

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
	
	// checkpoint we've got to show stuff for
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
	 * @return
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
	}


	/**
	 * 
	 */
	private void addImage() {

		final int CAPTURE = 0;
		final int IMPORT = 1;
		// show a dialog to choose and image source
		String[] sources = { getString(R.string.new_image_capture), getString(R.string.new_image_import) };
		new AlertDialog.Builder(getActivity()).setTitle(R.string.action_new_image).setItems(sources, new DialogInterface.OnClickListener() {
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
		captureTmpFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tmp_" + System.currentTimeMillis()
				+ ".jpg");
		// make sure it's deleted
		try {
			captureTmpFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// let user take a picture. It will be stored to the passed temporary
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
			   String path =  cursor.getString(column_index);	               	               
			   cursor.close();				
			   added = App.getImageManager().add(imageId, new File(path));
			}

			// succeeded in adding image files?
			if (!added) {
				// delete it from database as it makes no sense to keep a record
				// without associated files
				getActivity().getContentResolver().delete(imageUri, null, null);

				// show alert dialog
				new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_add_image_error_title)
						.setMessage(R.string.dialog_add_image_error_message).setPositiveButton(R.string.dialog_ok, null).create().show();
			}
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
				
				return new AlertDialog.Builder(getActivity())
						.setTitle(R.string.dialog_delete_image_title)
						.setMessage(R.string.dialog_delete_image_message)
						.setView(imageView)
    				    .setIcon(R.drawable.ic_delete)
						.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {

								// delete checkpoint from database
								Uri imageUri = Contract.Images.getUriId(imageIdFinal);
								getActivity().getContentResolver().delete(imageUri, null, null);

								// remove files
								App.getImageManager().delete(imageIdFinal);

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
