package ch.gpschase.app.util;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * 
 */
public abstract class SelectableListFragment extends ListFragment implements ActionMode.Callback, AdapterView.OnItemLongClickListener {
	
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
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// set to single choice mode
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		// listen to long clock
		getListView().setOnItemLongClickListener(this);
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