package ca.uwaterloo.lab4_201_03;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.TextView;

public class DirectionalCounter extends View implements SensorEventListener, Resettable{
	
	public enum StepType {NO_GRID, GRID};
	
	Context ctx;
	TextView outputView;
	StepDetector stepDetector;
	
	//for the compass widget
	private final int WIDTH;
	private final int HEIGHT;
	private Paint needlePaint = new Paint();
	
	//for calculating the azimuth
	private float[] acc = new float[3];
	private float[] mcurrent = new float[3];
	private float azimuth;
	private float offset;
	private final float SMOOTHING_CONST = 0.05f;
	
	//for counting steps
	private float stepsNS = 0;
	private float stepsEW = 0;
	private int prevStepCount = 0;
	public StepType stepType = StepType.GRID;
	
	//Represents the user's postion
	public UserPosition userPosition;
	
	/**
	 * Directional counts number of steps in the North/South and East/West directions
	 * Can be implemented as a view which displays a simple compass.
	 * 
	 * @param	stepDetector	Reference to a stepDetector object
	 * @param 	outputView		Label to output stats 
	 * @param	WIDTH			Width of the compass widget
	 * @param	HEIGHT			Height of the compass widget
	 * @param	ctx				Context
	 * 
	 * @return	DirectionalCounter	This.
	 */
	public DirectionalCounter(StepDetector stepDetector, TextView outputView, int WIDTH, int HEIGHT, UserPosition us, Context ctx) {
		super(ctx);
		this.stepDetector = stepDetector;
		this.outputView = outputView;
		this.WIDTH = WIDTH;
		this.HEIGHT = HEIGHT;
		this.needlePaint.setColor(Color.BLACK);
		this.userPosition = us;
		this.ctx = ctx;
	}

	@Override
	public void onSensorChanged(SensorEvent se) {		
		float[] I = new float[9];//value needed for getRotationMatrix	
		float[] R = new float[9];//value needed for getOrientation	
		float[] orient = new float[3];//stores result form getOrientation
	
		if(se.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
			for (int i = 0; i < 3.; i++) {
				//Get the sensor values
				this.mcurrent[i] = se.values[i];
			}
		}
		
		if(se.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
			for (int i = 0; i < 3.; i++) {
				//Get the sensor values
				this.acc[i] = se.values[i];
			}		
		}
		//Gives orientation relative to magnetic north
		//Because Sensor.TYPE_ORIENTATION is deprecated, the android docs recommend
		//getting orientation using this method
		boolean pass = SensorManager.getRotationMatrix (R, I, this.acc, this.mcurrent);
		if (pass) {
			SensorManager.getOrientation(R, orient);
			float north =  orient[0] + offset; //add offset if necessary
			north = DirectionalCounter.getBoundedAngle(north);
			this.azimuth = this.circularLowPass(this.azimuth, north);
			
			//if a step has occurred, add to the count
			if(this.stepDetector.stepCount != 0 && 
					this.stepDetector.stepCount != this.prevStepCount){
				this.updateSteps();
			}
			this.updateLabel();
			this.userPosition.updateMesage(DirectionalCounter.getBoundedAngle(azimuth - Math.PI / 2));
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	public void reset() {
		this.prevStepCount = 0;
		this.stepsNS = 0;
		this.stepsEW = 0;
		//this.offset = 0;
		this.updateLabel();
	}
	
	public void updateLabel() {
		this.outputView.setText(
				"Steps North: " + stepsNS +
				"\nSteps East: " + stepsEW +
				"\nAzimuth: " + this.azimuth);
		this.purge();
	}
	
	/**
	 * Sets an offset, which makes the compass behave as if North is in the direction
	 * of the current azimuth.
	 */
	public void calibrate() {
		this.offset -= this.azimuth;
	}
	
	/**
	 * Updates the North/South and East/West step counts. These will be incremented
	 * by whole integers if the StepType is GRID. If the stepType is NO_GRID they
	 * will be incremented as a function of the angle of the azimuth.
	 */
	private void updateSteps() {
		float oldNS = this.stepsNS;
		float oldEW = this.stepsEW;
		
		if (this.stepType == StepType.NO_GRID) {
			//breaks into components
			double northComponent = Math.cos(this.azimuth);
			double eastComponent = Math.sin(this.azimuth);
			this.stepsNS += northComponent / (Math.abs(northComponent) + Math.abs(eastComponent));
			this.stepsEW += eastComponent / (Math.abs(northComponent) + Math.abs(eastComponent));
		} else {
			//counts a single step in either the NS or EW direction
			if (this.azimuth < Math.PI/4 && this.azimuth > -Math.PI/4) {
				this.stepsNS ++; //North
			} else if (this.azimuth > Math.PI/4 && this.azimuth < 3*Math.PI/4) {
				this.stepsEW ++; //East
			} else if (this.azimuth < -Math.PI/4 && this.azimuth > -3*Math.PI/4) {
				this.stepsEW --; //West
			} else {
				this.stepsNS --; //South
			}
		}
		this.updateLabel();
		this.prevStepCount = this.stepDetector.stepCount;
		this.userPosition.update(stepsEW - oldEW, stepsNS - oldNS);
	}
	
	private float circularLowPass(double n0, double n1) {
		//Apply a lowpass filter to CIRCULAR data. 
		//E.g. an angle of 0 is different by 1 degree from 359.
		if (Math.abs(n1 - n0) < Math.PI) {
			n0 = n0 + this.SMOOTHING_CONST * (n1 - n0);
		} else if (n1 - n0 > 0) {
			//Rotated from -PI to PI
			n0 = (float) (n0 - this.SMOOTHING_CONST * ((Math.PI + n0) + (Math.PI - n1)));
		} else {
			//Rotated from PI to -PI
			n0 = (float) (n0 + this.SMOOTHING_CONST * ((Math.PI - n0) + (Math.PI + n1))) ;
		}
		
		//Ensure n0 is within the range {-Math.PI, Math.PI}
		n0 = DirectionalCounter.getBoundedAngle(n0);
		return (float) n0;
	}
	
	/**
	 * Returns an angle bounded between PI and -PI.
	 * Uses a circular coordinate system that rolls over from PI to -PI
	 * (this is different from mod PI!)
	 * 
	 * @param  n
	 * @return  n bounded between PI and -PI
	 */
	private static float getBoundedAngle(double n) {
		if (n > Math.PI) {
			n = -Math.PI + (n - Math.PI);
		} else if (n < -Math.PI) {
			n = Math.PI - (-Math.PI - n);
		}
		return (float) n;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see android.view.View#onMeasure(int, int)
	 */
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(this.WIDTH, this.HEIGHT);
	}
	
	/*
	 * Draws a line pointing north.
	 */
	private void drawLine(Canvas canvas)
	{
		//make bottom left (0,0) for convenience
		canvas.translate(0,canvas.getHeight());
		canvas.scale(1,-1);
		
		//draw compass needle
		float centerX, centerY, dirX, dirY;
		centerX = this.WIDTH / 2;
		centerY = this.HEIGHT / 2;
		dirX = centerX + (int)(Math.cos(this.azimuth + Math.PI/2) * Math.min(centerX, centerY));
		dirY = centerY + (int)(Math.sin(this.azimuth + Math.PI/2) * Math.min(centerX, centerY));
		canvas.drawLine(centerX, centerY, dirX, dirY, this.needlePaint);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		this.drawLine(canvas);
	}
	
	/*
	 * Redraws the graph
	 */
	private void purge(){
		invalidate();
	}
}
