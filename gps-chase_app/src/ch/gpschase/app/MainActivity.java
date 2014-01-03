package ch.gpschase.app;

import java.util.Date;
import java.util.UUID;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import ch.gpschase.app.data.Client;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.util.Link;

public class MainActivity extends Activity {

	private static final String FRAGMENT_TAG_TRAILS = "trails";
	private static final String FRAGMENT_TAG_CHASES = "chases";

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
	public static class TrailsFragment extends ListFragment {

		/**
		 * 
		 */
		public interface Listener {
			void onStartChase(long trailId);
		}

		/**
		 * 
		 */
		class LoaderCallback implements LoaderCallbacks<Cursor> {

			SimpleCursorAdapter adapter;

			public LoaderCallback(SimpleCursorAdapter adapter) {
				this.adapter = adapter;
			}

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				// create cursor loader
				return new CursorLoader(getActivity(), Contract.Trails.getUriDir(), Contract.Trails.READ_PROJECTION, null, null, null);
			}

			@Override
			public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
				adapter.swapCursor(data);
			}

			@Override
			public void onLoaderReset(Loader<Cursor> arg0) {
				adapter.swapCursor(null);
			}
		}

		/**
		 * 
		 */
		class ItemLongClickListener implements OnItemLongClickListener {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {

				// start action mode
				ActionCallBack ac = new ActionCallBack();
				getActivity().startActionMode(ac);
				ac.update(id, position);
				
				return true;
			}
		}

		/**
		 * 
		 */
		private class ActionCallBack implements ActionMode.Callback {

			private long trailId;
			private int position;
			
			private ActionMode actionMode;
			private MenuItem menuEdit;
			
			ListView listView;
			
			public ActionCallBack() {
				// get a reference to the list view
				listView = TrailsFragment.this.getListView();
				// set reference in fragment
				action = this;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				actionMode = null;				
				// also delete in fragment
				action = null;				
				// make sure item is unchecked
				listView.setItemChecked(position, false);				
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				this.actionMode = mode;
				// inflate a menu resource providing context menu items
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.cab_main_trail, menu);
				
				menuEdit = menu.findItem(R.id.action_edit_trail);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				// finish action mode
				finish();
				
				switch (item.getItemId()) {
				case R.id.action_chase_trail:
					// chase trail
					chaseTrail(trailId);
					return true;
					
				case R.id.action_edit_trail:
					// edit trail
					editTrail(trailId);
					return true;
				
				case R.id.action_delete_trail:
					// delete trail after asking user
					deleteTrail(trailId);
					return true;
				}
				return false;
			}

			/**
			 * Finishes the action mode
			 */
			public void finish() {
				if (actionMode != null) {
					actionMode.finish();
				}
			}

			/**
			 * Updates the action mode
			 */
			public void update(long trailId, int position) {
				if (trailId != this.trailId) {
					// keep id and position
					this.trailId = trailId;
					this.position = position;
										
					// highlight the item
					listView.setItemChecked(position, true);

				}
			}

		}

		// action mode (if currently active)
		private ActionCallBack action = null;

		private Listener listener = null;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// create list adapter and init loader
			SimpleCursorAdapter adapter = new SimpleCursorAdapter (
													getActivity(), 
													R.layout.listrow_trail, null, 
													new String[] { Contract.Trails.COLUMN_NAME_NAME, Contract.Trails.COLUMN_NAME_DESCRIPTION }, 
													new int[] { R.id.textView_trail_name, R.id.textView_trail_description },
													0);			
			setListAdapter(adapter);
			getLoaderManager().initLoader(0, null, new LoaderCallback(adapter));

			// we want to create our own option menu
			setHasOptionsMenu(true);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			// we want to receive long clicks
			ListView listView = getListView(); 
			listView.setOnItemLongClickListener(new ItemLongClickListener());			
			// set to single choice mode
			listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			// provide own option menu here
			inflater.inflate(R.menu.menu_main_trails, menu);
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
		public void onListItemClick(ListView listView, View view, int position, long id) {
			super.onListItemClick(listView, view, position, id);			
			// in action mode?
			if (action == null) {
				// make sure item is not checked
				getListView().setItemChecked(position, false);
				// TODO either start edit mode or start a chase
				chaseTrail(id);
			}
			else {
				// update selection
				action.update(id, position);
			}
		}

		/**
		 * 
		 * @param listener
		 */
		public void setListener(Listener listener) {
			this.listener = listener;
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

							// refresh list
							TrailsFragment.this.getLoaderManager().getLoader(0).forceLoad();

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
			
			final Context ctx = context;
			
			/**
			 *
			 */
			class DownloadTask extends AsyncTask<UUID, Void, Long> {
				ProgressDialog pd = null;
				
				@Override
				protected void onPreExecute() {
					pd = new ProgressDialog(ctx);				
					pd.setIndeterminate(true);
					pd.setMessage(ctx.getResources().getText(R.string.dialog_downloading));
					pd.setIcon(R.drawable.ic_upload);
					pd.show();
				}

				@Override
				protected Long doInBackground(UUID... params) {
					try {
						Client client = new Client(ctx);
						Long trailId = client.downloadTrail(params[0]);
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
						new AlertDialog.Builder(ctx)										//
							.setIcon(android.R.drawable.ic_dialog_alert)					//
							.setTitle(R.string.dialog_download_trail_error_title)			//
							.setMessage(R.string.dialog_download_trail_error_message)		//
							.setPositiveButton(R.string.dialog_ok, null)					//
							.show();														//
						return;
					}		
					// TODO start chase?
				}
			}						
			// execute task
			new DownloadTask().execute(trailUuid);
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
									// refresh list
									TrailsFragment.this.getLoaderManager().getLoader(0).forceLoad();
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
			// start chase through listener
			if (listener != null) {
				listener.onStartChase(trailId);
			}
		}

		/**
		 * 
		 * @param trailId
		 * @return
		 */
		private String getTrailName(long trailId) {
			String name = null;
			Uri uri = Contract.Trails.getUriId(trailId);
			Cursor cursor = getActivity().getContentResolver().query(uri, Contract.Trails.READ_PROJECTION, null, null, null);
			if (cursor.moveToNext()) {
				name = cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX);
			}
			cursor.close();

			return name;
		}

	}

	
	/**
	 * Fragment to display a list of chases
	 */
	public static class ChasesFragment extends ListFragment {

		/**
		 * 
		 */
		class LoaderCallback implements LoaderCallbacks<Cursor> {

			SimpleCursorAdapter adapter;

			public LoaderCallback(SimpleCursorAdapter adapter) {
				this.adapter = adapter;
			}

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				// create cursor loader
				return new CursorLoader(getActivity(), Contract.Chases.getUriDirEx(), Contract.Chases.READ_PROJECTION_EX, null, null, null);
			}

			@Override
			public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
				adapter.swapCursor(data);
			}

			@Override
			public void onLoaderReset(Loader<Cursor> arg0) {
				adapter.swapCursor(null);
			}
		}

		/**
		 * 
		 */
		class Adapter extends SimpleCursorAdapter {

			private java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
			private java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());

			public Adapter(Context context) {
				super(context, R.layout.listrow_chase, null, new String[] {}, new int[] {}, 0);
			}

			@Override
			public void bindView(View view, Context context, Cursor cursor) {
				((TextView) view.findViewById(R.id.textView_trail_name)).setText(cursor
						.getString(Contract.Chases.READ_PROJECTION_EX_TRAIL_NAME_INDEX));
				((TextView) view.findViewById(R.id.textView_player)).setText(cursor
						.getString(Contract.Chases.READ_PROJECTION_EX_PLAYER_INDEX));
				long started = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_STARTED_INDEX);
				long finished = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_FINISHED_INDEX);
				if (started != 0) {
					Date dateTime = new Date(started);
					((TextView) view.findViewById(R.id.textView_started)).setText(dateFormat.format(dateTime) + " "
							+ timeFormat.format(dateTime));
				}
				if (finished != 0) {
					((TextView) view.findViewById(R.id.textView_time)).setText(Utils.formatDuration(finished - started));
				} else {
					((TextView) view.findViewById(R.id.textView_time)).setText("");
				}
			}
		}

		/**
		 * 
		 */
		class ItemLongClickListener implements OnItemLongClickListener {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
				// start action mode
				ActionCallBack ac = new ActionCallBack();
				getActivity().startActionMode(ac);
				ac.update(id,  position);;
				return true;
			}
		}

		/**
		 * 
		 */
		private class ActionCallBack implements ActionMode.Callback {

			private long chaseId;

			private int position;
			
			private ActionMode actionMode;
			
			ListView listView;

			public ActionCallBack() {
				// get a reference to the list view
				listView = ChasesFragment.this.getListView();	
				// set a reference in the fragment
				action = this;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				actionMode = null;
				// also delete in fragment
				action = null;
				// make sure item is unchecked
				listView.setItemChecked(position, false);				
				
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				this.actionMode = mode;
				// inflate a menu resource providing context menu items
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.cab_chase, menu);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.action_delete_chase:
					// finish action mode
					finish();
					// delete trail after asking user
					deleteChase(chaseId);
					return true;
				}
				return false;
			}

			/**
			 * 
			 */
			public void finish() {
				if (actionMode != null) {
					actionMode.finish();
				}
			}

			/**
			 * 
			 */
			public void update(long chaseId, int position) {
				if (chaseId != this.chaseId) {
					// keep id and position
					this.chaseId = chaseId;
					this.position = position;										
					// highlight the item
					listView.setItemChecked(position, true);

				}
			}
		}

		
		/**
		 * 	
		 */
		private class ChaseCreator {

			// used to keep information gathered in dialogs
			private long trailId;
			private String playerName;

			Context context;
						
			public ChaseCreator(Context context) {
				this.context = context;
			}

			/**
			 * 
			 */
			private void askForPlayerName() {
				// create a dialog which has it's OK button enabled when the
				// text entered isn't empty
				final EditText editText = new EditText(context);	//TODO set default value
				final AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.dialog_player_title)
						.setMessage(R.string.dialog_player_message).setView(editText)
						.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// keep player name
								playerName = editText.getText().toString().trim();

								dialog.dismiss();

								// continue
								createAndSwitchToChase();
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
				// the button is initially deactivated, as the field is
				// initially empty:
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
			}

			/**
			 * 
			 */
			private void createAndSwitchToChase() {

				// TODO show Progress dialog
				
				// create a new chase with information gathered
				long now = System.currentTimeMillis();
				ContentValues values = new ContentValues();
				values.put(Contract.Chases.COLUMN_NAME_TRAIL_ID, trailId);
				values.put(Contract.Chases.COLUMN_NAME_PLAYER, playerName);
				values.put(Contract.Chases.COLUMN_NAME_STARTED, now);
				Uri chaseIdUri = context.getContentResolver().insert(Contract.Chases.getUriDir(), values);

				// switch to chase activity
				Intent intent = new Intent(Intent.ACTION_DEFAULT, chaseIdUri, context, ChaseTrailActivity.class);
				startActivity(intent);

				// refresh list
				getLoaderManager().getLoader(0).forceLoad();
			}

			/**
			 * 
			 */
			public void start(long trailId) {
				// keep trail id
				this.trailId = trailId;
				// start directly with player name
				askForPlayerName();
			}
		}

		// action mode (if currently active)
		private ActionCallBack action = null;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// create list adapter and init loader
			Adapter adapter = new Adapter(getActivity());
			setListAdapter(adapter);
			getLoaderManager().initLoader(0, null, new LoaderCallback(adapter));
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			// we want to receive long clicks
			ListView listView = getListView(); 
			listView.setOnItemLongClickListener(new ItemLongClickListener());			
			// set to single choice mode
			listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		}
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			// inflate option menu
			inflater.inflate(R.menu.menu_main_chases, menu);
		}

		@Override
		public void onListItemClick(ListView listView, View view, int position, long id) {
			super.onListItemClick(listView, view, position, id);
			
			// not in action mode?
			if (action == null) {
				// make sure item is not selected
				getListView().setItemChecked(position, false);
				// switch to chase activity
				Uri chaseIdUri = Contract.Chases.getUriId(id);
				Intent intent = new Intent(Intent.ACTION_DEFAULT, chaseIdUri, getActivity(), ChaseTrailActivity.class);
				startActivity(intent);

			} else {
				// update selection
				action.update(id, position);
			}
		}

		/**
		 * Chases the specified trail
		 * 
		 * @param trailId
		 */
		public void chaseTrail(Context context, long trailId) {
			new ChaseCreator(context).start(trailId);
		}

		/**
		 * 
		 * @param chaseId
		 */
		private void deleteChase(long chaseId) {

			final Uri chaseUri = Contract.Chases.getUriId(chaseId);

			/**
			 * Dialog to ask before a trail is deleted
			 */
			class DeleteDialogFragment extends DialogFragment {
				@Override
				public Dialog onCreateDialog(Bundle savedInstanceState) {

					return new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_delete_chase_title)
							.setMessage(R.string.dialog_delete_chase_message).setIcon(R.drawable.ic_delete)
							.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// delete chase in database
									getActivity().getContentResolver().delete(chaseUri, null, null);
									// refresh list
									ChasesFragment.this.getLoaderManager().getLoader(0).forceLoad();
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
		 * @return
		 */
		private String getChaseTitle(long chaseId) {
			String title = null;
			Uri uri = Contract.Chases.getUriIdEx(chaseId);
			Cursor cursor = getActivity().getContentResolver().query(uri, Contract.Chases.READ_PROJECTION_EX, null, null, null);
			if (cursor.moveToNext()) {
				title = cursor.getString(Contract.Chases.READ_PROJECTION_EX_TRAIL_NAME_INDEX) + " - "
						+ cursor.getString(Contract.Chases.READ_PROJECTION_EX_PLAYER_INDEX);
			}
			cursor.close();

			return title;
		}
	}

	/**
	 * Create both fragments
	 */
	private TrailsFragment trailsFragment;
	private ChasesFragment chasesFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load layout
		setContentView(R.layout.activity_main);

		// create fragments (if not recreated by the system)
		trailsFragment = (TrailsFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_TRAILS);
		if (trailsFragment == null) {
			trailsFragment = new TrailsFragment();
		}
		chasesFragment = (ChasesFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG_CHASES);
		if (chasesFragment == null) {
			chasesFragment = new ChasesFragment();
		}

		// setup action bar
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);

		final Tab trailsTab = actionBar.newTab().setText(R.string.tab_title_trails).setTag(trailsFragment);
		trailsTab.setTabListener(new TabListener<TrailsFragment>(trailsFragment, FRAGMENT_TAG_TRAILS));
		actionBar.addTab(trailsTab);

		final Tab chasesTab = actionBar.newTab().setText(R.string.tab_title_chases).setTag(chasesFragment);
		chasesTab.setTabListener(new TabListener<ChasesFragment>(chasesFragment, FRAGMENT_TAG_CHASES));
		actionBar.addTab(chasesTab);

		actionBar.selectTab(trailsTab); // start with my trails tab

		// set a listener for the trails fragment
		trailsFragment.setListener(new TrailsFragment.Listener() {
			@Override
			public void onStartChase(long trailId) {
				
				actionBar.selectTab(chasesTab);
								
				// forward to
				chasesFragment.chaseTrail(MainActivity.this, trailId);
			}
		});
		
		// check if we have to open a download link
		Intent intent = getIntent();
		if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null) {
			Link.DownloadData data = null;
			try {
				data = Link.parseDownloadLink(intent.getData());
			}
			catch (Exception ex) {
				Log.d("MainActivity", "Error while parsing link", ex);
			}
			
			// start downloading the trail
			if (data != null) {
				trailsFragment.downloadTrail(this,  data.trailUuid);
			}
		}
	}

	
	@Override
	protected void onStart() {
		super.onStart();		
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
		case R.id.action_settings:
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		}
		return false;
	}

	//
	// /**
	// *
	// * @param trailId
	// */
	// private void downloadTrail(UUID trailUuid) {
	//
	// /**
	// *
	// */
	// class DownloadTask extends AsyncTask<UUID, Void, Long> {
	// ProgressDialog pd;
	//
	// @Override
	// protected void onPreExecute() {
	// pd = new ProgressDialog(getActivity());
	// pd.show();
	// }
	//
	// @Override
	// protected Long doInBackground(UUID... params) {
	// try {
	// Client client = new Client(getActivity());
	// long trailId = client.downloadTrail(params[0]);
	// return trailId;
	// } catch (Exception ex) {
	// Log.e("downloadTrail", "Error while downloading trail", ex);
	// return 0L;
	// }
	// }
	//
	// @Override
	// protected void onPostExecute(Long result) {
	// pd.dismiss();
	// if (result == 0) {
	// // TODO show dialog to inform user about failure
	//
	// }
	// }
	// }
	//
	// new DownloadTask().execute(trailUuid);
	// }

}
