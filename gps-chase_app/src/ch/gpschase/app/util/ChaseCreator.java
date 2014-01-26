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
import ch.gpschase.app.data.Chase;
import ch.gpschase.app.data.Trail;

/**
 *	Dialog to enter a player name for creating a new chase 	
 */
public class ChaseCreator {

	private Context context;

	private Trail trail;

	/**
	 * Constructor
	 * @param context
	 */
	public ChaseCreator(Context context) {
		this.context = context;
	}
		
	/**
	 * 
	 */
	private void askForPlayerName() {
		// create a dialog which has it's OK button enabled when the
		// text entered isn't empty
		final EditText editText = new EditText(context);
		editText.setText(R.string.dialog_new_chase_default_player);
		editText.setHint(R.string.dialog_new_chase_player_hint);
		editText.setSingleLine();
		
		new AlertDialog.Builder(context)											//
				.setTitle(R.string.action_chase_trail)								//
				.setIcon(R.drawable.ic_play)										//
				.setView(editText)													//
				.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// get player name
						String playerName = editText.getText().toString().trim();
						dialog.dismiss();
						
						// create a new chase
						Chase chase = Chase.create(trail);
						chase.player = playerName;
						chase.save(context);
						
						// show activity
						ChaseTrailActivity.show(context, chase);
					}
				})																	//
				.setNegativeButton(R.string.dialog_cancel, null)					//
				.create()															//
				.show();
	}

	
	/**
	 * 
	 */
	public void show(Trail trail) {
		// keep trail
		this.trail = trail;
		
		// start directly with player name
		askForPlayerName();
	}
}
