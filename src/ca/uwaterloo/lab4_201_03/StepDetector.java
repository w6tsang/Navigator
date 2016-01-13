package ca.uwaterloo.lab4_201_03;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

@SuppressLint("NewApi")
public class StepDetector implements Resettable {
	
	enum State {
		INIT, NOSTEP, STEP1, STEP2, WALKING, NOISE
	}
	
	String message = "";
	State state;
	
	TextView outputView;
	LineGraphView graph;
	Context ctx;
	
	StepObj fastStep;
	StepObj medStep;
	StepObj slowStep;
	StepObj lastStep;
		
	private static final long MAX_STEP_TIME = 1500;
	private static final long MIN_STEP_TIME = 200;
	long timeOfLastStep = 0;
	long timeBetweenSteps = 0;
	
	float[] datax;
	float[] datay;
	float[] dataz;
	
	//Thresholds for valid steps
	//{{x +min, x +max, x -min, x -max},
	//    {y +min, y +max, y -min, y -max},
	//    {z +min, x +max, z -min, z -max}}
	float[][] hardTh = {{0.1f, 5f, -0.1f, -5f}, //x
			{0.5f, 3.5f, -2f, -4f},             //y
			{2f, 10f, -2f, -9f}};               //z
	float[][] easyTh = {{0f, 10f, 0f, -10f}, //x
			{0f, 6f, 0f, -6f},               //y
			{2f, 15f, -1.5f, -15f}};         //z
	
	int updateCount = 0; //number of times updateData() has been called
	int stepCount = 0;
	
	public StepDetector(TextView _outputView, LineGraphView _graph, Context _ctx){
		//hook detector up to an output label; initialize it
		outputView = _outputView;
		graph = _graph;
		ctx = _ctx;
		
		state = State.INIT;
		
		//Step Objects
		fastStep = new StepObj("FAST", _ctx);
		medStep = new StepObj("MED", _ctx);
		slowStep = new StepObj("SLOW", _ctx);
		
		//Initialize the data lists
		datax = new float[150];
		datay = new float[150];
		dataz = new float[150];
		for (int i = 0; i < datax.length; i++){
			datax[i] = Integer.MAX_VALUE;
			datay[i] = Integer.MAX_VALUE;
			dataz[i] = Integer.MAX_VALUE;
		}
		
		//Initialize the lastStep to something unmatchable
		float[] bigNum = {Integer.MAX_VALUE};
		lastStep = new StepObj(bigNum, bigNum, bigNum);
	}
	
	/*
	 * Resets steps, update label, set state back to INIT
	 */
	public void reset(){
		stepCount = 0;
		zeroData();
		state = State.INIT;
		updateLabel();
	}
	
	/*
	 * Main update method
	 */
	public void update(float[] _newPoints){
		updateData(_newPoints);
		updateState();
		updateLabel();
		
		updateCount ++;
	}
	
	private void updateLabel(){
		outputView.setText("Step Count = " + stepCount);
//		outputView.setText("Step Count = " + stepCount +
//				"\nState = " + state.name() +
//				"\n" + message +
//				"\nUpdate Count = " + updateCount);
	}
	
	private void updateData(float[] point){
		//shift all data in the array. Drop the last element
		for (int i = (datax.length - 2); i >= 0; i--) {                
		    datax[i+1] = datax[i];
		    datay[i+1] = datay[i];
		    dataz[i+1] = dataz[i];
		}
		
		datax[0] = point[0];
		datay[0] = point[1];
		dataz[0] = point[2];
	}

	private void updateState(){
		float [] fastScore;
		float [] medScore;
		float [] slowScore;
		
		//Calculate score for current data. Use easy thresholds if the state indicates WALKING
		if (state != State.WALKING){
			fastScore = fastStep.similarity(datax, datay, dataz, hardTh);
			medScore = medStep.similarity(datax, datay, dataz, hardTh);
			slowScore = slowStep.similarity(datax, datay, dataz, hardTh);
		} else {
			fastScore = fastStep.similarity(datax, datay, dataz, hardTh);
			medScore = medStep.similarity(datax, datay, dataz, hardTh);
			slowScore = slowStep.similarity(datax, datay, dataz, hardTh);
//			fastScore = fastStep.similarity(datax, datay, dataz, easyTh);
//			medScore = medStep.similarity(datax, datay, dataz, easyTh);
//			slowScore = slowStep.similarity(datax, datay, dataz, easyTh);
		}
		
		//Find out which type of step was the best match
		float xscore = Integer.MAX_VALUE;
		float yscore = Integer.MAX_VALUE;
		float zscore = Math.min(fastScore[2], Math.min(medScore[2], slowScore[2])); 
		int stepLength = -1; //This value should always change due to the if-statement below. If it remains -1 this indicates an error 
		
		//Only use the scores for the match with the best z-component
		if (zscore == fastScore[2]){
			stepLength = fastStep.xpoints.length;
			xscore = fastScore[0];
			yscore = fastScore[1];
		} else if (zscore == medScore[2]){
			stepLength = medStep.xpoints.length;
			xscore = medScore[0];
			yscore = medScore[1];
		} else if (zscore == slowScore[2]){
			stepLength = slowStep.xpoints.length;
			xscore = slowScore[0];
			yscore = slowScore[1];
		}
		
		
		//STATE MACHINE! Decide how to increment the step count based on past history
		int oldStepCount = stepCount;
		switch(state){
			case INIT:
				if (datax[datax.length - 1] != 0){ //data has been filled with initial values
					state = State.NOSTEP;
				}
				break;
			case NOSTEP:
				if (((zscore < 0.08 && yscore < 0.9) || (zscore < 0.7))) { //Easy step to register
					state = State.STEP1;
					zeroData();
					stepCount ++;
				}
				break;
			case STEP1:
				if (System.currentTimeMillis() - timeOfLastStep < MAX_STEP_TIME){
					if (((zscore < 0.08 && yscore < 0.9) || (zscore < 0.7)) &&
							timeOfLastStep < System.currentTimeMillis() - MIN_STEP_TIME) { //Easy step to register WITH timer constraint
						state = State.STEP2;
						zeroData();
						stepCount ++;
					}
				} else {
					state = State.NOSTEP;
				}
				break;
			case STEP2:
				if (System.currentTimeMillis() - timeOfLastStep < MAX_STEP_TIME){
					if (((zscore < 0.08 && yscore < 0.9) || (zscore < 0.7)) &&
							timeOfLastStep < System.currentTimeMillis() - MIN_STEP_TIME) { //Easy step to register WITH timer constraint
						state = State.WALKING;
						zeroData();
						timeBetweenSteps = System.currentTimeMillis() - timeOfLastStep;
						stepCount ++;
					}
				} else {
					state = State.NOSTEP;
				}
				break;
			case WALKING:
				if (System.currentTimeMillis() - timeOfLastStep < MAX_STEP_TIME*1.5){
					long diffFactor = (System.currentTimeMillis() - timeOfLastStep) / timeBetweenSteps;
					if (zscore < 0.1 && timeOfLastStep < System.currentTimeMillis() - MIN_STEP_TIME) { //Now walking, register most activity as steps
						zeroData();
						timeBetweenSteps = System.currentTimeMillis() - timeOfLastStep;
						if (diffFactor > 1.8){ //Most likely a step was missed
							stepCount +=2;
						} else {
							stepCount ++;
						}
					}
				} else {
					state = State.NOSTEP;
				}
				break;
			default: break;
		}
		
		//Keep step count positive!
		if (stepCount < 0) {
			stepCount = 0;
		}
		
		//Update message and timeOfLastStep (if necessary)
		message += "\nnothing yet";
		String olde = message.split("\n")[1];
		if (stepCount > oldStepCount){
			timeOfLastStep = System.currentTimeMillis();
			if (xscore == fastScore[0]){
				message = "\nFAST STEP \r-- xscore = " + xscore + "\r-- yscore = " + yscore + "\r-- zscore = " + zscore; 
			} else if (xscore == medScore[0]){
				message = "\nMED STEP \r-- xscore = " + xscore + "\r-- yscore = " + yscore + "\r-- zscore = " + zscore;
			} else if (xscore == slowScore[0]){
				message = "\nSLOW STEP \r-- xscore = " + xscore + "\r-- yscore = " + yscore + "\r-- zscore = " + zscore;
			}
		}else{
			message = "\n" + olde;
		}
		message += "\nxscore = " + xscore + "\nyscore = " + yscore + "\nzscore = " + zscore ;
	}
	
	
	/*
	 * Sets the data to unmatchable values.
	 * This prevents multiple steps from being counted in a row. 
	 */
	private void zeroData(){
		for (int i = 0; i < datax.length; i++){
			datax[i] = Integer.MAX_VALUE;
			datay[i] = Integer.MAX_VALUE;
			dataz[i] = Integer.MAX_VALUE;
		}
	}
}

