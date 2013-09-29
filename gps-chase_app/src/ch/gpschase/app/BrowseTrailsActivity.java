package ch.gpschase.app;

import ch.gpschase.app.data.Client;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;


/**
 * 
 */
public class BrowseTrailsActivity extends Activity {

	/**
	 * 
	 */
	private class QueryButtonClickListener implements OnClickListener {
		
		@Override
		public void onClick(View v) {

			/**
			 *
			 */
			class QueryTask extends AsyncTask<Void, Void, Boolean> 
			{				
				ProgressDialog pd;

				@Override
				protected void onPreExecute() {
					pd = new ProgressDialog(BrowseTrailsActivity.this);
					pd.show();
				}
				
				@Override
				protected Boolean doInBackground(Void... param) {
					try {

						Client client = new Client(BrowseTrailsActivity.this);
						client.findTrails();
						return true;
					}
					catch (Exception ex) {
						Log.e("publishTrail", "Error while querying for trails", ex);				
						return false;
					}
				}

				@Override
				protected void onPostExecute(Boolean result) {
					pd.dismiss();
					if (!result) {
						// TODO show dialog to inform user about failure
						
					}
				}				
			}
			
			new QueryTask().execute();
			
		}
	}
	
	
	// references to UI elements
	private ImageButton buttonQuery;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// load layout
		setContentView(R.layout.activity_browse_trails);

		// get references to UI elements
		buttonQuery = (ImageButton)findViewById(R.id.button_query);

		// add listeners
		buttonQuery.setOnClickListener(new QueryButtonClickListener());
		
		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_settings);
	}

}
