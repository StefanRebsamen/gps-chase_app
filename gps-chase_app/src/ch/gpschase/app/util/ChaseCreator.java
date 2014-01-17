package ch.gpschase.app.util;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import ch.gpschase.app.ChaseTrailActivity;
import ch.gpschase.app.R;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.TrailInfo;

/**
 * 	
 */
public class ChaseCreator {

	private TrailInfo trail;

	// used to keep information gathered in dialogs
	private String playerName;

	Context context;
				
	public ChaseCreator(Context context) {
		this.context = context;
	}

//			/**
//			 * 
//			 */
//			private void chooseTrail() {
//
//				// prepare list of trails
//				final List<Long> trailIds = new ArrayList<Long>();
//				List<String> trailNames = new ArrayList<String>();
//				Uri chaseDirUri = Contract.Trails.getUriDir();
//				Cursor cursor = getActivity().getContentResolver().query(chaseDirUri, Contract.Trails.READ_PROJECTION, null, null, null);
//				while (cursor.moveToNext()) {
//					trailIds.add(cursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX));
//					trailNames.add(cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX));
//				}
//
//				// create a dialog to select a trail
//				final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_new_chase_trail)
//						.setSingleChoiceItems(trailNames.toArray(new String[0]), 0, null)
//						.setIcon(R.drawable.ic_trail)
//						.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog, int whichButton) {
//								// get Id of selected trail
//								int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
//								trailId = trailIds.get(position);
//
//								// continue
//								askForPlayerName();
//							}
//						}).setNegativeButton(R.string.dialog_cancel, null).create();
//
//				// show the Dialog:
//				dialog.show();
//			}			
//			
	/**
	 * 
	 */
	private void askForPlayerName() {
		// create a dialog which has it's OK button enabled when the
		// text entered isn't empty
		final EditText editText = new EditText(context);
		editText.setText(R.string.dialog_new_chase_default_player);
		final AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.action_new_chase)
				.setIcon(R.drawable.ic_play)
				.setMessage(R.string.dialog_new_chase_player)
				.setView(editText)
				.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// keep player name
						playerName = editText.getText().toString().trim();
						dialog.dismiss();
						// continue
						create();
					}
				}).setNegativeButton(R.string.dialog_cancel, null).create();

		// show the Dialog:
		dialog.show();
	}

	/**
	 * 
	 */
	private void create() {
		
		// create a new chase with information gathered
		long now = System.currentTimeMillis();
		ContentValues values = new ContentValues();
		values.put(Contract.Chases.COLUMN_NAME_TRAIL_ID, trail.id);
		values.put(Contract.Chases.COLUMN_NAME_PLAYER, playerName);
		values.put(Contract.Chases.COLUMN_NAME_STARTED, now);
		values.put(Contract.Chases.COLUMN_NAME_FINISHED, 0);
		Uri chaseIdUri = context.getContentResolver().insert(Contract.Chases.getUriDir(), values);

		// show activity
		long chaseId = ContentUris.parseId(chaseIdUri);
		ChaseTrailActivity.show(context, chaseId);
	}

	/**
	 * 
	 */
	public void show(TrailInfo trail) {
		// keep trail
		this.trail = trail;
		// start directly with player name
		askForPlayerName();
	}
}
