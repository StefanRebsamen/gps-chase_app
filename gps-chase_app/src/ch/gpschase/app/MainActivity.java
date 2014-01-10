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
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import ch.gpschase.app.R.menu;
import ch.gpschase.app.data.BackendClient;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.Link;
import ch.gpschase.app.util.DownloadTask;
import ch.gpschase.app.util.Duration;

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
	 * 
	 */
	public static abstract class SelectableListFragment extends ListFragment implements ActionMode.Callback, AdapterView.OnItemLongClickListener {
		
		// Uri and projection of the data
		private Uri uri; 
		private String [] projection;
		
		// Resource ids for option and contextual menu 
		private int optionsMenuRes;
		private int contextualMenuRes;
		
		// id and position of the selected item
		protected long selectedId = 0;
		protected int selectedPosition = -1;
		
		// action mode
		protected ActionMode actionMode; 
		
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
				return new CursorLoader(getActivity(), uri, projection, null, null, null);
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
		 * Constructor
		 * @param uri
		 * @param projection
		 * @param optionsMenuRes
		 * @param contextualMenuRes
		 */
		public SelectableListFragment (Uri uri, String [] projection, int optionsMenuRes, int contextualMenuRes) {
			super();
			this.uri = uri;
			this.projection = projection;
			this.optionsMenuRes = optionsMenuRes;
			this.contextualMenuRes = contextualMenuRes;
		}

		/**
		 * Called
		 * @return Adpter
		 */
		protected abstract SimpleCursorAdapter onCreateAdapter();

		/**
		 * Gets called when an menu item on the contextual action bar got clicked
		 * @param itemId
		 * @param position
		 * @param id
		 * @return
		 */
		protected abstract boolean onActionItemClicked(MenuItem item, int position, long id);

		/**
		 * @param position
		 * @param id
		 */
		public abstract void onListItemClick(int position, long id);

		/**
		 * Gets called when the selection changed
		 * @param position
		 * @param id
		 */
		public abstract void onSelectionChanged(int position, long id);
			
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// create list adapter and init loader
			SimpleCursorAdapter adapter = onCreateAdapter();
			setListAdapter(adapter);

			getLoaderManager().initLoader(0, null, new LoaderCallback(adapter));

			// we want to create our own option menu
			setHasOptionsMenu(true);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			// set to single choice mode
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			
			getListView().setOnItemLongClickListener(this);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			// provide own option menu here
			inflater.inflate(R.menu.menu_main_trails, menu);
		}

		
		@Override
		public void onListItemClick(ListView listView, View view, int position, long id) {
			
			// not yet in action mode?
			if (actionMode == null) {
				// cancel selection
				getListView().setItemChecked(position, false);
				// forward
				onListItemClick(position, id);
			}
			else {
				// update selection
				getListView().setItemChecked(position, true);
				this.selectedId = id;
				this.selectedPosition = position;
				// notify about the change
				onSelectionChanged(selectedPosition, selectedId);
			}
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			
			// not yet in action mode?
			if (actionMode == null) {				
				actionMode = getActivity().startActionMode(this);
			}
			// update selection
			getListView().setItemChecked(position, true);
			this.selectedId = id;
			this.selectedPosition = position;
			
			// notify about the change
			onSelectionChanged(selectedPosition, selectedId);

			return true;
		}	
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;
			// make sure selection is cleared
			if (this.selectedPosition != -1) {
				getListView().setItemChecked(selectedPosition, false);
			}
			this.selectedId = 0;
			this.selectedPosition = -1;
			
			// notify about the change
			onSelectionChanged(selectedPosition, selectedId);
		}

		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			this.actionMode = mode;
			// inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(this.contextualMenuRes, menu);			
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return onActionItemClicked(item, this.selectedPosition, this.selectedId);
		}		

	
		@Override
		public void onStart() {
			super.onStart();
			
			// refresh list
			getLoaderManager().getLoader(0).forceLoad();			
		}
		
		@Override
		public void onStop() {
			super.onStop();
			
			finishActionMode();			
		}
		
		/**
		 * Finishes the action mode
		 */
		public void finishActionMode() {
			if (actionMode != null) {
				actionMode.finish();
			}
		}

	}	
	
	/**
	 * Fragment to display list of trails
	 */
	public static class TrailsFragment extends SelectableListFragment {

		/**
		 * 
		 */
		public interface Listener {
			void onCreateNewChase(long trailId);
		}


		private Listener listener = null;

		/**
		 * 
		 */
		public TrailsFragment() {
			super(Contract.Trails.getUriDir(), Contract.Trails.READ_PROJECTION, R.menu.menu_main_trails, R.menu.cab_main_trail);
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
						((ImageView) view.findViewById(R.id.imageView_trail_downloaded)).setVisibility(View.VISIBLE);
					} else {
						((ImageView) view.findViewById(R.id.imageView_trail_downloaded)).setVisibility(View.INVISIBLE);
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
				createNewChase(id);
				return true;
				
			case R.id.action_edit_trail:
				// edit trail
				editTrail(id);				
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
					createNewChase(id);
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
				}
				else {
					actionMode.setTitle("");
				}
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
					.setIcon(R.drawable.ic_new_trail)
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
			new DownloadTask(context, trailUuid).execute();
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
		private void createNewChase(long trailId) {
			// start chase through listener
			if (listener != null) {
				listener.onCreateNewChase(trailId);
			}
		}







	}

	
	/**
	 * Fragment to display a list of chases
	 */
	public static class ChasesFragment extends SelectableListFragment {

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
				final EditText editText = new EditText(context);
				editText.setText(R.string.dialog_new_chase_default_player);
				final AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.action_new_chase)
						.setIcon(R.drawable.ic_new_chase)
						.setMessage(R.string.dialog_new_chase_player)
						.setView(editText)
						.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// keep player name
								playerName = editText.getText().toString().trim();
								dialog.dismiss();
								// continue
								create();
							}
						}).setNegativeButton(R.string.dialog_cancel, null).create();

				// show the Dialog:
				dialog.show();
			}

			/**
			 * 
			 */
			private void create() {
				
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
			}

			/**
			 * 
			 */
			public void show(long trailId) {
				// keep trail id
				this.trailId = trailId;
				// start directly with player name
				askForPlayerName();
			}
		}

		/**
		 * 
		 */
		public ChasesFragment() {
			super(Contract.Chases.getUriDirEx(), Contract.Chases.READ_PROJECTION_EX, R.menu.menu_main_chases, R.menu.cab_main_chase);
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
					super(getActivity(), R.layout.listrow_chase, null, new String[] {}, new int[] {}, 0);
				}

				@Override
				public void bindView(View view, Context context, Cursor cursor) {
					
					String name = cursor.getString(Contract.Chases.READ_PROJECTION_EX_TRAIL_NAME_INDEX);
					String player = cursor.getString(Contract.Chases.READ_PROJECTION_EX_PLAYER_INDEX);
					long started = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_STARTED_INDEX);
					long finished = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_FINISHED_INDEX);
					
					// set texts
					((TextView) view.findViewById(R.id.textView_trail_name)).setText(name);
					((TextView) view.findViewById(R.id.textView_player)).setText(player);
					Date dateTime = new Date(started);
					((TextView) view.findViewById(R.id.textView_started)).setText(dateFormat.format(dateTime) + " "
							+ timeFormat.format(dateTime));
					if (finished != 0) {
						((TextView) view.findViewById(R.id.textView_time)).setText(Duration.format(finished - started));
					} else {
						((TextView) view.findViewById(R.id.textView_time)).setText("");
					}
					
					// set tags
					view.setTag(R.id.tag_trail_name, name);
					view.setTag(R.id.tag_player, player);
					view.setTag(R.id.tag_running, finished == 0);
				}
			}
			
			return new Adapter();
		}

		@Override
		protected boolean onActionItemClicked(MenuItem item, int position, long id) {
			
			switch (item.getItemId()) {			
			case R.id.action_continue_chase:
				// continue chase
				continueChase(id);
				return true;
			
			case R.id.action_delete_chase:
				// delete trail after asking user
				deleteChase(id);
				return true;
			}
			return false;
		}

		@Override
		public void onListItemClick(int position, long id) {
			View view = getListView().getChildAt(position);
			if (view != null) {					
				if (((Boolean)view.getTag(R.id.tag_running)).booleanValue()) {
					continueChase(id);
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
					actionMode.setSubtitle((String)view.getTag(R.id.tag_player));					
					// modify menu
					MenuItem menuContinue =  actionMode.getMenu().findItem(R.id.action_continue_chase);
					if (menuContinue != null) {
						menuContinue.setVisible((Boolean)view.getTag(R.id.tag_running));
					}
				}
				else {
					actionMode.setTitle("");
					actionMode.setSubtitle("");
				}
			}			
		}		

		
		/**
		 * Chases the specified trail
		 * 
		 * @param trailId
		 */
		public void createNewChase(Context context, long trailId) {
			
			// TODO check if there's an open chase for this trail
						
			
			new ChaseCreator(context).show(trailId);
		}

		
		/**
		 * Continues the specified chase
		 * @param chaseId
		 */
		private void continueChase(long chaseId) {
			// switch to chase activity
			Uri chaseIdUri = Contract.Chases.getUriId(chaseId);
			Intent intent = new Intent(Intent.ACTION_DEFAULT, chaseIdUri, getActivity(), ChaseTrailActivity.class);
			startActivity(intent);
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

					return new AlertDialog.Builder(getActivity()).setTitle(R.string.action_delete_chase)
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
			public void onCreateNewChase(long trailId) {				
				// forward to
				chasesFragment.createNewChase(MainActivity.this, trailId);
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
			// show settings activity
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		}
		return false;
	}
}
