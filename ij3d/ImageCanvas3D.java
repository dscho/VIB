package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.Canvas3D;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.awt.Image;
import java.awt.image.BufferedImage;

import java.awt.GraphicsConfiguration;
import java.awt.Polygon;
import java.awt.Point;
import java.awt.Color;
import java.awt.Font;

import java.util.List;
import java.util.ArrayList;

import ij.process.ByteProcessor;
import ij.gui.Toolbar;
import ij.gui.ImageCanvas;
import ij.ImagePlus;
import ij.gui.Roi;

public class ImageCanvas3D extends Canvas3D {

	private RoiImagePlus roiImagePlus;
	private ImageCanvas roiImageCanvas;

	private class RoiImagePlus extends ImagePlus {
		public RoiImagePlus(String title, ByteProcessor ip) {
			super();
			setProcessor(title, ip);
		}

		public ImageCanvas getCanvas() {
			return roiImageCanvas;
		}
	}

	public ImageCanvas3D(int width,int height) {
		super(SimpleUniverse.getPreferredConfiguration());
		setSize(width, height);
		ByteProcessor ip = new ByteProcessor(width, height);
		roiImagePlus = new RoiImagePlus("RoiImage", ip); 
		roiImageCanvas = new ImageCanvas(roiImagePlus) {
			/* prevent ROI to enlarge/move on mouse click */
			public void mousePressed(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.MAGNIFIER)
					super.mousePressed(e);
			}
		};
		roiImageCanvas.disablePopupMenu(true);
		
		addListeners();
		addMouseListener(roiImageCanvas);
		addMouseMotionListener(roiImageCanvas);
	}

	private boolean isSelectionTool() {
		 int tool = Toolbar.getToolId();
		 return tool == Toolbar.RECTANGLE || tool == Toolbar.OVAL 
		 	|| tool == Toolbar.POLYGON || tool == Toolbar.FREEROI
			|| tool == Toolbar.LINE || tool == Toolbar.POLYLINE
			|| tool == Toolbar.FREELINE || tool == Toolbar.POINT
			|| tool == Toolbar.WAND;
	}

	private void addListeners() {
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if(isSelectionTool())
					postRender();
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(isSelectionTool())
					render();
			}
			public void mouseReleased(MouseEvent e) {
				if(isSelectionTool())
					render();
			}
			public void mousePressed(MouseEvent e) {
				if(!isSelectionTool())
					roiImagePlus.killRoi();
			}
		});
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				ByteProcessor ip = new ByteProcessor(
								getWidth(), 
								getHeight());
				roiImagePlus.setProcessor("RoiImagePlus", ip);
				render();
			}
		});
	} 

	public Roi getRoi() {
		return roiImagePlus.getRoi();
	}

	public void render() {
		stopRenderer();
		swap();
		startRenderer();
	}

	private String status = "";
	private Color color = Color.WHITE;

//	public void setStatus(String bla) {
//		status = bla;
//		color = Color.WHITE;
//		postRender();
//		new Thread(new Runnable() {
//			public void run() {
//				try {
//					Thread.currentThread().sleep(2000);
//					color = Color.LIGHT_GRAY;
//					postRender();
//					Thread.currentThread().sleep(200);
//					color = Color.GRAY;
//					postRender();
//					Thread.currentThread().sleep(200);
//					color = Color.DARK_GRAY;
//					postRender();
//					Thread.currentThread().sleep(200);
//					color = Color.BLACK;
//					postRender();
//					status = "";
//				} catch(Exception e) {}
//			}
//		}).start();
//	}

	Font font = new Font("Helvetica", Font.PLAIN, 20);

	public void postRender() {
		J3DGraphics2D g3d = getGraphics2D();
		Roi roi = roiImagePlus.getRoi();
		if(roi != null) {
			roi.draw(g3d);
		}
		if(!status.equals("")) {
			g3d.setColor(color);
			g3d.setFont(font);
			g3d.drawString(status, 20, getHeight()-20);
		}
		g3d.flush(true);
	}
} 

