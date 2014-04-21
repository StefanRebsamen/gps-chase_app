package ch.gpschase.app;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.TextView;
import ch.gpschase.app.data.BackendClient;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.SelectableListFragment;
import ch.gpschase.app.util.TrailActions;

/**
 * Fragment to display list of trails from the local database
 */
public class LocalTrailsFragment extends SelectableListFragment<Trail> {

	/**
	 * 
	 */
	public enum Mode {
		EDITABLE,
		DOWNLOADED
	}
	
	private Mode mode = Mode.EDITABLE;
	
	// argument key used to pass the mode
	private static final String ARG_MODE = "mode";  
	
	/**
	 * 
	 */
	public LocalTrailsFragment() {
		super(R.menu.menu_main_trails, R.menu.cab_trail);
	}

	/**
	 * Factory method
	 * @param mode
	 * @return
	 */
	public static LocalTrailsFragment create(Mode mode) {
		LocalTrailsFragment instance = new LocalTrailsFragment();
		Bundle args = new Bundle();
		args.putString(ARG_MODE, mode.toString());
		instance.setArguments(args);
		return instance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mode = Mode.valueOf(getArguments().getString(ARG_MODE));
		
		// we want to create our own option menu (only in mode editable)
		setHasOptionsMenu(mode != Mode.DOWNLOADED);
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
		if (mode ==Mode.EDITABLE) { 
			return Trail.listEditable(getActivity());
		}
		else {
			return Trail.listDownloaded(getActivity());			
		}
	}

	@Override
	protected View getItemView(Trail item, View convertView, ViewGroup parent) {

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
		TextView tvDescr = (TextView)view.findViewById(R.id.textView_trail_description); 
		if (!TextUtils.isEmpty(item.description)) {
			tvDescr.setText(item.description);
			tvDescr.setVisibility(View.VISIBLE);
		}
		else {
			tvDescr.setText("");
			tvDescr.setVisibility(View.GONE);
		}
		((TextView) view.findViewById(R.id.textView_trail_updated)).setText(App.formatDateTime(item.updated));
		
		return view;
	}

	
	@Override
	protected boolean onActionItemClicked(MenuItem item, int position, long id) {

		View view = getListView().getChildAt(position);
		if (view != null) {
			Trail trail = (Trail) view.getTag();
			switch (item.getItemId()) {
			case R.id.action_chase_trail:
				// chase trail
				TrailActions.chaseTrail(getActivity(), trail);
				return true;

			case R.id.action_edit_trail:
				// edit trail
				EditTrailActivity.show(getActivity(), trail);
				return true;

			case R.id.action_share_trail:
				// upload and share trail
				TrailActions.shareTrail(getActivity(), trail);
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
			// show info
			TrailInfoActivity.show(getActivity(), trail);
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
					menuEdit.setVisible(trail.isEditable());
				}
				MenuItem menuShare = actionMode.getMenu().findItem(R.id.action_share_trail);
				if (menuShare != null) {
					menuShare.setVisible(!trail.isDownloaded());
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
		editText.setHint(R.string.field_name);
		editText.setSingleLine();
		
		final AlertDialog dialog = new AlertDialog.Builder(getActivity())							//
											.setTitle(R.string.action_new_trail)				//
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
						
													Trail trail = Trail.create();
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
					LocalTrailsFragment.this.reload();	
				}
				else {
					// show dialog to inform user about failure
					new AlertDialog.Builder(getActivity())								//
						.setIcon(android.R.drawable.ic_dialog_alert)					//
						.setTitle(R.string.dialog_title_error)				//
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




}