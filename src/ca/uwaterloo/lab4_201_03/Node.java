package ca.uwaterloo.lab4_201_03;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;
import android.graphics.PointF;

class Node implements Comparable<Node> {
	public Node parent;
	public List<Node> neighbours = new ArrayList<Node>();
	PointF point;
	double f, g, h; //f: total cost, g: cost to get to node, h: estimated cost to end
	
	public Node(Node parent, PointF point, PointF end) {
		this.parent = parent;
		this.point = point;
		this.initHGF(end);
	}
	
	private void initHGF(PointF end) {
		if (this.parent != null) {
			if (this.parent.point.x != this.point.x && this.parent.point.y != this.point.y) { 
				this.g = this.parent.g + Pathfinder.STEP_DIST * Pathfinder.ROOT_2; 
			} else {
				this.g = this.parent.g + Pathfinder.STEP_DIST;
			}
			this.h = Pathfinder.dist(this.point, end);
			this.f = this.h + this.g;
		}
	}
	
	public void addNeighbour(Node n) {
		neighbours.add(n);
	}
	
	public void setBetterPath(Node n) {
		this.parent = n.parent;
		this.f = n.f;
		this.h = n.h;
		this.g = n.g;
	}
	
	@Override
	public int compareTo(Node n) {
		if (this.f > n.f) {
			return 1;
		} else if (this.f < n.f) {
			return -1;
		}
		return 0;
	}
}
