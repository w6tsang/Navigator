package ca.uwaterloo.lab4_201_03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.uwaterloo.mapper.*;
import android.graphics.Point;
import android.graphics.PointF;

public class Pathfinder {
	//rooms that require finer grid
	public static final ArrayList<String> fineMaps = new ArrayList<String>(){{
	    add("Lab-room-inclined-16deg.svg");
	    add("Lab-room-inclined-9.4deg.svg");
	}};
	
	//Math.sqrt() is expensive: do it once only
	public static final double ROOT_2 = Math.sqrt(2);
	
	//grid separation
	public static final double REG_DIST = 1;
	public static final double SMALL_DIST = 0.5;
	
	//default distance
	public static double STEP_DIST = 1;
	
	private Pathfinder() {} //this is a static class
	
	
	/**
	 * Uses a* algorithm to generate a path from a starting point to an end point without
	 * traversing through walls
	 * 
	 * @param start      The starting point
	 * @param end        The ending point 
	 * @param map        The MapView to handle
	 * @param mapName    Name of the map loaded to the MapView (include extension)
	 * @return
	 */
	public static List<PointF> generatePath(PointF start, PointF end, NavigationalMap map, String mapName) {
		start = new PointF(start.x, start.y);
		end = new PointF(end.x, end.y);
		
		//Set the grid size for a* according to the map
		if (Pathfinder.fineMaps.contains(mapName)) {
			STEP_DIST = SMALL_DIST;
		} else {
			STEP_DIST = REG_DIST;
		}
		
		//If there is a direct route between start and end, take it
		if (map.calculateIntersections(start, end).size() == 0) {
			List<PointF> directRoute = new ArrayList<PointF>();
			directRoute.add(start);
			directRoute.add(end);
			return directRoute;
		}
		
		//a* algorithm
		List<List<Node>> nodes = generateNodes(map);
		Set<Node> open = new HashSet<Node>();  //Use hashsets for O(1) "contains" operations
		Set<Node> closed = new HashSet<Node>();
		Node finalNode = null;
		Node startNode = new Node(null, start, end);
		Pathfinder.populateNeighbours(startNode, nodes);
		open.add(startNode); //start at the "start" node
		while (open.size() > 0 && finalNode == null) { //continue searching until solution is found, or space has been exhaustively searched
			Node q = Pathfinder.smallestF(open); //go to the next least cost node
			open.remove(q);
			for (Node s : q.neighbours) { //See if paths exist between the current node and it's neighbors
				if (!closed.contains(s)) { //if it has not already been visited
					if (map.calculateIntersections(q.point, s.point).size() == 0) { //and there is a way to get there without going through walls
						Node newNode = new Node(q, s.point, end);
						if (s.parent == null || newNode.f < s.f) {
							s.setBetterPath(newNode);
							
							//if it is the final node
							if (Pathfinder.dist(s.point, end) <= Pathfinder.STEP_DIST * Pathfinder.ROOT_2 && map.calculateIntersections(s.point, end).size() == 0) {
								if (finalNode == null) {
									finalNode = new Node(s, end, end);
								} else if (s.f + Pathfinder.dist(s.point, end) < finalNode.f) {
									finalNode = new Node(s, end, end);
								}
							}
						}
						open.add(s);
					}
				}
			}
			closed.add(q);
		}
		
		//backtrack through the shortest route
		List<PointF> path = new ArrayList<PointF>();
		while (finalNode != null) {
			path.add(finalNode.point);
			finalNode = finalNode.parent;
		}
		
		return path;
	}
	
	//generate a grid of nodes. Give each node 8 neighbors
	private static List<List<Node>> generateNodes(NavigationalMap map) {
		List<List<Node>> nodes = new ArrayList<List<Node>>();
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		List<List<PointF>> paths = map.getPaths();
		for (List<PointF> line : paths) {
			for (PointF p : line) {
				if ((int)(p.x / (float)Pathfinder.STEP_DIST) > maxX) {
					maxX = (int)Math.ceil(p.x / (float)Pathfinder.STEP_DIST);
				}
				if ((int)(p.y / (float)Pathfinder.STEP_DIST) > maxY) {
					maxY = (int)Math.ceil(p.y / (float)Pathfinder.STEP_DIST);
				}
			}
		}
		
		maxX = (int)(maxX / (float)Pathfinder.STEP_DIST);
		maxY = (int)(maxY / (float)Pathfinder.STEP_DIST);
		for (int y = 0; y <= maxY; y ++) {
			nodes.add(new ArrayList<Node>());
			for (int x = 0; x <= maxX; x ++) {
				PointF p = new PointF(x*(float)Pathfinder.STEP_DIST, y*(float)Pathfinder.STEP_DIST);
				Node n = new Node(null, p, null);
				
				if (x > 0) {//LEFT
					nodes.get(y).get(x-1).addNeighbour(n);
					n.addNeighbour(nodes.get(y).get(x-1));
				}
				if (x > 0 && y > 0) {//UPPERLEFT
					nodes.get(y-1).get(x-1).addNeighbour(n);
					n.addNeighbour(nodes.get(y-1).get(x-1));
				}
				if (y > 0) {//UP
					nodes.get(y-1).get(x).addNeighbour(n);
					n.addNeighbour(nodes.get(y-1).get(x));
				}
				if (y > 0 && x < maxX ) {//UPPERRIGHT
					nodes.get(y-1).get(x+1).addNeighbour(n);
					n.addNeighbour(nodes.get(y-1).get(x+1));
				}
				nodes.get(y).add(n);
			}
		}
		
		return nodes;
	}
	
	//return the node with smallest f value
	private static Node smallestF(Set<Node> nodes) {
		Node smallestNode = new Node(null, new PointF(), null);
		smallestNode.f = Float.MAX_VALUE;
		for (Node n : nodes) {
			if (n.f < smallestNode.f) {
				smallestNode = n;
			}
		}
		return smallestNode;
	}
	
	//Give a new node 8 neighbors
	private static void populateNeighbours(Node node, List<List<Node>> nodes ) {
		int x = (int)Math.round(node.point.x / Pathfinder.STEP_DIST);
		int y = (int)Math.round(node.point.y / Pathfinder.STEP_DIST);
		int maxX = nodes.get(0).size() - 1;
		int maxY = nodes.size() - 1;
		
		if (x > maxX) { x = maxX; }
		if (x < 0) { x = 0; }
		if (y > maxY) { y = maxY; }
		if (y < 0) { y = 0; }
		
		if (x > 0) { node.addNeighbour(nodes.get(y).get(x - 1)); }//LEFT
		if (x > 0 && y > 0) { node.addNeighbour(nodes.get(y-1).get(x -1)); }//UPPERLEFT
		if (y > 0) { node.addNeighbour(nodes.get(y - 1).get(x)); }//UP
		if (y > 0 && x < maxX) { node.addNeighbour(nodes.get(y - 1).get(x + 1)); }//UPPERRIGHT
		if (x < maxX) { node.addNeighbour(nodes.get(y).get(x + 1)); }//RIGHT
		if (x < maxX && y < maxY) { node.addNeighbour(nodes.get(y + 1).get(x + 1)); }//LOWERRIGHT
		if (y < maxY) { node.addNeighbour(nodes.get(y + 1).get(x)); }//DOWN
		if (y < maxY && x > 0) { node.addNeighbour(nodes.get(y + 1).get(x - 1)); }//LOWERLEFT
	}
	
	//get the distance between two points (without using Math.sqrt, assumes all points are on grid)
	public static double dist(PointF a, PointF b) {
		float diffX = Math.abs(a.x - b.x);
		float diffY = Math.abs(a.y - b.y);
		float min = Math.min(diffX, diffY);
		
		double d = min * Pathfinder.ROOT_2 + Math.max(diffX - min, diffY - min);
		return d;
	}
	
	
}
