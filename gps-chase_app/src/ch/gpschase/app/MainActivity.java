package ch.gpschase.app;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.ls.LSInput;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Space;
import android.widget.TextView;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.DownloadTask;
import ch.gpschase.app.util.TrailDownloadLink;

/**
 * 
 */
public class MainActivity extends Activity {

	/**
	 * Abstract base class for an item in the drawer
	 */
	private abstract static class DrawerItem {
        // possible types
        static enum Type {
        	SPACE,
        	SECTION,
        	SELECTABLE
        }
    	
        Type type;
        int value;
        int labelResId;
        int iconResId;
    }

	/**
	 * A space in the drawer
	 */
	private class DrawerSpace extends DrawerItem {
		
		public DrawerSpace() {
			type = Type.SPACE;
		}

	}

	/**
	 * A section in the drawer
	 */
	private class DrawerSection extends DrawerItem {
		
		public DrawerSection(int labelResId) {
			type = Type.SECTION;
			this.labelResId = labelResId;
		}

	}

	/**
	 * A selectable item in the drawer
	 */
	private class DrawerSelectable extends DrawerItem {
		
		public DrawerSelectable(int labelResId, int iconResId,  int value) {
			type = Type.SELECTABLE;
			this.labelResId = labelResId;
			this.iconResId = iconResId;
			this.value = value;
		}

	}

	
	/**
	 * Handles toggling of the navigation drawer
	 */
	private final class DrawerToggle extends ActionBarDrawerToggle {
		private DrawerToggle(Activity activity, DrawerLayout drawerLayout, int drawerImageRes, int openDrawerContentDescRes,
				int closeDrawerContentDescRes) {
			super(activity, drawerLayout, drawerImageRes, openDrawerContentDescRes, closeDrawerContentDescRes);
		}

		/** Called when a drawer has settled in a completely closed state. */
		public void onDrawerClosed(View view) {
		    super.onDrawerClosed(view);
		    // show menu
		    invalidateOptionsMenu();
		}

		/** Called when a drawer has settled in a completely open state. */
		public void onDrawerOpened(View drawerView) {
		    super.onDrawerOpened(drawerView);
		    // hide menu
		    invalidateOptionsMenu();
		}
	}

	/**
	 * Listens to clicks on the navigation drawer items
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView parent, View view, int position, long id) {
	    	DrawerSelectable selectable = (DrawerSelectable)drawerItems.get(position);
	    	// select item
	        selecDrawertItem(selectable);
	    }
	}
	

	/**
	 * Provides the drawer with its content
	 */
	private class DrawerListAdapter extends BaseAdapter {

		final DrawerItem.Type[] typeValues = DrawerItem.Type.values();

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public int getCount() {
			return drawerItems.size();
		}

		@Override
		public Object getItem(int position) {
			return drawerItems.get(position);		
		}

		@Override
		public long getItemId(int position) {			
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			DrawerItem item = drawerItems.get(position);  
			switch (item.type) {
			case SELECTABLE:
				return getSelectableView((DrawerSelectable)item, convertView, parent);

			case SECTION :
				return getSectionView((DrawerSection)item, convertView, parent);

			case SPACE:
				return getSpaceView((DrawerSpace)item, convertView, parent);

			default:
				throw new IllegalArgumentException();
			}
		}

		
		private View getSectionView(DrawerSection item, View convertView, ViewGroup parent) {
			// make sure we've got a view
			View view = convertView;
			if (view == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    view = vi.inflate(R.layout.drawer_section, null);			
			}
			
			// set text
			((TextView)view.findViewById(R.id.textView_drawer_label)).setText(item.labelResId);
			
			return view;			
		}
		
		private View getSpaceView(DrawerSpace item, View convertView, ViewGroup parent) {
			// make sure we've got a view
			View view = convertView;
			if (view == null) {
				view = new TextView(MainActivity.this);
				view.setMinimumHeight(48);
			}
			return view;			
		}

		private View getSelectableView(DrawerSelectable item, View convertView, ViewGroup parent) {
			// make sure we've got a view
			View view = convertView;
			if (view == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    view = vi.inflate(R.layout.drawer_selectable, null);			
			}
			
			// set text and icon
			((TextView)view.findViewById(R.id.textView_drawer_label)).setText(item.labelResId);
			((ImageView)view.findViewById(R.id.imageView_drawer_icon)).setImageResource(item.iconResId);
			
			return view;			
		}
		
		
		@Override
		public int getViewTypeCount() {
			return typeValues.length;
		}

		@Override
		public int getItemViewType(int position) {			
			DrawerItem item = drawerItems.get(position);
			for (int i = 0; i<typeValues.length; i++ ) {
				if (typeValues[i] == item.type) {
					return i;
				}
			}
			return Adapter.IGNORE_ITEM_VIEW_TYPE;
		}
		
		@Override
		public boolean isEnabled (int position) {			
			return drawerItems.get(position).type == DrawerItem.Type.SELECTABLE;
		}
	}
	

	private final int SELECTABLE_MYTRAILS = 10;
	private final int SELECTABLE_CLOUDTRAILS = 20;
	private final int SELECTABLE_CHASES = 30;
	private final int SELECTABLE_PREFERENCES = 40;
	
	
	// navigation drawer related variables
	private final List<DrawerItem> drawerItems = new ArrayList<DrawerItem>(); 
	private DrawerLayout drawerLayout;
	private ListView drawerList;
	private ActionBarDrawerToggle drawerToggle;

	// what is currently active on drawer
	DrawerSelectable currentSelectable = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load layout
		setContentView(R.layout.activity_main);
		
		// define the items we want to have in the drawer
        drawerItems.add(new DrawerSpace());
        drawerItems.add(new DrawerSelectable(R.string.drawer_selectable_my_trails, R.drawable.ic_phone,  SELECTABLE_MYTRAILS));
        drawerItems.add(new DrawerSelectable(R.string.drawer_selectable_cloud_trails, R.drawable.ic_cloud, SELECTABLE_CLOUDTRAILS));
        drawerItems.add(new DrawerSpace());
        drawerItems.add(new DrawerSelectable(R.string.drawer_selectable_chases, R.drawable.ic_chases, SELECTABLE_CHASES));
        drawerItems.add(new DrawerSpace());
        drawerItems.add(new DrawerSelectable(R.string.drawer_selectable_settings, R.drawable.ic_settings, SELECTABLE_PREFERENCES));
                
        // set the adapter and listener for the list view
        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerList.setAdapter(new DrawerListAdapter());        
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        // create a drawer toggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
	        drawerToggle = new DrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
	        drawerLayout.setDrawerListener(drawerToggle);        
        }
        
        // tune action bar
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);        
        
        // start with trail on device
        selectDrawertItem(SELECTABLE_MYTRAILS);        
        
		//////////////////////////////////////////
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
				// load existing or create a new trail
				Trail trail = Trail.loadOrCreate(this, data.trailUuid);				
				// download in async task 
				new DownloadTask(this, trail, true).execute();				
			}
		}
		
	}

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        
        // sync drawer (if available)
        if (drawerLayout != null) {	   
	        drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // tell it drawer (if available)
        if (drawerLayout != null) {
        	drawerToggle.onConfigurationChanged(newConfig);
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
        // If the nav drawer is open, hide action items related to the content view
        if (drawerLayout != null) {
	   	    boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
	   	    return !drawerOpen;
        }
        else {
        	return true;
        }
    }	
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
    	if (drawerLayout != null) {
    		if (drawerToggle.onOptionsItemSelected(item)) {
    	          return true;    			
    		}
        }
        // handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }
    
	/**
	 * Selects the specified to show the desired fragment
	 * @param value
	 */
	private void selectDrawertItem(int value) {
		for (DrawerItem item : drawerItems) {
			if (item instanceof DrawerSelectable &&  item.value == value) {
				selecDrawertItem((DrawerSelectable)item);
			}
		}
	}
	
	/**
	 * 
	 * @param value
	 */
	private void selecDrawertItem(DrawerSelectable selectable) {

		// nothing to do if nothing changed
		if (selectable == currentSelectable) {
			return;
		}
		
		// create a new fragment, dependent on what was chosen
	    Fragment fragment = null;
	    switch (selectable.value) {
	    case SELECTABLE_MYTRAILS:
	    	fragment = new MyTrailsFragment();
	    	break;

	    case SELECTABLE_CLOUDTRAILS:	
	    	fragment = new CloudTrailsFragment();
	    	break;

	    case SELECTABLE_CHASES:	
	    	fragment = new ChasesFragment();
	    	break;
	    	
	    case SELECTABLE_PREFERENCES:	
	    	fragment = new SettingsFragment();
	    	break;
	    }
	    
	    // keep current selection
	    currentSelectable = selectable;	    

	    // close the drawer (if available)
	    if (drawerLayout != null) {
	    	drawerLayout.closeDrawer(drawerList);
	    }
	    
	    // highlight the selected item, update the title, and close the drawer
	    drawerList.setItemChecked(drawerItems.indexOf(selectable), true);
	    setTitle(currentSelectable.labelResId);

	    
	    // insert the fragment by replacing any existing fragment
	    FragmentManager fragmentManager = getFragmentManager();
	    fragmentManager.beginTransaction()
	                   .replace(R.id.content_frame, fragment)
	                   .commit();

	}
	
}
