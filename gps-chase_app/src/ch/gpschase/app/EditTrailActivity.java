package ch.gpschase.app;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ch.gpschase.app.data.Contract;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class EditTrailActivity extends Activity {

	
	private static final String FRAGMENT_TAG_CPEDIT = "cpedit";
	private static final String FRAGMENT_TAG_MAP = "map";
	
	/**
	 * 
	 */
	private class MapListener implements TrailMapFragment.MapListener {
		@Override
		public void onClickedPoint(long pointId) {
			// select checkpoint
			selectCheckpoint(pointId);
		}

		@Override
		public void onClickedMap(LatLng position) {
			// deselect checkpoint
			selectCheckpoint(0);
		}

		@Override
		public void onStartPositioningPoint(long pointId) {
			// nothing to do
		}

		@Override
		public void onPositionedPoint(long pointId, LatLng position) {
			// persist to database
			ContentValues values = new ContentValues();
			values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, position.latitude);
			values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, position.longitude);
			Uri checkpointUri = Contract.Checkpoints.getUriId(pointId);
			getContentResolver().update(checkpointUri, values, null, null);
		}
	}

	/**
	 * 
	 */
	private class EditButtonListener implements EditCheckpointFragment.OnButtonListener {

		@Override
		public void onReorderBackwardClicked() {
			reorderCheckpointBackward(selectedCheckpointId);
		}

		@Override
		public void onReorderForwardClicked() {
			reorderCheckpointForward(selectedCheckpointId);
		}

		@Override
		public void onDeleteCheckpointClicked() {
			// show dialog to ask if checkpoint should be really deleted
			deleteCheckpoint(selectedCheckpointId);
		}		
	}

	
	// ID of the trail we're editing
	private long trailId;

	// map fragment to be reused
	private TrailMapFragment map;

	// fragment to edit a single checkpoint
	private EditCheckpointFragment checkpointEdit;

	// list of checkpoints
	private final List<Long> checkpointList = new ArrayList<Long>();

	// currently selected checkpoint
	long selectedCheckpointId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("EditTrailActivit", "onCreate");

		// keep trail id
		trailId = ContentUris.parseId(getIntent().getData());

		// load layout
		setContentView(R.layout.activity_edit_trail);
		
		// create and add fragments (if not recreated)
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		
		map = (TrailMapFragment)getFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
		if (map == null) {
			map = new TrailMapFragment();
			ft.replace(R.id.layout_map, map, FRAGMENT_TAG_MAP);
		}		
		map.setMapListener(new MapListener());
		map.setDraggable(true);
		
		checkpointEdit = (EditCheckpointFragment)getFragmentManager().findFragmentByTag(FRAGMENT_TAG_CPEDIT);
		if (checkpointEdit == null) {
			checkpointEdit = new EditCheckpointFragment();
			checkpointEdit.setOnButtonListener(new EditButtonListener());		
			ft.replace(R.id.layout_checkpoint_edit, checkpointEdit, FRAGMENT_TAG_CPEDIT);
		}		
		ft.commit();
		
		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_edit_trail);

		// show trails name as subtitle
		Cursor cursor = getContentResolver().query(getIntent().getData(), Contract.Trails.READ_PROJECTION, null, null, null);
		if (cursor.moveToNext()) {
			String name = cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX);
			getActionBar().setSubtitle(name);
		}
		cursor.close();

		// make sure nothing is selected
		selectedCheckpointId = -1;
		selectCheckpoint(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate menu
		getMenuInflater().inflate(R.menu.menu_edit_trail, menu);

		return true;
	}

	@Override
	public void onStart() {
		super.onStart();

		Log.d("EditTrailActivit", "onStart");

		// clear old checkpoints
		map.clearPoints();
		checkpointList.clear();

		// load all checkpoints
		boolean first = true;
		Uri checkpointDir = Contract.Checkpoints.getUriDir(trailId);
		Cursor cursor = getContentResolver().query(checkpointDir, Contract.Checkpoints.READ_PROJECTION, null, null, null);
		while (cursor.moveToNext()) {

			Long id = cursor.getLong(Contract.Checkpoints.READ_PROJECTION_ID_INDEX);
			LatLng location = new LatLng(cursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LAT_INDEX),
					cursor.getDouble(Contract.Checkpoints.READ_PROJECTION_LOC_LNG_INDEX));

			// append to our list
			checkpointList.add(id);

			// add marker
			map.addPoint(id, location, false, false);

			// mark point as selected if necessary
			if (id == selectedCheckpointId) {
				map.selectPoint(id);
			}

			// set camera to start
			if (first) {
				map.setCameraTarget(location);
				map.setCameraZoom(TrailMapFragment.DEFAULT_ZOOM);
				first = false;
			}
		}
		cursor.close();

		// refresh map
		map.refresh();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;

		case R.id.action_new_checkpoint:
			LatLng position = map.getCameraTarget();
			addCheckpoint(position);
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 */
	private void addCheckpoint(LatLng position) {

		// determine number it gets
		int no = checkpointList.size() + 1;

		// create new checkpoint
		ContentValues values = new ContentValues();
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LAT, position.latitude);
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_LNG, position.longitude);
		values.put(Contract.Checkpoints.COLUMN_NAME_LOC_SHOW, 1);	// shown by default
		values.put(Contract.Checkpoints.COLUMN_NAME_NO, no);

		Uri checkpointsDir = Contract.Checkpoints.getUriDir(trailId);
		Uri checkpointUri = getContentResolver().insert(checkpointsDir, values);
		long checkpointId = ContentUris.parseId(checkpointUri);

		// append to our list
		checkpointList.add(checkpointId);

		// add marker
		map.addPoint(checkpointId, position, false, true);

		// select added point
		selectCheckpoint(checkpointId);
	}

	/**
	 * 
	 * @param pointId
	 */
	private void selectCheckpoint(long pointId) {
		// do nothing if the same point was checked before
		if (selectedCheckpointId == pointId) {
			return;
		}

		// make it the new selected point
		selectedCheckpointId = pointId;

		// select point
		map.selectPoint(pointId);

		// load in checkpoint edit fragment
		checkpointEdit.setCheckpoint(pointId);

		if (pointId != 0 && checkpointEdit.isHidden()) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.show(checkpointEdit);
			ft.commit();
		} else if (pointId == 0 && !checkpointEdit.isHidden()) {
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
	private void deleteCheckpoint(long checkpointId) {
		// keep pointId and index of point
		final long checkpointIdFinal = checkpointId;
		final int index = checkpointList.indexOf(Long.valueOf(checkpointIdFinal));

		/**
		 * Dialog to ask user about deletion of the checkpoint
		 */
		class DeleteDialogFragment extends DialogFragment {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				return new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_delete_checkpoint_title)
						.setMessage(R.string.dialog_delete_checkpoint_message)
						.setIcon(R.drawable.ic_delete)
						.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {

								// unselect checkpoint
								selectCheckpoint(0);

								// remove from map
								map.removePoint(checkpointIdFinal);

								// delete checkpoint from database
								Uri checkpointUri = Contract.Checkpoints.getUriId(checkpointIdFinal);
								getContentResolver().delete(checkpointUri, null, null);
								
								// remove from our list
								checkpointList.remove(Long.valueOf(checkpointIdFinal));

								// update button on checkpoint edit fragment
								updateEditButtons();

								// renumber in database
								renumberCheckpoints(index);
							}
						}).setNegativeButton(R.string.dialog_no, null).create();
			}

		}

		// show dialog
		if (index >= 0) {
			// show dialog to delete checkpoint
			new DeleteDialogFragment().show(getFragmentManager(), null);
		}
	}

	/**
	 * 
	 * @param checkpointId
	 */
	private void reorderCheckpointBackward(long checkpointId) {
		// get index of checkpoint
		int index = checkpointList.indexOf(Long.valueOf(checkpointId));
		if (index > 0) {
			// move it in list
			int newIndex = index - 1;
			checkpointList.remove(index);
			checkpointList.add(newIndex, checkpointId);

			// do it on the map
			map.setPointIndex(checkpointId, newIndex);

			// renumber in database
			renumberCheckpoints(newIndex);

			// update button on checkpoint edit fragment
			updateEditButtons();
		}
	}

	/**
	 * 
	 * @param checkpointId
	 */
	private void reorderCheckpointForward(long checkpointId) {
		// get index of checkpoint
		int index = checkpointList.indexOf(Long.valueOf(checkpointId));
		if (index < checkpointList.size() - 1) {
			// move it in list
			int newIndex = index + 1;
			checkpointList.remove(index);
			checkpointList.add(newIndex, checkpointId);

			// do it on the map
			map.setPointIndex(checkpointId, newIndex);

			// update button on checkpoint edit fragment
			updateEditButtons();
			
			// renumber in database
			renumberCheckpoints(index);


		}
	}

	/**
	 * 
	 * @param index
	 */
	private void renumberCheckpoints(int index) {
		// anything to do?
		if (index <= checkpointList.size() - 1) {
			int no = index + 1;
			// modify number of affected checkpoints in database
			for (long id : checkpointList.subList(index, checkpointList.size() - 1)) {
				ContentValues values = new ContentValues();
				values.put(Contract.Checkpoints.COLUMN_NAME_NO, no);
				Uri checkpointUri = Contract.Checkpoints.getUriId(id);
				getContentResolver().update(checkpointUri, values, null, null);
				// continue with next number
				no++;
			}
		}
	}

	/**
	 * 
	 */
	private void updateEditButtons() {		

		int index = checkpointList.indexOf(Long.valueOf(selectedCheckpointId));
		boolean backward = (index > 0);
		boolean forward = (index >= 0) && (index < checkpointList.size() - 1);
		checkpointEdit.enableReorderButtons(backward,  forward);						
	}

}
