package ch.gpschase.app.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import ch.gpschase.app.R;
import ch.gpschase.app.data.BackendClient;
import ch.gpschase.app.data.Trail;

/**
 * An asynchronous task to download a trail.
 * While downloading a trail, it displays a progress dialog. 
 */
public class DownloadTask extends AsyncTask<Void, Void, Boolean> {

	// context
	private Context context;

	// trail
	Trail trail;
	
	// progress dialog
	private ProgressDialog pd = null;

	// The trail was that actually downloaded
	private Trail downloadedTrail = null;
	
	// flag that the progress dialg should be shown from the start, not just after a progress event
	boolean initialProgressDialog = false;
	
	/**
	 * Constructor
	 * @param context
	 * @param trailUuid
	 */
	public DownloadTask(Context context, Trail trail, boolean initialProgressDialog) {
		super();

		this.context = context;
		this.trail = trail;
		this.initialProgressDialog = initialProgressDialog;			
	}


	/**
	 * 
	 * @param values
	 */
	protected void onProgressUpdate() {
		
		// show progress dialog if not already done
		if (pd == null) {
			pd = new ProgressDialog(context);
			pd.setCancelable(false);
			pd.setIndeterminate(true);
			pd.setMessage(context.getResources().getText(R.string.dialog_downloading));
			pd.setIcon(R.drawable.ic_download);
			pd.setTitle(R.string.action_download_trail);
			pd.show();
		}
	}
	
	
	@Override
	protected void onPreExecute() {
		// force progress dialog right from start if required
		if (initialProgressDialog) {
			onProgressUpdate();
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			// create a client and download the trail
			BackendClient client = new BackendClient(context);
			downloadedTrail = client.downloadTrail(trail);
			return true;
		} catch (Exception ex) {
			Log.e("downloadTrail", "Error while downloading trail", ex);
			return false;
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		// dismiss progress dialog
		if (pd != null) {
			pd.dismiss();
			pd = null;
		}
		
		if (!result) {
			// show dialog to inform user about failure
			new AlertDialog.Builder(context) //
					.setIcon(android.R.drawable.ic_dialog_alert) //
					.setTitle(R.string.dialog_title_error) //
					.setMessage(R.string.dialog_download_trail_error_message) //
					.setPositiveButton(R.string.dialog_ok, null) //
					.show(); //
			return;
		}
		
		// continue
		onDownloaded(downloadedTrail);
	}

	/**
	 * Called at the end of a download. Might be overridden to execute something
	 * after a successful download
	 */
	protected void onDownloaded(Trail trail) {

	}
}
