package ch.gpschase.app;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import ch.gpschase.app.R.menu;
import ch.gpschase.app.data.BackendClient;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.data.TrailInfo;
import ch.gpschase.app.util.ChaseCreator;
import ch.gpschase.app.util.SelectableListFragment;
import ch.gpschase.app.util.TrailDownloadLink;
import ch.gpschase.app.util.DownloadTask;
import ch.gpschase.app.util.Duration;
import ch.gpschase.app.util.UploadTask;

public class MainActivity extends Activity {

	private static final String FRAGMENT_TAG_MY_TRAILS = "myTrails";
	private static final String FRAGMENT_TAG_CLOUD_TRAILS = "cloudTrails";
		
	private MyTrailsFragment myTailsFragment;
	private CloudTrailsFragment cloudTrailsFragment;
	
	/**
	 * 
	 */
	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {

		private final Fragment fragment;
		private final String tag;

		public TabListener(Fragment fragment, String tag) {
			this.fragment = fragment;
			this.tag = tag;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment.isDetached()) {
				ft.attach(fragment);
			} else if (!fragment.isAdded()) {
				ft.add(R.id.layout_container, fragment, tag);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.detach(fragment);
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

	/**
	 * Fragment to display list of trails
	 */
	public static class MyTrailsFragment extends SelectableListFragment {

		/**
		 * 
		 */
		public MyTrailsFragment() {
			super(Contract.Trails.getUriDir(), Contract.Trails.READ_PROJECTION, R.menu.menu_main_trails, R.menu.cab_main_trail);
		}

		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// we want to create our own option menu
			setHasOptionsMenu(true);
		}		

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			// set empty text
			CharSequence emptText = getResources().getText(R.string.empty_text_trails);
			setEmptyText(emptText);			
		}			
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			// provide own option menu here
			inflater.inflate(R.menu.menu_main_trails, menu);
		}
		
		@Override
		protected SimpleCursorAdapter onCreateAdapter() {
			/**
			 * 
			 */
			class Adapter extends SimpleCursorAdapter {
			
				private java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
				private java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
			
				public Adapter() {
					super(getActivity(), R.layout.listrow_trail, null, new String[] {}, new int[] {}, 0);
				}
			
				@Override
				public void bindView(View view, Context context, Cursor cursor) {
					
					String name = cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX);
					String description = cursor.getString(Contract.Trails.READ_PROJECTION_DESCRIPTION_INDEX);
					long updated = cursor.getLong(Contract.Trails.READ_PROJECTION_UPDATED_INDEX);
					boolean downloaded = cursor.getLong(Contract.Trails.READ_PROJECTION_DOWNLOADED_INDEX) != 0; 
					
					// set texts
					((TextView) view.findViewById(R.id.textView_trail_name)).setText(name);
					((TextView) view.findViewById(R.id.textView_trail_description)).setText(description);
					Date dateTime = new Date(updated);
					((TextView) view.findViewById(R.id.textView_trail_updated))
								.setText(dateFormat.format(dateTime) + " " + timeFormat.format(dateTime));
					
					if (downloaded) {
						((ImageView) view.findViewById(R.id.imageView_trail_type)).setImageResource(R.drawable.ic_download);
					} else {
						((ImageView) view.findViewById(R.id.imageView_trail_type)).setImageResource(R.drawable.ic_edit);
					}
					
					// set tags
					view.setTag(R.id.tag_trail_name, name);
					view.setTag(R.id.tag_downloaded, downloaded);
				}
			}
			
			return new Adapter();
		}

		@Override
		protected boolean onActionItemClicked(MenuItem item, int position, long id) {
			
			switch (item.getItemId()) {
			case R.id.action_new_chase:
				// chase trail
				chaseTrail(id);
				return true;
				
			case R.id.action_edit_trail:
				// edit trail
				editTrail(id);				
				return true;
			
			case R.id.action_share_trail:
				// upload and share trail
				shareTrail(id);
				return true;

			case R.id.action_delete_trail:
				// delete trail after asking user
				deleteTrail(id);
				return true;
			}
			return false;
		}


		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.action_new_trail:
				createNewTrail();
				return true;
			}
			return false;
		}

		@Override
		public void onListItemClick(int position, long id) {
			View view = getListView().getChildAt(position);
			if (view != null) {					
				if (((Boolean)view.getTag(R.id.tag_downloaded)).booleanValue()) {
					chaseTrail(id);
				}
				else {
					editTrail(id);
				}
			}			
		}

		@Override
		public void onSelectionChanged(int position, long id) {
			if (actionMode != null) {				
				View view = getListView().getChildAt(position);
				if (view != null) {
					// update title				
					actionMode.setTitle((String)view.getTag(R.id.tag_trail_name));					
					// modify menu
					MenuItem menuEdit =  actionMode.getMenu().findItem(R.id.action_edit_trail);
					if (menuEdit != null) {
						menuEdit.setVisible(!(Boolean)view.getTag(R.id.tag_downloaded));
					}
					MenuItem menuShare =  actionMode.getMenu().findItem(R.id.action_share_trail);
					if (menuShare != null) {
						menuShare.setVisible(!(Boolean)view.getTag(R.id.tag_downloaded));
					}
				}
			}			
		}
		
		/**
		 * 
		 * @param name
		 */
		private void createNewTrail() {

			// create a dialog which has it's OK button enabled when the text
			// entered isn't empty
			final EditText editText = new EditText(getActivity());
			final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_new_trail_title)
					.setMessage(R.string.dialog_new_trail_message).setView(editText)
					.setIcon(R.drawable.ic_new)
					.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							// insert new trail with specified name (or
							// <untitled>
							// if empty)
							String name = editText.getText().toString().trim();

							if (TextUtils.isEmpty(name)) {
								name = getString(android.R.string.untitled);
							}
							ContentValues values = new ContentValues();
							values.put(Contract.Trails.COLUMN_NAME_NAME, name);
							values.put(Contract.Trails.COLUMN_NAME_UPDATED, System.currentTimeMillis());
							Uri trailUri = getActivity().getContentResolver().insert(Contract.Trails.getUriDir(), values);

							// switch to edit activity
							Intent intent = new Intent(Intent.ACTION_DEFAULT, trailUri, getActivity(), EditTrailActivity.class);
							startActivity(intent);							
						}
					}).setNegativeButton(R.string.dialog_cancel, null).create();
			// add listener to enable/disable OK button
			editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence c, int i, int i2, int i3) {
				}

				@Override
				public void onTextChanged(CharSequence c, int i, int i2, int i3) {
				}

				@Override
				public void afterTextChanged(Editable editable) {
					if (editable.toString().trim().length() == 0) {
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					} else {
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}
				}
			});

			// show the Dialog:
			dialog.show();
			// the button is initially deactivated, as the field is initially
			// empty:
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
		}

		
		/**
		 * Downloads a trail from the server in an asynchronous task
		 */
		public void downloadTrail(Context context, UUID trailUuid) {			
			// execute task
			TrailInfo info =  new TrailInfo();
			info.uuid = trailUuid;
			new DownloadTask(context, info, true).execute();
		}
		
		/**
		 * 
		 * @param trailId
		 */
		private void deleteTrail(long trailId) {

			final Uri trailUri = Contract.Trails.getUriId(trailId);

			/**
			 * Dialog to ask before a trail is deleted
			 */
			class DeleteDialogFragment extends DialogFragment {
				@Override
				public Dialog onCreateDialog(Bundle savedInstanceState) {

					return new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_delete_trail_title)
							.setMessage(R.string.dialog_delete_trail_message).setIcon(R.drawable.ic_delete)
							.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// delete trail in database
									getActivity().getContentResolver().delete(trailUri, null, null);
									
									// TODO delete on server
									
									// finish action mode
									finishActionMode();									
									// refresh list
									MyTrailsFragment.this.getLoaderManager().getLoader(0).forceLoad();
								}
							}).setNegativeButton(R.string.dialog_no, null).create();
				}
			}

			// show dialog to create new trail
			new DeleteDialogFragment().show(getFragmentManager(), null);
		}

		/**
		 * 
		 * @param trailId
		 */
		private void editTrail(long trailId) {
			// switch to edit activity
			Uri trailUri = Contract.Trails.getUriId(trailId);
			Intent intent = new Intent(Intent.ACTION_DEFAULT, trailUri, getActivity(), EditTrailActivity.class);
			startActivity(intent);
		}

		/**
		 * 
		 * @param trailId
		 */
		private void chaseTrail(long trailId) {
			// check if there's an open chase for this trail
			long openChaseId = 0;
			String selection = Contract.Chases.COLUMN_NAME_TRAIL_ID + "=" + trailId		// 
								+ " AND " 												//
								+ Contract.Chases.COLUMN_NAME_FINISHED + "=0"; 			//
			Cursor chasesCursor =  getActivity().getContentResolver().query(									//
														Contract.Chases.getUriDir(), 					//
														Contract.Chases.READ_PROJECTION, 				//
														selection, 										//
														null, 											//
														null);											//
			if (chasesCursor.moveToNext()) {
				openChaseId = chasesCursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX);
			}
			chasesCursor.close();						
			
			// found an open chase?
			if (openChaseId != 0) {
				// continue
				ChaseTrailActivity.show(getActivity(), openChaseId);
			}
			else {
				// create a new one
				new ChaseCreator(getActivity()).show(trailId);
			}		
		}


		/**
		 * Upload a trail to the server and share it afterwards in an asynchronous task
		 */
		private void shareTrail(long trailId) {

			TrailInfo trail = new TrailInfo();
			
			// execute task
			new UploadTask(getActivity(), trail, true).execute();
		}

	}

	
	/**
	 * Fragment to display list of trails on the clousd
	 */
	public static class CloudTrailsFragment extends Fragment {
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load layout
		setContentView(R.layout.activity_main);

		// create fragments (if not recreated by the system)
		myTailsFragment = (MyTrailsFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_MY_TRAILS);
		if (myTailsFragment == null) {
			myTailsFragment = new MyTrailsFragment();
		}
		cloudTrailsFragment = (CloudTrailsFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_CLOUD_TRAILS);
		if (cloudTrailsFragment == null) {
			cloudTrailsFragment = new CloudTrailsFragment();
		}

		// setup action bar
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);

		final Tab myTrailsTab = actionBar.newTab().setText(R.string.tab_title_my_trails).setTag(myTailsFragment).setIcon(R.drawable.ic_phone);
		myTrailsTab.setTabListener(new TabListener<MyTrailsFragment>(myTailsFragment, FRAGMENT_TAG_MY_TRAILS));
		actionBar.addTab(myTrailsTab);

		final Tab cloudTrailsTab = actionBar.newTab().setText(R.string.tab_title_cloud_trails).setTag(cloudTrailsFragment).setIcon(R.drawable.ic_cloud);
		cloudTrailsTab.setTabListener(new TabListener<CloudTrailsFragment>(cloudTrailsFragment, FRAGMENT_TAG_CLOUD_TRAILS));
		actionBar.addTab(cloudTrailsTab);

		actionBar.selectTab(myTrailsTab); // start with my trails tab
		
		// check if we have to open a download link
		Intent intent = getIntent();
		if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null) {
			TrailDownloadLink.DownloadData data = null;
			try {
				data = TrailDownloadLink.parseDownloadLink(intent.getData());
			}
			catch (Exception ex) {
				Log.d("MainActivity", "Error while parsing link", ex);
			}
			
			// start downloading the trail
			if (data != null) {
				myTailsFragment.downloadTrail(this,  data.trailUuid);
			}
		}
	}


	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		case R.id.action_show_chases:
			// show chases activity
			Intent chasesIntent = new Intent(this, ChasesActivity.class);
			startActivity(chasesIntent);
			return true;

		case R.id.action_settings:
			// show settings activity
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		}
		return false;
	}
}
