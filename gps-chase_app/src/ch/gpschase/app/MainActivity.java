package ch.gpschase.app;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.app.ListFragment;
import android.app.LoaderManager;
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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import ch.gpschase.app.data.Contract;


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
	public static class TrailsFragment extends ListFragment {

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
				getActivity().startActionMode(new ActionCallBack(id));

				return true;

			}
		}

		/**
		 * 
		 */
		private class ActionCallBack implements ActionMode.Callback {

			private long trailId;

			private ActionMode actionMode = null;

			public ActionCallBack(long trailId) {
				this.trailId = trailId;

				// set a reference
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
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				this.actionMode = mode;

				// inflate a menu resource providing context menu items
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.cab_main_trail, menu);

				// show trail name in title
				setTitle();

				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

				switch (item.getItemId()) {
				case R.id.action_delete_trail:
					// finish action mode
					finish();
					// delete trail after asking user
					deleteTrail(trailId);
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
			public void update(long trailId) {
				if (trailId != this.trailId) {
					this.trailId = trailId;

					// update trail name in title
					setTitle();
				}
			}

			private void setTitle() {
				if (actionMode != null) {
					actionMode.setTitle(getTrailName(trailId));
				}
			}

		}

		// action mode (if currently active)
		private ActionCallBack action = null;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// create list adapter and init loader
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.listrow_trail, null,
					new String[] { Contract.Trails.COLUMN_NAME_NAME }, new int[] { R.id.textView_trail_name }, 0);
			setListAdapter(adapter);
			TrailsFragment.this.getLoaderManager().initLoader(0, null, new LoaderCallback(adapter));

			// we want to create our own option menu
			setHasOptionsMenu(true);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			// we want to receive long clicks
			getListView().setOnItemLongClickListener(new ItemLongClickListener());
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
			
			// not in action mode?
			if (action == null) {
				// switch to edit mode
				editTrail(id);
			} else {
				// update selection
				action.update(id);
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
							.setMessage(R.string.dialog_delete_trail_message)
							.setIcon(R.drawable.ic_delete)							
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
	 * 
	 */
	public static class ChasesFragment extends ListFragment {

		/**
		 * 	
		 */
		private class ChaseCreator {

			// used to keep information gathered in dialogs
			private long trailId;
			private String playerName;

			/**
			 * 
			 */
			private void chooseTrail() {

				// prepare list of trails
				final List<Long> trailIds = new ArrayList<Long>();
				List<String> trailNames = new ArrayList<String>();
				Uri chaseDirUri = Contract.Trails.getUriDir();
				Cursor cursor = getActivity().getContentResolver().query(chaseDirUri, Contract.Trails.READ_PROJECTION, null, null, null);
				while (cursor.moveToNext()) {
					trailIds.add(cursor.getLong(Contract.Trails.READ_PROJECTION_ID_INDEX));
					trailNames.add(cursor.getString(Contract.Trails.READ_PROJECTION_NAME_INDEX));
				}

				// create a dialog to select a trail
				final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_select_trail_title)
						.setSingleChoiceItems(trailNames.toArray(new String[0]), 0, null)
						.setIcon(R.drawable.ic_trail)
						.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// get Id of selected trail
								int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
								trailId = trailIds.get(position);

								// continue
								askForPlayerName();
							}
						}).setNegativeButton(R.string.dialog_cancel, null).create();

				// show the Dialog:
				dialog.show();
			}

			/**
			 * 
			 */
			private void askForPlayerName() {
				// create a dialog which has it's OK button enabled when the
				// text
				// entered isn't empty
				final EditText editText = new EditText(getActivity());
				final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_player_title)
						.setMessage(R.string.dialog_player_message).setView(editText)
						.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// keep player name
								playerName = editText.getText().toString().trim();

								dialog.dismiss();
								
								//TODO show Progress dialog
								
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

				// create a new chase with information gathered
				long now = System.currentTimeMillis();
				ContentValues values = new ContentValues();
				values.put(Contract.Chases.COLUMN_NAME_TRAIL_ID, trailId);
				values.put(Contract.Chases.COLUMN_NAME_PLAYER, playerName);
				values.put(Contract.Chases.COLUMN_NAME_STARTED, now);
				Uri chaseIdUri = getActivity().getContentResolver().insert(Contract.Chases.getUriDir(), values);

				// switch to chase activity
				Intent intent = new Intent(Intent.ACTION_DEFAULT, chaseIdUri, getActivity(), ChaseTrailActivity.class);
				startActivity(intent);
				
				// refresh list
				ChasesFragment.this.getLoaderManager().getLoader(0).forceLoad();					
			}

			/**
			 * 
			 */
			public void run() {
				// start by choosing a trail
				chooseTrail();
			}
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
				super(context,  R.layout.listrow_chase, null, new String[] {}, new int[]{}, 0);				
			}
			
			@Override
			public void bindView (View view, Context context, Cursor cursor) {				
				((TextView)view.findViewById(R.id.textView_trail_name)).setText(cursor.getString(Contract.Chases.READ_PROJECTION_EX_TRAIL_NAME_INDEX) );				
				((TextView)view.findViewById(R.id.textView_player)).setText(cursor.getString(Contract.Chases.READ_PROJECTION_EX_PLAYER_INDEX) );
				long started = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_STARTED_INDEX);
				long finished = cursor.getLong(Contract.Chases.READ_PROJECTION_EX_FINISHED_INDEX);				
				if (started !=0) {
					Date dateTime = new Date(started);
					((TextView)view.findViewById(R.id.textView_started)).setText(dateFormat.format(dateTime) + " " + timeFormat.format(dateTime));
				}
				if (finished !=0) {
					((TextView)view.findViewById(R.id.textView_time)).setText(Utils.formatDuration(finished-started));
				}				
				else {
					((TextView)view.findViewById(R.id.textView_time)).setText("");					
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
				getActivity().startActionMode(new ActionCallBack(id));

				return true;

			}
		}

		/**
			 * 
			 */
		private class ActionCallBack implements ActionMode.Callback {

			private long chaseId;

			private ActionMode actionMode = null;

			public ActionCallBack(long chaseId) {
				this.chaseId = chaseId;

				// set a reference
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
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				this.actionMode = mode;

				// inflate a menu resource providing context menu items
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.cab_main_chase, menu);

				// show run name in title
				setTitle();

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
			public void update(long chaseId) {
				if (chaseId != this.chaseId) {
					this.chaseId = chaseId;

					// update trail name in title
					setTitle();
				}
			}

			private void setTitle() {
				if (actionMode != null) {
					actionMode.setTitle(getChaseTitle(chaseId));
				}
			}

		}

		// action mode (if currently active)
		private ActionCallBack action = null;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			Log.v("TrailsFragment", "onCreate");
			
			// create list adapter and init loader
			Adapter adapter = new Adapter(getActivity());
			setListAdapter(adapter);
			getLoaderManager().initLoader(0, null, new LoaderCallback(adapter));

			// we want to create our own option menu
			setHasOptionsMenu(true);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			// we want to receive long clicks
			getListView().setOnItemLongClickListener(new ItemLongClickListener());
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			// provide own option menu here
			inflater.inflate(R.menu.menu_main_chases, menu);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.action_new_chase:
				new ChaseCreator().run();
				return true;
			}
			return false;
		}

		@Override
		public void onListItemClick(ListView listView, View view, int position, long id) {
			super.onListItemClick(listView, view, position, id);

			// not in action mode?
			if (action == null) {
				// switch to chase activity
				Uri chaseIdUri = Contract.Chases.getUriId(id);
				Intent intent = new Intent(Intent.ACTION_DEFAULT, chaseIdUri, getActivity(), ChaseTrailActivity.class);
				startActivity(intent);

			} else {
				// update selection
				action.update(id);
			}
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

					return new AlertDialog.Builder(getActivity())
							.setTitle(R.string.dialog_delete_chase_title)
							.setMessage(R.string.dialog_delete_chase_message)
							.setIcon(R.drawable.ic_delete)
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
				title = cursor.getString(Contract.Chases.READ_PROJECTION_EX_TRAIL_NAME_INDEX) + " - " + cursor.getString(Contract.Chases.READ_PROJECTION_EX_PLAYER_INDEX);
			}
			cursor.close();

			return title;
		}
	}

	/**
	 * Create both fragments
	 */
	private ChasesFragment chasesFragment;
	private TrailsFragment trailsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		// load layout
		setContentView(R.layout.activity_main);
		
		// create fragments (if not recreated by the system)
		chasesFragment = (ChasesFragment)getFragmentManager().findFragmentByTag(FRAGMENT_TAG_CHASES);
		if (chasesFragment == null) {
			chasesFragment = new ChasesFragment();
		}
		
		trailsFragment = (TrailsFragment)getFragmentManager().findFragmentByTag(FRAGMENT_TAG_TRAILS);
		if (trailsFragment == null) {
			trailsFragment = new TrailsFragment();
		}
				
		// setup action bar
		ActionBar actionBar = getActionBar();	
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);		
			
		Tab chasesTab = actionBar.newTab().setText(R.string.tab_title_chases).setTag(chasesFragment);
		chasesTab.setTabListener(new TabListener(chasesFragment, FRAGMENT_TAG_CHASES));
		chasesTab.setIcon(R.drawable.ic_chase);
		actionBar.addTab(chasesTab);	
		
		Tab trailsTab = actionBar.newTab().setText(R.string.tab_title_trails).setTag(trailsFragment);
		trailsTab.setTabListener(new TabListener(trailsFragment, FRAGMENT_TAG_TRAILS));
		trailsTab.setIcon(R.drawable.ic_trail);
		actionBar.addTab(trailsTab);
		
		actionBar.selectTab(chasesTab); 	// start with chases tab
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
			//App.getImageManager().cleanupFiles();

			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			
			return true;
		}
		return false;
	}
	
}
