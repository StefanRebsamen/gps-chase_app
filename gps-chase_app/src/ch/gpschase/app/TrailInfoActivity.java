package ch.gpschase.app;

import ch.gpschase.app.ChaseTrailActivity.ViewCheckpointFragment;
import ch.gpschase.app.data.Chase;
import ch.gpschase.app.data.Checkpoint;
import ch.gpschase.app.data.Trail;
import ch.gpschase.app.util.TrailMapFragment;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;


public class TrailInfoActivity extends Activity {

	// name to identify trail id passed as extra in intent 
	public static final String INTENT_EXTRA_TRAILID = "trailId";
	
	/**
	 * Open the activity for the specified trail
	 * @param trail
	 */
	public static void show(Context context, Trail trail) {
		// switch to chase activity
		Intent intent = new Intent(context, TrailInfoActivity.class);
		intent.putExtra(INTENT_EXTRA_TRAILID, trail.getId());
		context.startActivity(intent);
	}
	
	// UI elements
	private EditText editTextName;
	private EditText editTextDescr;
	private TextView textViewNoCp;
	private TextView textViewDistance;
	
	// the trail it's all about
	Trail trail = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load layout and get references to UI elements
		setContentView(R.layout.activity_trail_info);
		editTextName = (EditText) findViewById(R.id.editText_name);
		editTextDescr = (EditText) findViewById(R.id.editText_description);
		textViewNoCp = (TextView) findViewById(R.id.textView_no_checkpoints_value);
		textViewDistance = (TextView) findViewById(R.id.textView_distance_value);

		// adjust action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.activity_title_trail_info);
				
		// load trail including its checkpoints
		long trailId = getIntent().getLongExtra(INTENT_EXTRA_TRAILID, 0);
		trail = Trail.load(this, trailId);
		trail.loadCheckpoints(this);
		
		// load stuff into UI
		editTextName.setText(trail.name);
		editTextDescr.setText(trail.description);
		
		// go through checkpoints and calculate info 
		int cpCount = 0;
		float distance = 0.0f;
		Checkpoint previousCp = null;
		for (Checkpoint cp : trail.getCheckpoints()) {
			cpCount++;
			if (previousCp != null) {
				distance += cp.location.distanceTo(previousCp.location);
			}
			previousCp = cp;			
		}						
		textViewNoCp.setText(Integer.valueOf(cpCount).toString());		
		textViewDistance.setText(Integer.valueOf(Math.round(distance)).toString() +  "m");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// save if something really changed
		if (trail != null) {			
			String name = editTextName.getText().toString();
			String descr = editTextDescr.getText().toString();
			
			boolean changed = false;
			if (!name.equals(trail.name)) {
				changed = true;
				trail.name = name;
			}
			if (!descr.equals(trail.description)) {
				changed = true;
				trail.description = descr;
			}
			if (changed) {
				trail.save(this);
			}			
		}
	}
	
}
