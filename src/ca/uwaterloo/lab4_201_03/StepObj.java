package ca.uwaterloo.lab4_201_03;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import android.annotation.SuppressLint;
import android.content.Context;

public class StepObj {
		
	public float[] xpoints;
	public float[] ypoints;
	public float[] zpoints;
	
	public static final float SMOOTHING_CONSTANT = 4f;
	
	/*
	 * Creates a StepObj from two point arrays.
	 * Does not Normalize or Smooth points.
	 */
	public StepObj(float[] _xpoints, float[] _zpoints, float[] _ypoints){
		xpoints = _xpoints;
		zpoints = _zpoints;
		ypoints = _ypoints;
	}
	
	/*
	 * Creates a StepObj from a file.
	 * Uses the resource "step1.txt"
	 */
	public StepObj(String name, Context ctx){
		float[] rawZ = null;
		float[] rawX = null;
		float[] rawY = null;
		BufferedReader br;
	    try {
	    	int id = ctx.getResources().getIdentifier("step1", "raw", ctx.getPackageName());
	    	InputStream is = ctx.getResources().openRawResource(id);
	        br = new BufferedReader(new InputStreamReader(is));
	        String line = br.readLine();

	        while (line != null) {
	            if (line.startsWith(name)){
	            	line = br.readLine();
	            	String[] sz = line.split(","); // Z as string
	            	br.readLine();
	            	line = br.readLine();
	            	String[] sx = line.split(","); // X as string
	            	br.readLine();
	            	line = br.readLine();
	            	String[] sy = line.split(","); // Y as string
	            	
	            	rawZ = new float[sz.length];
	            	rawX = new float[sx.length];
	            	rawY = new float[sy.length];
	            	for (int i = 0; i < sz.length; i++){
	            		rawZ[i] = Float.parseFloat(sz[i]);
	            		rawX[i] = Float.parseFloat(sx[i]);
	            		rawY[i] = Float.parseFloat(sy[i]);
	            	}
	            }
	            line = br.readLine();
	        }
	        br.close();
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    Collections.reverse(Arrays.asList(rawZ));
	    Collections.reverse(Arrays.asList(rawX));
	    Collections.reverse(Arrays.asList(rawY));
	    zpoints = StepObj.smooth(rawZ);
	    xpoints = StepObj.smooth(rawX);
	    ypoints = StepObj.smooth(rawY);
	    
	    zpoints = StepObj.normalize(zpoints, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
	    xpoints = StepObj.normalize(xpoints, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
	    ypoints = StepObj.normalize(ypoints, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}
	
	//Apply a low pass filter to a float array
	public static float[] smooth(float[] raw){
		float[] result = new float[raw.length];
		result[raw.length - 1] = raw[raw.length - 1];
		for (int i = (raw.length - 2); i >= 0; i--){
			result[i] = result[i+1] + (raw[i] - result[i+1])/SMOOTHING_CONSTANT;
		}
		return result;
	}
	
	//Normalize data, and indicate if any data point is outside of certain thresholds
	public static float[] normalize(float[] arr, float minthresholdPos, float maxthresholdPos, float minThresholdNeg, float maxthresholdNeg){
		float[] narr = new float[arr.length];
		
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		for(int i = 0; i < arr.length; i++) {
		      if(arr[i] > max) {
		         max = arr[i];
		      }
		      if(arr[i] < min) {
		    	  min = arr[i];
		      }
		}
		
		//If any value is outside of the threshold, normalize the signal as noise
		if (max < minthresholdPos || max > maxthresholdPos 
				|| min > minThresholdNeg || min < maxthresholdNeg){
			for (int i = 0; i < narr.length; i++){
				narr[i] = Integer.MAX_VALUE;
			}
		} else { //normalize the signal based on max and min
			for (int i = 0; i < narr.length; i++){
				narr[i] = (arr[i] + Math.abs(min)) / (Math.abs(max - min));
			}
		}
		
		return narr;
	}
	
	/*
	 * Returns similarity between x, y, and z seperately as an array: 
	 * float = {xsimilarity, ysimilarity, zsimilarity}
	 * 
	 * Smaller numbers mean greater similarity.
	 */
	@SuppressLint("NewApi")
	public float[] similarity(float[] compx, float[] compy, float[] compz, float[][]th){
		float[] similarity = {0f, 0f, 0f};
		
		if(compx.length < xpoints.length || compy.length < ypoints.length || compz.length < zpoints.length){
			//indicates that the given array is too short to be compared
			similarity[0] = Integer.MAX_VALUE; 
			similarity[1] = Integer.MAX_VALUE;
			similarity[2] = Integer.MAX_VALUE;
			return similarity;
		} else {
			//cut arrays to size
			float[] shortx = Arrays.copyOfRange(compx, 0, xpoints.length);
			float[] shorty = Arrays.copyOfRange(compy, 0, ypoints.length);
			float[] shortz = Arrays.copyOfRange(compz, 0, zpoints.length);
			
			//Normalize the values, using sensible thresh-holds
			shortx = StepObj.normalize(StepObj.smooth(shortx), th[0][0], th[0][1], th[0][2], th[0][3]);
			shorty = StepObj.normalize(StepObj.smooth(shorty), th[1][0], th[1][1], th[1][2], th[1][3]);
			shortz = StepObj.normalize(StepObj.smooth(shortz), th[2][0], th[2][1], th[2][2], th[2][3]);
			
			//Ensure step begins with a trough
			if (//Math.abs(shortx[0] - xpoints[0]) > 0.2||
					Math.abs(shortz[0] - zpoints[0]) > 0.2){
				similarity[0] = Integer.MAX_VALUE;
				similarity[1] = Integer.MAX_VALUE;
				similarity[2] = Integer.MAX_VALUE;
				return similarity;
			}
			
			//calculate similarity
			for(int i = 1;i < zpoints.length - 1 ; i++){
				similarity[0] += Math.pow(Math.abs(shortx[i] - xpoints[i]), 2); //x-similarity
				similarity[1] += Math.pow(Math.abs(shorty[i] - ypoints[i]), 2); //y-similarity
				similarity[2] += Math.pow(Math.abs(shortz[i] - zpoints[i]), 2); //z-similarity
			}
			
			//adjust similarity to account for step length
			similarity[0] /= xpoints.length;
			similarity[1] /= ypoints.length;
			similarity[2] /= zpoints.length;
			
			return similarity;
		}
	}
	
	/*
	 * Add a factor to the similarity scores
	 */
	public float[] similarity(float[] compx, float[] compy, float[] compz, float[][]th, float factor){
		float[] similarity = similarity(compx, compy, compz, th);
		similarity[0] *= factor;
		similarity[1] *= factor;
		similarity[2] *= factor;
		return similarity;
	}
}
