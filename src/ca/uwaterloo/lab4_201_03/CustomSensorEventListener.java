package ca.uwaterloo.lab4_201_03;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.TextView;

public class CustomSensorEventListener implements SensorEventListener  {
	TextView outputView;
	String[] sensorNames;
	int sensorEventValue;
	
	String name;
	String units;
	float[] currentValues;
	float[] maximumValues;
	
	/**
	 * Allows an event sensor to be hooked up to a label.
	 * Displays current values and max values. 
	 * Only works with Sensor TYPE_ACCELEROMETER, TYPE_ROTATION_VECTOR
	 *  TYPE_MAGNETIC_FIELD, and TYPE_LIGHT.
	 * @param _outputView
	 * @param _sensorEventValue
	 */
	public CustomSensorEventListener (
			TextView _outputView,
			int _sensorEventValue){
		outputView = _outputView; //The output label
		sensorEventValue = _sensorEventValue; //The sensor type
		
		//Determine the sensor name, units, and number of values based
		// on the sensorEventValue
		switch(sensorEventValue) {
			case Sensor.TYPE_ACCELEROMETER:
				name = "Accelerometer";
				units = "m/s^2";
				sensorNames = new String[]{"x", "y", "z"};
				break;
			case Sensor.TYPE_ROTATION_VECTOR:
				name = "Rotation Vector";
				units = "unitless";
				sensorNames = new String[]{"x", "y", "z"};
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				name = "Magnetic Field";
				units = "uT";
				sensorNames = new String[]{"x", "y", "z"};
				break;
			case Sensor.TYPE_LIGHT:
				name = "Light Sensor";
				units = "lux";
				sensorNames = new String[]{"lux"};
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				name = "Linear Acceleration";
				units = "m/s^2";
				sensorNames = new String[]{"x", "y", "z"};
			default:
				break;
		}
		
		//Initialize sensor values to zero
		currentValues = new float[sensorNames.length];
		maximumValues = new float[sensorNames.length];
		for (int i = 0; i < sensorNames.length; i++) {
			currentValues[i] = 0f;
			maximumValues[i] = 0f;
		}
	}
		
	public void resetRecords() {
		//Set all records to the current values
		for (int i = 0; i < currentValues.length; i++) {
			maximumValues[i] = currentValues[i];
		}
		outputToLabel();
	}
	
	private void outputToLabel() {
		String output;
		output = String.format("%s (%s)", name, units); //Title
		for (int i = 0; i < sensorNames.length; i++) {  //Values and (record:)
			output += String.format("\n\t%s: %.2f (record: %.2f)",
					sensorNames[i], currentValues[i], maximumValues[i]);
		}
		outputView.setText(output);
	}
	
	public void onSensorChanged(SensorEvent se) {
		if (se.sensor.getType() == sensorEventValue) {
			for (int i = 0; i < sensorNames.length; i++) {
				//Get the sensor values
				float currentValue = se.values[i];
				currentValues[i] = currentValue;
				
				//Update max values
				if (Math.abs(currentValue) > Math.abs(maximumValues[i])) {
					maximumValues[i] = currentValue;
				}
			}
			//Output to label
			outputToLabel();	
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {	}
	
}
