package ch.gpschase.app.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import ch.gpschase.app.R;
import ch.gpschase.app.data.Trail;


/**
 * A dialog to enter the password for the specified trail
 */
public abstract class TrailPasswordRequestDialog {

	Context context;
	Trail trail;

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param trail
	 */
	public TrailPasswordRequestDialog(Context context, Trail trail) {
		this.context = context;
		this.trail = trail;
	}

	/**
	 * Show the request dialog. It is repeated until the right password is entered or cancel is pressed
	 */
	public void show(final int titleResId, final int iconResId, boolean retry) {

		LinearLayout.LayoutParams lp; 
		
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);

		final EditText editText = new EditText(context);
		editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
		editText.setHint(R.string.field_password);
		editText.setSingleLine();
		lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.setMargins(8,16,8,16);
		editText.setLayoutParams(lp);
		layout.addView(editText);
		
		final AlertDialog dialog = new AlertDialog.Builder(context) //
				.setTitle(titleResId) 								//
				.setView(layout)									//
				.setIcon(iconResId) 								//
				.setCancelable(false)								//
				.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface d, int whichButton) {
						// check password
						String password = editText.getText().toString();
						String hash = "aVeryUnlikelyValue"; 
						try {
							hash = HashUtils.computeSha1Hash(password);
						}
						catch (Exception ex){
							Log.e("show", "error while calculating password hash", ex);
						}
						if (hash.equals(trail.password)) {
							onSuccess();
						} else {
							// show dialog again
							show(titleResId, iconResId, true);
						}
					}
				})
				// cancel button
				.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface d, int whichButton) {
						// callback
						onCancelled();
					}
				}).create();

		dialog.show();
	}

	/**
	 * Called after a successful entry of the password
	 */
	protected abstract void onSuccess();

	/**
	 * Called after the user cancelled the attempt
	 */
	protected abstract void onCancelled();

}
