package ca.uwaterloo.lab4_201_03;

import java.util.ArrayList;
import java.util.List;

import ca.uwaterloo.mapper.MapView;
import ca.uwaterloo.mapper.VectorUtils;
import android.graphics.PointF;
import android.widget.TextView;

public class UserPosition {
	
	PointF pos;
	float stepLength = 1.15f;
	TextView outputView;
	MapView mv;
	String mapName;
	List<PointF> lastPath = new ArrayList<PointF>();
	List<PointF> lastGoodPath = new ArrayList<PointF>();
	
	//holds current user position and contextual info (map, route to end, etc.)
	public UserPosition (MapView mv, TextView outputView, String mapName) {
		this.pos = new PointF(0, 0);
		this.mv = mv;
		this.mapName = mapName;
		this.outputView = outputView;
	}
	
	/**
	 * Teleport the user to a point
	 */
	public void setPos(PointF pos) {
		this.pos = new PointF(pos.x, pos.y);
		List<PointF> lastPath = new ArrayList<PointF>();
		List<PointF> lastGoodPath = new ArrayList<PointF>();
		this.update(0, 0);
	}
	
	/**
	 * Update the user position incrementally by a step length
	 * @param x   Set to 1 for step North, -1 for step South
	 * @param y   Set to 1 for step East, -1 for step West
	 */
	public void update(float x, float y) {
		this.pos.x += (x * this.stepLength);
		this.pos.y -= (y * this.stepLength);
		
		this.mv.setUserPoint(this.pos);
		
		this.lastPath = Pathfinder.generatePath(pos, mv.getDestinationPoint(), mv.getMap(), mapName);
		if (this.lastPath.size() > 0) {
			this.lastGoodPath = this.lastPath;
		}
		mv.setUserPath(lastGoodPath);
	}
	
	/**
	 * Display directions to the the user via a TextView
	 * 
	 * @param bearing   The direction the phone is currently facing
	 */
	public void updateMesage(float bearing) {
		//no path to dest
		if (lastPath.size() <= 1 && lastGoodPath.size() == 0) {
			this.outputView.setText("No path to destination!");
			return;
		}
		
		if (VectorUtils.distance(this.mv.getDestinationPoint(), this.pos) < this.stepLength) { 
			this.outputView.setText("You have arrived!");
			return;
		}
		
		boolean inWall = false;
		PointF end1 = null; //next point on path
		if (this.lastPath.size() == 2) {
			end1 = this.lastPath.get(1);
		} else if (this.lastPath.size() > 2) {
			end1 = this.lastPath.get(this.lastPath.size() - 3);
		} else {
			//User point is in a wall
			end1 = this.lastGoodPath.get(this.lastGoodPath.size() - 1);
			inWall = true;
		}
		
		PointF end2 = new PointF(pos.x + (float)Math.cos(bearing), pos.y + (float)Math.sin(bearing)); //direction facing
		float angle = (float)(VectorUtils.angleBetween(this.pos, end1, end2) / Math.PI) * 180;
		
		//If facing correct direction indicate "walk forward". Otherwise tell user to turn
		if (Math.abs(angle) < 50) {
			this.outputView.setText("Walk Forward!");
			if (inWall) {
				this.outputView.setText("Get out of the Wall! \nWalk Forward!");
			}
		} else if (angle < 0) {
			angle *= -1;
			angle = Math.round(angle / 10) * 10;
			this.outputView.setText("Turn " + angle + "degrees right");
			if (inWall) {
				this.outputView.setText("Get out of the Wall! \nTurn " + angle + "degrees right");
			}
		} else {
			angle = Math.round(angle / 10) * 10;
			this.outputView.setText("Turn " + angle + "degrees left");
			if (inWall) {
				this.outputView.setText("Get out of the Wall! \nTurn " + angle + "degrees left");
			}
		}
	}
	
}
