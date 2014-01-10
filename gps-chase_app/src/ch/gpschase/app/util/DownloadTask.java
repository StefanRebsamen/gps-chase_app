package ch.gpschase.app.util;

import java.util.UUID;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import ch.gpschase.app.R;
import ch.gpschase.app.data.BackendClient;

/**
 * An asynchronous task to download a trail.
 * While downloading a trail, it displays a progress dialog. 
 */
public class DownloadTask extends AsyncTask<Void, Void, Long> {

	// context
	private Context context;

	// trail uuid
	UUID trailUuid;
	
	// progress dialog
	private ProgressDialog pd = null;

	/**
	 * Constructor
	 * @param context
	 * @param trailUuid
	 */
	public DownloadTask(Context context, UUID trailUuid) {
		super();

		this.context = context;
		this.trailUuid = trailUuid;
	}

	
	@Override
	protected void onPreExecute() {
		pd = new ProgressDialog(context);
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.setMessage(context.getResources().getText(R.string.dialog_downloading));
		pd.setIcon(R.drawable.ic_download);
		pd.setTitle(R.string.action_download_trail);
		pd.show();
	}

	@Override
	protected Long doInBackground(Void... params) {
		try {
			// create a client and download the dialog
			BackendClient client = new BackendClient(context);
			Long trailId = client.downloadTrail(trailUuid);
			return trailId;
		} catch (Exception ex) {
			Log.e("downloadTrail", "Error while downloading trail", ex);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Long result) {
		pd.dismiss();
		if (result == null) {
			// show dialog to inform user about failure
			new AlertDialog.Builder(context) //
					.setIcon(android.R.drawable.ic_dialog_alert) //
					.setTitle(R.string.dialog_download_trail_error_title) //
					.setMessage(R.string.dialog_download_trail_error_message) //
					.setPositiveButton(R.string.dialog_ok, null) //
					.show(); //
			return;
		}
		// continue
		onComplete(result);
	}

	/**
	 * Called at the end of a download. Might be overridden to execute something
	 * after a successful download
	 */
	protected void onComplete(long trailId) {

	}
}
