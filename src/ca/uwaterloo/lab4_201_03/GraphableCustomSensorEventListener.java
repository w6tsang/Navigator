package ca.uwaterloo.lab4_201_03;

import android.hardware.SensorEvent;
import android.widget.TextView;

public class GraphableCustomSensorEventListener extends CustomSensorEventListener {
	
	LineGraphView graph;
	StepDetector stepDetector;
	
	/**
	 * Allows a CustomSensorEventListener to be connected to a LineGraphView
	 * @param _outputView
	 * @param _sensorEventValue
	 * @param _graph
	 */
	public GraphableCustomSensorEventListener(
			TextView _outputView,
			int _sensorEventValue,
			LineGraphView _graph,
			StepDetector _stepDetector) {
		super(_outputView, _sensorEventValue);
		graph = _graph;
		stepDetector = _stepDetector;
	}
	
	@Override
	public void onSensorChanged(SensorEvent se) {
		//Make call to super
		super.onSensorChanged(se);
		
		if (se.sensor.getType() == super.sensorEventValue) {
			//Output to graph
			stepDetector.update(super.currentValues);
			float[] g = new float[1];
			g[0] = stepDetector.dataz[0];
			graph.addPoint(g);
		}
	}

}
