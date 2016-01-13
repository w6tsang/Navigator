package ca.uwaterloo.lab4_201_03;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;

public class SaveDataButton implements OnClickListener{

	StepDetector stepDetector;
	Context ctx;
	
	public SaveDataButton(StepDetector _stepDetector, Context _ctx){
		stepDetector = _stepDetector;
		ctx = _ctx;
	}
	
	@Override
	//save the last 1000 data points to a file in "/saved_data"
	public void onClick(View v) {
		long time = System.currentTimeMillis();
		String filez = "dataz_"+time+".txt";
		String filex = "datax_"+time+".txt";
		String filey = "datay_"+time+".txt";
		String[] files = new String[]{filex, filey, filez};
		String root = Environment.getExternalStorageDirectory().toString();
		File dir = new File(root + "/saved_data");
		dir.mkdirs();
		int coordinate = 0;
		for (String filename : files){
			String output = "";
			File file = new File (dir, filename);
			float[] points = stepDetector.datax;
			
			for (int i = 0; i < points.length; i++){
				output += Float.toString(points[i]) + ",\n";
			}
	
			try {
	
			    FileOutputStream outputStream = new FileOutputStream(file);
			    outputStream.write(output.getBytes());
			    outputStream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			coordinate++;
		}
		
		System.out.println("Saved");
		
	}
}