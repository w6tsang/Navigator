package ca.uwaterloo.lab4_201_03;

import android.view.View;
import android.view.View.OnClickListener;

public class CalibrateButton implements OnClickListener {

	private DirectionalCounter dc;
	public CalibrateButton (DirectionalCounter dc) {
		this.dc = dc;
	}
	
	@Override
	public void onClick(View v) {
		this.dc.calibrate();
	}
}
