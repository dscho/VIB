package video;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.PolygonRoi;
import ij.gui.Line;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.Blitter;

import java.io.File;
import java.io.*;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.Panel;
import java.awt.Label;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import java.awt.FlowLayout;
import java.awt.Button;
import java.awt.Color;

import java.awt.geom.GeneralPath;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Edit_Video implements PlugIn, ActionListener {

	private VideoStack stack;
	private ImagePlus preview;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Video editing");
		Panel all = new Panel();
		BoxLayout bl = new BoxLayout(all, BoxLayout.Y_AXIS);
		all.setLayout(bl);


		Panel p = new BorderPanel();
		p.setLayout(new GridLayout(1, 2, 5, 5));
		p.add(new Label("Open video"));
		Button b = new Button("Open");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(3,2));
		p.add(new Label("Add empty frame"));
		b = new Button("Add Frame");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Copy frame"));
		b = new Button("Copy Frame");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Delete Frame"));
		b = new Button("Delete Frame");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(2,2));
		p.add(new Label("Open Frame"));
		b = new Button("Open Frame");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Set Frame"));
		b = new Button("Set Frame");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(5, 2));
		p.add(new Label("Fade over"));
		b = new Button("Fade over");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Draw Line"));
		b = new Button("Draw Line");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Draw Oval"));
		b = new Button("Draw Oval");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Draw Roi"));
		b = new Button("Draw Roi");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Move Selection"));
		b = new Button("Move Selection");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new Label("Create AVI"));
		b = new Button("Create AVI");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Play AVI"));
		b = new Button("Play AVI");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		gd.addPanel(all);
		gd.setModal(false);
		gd.showDialog();
		bl.layoutContainer(all);
		all.repaint();
		if(gd.wasCanceled())
			return;
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("Open")) {
			stack = new VideoStack();
			String dir = 
			"/home/bene/Desktop/sandbox/createMovieFromFrames";
			String basename = "template";
			String ending = "png";
			stack.open(dir, basename, ending);
			preview = new ImagePlus("Preview", stack.getPreview());
			preview.show();
		} else if(e.getActionCommand().equals("Add Frame")) {
			int index = preview.getCurrentSlice();
			stack.addSlice(index, null);
			preview.setSlice(index + 1);
			preview.updateAndDraw();
		} else if(e.getActionCommand().equals("Copy Frame")) {
			int index = preview.getCurrentSlice();
			ImageProcessor ip = stack.getProcessor(index);
			stack.addSlice(index, ip);
			preview.setSlice(index + 1);
			preview.updateAndDraw();
		} else if(e.getActionCommand().equals("Open Frame")) {
			int index = preview.getCurrentSlice();
			ImageProcessor ip = stack.getProcessor(index);
			new ImagePlus(stack.getPreview().
				getSliceLabel(index), ip).show();
		} else if(e.getActionCommand().equals("Set Frame")) {
			int index = preview.getCurrentSlice();
			if(!IJ.getImage().getTitle().equals(
				stack.getPreview().getSliceLabel(index)))
				return;
			ImageProcessor ip = IJ.getImage().getProcessor();
			stack.setSlice(index, ip);
			IJ.getImage().changes = false;
			IJ.getImage().close();
			preview.setStack(null, stack.getPreview());
			preview.setSlice(index);
		} else if(e.getActionCommand().equals("Delete Frame")) {
			int index = preview.getCurrentSlice();
			stack.deleteSlice(index);
			preview.setStack(null, stack.getPreview());
			preview.setSlice(index);
		} else if(e.getActionCommand().equals("Fade over")) {
			fade(20);
		} else if(e.getActionCommand().equals("Draw Line")) {
			int index = preview.getCurrentSlice();
			if(!IJ.getImage().getTitle().equals(
				stack.getPreview().getSliceLabel(index)))
				return;
			Roi roi = IJ.getImage().getRoi();
			if(roi == null || roi.getType() != Roi.LINE)
				return;
			Line line = (Line)roi;
			drawLine(line.x1, line.y1, line.x2, line.y2, 5);
			IJ.getImage().changes = false;
			IJ.getImage().close();
		} else if(e.getActionCommand().equals("Draw Roi")) {
			int index = preview.getCurrentSlice();
			if(!IJ.getImage().getTitle().equals(
				stack.getPreview().getSliceLabel(index)))
				return;
			Roi roi = IJ.getImage().getRoi();
			if(roi == null || !(roi	instanceof PolygonRoi))
				return;
			drawRoi(roi);
			IJ.getImage().changes = false;
			IJ.getImage().close();
		} else if(e.getActionCommand().equals("Draw Oval")) {
			int index = preview.getCurrentSlice();
			if(!IJ.getImage().getTitle().equals(
				stack.getPreview().getSliceLabel(index)))
				return;
			Roi roi = IJ.getImage().getRoi();
			if(roi == null || roi.getType() != Roi.OVAL)
				return;
			Rectangle r = roi.getBounds();
			drawCircle(r.x, r.y, r.width, r.height);
			IJ.getImage().changes = false;
			IJ.getImage().close();
		} else if(e.getActionCommand().equals("Move Selection")) {
			int index = preview.getCurrentSlice();
			if(!IJ.getImage().getTitle().equals(
				stack.getPreview().getSliceLabel(index)))
				return;
			Roi roi = IJ.getImage().getRoi();
			if(roi == null)
				return;
			GenericDialog gd = new GenericDialog("Move selection");
			gd.addNumericField("dx", 0, 0);
			gd.addNumericField("dy", 0, 0);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			moveSelection(roi, (int)gd.getNextNumber(),
				(int)gd.getNextNumber());
			IJ.getImage().changes = false;
			IJ.getImage().close();
		} else if(e.getActionCommand().equals("Create AVI")) {
			createAVI();
		} else if(e.getActionCommand().equals("Play AVI")) {
			playAVI();
		}
		System.out.println("done");
	}

	public void moveSelection(Roi roi, int dx, int dy) {
		int x = roi.getBounds().x;
		int y = roi.getBounds().y;
		int index = preview.getCurrentSlice();
		int dt = dx > dy ? dx : dy;
		ImageProcessor ip = stack.getProcessor(index);
		ip.setRoi(roi);
		ImageProcessor copy = ip.crop();
		ImageProcessor ip2;
		for(int i = 0; i < dt; i++) {
			int xt = x + Math.round((float)i * dx/dt);
			int yt = y + Math.round((float)i * dy/dt);
			ip2 = ip.duplicate();
			ip2.snapshot();
			roi.setLocation(xt, yt);
			ip2.setRoi(roi);
			ip2.copyBits(copy, xt, yt, 
					Blitter.COPY_ZERO_TRANSPARENT);

			ip2.reset(ip2.getMask());
			if(!stack.addSlice(index + i, ip2))
				break;
		}
	}

	public void drawCircle(int x, int y, int w, int h) {
		int index = preview.getCurrentSlice();
		int red = (255 & 0xff) << 16;
		int lw = 3;
		Ellipse2D e = new Ellipse2D.Float(x, y, w, h);
		float[] v = new float[6];
		boolean finished = false;

		ImageProcessor ip = stack.getProcessor(index);
		for(int z = 0; !finished; z++) {
			ip = ip.convertToRGB();
			ip.setValue(red);
			ip.setLineWidth(lw);
			FlatteningPathIterator it = new FlatteningPathIterator(
						e.getPathIterator(null), 1);
			it.currentSegment(v);
			ip.moveTo((int)v[0], (int)v[1]);
			it.next();
			for(int i = 0; i < z; i++) {
				it.currentSegment(v);
				ip.lineTo((int)v[0], (int)v[1]);
				it.next();
				finished = it.isDone();
			}
			if(!stack.addSlice(index + z, ip))
				break;
		}
	}

	public void drawRoi(Roi roi) {
		int index = preview.getCurrentSlice();
		int red = (255 & 0xff) << 16;
		int lw = 3;
		Polygon poly = ((PolygonRoi)roi).getPolygon();
		int n = poly.npoints;
		int[] x = poly.xpoints;
		int[] y = poly.ypoints;
		boolean finished = false;

		for(int z = 0; z < n-1; z++) {
			preview.setSlice(index + z);
			drawLine(x[z], y[z], x[z+1], y[z+1], 3);

			for(int i = 0; i < z; i++) {
				drawLine(x[i], y[i], x[i+1], y[i+1], 3);
			}
		}
	}

	public void drawLine(int x1, int y1, int x2, int y2, int speed) {
		int index = preview.getCurrentSlice();
		int red = (255 & 0xff) << 16;
		int lw = 3;
		int dx = x2 - x1;
		int dy = y2 - y1;

		int dt = Math.abs(dx) > Math.abs(dy) ? dx : dy;
		dt = Math.abs(dt);
		if(dt == 0)
			dt = 1;

		double dx_dt = (double)dx/dt;
		double dy_dt = (double)dy/dt;

		dx = Math.abs(dx);
		dy = Math.abs(dy);

		boolean finished = false;
		double x = x1, y = y1;

		ImageProcessor ip = stack.getProcessor(index);
		for(int z = 0; !finished; z++) {
			ip = ip.convertToRGB();
			ip.setValue(red);
			ip.setLineWidth(lw);

			ip.moveTo(x1, y1);
			x = x1 + (speed * z + 1) * dx_dt;
			y = y1 + (speed * z + 1) * dy_dt;
			for(int i = 0; i < z; i++) {
				ip.lineTo((int)x, (int)y);
				finished = Math.abs((int)x - x1) >= dx &&
					Math.abs((int)y - y1) >= dy;
				x = x1 + (speed * i + 1) * dx_dt;
				y = y1 + (speed * i + 1) * dy_dt;
			}
			if(!stack.addSlice(index + z, ip))
				break;
		}
	}

	public void fade(int numSlices) {
		int index = preview.getCurrentSlice();
		int[] before = (int[])(stack.getProcessor(index).
				convertToRGB().getPixels());
		int[] after = (int[])(stack.getProcessor(index+1).
				convertToRGB().getPixels());

		for(int z = 1; z < numSlices; z++) {
			ColorProcessor bp = new ColorProcessor(
				stack.getWidth(), stack.getHeight());
			int[] pixels = (int[])bp.getPixels();
			double dp = z;
			double dn = numSlices-z;

			for(int i = 0; i < pixels.length; i++) {
				pixels[i] = interpolate(before[i], dp,
							after[i], dn);
			}
			if(!stack.addSlice(index + z - 1, bp))
				break;
		}
	}

	public void createAVI() {
		int w = stack.getWidth();
		int h = stack.getHeight();
		// optimal bit rate
		int obr = 50 * 25 * w * h / 256;
		// mpeg codec options
		String opt = "vbitrate=" + obr + ":mbd=2:keyint=132:vqblur=1.0:"
				+ "cmp=2:subcmp=2:dia=2:mv0:last_pred=3";
		String codec="msmpeg4v2";
		// clean temporary files that can interfere with the 
		// compression phase
		File cwd = new File(stack.getDir());
		new File(cwd, "divx2pass.log").delete();
		new File(cwd, "frameno.avi").delete();
		// compress
		String cmd;
		try {
			cmd = "mencoder -ovc lavc -lavcopts vcodec=" + codec + 
				":vpass=1:" + opt + " -mf type=png:w=" + w + 
				":h=" + h + ":fps=25 -nosound -o /dev/null " + 
				"mf://*.png";
			Process pro = Runtime.getRuntime().exec(cmd, null, cwd);
			pro.waitFor();
			cmd = "mencoder -ovc lavc -lavcopts vcodec=" + codec + 
				":vpass=2:" + opt + " -mf type=png:w=" + w + 
				":h=" + h + ":fps=25 -nosound -o output.avi " + 
				"mf://*.png";
			pro = Runtime.getRuntime().exec(cmd, null, cwd);
			pro.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void playAVI() {
		String cmd = "mplayer -loop 0 output.avi";
		File cwd = new File(stack.getDir());
		try {
			Process pro = Runtime.getRuntime().exec(cmd, null, cwd);
			pro.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public int interpolate(int p, double dp, int n, double dn) {
		int rp = (p&0xff0000) >> 16;
		int rn = (n&0xff0000) >> 16;
		int gp = (p&0xff00) >> 8;
		int gn = (n&0xff00) >> 8;
		int bp = p&0xff;
		int bn = n&0xff;

		byte r_int = (byte) ((rn*dp + rp*dn) / (dn + dp));
		byte g_int = (byte) ((gn*dp + gp*dn) / (dn + dp));
		byte b_int = (byte) ((bn*dp + bp*dn) / (dn + dp));

		return ((r_int&0xff) << 16) + ((g_int&0xff) << 8) + (b_int&0xff);
	}

	private static final Shape roiToShape(Roi roi) {
		Shape shape = null;
		Rectangle r = roi.getBounds();
		int[] xCoords = null;
		int[] yCoords = null;
		int nCoords = 0;
		switch(roi.getType()) {
			case Roi.LINE:
				Line line = (Line)roi;
				shape = new Line2D.Double(
						(double)(line.x1-r.x),
						(double)(line.y1-r.y),
						(double)(line.x2-r.x),
						(double)(line.y2-r.y) );
				break;
			case Roi.RECTANGLE:
				shape = new Rectangle2D.Double(0.0, 0.0,
					(double)r.width, (double)r.height);
				break;
			case Roi.OVAL:
				Polygon p = roi.getPolygon();
				for (int i=0; i<p.npoints; i++) {
					p.xpoints[i] -= r.x;
					p.ypoints[i] -= r.y;
				}
				break;
			case Roi.POLYGON:
				 nCoords =((PolygonRoi)roi).getNCoordinates();
				 xCoords = ((PolygonRoi)roi).getXCoordinates();
				 yCoords = ((PolygonRoi)roi).getYCoordinates();
				 shape = new Polygon(xCoords,yCoords,nCoords);
				 break;
			case Roi.FREEROI: case Roi.TRACED_ROI:
				 nCoords =((PolygonRoi)roi).getNCoordinates();
				 xCoords = ((PolygonRoi)roi).getXCoordinates();
				 yCoords = ((PolygonRoi)roi).getYCoordinates();
				 shape = new GeneralPath(
				 	GeneralPath.WIND_EVEN_ODD,nCoords);
				 ((GeneralPath)shape).moveTo(
				 	(float)xCoords[0], (float)yCoords[0]);
				 for (int i=1; i<nCoords; i++)
					 ((GeneralPath)shape).lineTo(
							(float)xCoords[i],
							(float)yCoords[i]);
				 ((GeneralPath)shape).closePath();
				 break;
			case Roi.POLYLINE: case Roi.FREELINE: case Roi.ANGLE:
				 nCoords =((PolygonRoi)roi).getNCoordinates();
				 xCoords = ((PolygonRoi)roi).getXCoordinates();
				 yCoords = ((PolygonRoi)roi).getYCoordinates();
				 shape = new GeneralPath(
				 	GeneralPath.WIND_NON_ZERO,nCoords);
				 ((GeneralPath)shape).moveTo(
				 	(float)xCoords[0], (float)yCoords[0]);
				 for (int i=1; i<nCoords; i++)
					 ((GeneralPath)shape).lineTo(
							(float)xCoords[i],
							(float)yCoords[i]);
				 break;
			default:
				throw new IllegalArgumentException(
						"Roi type not supported");
		}
		return shape;
	}

	private class BorderPanel extends Panel {

		final Color BC = new Color(139, 142, 255);

		public Insets getInsets() {
			return new Insets(10, 10, 10, 10);
		}

		public void update(Graphics g) {
			paint(g);
		}
		
		public void paint(Graphics g) {
			super.paint(g);
			if(getWidth() == 0 || getHeight() == 0)
				return;
			g.setColor(BC);
			g.drawRect(5, 5, getWidth()-10, getHeight()-10);
		}
	}
}

