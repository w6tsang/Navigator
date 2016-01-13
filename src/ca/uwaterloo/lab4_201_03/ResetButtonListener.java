package ca.uwaterloo.lab4_201_03;

import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;

public class ResetButtonListener implements OnClickListener {

	Resettable[] resettableObject;
	
	public ResetButtonListener(Resettable[] resettableObject) {
		this.resettableObject = resettableObject;
	}
	
	@Override
	//reset all ressetableObjects on click
	public void onClick(View v) {
		for (Resettable r : this.resettableObject) {
			r.reset();
		}
	}
}
