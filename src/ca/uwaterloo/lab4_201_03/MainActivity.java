package ca.uwaterloo.lab4_201_03;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.uwaterloo.lab4_201_03.CustomSensorEventListener;
import ca.uwaterloo.lab4_201_03.GraphableCustomSensorEventListener;
import ca.uwaterloo.lab4_201_03.LineGraphView;
import ca.uwaterloo.lab4_201_03.R;
import ca.uwaterloo.lab4_201_03.ResetButtonListener;
import ca.uwaterloo.mapper.MapLoader;
import ca.uwaterloo.mapper.MapView;
import ca.uwaterloo.mapper.NavigationalMap;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Build;

public class MainActivity extends ActionBarActivity {
	
	public static MapView mapView;
	public static final String MAP_NAME = "E2-3344.svg";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Prepare the map
		mapView = new MapView (getApplicationContext(), 1000, 900, 40, 40);
		registerForContextMenu ( mapView ); 
		NavigationalMap map = MapLoader.loadMap(getExternalFilesDir(null), MAP_NAME);
		mapView.setMap(map);
		
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	//for the map view
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		mapView.onCreateContextMenu(menu, v, menuInfo);
	}

	//more map view stuff
	@Override
	public boolean onContextItemSelected ( MenuItem item ) {
		return super . onContextItemSelected ( item ) || mapView . onContextItemSelected ( item );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			//Reference to the LinearLayout (see fragment_main.xml)
			LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.linearLayout);
			layout.setOrientation(LinearLayout.VERTICAL);
			
			//Create graph
			LineGraphView graph = new LineGraphView(
					getActivity().getApplication(), 
					1000,
					10f,
					Arrays.asList("z"));
			
			//Create graph
			LineGraphView graph2 = new LineGraphView(
					getActivity().getApplication(), 
					1000,
					10f,
					Arrays.asList("a"));
			
			//Create labels
			TextView graphLabel = new TextView(rootView.getContext());
			TextView compassLabel = new TextView(rootView.getContext());
			TextView stepCountLabel = new TextView(rootView.getContext());
			TextView directionalStepCountLabel = new TextView(rootView.getContext());
			TextView linAccelSensorLabel = new TextView(rootView.getContext());
			TextView directionsLabel = new TextView(rootView.getContext());
			stepCountLabel.setPadding(0, 20, 0, 0);
			
			//Create StepDetector, give it a label
			StepDetector stepDetector = new StepDetector(stepCountLabel, graph2, rootView.getContext());
			
			//Create user
			UserPosition userPosition = new UserPosition(mapView, directionsLabel, MAP_NAME);
			
			//Create map controller
			MapController mc = new MapController(mapView, userPosition, MAP_NAME);
			
			//Create DirectionalCounter, give it a label
			DirectionalCounter directionalCounter = new DirectionalCounter(stepDetector, directionalStepCountLabel, 1000, 400, userPosition, rootView.getContext());
			
			//Request the sensor manager
			SensorManager sensorManager = (SensorManager)
					rootView.getContext().getSystemService(SENSOR_SERVICE);
			
			//Create the sensor event listeners and connect the labels
			Sensor linAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
			GraphableCustomSensorEventListener linAccelListener = new GraphableCustomSensorEventListener(linAccelSensorLabel, Sensor.TYPE_LINEAR_ACCELERATION, graph, stepDetector);
			sensorManager.registerListener(linAccelListener, linAccelSensor, SensorManager.SENSOR_DELAY_FASTEST);
			
			Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			Sensor accelListener = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(directionalCounter, magneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
			sensorManager.registerListener(directionalCounter, accelListener, SensorManager.SENSOR_DELAY_FASTEST);

			//Create reset button; reset button needs references to listeners
			List<CustomSensorEventListener> listeners = new ArrayList<CustomSensorEventListener>();
			Button resetButton = new Button(rootView.getContext());
			resetButton.setText("Reset");
			resetButton.setOnClickListener(new ResetButtonListener(new Resettable[]{stepDetector, directionalCounter, mc}));
			
			//Create save data button; needs reference to stepDetector
			Button saveDataButton = new Button(rootView.getContext());
			saveDataButton.setText("Save Data");
			saveDataButton.setOnClickListener(new SaveDataButton(stepDetector, rootView.getContext()));
			
			//Create calibrate button; needs reference to DirectionalCounter
			Button calibrateButton = new Button(rootView.getContext());
			calibrateButton.setText("Calibrate");
			calibrateButton.setOnClickListener(new CalibrateButton(directionalCounter));
			
			//Add map
			layout.addView(mapView);
			mapView.addListener(mc);
			
			//directions label
			layout.addView(directionsLabel);
			
			//Add compass
			layout.addView(compassLabel);
			compassLabel.setText("Compass:");
			layout.addView(directionalCounter);
			directionalCounter.setVisibility(View.VISIBLE);
			
			//Add buttons
			layout.addView(resetButton);
			//layout.addView(saveDataButton);
			layout.addView(calibrateButton);
			
			//Add labels
			layout.addView(stepCountLabel);
			layout.addView(directionalStepCountLabel);
			
			return rootView;			
		}
	}
}
