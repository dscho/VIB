package vib_app.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.Container;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentAdapter;

public class Arrow extends Component{
	
	private Point start;
	private Point end;
	private Point control;
	private Color color = Color.WHITE;
	private double angleEnd;
	private GeneralPath path;
	private Polygon triangle_l;
	private Polygon triangle_s;
	
	private static final int ARROW_W = 40;
	private static final int LINE_W = 4;
	
	public Arrow(Container parent, Point start, Point end, Point control) {
		this.start = start;
		this.end = end;
		this.control = control;
		this.calculatePath();
		this.calculateTriangle();
		this.setSize(new Dimension(640,480)); // <- like the parent container
		parent.addComponentListener(new ComponentAdapter(){
			
		});
	}

	public Arrow(Container parent, 
			Point start, Point end, Point control, Color color) {
		this(parent, start, end, control);
		this.color = color;
	}

	public void calculatePath() {
		path = new GeneralPath();
		path.moveTo(start.x, start.y);
		path.quadTo(control.x, control.y, end.x, end.y);
		PathIterator pi = path.getPathIterator(null,2);
		double[] d1 = new double[2];
		double[] d2 = new double[2];
		while(!pi.isDone()) {
			System.arraycopy(d2, 0, d1, 0, 2);
			pi.currentSegment(d2);
			pi.next();
		}
		double dx = d2[0] - d1[0];
		double dy = d2[1] - d1[1];

		angleEnd = Math.atan2(dy, dx);
	}

	public void calculateTriangle() {
		int s = (int)ARROW_W/2 + 5; 
		triangle_s = new Polygon(new int[]{end.x, end.x + s, end.x},
								new int[]{end.y + s, end.y, end.y - s},
								3);
		triangle_l = new Polygon(new int[]{end.x-LINE_W, 
										end.x + s + (int)Math.sqrt(2)*LINE_W, 
				   						end.x-LINE_W},
								new int[]{end.y + s + 2*LINE_W,
										end.y,
				   						end.y - s - 2*LINE_W},
								3);
	}

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke());
		g2d.rotate(angleEnd, end.x, end.y);
		g2d.fillPolygon(triangle_l);
		g2d.rotate(-angleEnd, end.x, end.y);

		g2d.setStroke(new BasicStroke(ARROW_W, BasicStroke.CAP_BUTT, 
					BasicStroke.JOIN_MITER));
		g2d.draw(path);

		g2d.setColor(color);
		g2d.setStroke(new BasicStroke());
		g2d.rotate(angleEnd, end.x, end.y);
		g2d.fillPolygon(triangle_s);
		g2d.rotate(-angleEnd, end.x, end.y);
		
		g2d.setStroke(new BasicStroke(ARROW_W - 2*LINE_W, BasicStroke.CAP_BUTT, 
					BasicStroke.JOIN_MITER));
		g2d.draw(path);

		// debug
		drawPoint(g2d, start);
		drawPoint(g2d, end);
		drawPoint(g2d, control);

	}

	private void drawPoint(Graphics2D g2d, Point p) {
		g2d.setColor(Color.red);
		g2d.fillOval(p.x - 2, p.y - 2, 4, 4);
	}
	
	/*
	public static void main(String[] args) {
		java.awt.Frame f = new java.awt.Frame();
		f.setSize(640, 480);
		Point st = new Point(50, 50);
		Point e = new Point(100, 200);
		Point c = new Point(0, 150);
		f.add(new Arrow(st, e, c, Color.GRAY));
		f.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				System.exit(0);
			}
		});
		f.setVisible(true);
	}*/
}


