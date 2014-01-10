package ch.gpschase.app.util;

import java.util.UUID;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import ch.gpschase.app.data.BackendClient;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.EditTrailActivity;
import ch.gpschase.app.R;

	/**
	 *
	 */
	public class UploadTask extends AsyncTask<Void, Void, UUID> {
		
		// context
		private Context context;

		// progress dialog
		private ProgressDialog pd = null;

		// trail id
		long trailId;
		
		// indicates if the link to the trail should be shared afterwards
		boolean shareLink;
		
		/**
		 * Constructor
		 * @param context
		 * @param trailId
		 * @param shareLink
		 */
		public UploadTask(Context context, long trailId, boolean shareLink) {
			super();

			this.context = context;
			this.trailId = trailId;
			this.shareLink = shareLink;
		}
		
		@Override
		protected void onPreExecute() {
			pd = new ProgressDialog(context);
			pd.setCancelable(false);
			pd.setIndeterminate(true);
			pd.setMessage(context.getResources().getText(R.string.dialog_uploading));
			pd.setIcon(R.drawable.ic_upload);
			pd.setTitle(R.string.action_download_trail);			
			pd.show();
		}

		@Override
		protected UUID doInBackground(Void... params) {
			try {
				BackendClient client = new BackendClient(context);
				UUID trailUuid = client.uploadTrail(trailId);
				return trailUuid;
			} catch (Exception ex) {
				Log.e("uploadTrail", "Error while uploading trail", ex);
				return null;
			}
		}

		@Override
		protected void onPostExecute(UUID result) {
			pd.dismiss();
			if (result == null) {
				// show dialog to inform user about failure
				new AlertDialog.Builder(context)						//
					.setIcon(android.R.drawable.ic_dialog_alert)					//
					.setTitle(R.string.dialog_upload_trail_error_title)				//
					.setMessage(R.string.dialog_upload_trail_error_message)			//
					.setPositiveButton(R.string.dialog_ok, null)					//
					.show();														//
				return;
			}
			
			if (shareLink) {
				// create link
				Uri link = Link.createDownloadLink(result);							
				
				// get the name of the trail
				String name = Trail.getName(context, trailId);
				
				// send through any app that is capable 
				CharSequence appName = context.getResources().getText(R.string.app_name);
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Link to trail '" + name + "'");
				sendIntent.putExtra(Intent.EXTRA_TEXT, "Please use the Android app " + appName + " to download trail '" + name + "': " + link);
				sendIntent.setType("text/plain");
				context.startActivity(Intent.createChooser(sendIntent, context.getResources().getText(R.string.dialog_share_trail_title)));					
			}
		}
	}
