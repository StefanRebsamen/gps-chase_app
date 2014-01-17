package ch.gpschase.app;

import java.util.Date;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import ch.gpschase.app.data.ChaseInfo;
import ch.gpschase.app.data.Contract;
import ch.gpschase.app.data.TrailInfo;
import ch.gpschase.app.util.ChaseCreator;
import ch.gpschase.app.util.Duration;
import ch.gpschase.app.util.SelectableListFragment;

public class ChasesActivity  extends Activity {
	
	/**
		 * Fragment to display a list of chases
		 */
		public static class ChasesFragment extends SelectableListFragment {
		
	
			/**
			 * 
			 */
			public ChasesFragment() {
				super(Contract.Chases.getUriDirEx(), Contract.Chases.READ_PROJECTION_EX, 0, R.menu.cab_chases_chase);
			}
			
			@Override
			public void onActivityCreated(Bundle savedInstanceState) {
				super.onActivityCreated(savedInstanceState);
	
				// set empty text
				CharSequence emptText = getResources().getText(R.string.empty_text_chases);
				setEmptyText(emptText);			
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
						
						// create info and set as tag
						ChaseInfo chase = ChaseInfo.fromCursor(context, cursor);
						view.setTag(chase);
												
						// set texts
						((TextView) view.findViewById(R.id.textView_trail_name)).setText(chase.trail.name);
						((TextView) view.findViewById(R.id.textView_player)).setText(chase.player);
						Date dateTime = new Date(chase.started);
						((TextView) view.findViewById(R.id.textView_started)).setText(dateFormat.format(dateTime) + " "
								+ timeFormat.format(dateTime));
						if (chase.finished != 0) {
							((TextView) view.findViewById(R.id.textView_time)).setText(Duration.format(chase.finished - chase.started));
						} else {
							((TextView) view.findViewById(R.id.textView_time)).setText("");
						}
	
						if (chase.finished != 0) {
							((ImageView) view.findViewById(R.id.imageView_chase_state)).setImageResource(R.drawable.ic_stop);
						} else {
							((ImageView) view.findViewById(R.id.imageView_chase_state)).setImageResource(R.drawable.ic_play);
						}						
					}
				}
				
				return new Adapter();
			}
	
			@Override
			protected boolean onActionItemClicked(MenuItem item, int position, long id) {

				View view = getListView().getChildAt(position);
				if (view != null) {					
					ChaseInfo chase = (ChaseInfo)view.getTag();
				
					switch (item.getItemId()) {
					
					case R.id.action_continue_chase:
						// continue chase
						ChaseTrailActivity.show(getActivity(), chase.id);
						return true;
						
					case R.id.action_delete_chase:
						// delete trail after asking user
						deleteChase(chase);
						return true;
					}
				}
				
				return false;
			}
	
			@Override
			public void onListItemClick(int position, long id) {
				View view = getListView().getChildAt(position);
				if (view != null) {					
					ChaseInfo chase = (ChaseInfo)view.getTag();
					if (chase.finished == 0) {
						// continue chase
						ChaseTrailActivity.show(getActivity(), chase.id);
					}
					else {
						// inform user that trail is finished already
						Toast.makeText(getActivity(), R.string.toast_chase_already_finished, Toast.LENGTH_SHORT).show();
					}
				}
			}
	
			@Override
			public void onSelectionChanged(int position, long id) {
				if (actionMode != null) {				
					View view = getListView().getChildAt(position);
					if (view != null) {					
						ChaseInfo chase = (ChaseInfo)view.getTag();
						// update title				
						actionMode.setTitle(chase.trail.name);					
						actionMode.setSubtitle(chase.player);					
						// modify menu
						MenuItem menuContinue =  actionMode.getMenu().findItem(R.id.action_continue_chase);
						if (menuContinue != null) {
							menuContinue.setVisible(chase.finished == 0);
						}
					}
				}			
			}		
			
					
			/**
			 * 
			 * @param chaseId
			 */
			private void deleteChase(ChaseInfo chase) {
	
				final Uri chaseUri = Contract.Chases.getUriId(chase.id);
	
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
										// finish action mode
										finishActionMode();
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

	private static final String FRAGMENT_TAG = "chases";
			
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load layout
		setContentView(R.layout.activity_chases);
		
		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_chases);
		
		// add fragment (if not already recreated)
		if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
			getFragmentManager().beginTransaction().replace(R.id.layout_container, new ChasesFragment(), FRAGMENT_TAG).commit();
		}
		
		
	}

}
