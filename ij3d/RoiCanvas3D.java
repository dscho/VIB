package ij3d;

import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.Canvas3D;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

import java.awt.GraphicsConfiguration;
import java.awt.Polygon;
import java.awt.Point;
import java.awt.Color;

import java.util.List;
import java.util.ArrayList;

import ij.gui.Toolbar;

public class RoiCanvas3D extends Canvas3D implements MouseListener, 
													MouseMotionListener {

	private List<Point> points = new ArrayList<Point>();
	private Polygon polygon;
	private boolean selecting = false;

	public RoiCanvas3D(GraphicsConfiguration config) {
		super(config);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	private boolean isLeftMouseButton(MouseEvent e) {
		return e.getButton() == MouseEvent.BUTTON1;
	}

	private boolean checkMouse(MouseEvent e) {
		boolean shouldPerform = Toolbar.getToolId() == Toolbar.FREELINE || 
								Toolbar.getToolId() == Toolbar.FREEROI;
		if(!shouldPerform) {
			polygon = null;
			points.clear();
		}
		return shouldPerform;
	}

	public void mouseExited(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {
		if(checkMouse(e) && isLeftMouseButton(e)) {
			points.clear();
			polygon = null;
			selecting = false;
		}
	}
	
	public void mousePressed(MouseEvent e) {
		if(isLeftMouseButton(e) && checkMouse(e)) {
			points.clear();
			points.add(e.getPoint());
			polygon = null;
			selecting = true;
		}
	}

	public void mouseReleased(MouseEvent e) {
		if(!checkMouse(e) || !isLeftMouseButton(e))
			return;
		points.add(points.get(0));
		initPolygon();
		stopRenderer();
		swap();
		startRenderer();
		selecting = false;
	}
		
	public void mouseDragged(MouseEvent e) {
		if(!selecting)
			return;
		points.add(e.getPoint());
		stopRenderer();
		swap();
		startRenderer();
	}

	public void mouseMoved(MouseEvent e) {}

	public void postRender(){
		drawRoi();
		if(polygon != null)
			fillRoi();
	}

	public void drawRoi() {
		J3DGraphics2D g3d = getGraphics2D();
		g3d.setColor(Color.yellow);
		for(int i=1; i<points.size(); i++) {
			Point cur = points.get(i);
			Point prev = points.get(i-1);
			g3d.drawLine(prev.x, prev.y, cur.x, cur.y);
		}
		g3d.flush(true);
	}

	public void fillRoi() {
		
		J3DGraphics2D g3d = getGraphics2D();
		g3d.setColor(new java.awt.Color(255,255,0,50));
		g3d.fillPolygon(polygon);
		g3d.flush(true);
	}

	public void initPolygon() {
		int[] x = new int[points.size()];
		int[] y = new int[points.size()];
		for(int i = 0; i < x.length; i++) {
			Point p = points.get(i);
			x[i] = p.x;
			y[i] = p.y;
		}
		polygon = new Polygon(x, y, x.length);
	}

	public Polygon getPolygon() {
		return polygon;
	}
} // end class RoiCanvas3D

