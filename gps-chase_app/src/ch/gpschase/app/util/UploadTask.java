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
	 * An asynchronous task to upload to specified trail
	 */
	public class UploadTask extends AsyncTask<Void, Void, Boolean> {
		
		// context
		private Context context;

		// progress dialog
		private ProgressDialog pd = null;

		// trail
		Trail trail;
				
		// indicates if the link to the trail should be shared afterwards
		boolean shareLink;
		
		/**
		 * Constructor
		 * @param context
		 * @param trail
		 * @param shareLink
		 */
		public UploadTask(Context context, Trail trail, boolean shareLink) {
			super();

			this.context = context;
			this.trail = trail;
			this.shareLink = shareLink;
		}
		
		@Override
		protected void onPreExecute() {
			pd = new ProgressDialog(context);
			pd.setCancelable(false);
			pd.setIndeterminate(true);
			pd.setMessage(context.getResources().getText(R.string.dialog_uploading));
			pd.setIcon(R.drawable.ic_upload);
			pd.setTitle(R.string.action_upload_trail);			
			pd.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				BackendClient client = new BackendClient(context);
				client.uploadTrail(trail);
				return true;
			} catch (Exception ex) {
				Log.e("uploadTrail", "Error while uploading trail", ex);
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			pd.dismiss();
			if (!result) {
				// show dialog to inform user about failure
				new AlertDialog.Builder(context)									//
					.setIcon(android.R.drawable.ic_dialog_alert)					//
					.setTitle(R.string.dialog_title_error)							//
					.setMessage(R.string.dialog_upload_trail_error_message)			//
					.setPositiveButton(R.string.dialog_ok, null)					//
					.show();														//
				return;
			}
			
			if (shareLink) {
				// create link
				Uri link = TrailDownloadLink.createDownloadLink(trail);							
								
				// send through any app that is capable 
				CharSequence appName = context.getResources().getText(R.string.app_name);
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.share_trail_subject, trail.name));
				sendIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.share_trail_text, context.getResources().getString(R.string.app_name), trail.name, link));
				sendIntent.setType("text/plain");
				context.startActivity(Intent.createChooser(sendIntent, context.getResources().getText(R.string.dialog_share_trail_title)));					
			}
		}
	}
