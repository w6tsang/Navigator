package ca.uwaterloo.lab4_201_03;

import java.util.ArrayList;
import java.util.List;

import android.graphics.PointF;
import ca.uwaterloo.mapper.MapView;
import ca.uwaterloo.mapper.NavigationalMap;
import ca.uwaterloo.mapper.PositionListener;

public class MapController implements PositionListener, Resettable {
	
	public String mapName;
	public NavigationalMap map;
	public PointF origin = null;
	public PointF dest = null;
	public UserPosition userPosition = null;
	
	/**
	 * Controls and updates a mapView object
	 * 
	 * @param mv   MapView to be controlled
	 * @param userPosition   A UserPosition object to represent current placement
	 * @param mapName   Name of the map
	 */
	public MapController(MapView mv, UserPosition userPosition, String mapName) {
		this.map = mv.getMap();
		this.mapName = mapName;
		this.userPosition = userPosition;
	}

	@Override
	public void originChanged(MapView source, PointF loc) {
		//Update the origin point and move the user to the new origin
		this.origin = loc;
		this.userPosition.setPos(loc);
		source.setUserPoint(loc);
		this.userPosition.update(0, 0); //calculate path to dest
	}

	@Override
	public void destinationChanged(MapView source, PointF dest) {
		//update the destination point. Do not move the user
		this.dest = dest;
		this.userPosition.update(0, 0); //recalculate path to dest
	}

	/**
	 * Return user to origin
	 */
	@Override
	public void reset() {
		if (this.userPosition != null && this.origin != null) {
			this.userPosition.setPos(new PointF(this.origin.x, this.origin.y));
		}
	}
}
