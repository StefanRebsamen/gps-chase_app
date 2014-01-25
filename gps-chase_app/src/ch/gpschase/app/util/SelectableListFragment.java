package ch.gpschase.app.util;

import java.util.List;

import ch.gpschase.app.MainActivity.MyTrailsFragment;
import ch.gpschase.app.data.Item;
import ch.gpschase.app.R;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * 
 */
public abstract class SelectableListFragment<T extends Item> extends ListFragment implements ActionMode.Callback, AdapterView.OnItemLongClickListener {
	
	// Resource ids for option and contextual menu 
	private int optionsMenuRes;
	private int contextualMenuRes;
	
	// id and position of the selected item
	protected long selectedId = 0;
	protected int selectedPosition = -1;
	
	// action mode
	protected ActionMode actionMode; 
	
	/**
	 * Adapter for items
	 */
	class Adapter extends BaseAdapter {
		
		// data to show
		List<T> data;
		
		public Adapter(List<T> data) {
			this.data = data;
		}
		
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return data.get(position).getId(); 
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {			
			T item = data.get(position);
			// delegate to fragment
			return SelectableListFragment.this.getView(item, convertView, parent);		
		}
		
	}
	
	
	/**
	 * 
	 */
	class LoaderCallback implements LoaderCallbacks< List<T> > {
		
		@Override
		public android.content.Loader<List<T>> onCreateLoader(int id, Bundle args) {
			
			/**
			 * Loader which loads data in the background
			 */
			 class Loader extends AsyncTaskLoader<List<T>> {

				public Loader(Context context) {
					super(context);
				}

				@Override
				public List<T> loadInBackground() {
					return SelectableListFragment.this.loadInBackground();
				}
				
			}
			 
			return new Loader(getActivity());			 			
		}

		@Override
		public void onLoadFinished(android.content.Loader<List<T>> loader, List<T> data) {
			// assign result to list view (through an adapter)
			setListAdapter(new Adapter(data));		
		}

		@Override
		public void onLoaderReset(android.content.Loader<List<T>> loader) {
			// nothing to do			
		}
		
	}
	
	/**
	 * Constructor
	 * @param uri
	 * @param projection
	 * @param optionsMenuRes
	 * @param contextualMenuRes
	 */
	public SelectableListFragment (int optionsMenuRes, int contextualMenuRes) {
		super();
		
		this.optionsMenuRes = optionsMenuRes;
		this.contextualMenuRes = contextualMenuRes;
	}
	
	/**
	 * Gets called when data needs to be loaded
	 * @return
	 */
	protected abstract List<T> loadInBackground();
	
	/**
	 * Gets called to create a view for the given item 
	 * @param position
	 * @param convertView
	 */
	protected abstract View getView(T item, View convertView, ViewGroup parent);
	
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

		getLoaderManager().initLoader(0, null, new LoaderCallback());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// set to single choice mode
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		// listen to long clock
		getListView().setOnItemLongClickListener(this);
		// 
		getListView().setDivider(getResources().getDrawable(R.color.green_light));
		getListView().setDividerHeight(2);
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
		reload();			
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

	/**
	 * Reloads the list
	 */
	public void reload() {
		getLoaderManager().getLoader(0).forceLoad();
	}
	
}