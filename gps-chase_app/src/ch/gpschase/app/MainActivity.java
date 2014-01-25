package ch.gpschase.app;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import ch.gpschase.app.data.BackendClient;
import ch.gpschase.app.data.Chase;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.ChaseCreator;
import ch.gpschase.app.util.DownloadTask;
import ch.gpschase.app.util.SelectableListFragment;
import ch.gpschase.app.util.TrailDownloadLink;
import ch.gpschase.app.util.UploadTask;

/**
 * 
 */
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
	public static class MyTrailsFragment extends SelectableListFragment<Trail> {
		
		/**
		 * 
		 */
		public MyTrailsFragment() {
			super(R.menu.menu_main_trails, R.menu.cab_main_trail);
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
		protected List<Trail> loadInBackground() {			
			return Trail.list(getActivity());
		}

		@Override
		protected View getView(Trail item, View convertView, ViewGroup parent) {

			// make sure we've got a view
			View view = convertView;
			if (view == null) {
				LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			     view = vi.inflate(R.layout.listrow_trail, null);			
			}
			
			// add item as tag
			view.setTag(item);

			// set UI elements
			((TextView) view.findViewById(R.id.textView_trail_name)).setText(item.name);
			((TextView) view.findViewById(R.id.textView_trail_description)).setText(item.description);
			((TextView) view.findViewById(R.id.textView_trail_updated)).setText(App.formatDateTime(item.updated));

			if (item.downloaded != 0) {
				((ImageView) view.findViewById(R.id.imageView_trail_type)).setImageResource(R.drawable.ic_download);
			} else {
				((ImageView) view.findViewById(R.id.imageView_trail_type)).setImageResource(R.drawable.ic_edit);
			}
			
			return view;
		}

		
		@Override
		protected boolean onActionItemClicked(MenuItem item, int position, long id) {

			View view = getListView().getChildAt(position);
			if (view != null) {
				Trail trail = (Trail) view.getTag();
				switch (item.getItemId()) {
				case R.id.action_new_chase:
					// chase trail
					chaseTrail(trail);
					return true;

				case R.id.action_edit_trail:
					// edit trail
					EditTrailActivity.show(getActivity(), trail);
					return true;

				case R.id.action_share_trail:
					// upload and share trail
					shareTrail(trail);
					return true;

				case R.id.action_delete_trail:
					// delete trail after asking user
					deleteTrail(trail);
					return true;
				}
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
				Trail trail = (Trail) view.getTag();
				if (((Trail) view.getTag()).downloaded != 0) {
					chaseTrail(trail);
				} else {
					EditTrailActivity.show(getActivity(), trail);
				}
			}
		}

		@Override
		public void onSelectionChanged(int position, long id) {
			if (actionMode != null) {
				View view = getListView().getChildAt(position);
				if (view != null) {
					Trail trail = (Trail) view.getTag();
					// update title
					actionMode.setTitle(trail.name);
					// modify menu
					MenuItem menuEdit = actionMode.getMenu().findItem(R.id.action_edit_trail);
					if (menuEdit != null) {
						menuEdit.setVisible(trail.downloaded == 0);
					}
					MenuItem menuShare = actionMode.getMenu().findItem(R.id.action_share_trail);
					if (menuShare != null) {
						menuShare.setVisible(trail.downloaded == 0);
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
			editText.setHint(R.string.dialog_new_trail_name_hint);
			editText.setSingleLine();
			
			final AlertDialog dialog = new AlertDialog.Builder(getActivity())							//
												.setTitle(R.string.dialog_new_trail_title)				//
												.setView(editText).setIcon(R.drawable.ic_new)			//
												.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog, int whichButton) {
							
														// insert new trail with specified name (or
														// <untitled>
														// if empty)
														String name = editText.getText().toString().trim();
							
														if (TextUtils.isEmpty(name)) {
															name = getString(android.R.string.untitled);
														}
							
														Trail trail = new Trail();
														trail.name = name;
														trail.updated = System.currentTimeMillis();
														trail.save(getActivity());
							
														// switch to edit activity
														EditTrailActivity.show(getActivity(), trail);
													}
												})
												.setNegativeButton(R.string.dialog_cancel, null).create();
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
			Trail trail = new Trail();
			trail.uuid = trailUuid;
			new DownloadTask(context, trail, true).execute();
		}

		/**
		 * 
		 * @param trailId
		 */
		private void deleteTrail(Trail trail) {

			final Trail passedTrail = trail;
			
			/**
			 * Asynchronous task to delete trail locally and from server
			 */
			class DeleteTask extends AsyncTask<Void, Void, Boolean> {

				// progress dialog
				private ProgressDialog pd = null;
				
				
				@Override
				protected void onPreExecute() {
					
					pd = new ProgressDialog(getActivity());
					pd.setCancelable(false);
					pd.setIndeterminate(true);
					pd.setMessage(getActivity().getResources().getText(R.string.dialog_deleting));
					pd.setIcon(R.drawable.ic_delete);
					pd.setTitle(R.string.action_delete_trail);			
					pd.show();
				}

				@Override
				protected Boolean doInBackground(Void... params) {
					
					// was it ever uploaded to the server?
					if (passedTrail.uploaded != 0) {
						// delete trail on server
						try {
							BackendClient client = new BackendClient(getActivity() );
							client.deleteTrail(passedTrail);
													
						} catch (Exception ex) {
							Log.e("uploadTrail", "Error while deleting trail", ex);
							return false;
						}
					}
					
					// delete trail in database
					passedTrail.delete(getActivity());
					
					return true;					
				}

				@Override
				protected void onPostExecute(Boolean result) {

					// hide progress dialog
					pd.dismiss();
					
					if (result) {
						// refresh list
						MyTrailsFragment.this.reload();	
					}
					else {
						// show dialog to inform user about failure
						new AlertDialog.Builder(getActivity())								//
							.setIcon(android.R.drawable.ic_dialog_alert)					//
							.setTitle(R.string.dialog_delete_trail_error_title)				//
							.setMessage(R.string.dialog_delete_trail_error_message)			//
							.setPositiveButton(R.string.dialog_ok, null)					//
							.show();														//
						return;
						
					}
				}
			}
			
			// show a dialog for user confirmation
			new DialogFragment() {
				@Override
				public Dialog onCreateDialog(Bundle savedInstanceState) {

					return new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_delete_trail_title)
							.setMessage(R.string.dialog_delete_trail_message).setIcon(R.drawable.ic_delete)
							.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									// finish action mode
									finishActionMode();

									// delete in async task
									new DeleteTask().execute();
									
								}
							}).setNegativeButton(R.string.dialog_no, null).create();
				}
			}.show(getFragmentManager(), null);
		}

		/**
		 * 
		 * @param trailId
		 */
		private void chaseTrail(Trail trail) {

			// find a running chase
			Chase runningChase = trail.getFirstRunningChase(getActivity());
			if (runningChase != null) {
				// continue
				ChaseTrailActivity.show(getActivity(), runningChase);
			} else {
				// create a new one
				new ChaseCreator(getActivity()).show(trail);
			}
		}

		/**
		 * Upload a trail to the server and share it afterwards in an
		 * asynchronous task
		 */
		private void shareTrail(Trail trail) {

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

		final Tab myTrailsTab = actionBar.newTab().setText(R.string.tab_title_my_trails).setTag(myTailsFragment)
				.setIcon(R.drawable.ic_phone);
		myTrailsTab.setTabListener(new TabListener<MyTrailsFragment>(myTailsFragment, FRAGMENT_TAG_MY_TRAILS));
		actionBar.addTab(myTrailsTab);

		final Tab cloudTrailsTab = actionBar.newTab().setText(R.string.tab_title_cloud_trails).setTag(cloudTrailsFragment)
				.setIcon(R.drawable.ic_cloud);
		cloudTrailsTab.setTabListener(new TabListener<CloudTrailsFragment>(cloudTrailsFragment, FRAGMENT_TAG_CLOUD_TRAILS));
		actionBar.addTab(cloudTrailsTab);

		actionBar.selectTab(myTrailsTab); // start with my trails tab

		
		// check if we have to open a download link
		Intent intent = getIntent();
		if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null) {
			TrailDownloadLink.DownloadData data = null;
			try {
				data = TrailDownloadLink.parseDownloadLink(intent.getData());
			} catch (Exception ex) {
				Log.d("MainActivity", "Error while parsing link", ex);
			}

			// start downloading the trail
			if (data != null) {
				myTailsFragment.downloadTrail(this, data.trailUuid); // todo nicht über fragment
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
			SettingsActivity.show(this);
			return true;
		}
		return false;
	}
}
