package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Background;
import javax.vecmath.Color3f;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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

import java.util.Map;
import java.util.HashMap;


import ij.process.ByteProcessor;
import ij.gui.Toolbar;
import ij.gui.ImageCanvas;
import ij.ImagePlus;
import ij.gui.Roi;

public class ImageCanvas3D extends Canvas3D implements KeyListener {

	private RoiImagePlus roiImagePlus;
	private ImageCanvas roiImageCanvas;
	private Map<Integer, Long> pressed, released; 
	private Background background = new Background(new Color3f(0, 0, 0)); // black by default

	private class RoiImagePlus extends ImagePlus {
		public RoiImagePlus(String title, ByteProcessor ip) {
			super();
			setProcessor(title, ip);
			pressed = new HashMap<Integer, Long>();
			released = new HashMap<Integer, Long>();
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
		roiImageCanvas.removeKeyListener(ij.IJ.getInstance());
		roiImageCanvas.disablePopupMenu(true);

		getGraphicsContext3D().setBackground(background);

		addListeners();
		addMouseListener(roiImageCanvas);
		addMouseMotionListener(roiImageCanvas);
	}

	public Background getBG() { //can't use getBackground()
		return background;
	}

	public void killRoi() {
		roiImagePlus.killRoi();
		render();
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
		addKeyListener(this);
	} 

	public Roi getRoi() {
		return roiImagePlus.getRoi();
	}

	public void preRender() {
		super.getGraphicsContext3D().clear(); // so background is painted
		super.preRender();
	}

	public void render() {
		stopRenderer();
		swap();
		startRenderer();
	}

	/*
	 * Needed for the isKeyDown() method. Problem:
	 * keyPressed() and keyReleased is fired periodically, 
	 * dependent on the operating system preferences,
	 * even if the key is hold down.
	 */
	public synchronized void keyPressed(KeyEvent e) {
		long when = e.getWhen();
		pressed.put(e.getKeyCode(), when);
	}

	public synchronized void keyReleased(KeyEvent e) {
		long when = e.getWhen();
		released.put(e.getKeyCode(), when);
	}

	public synchronized boolean isKeyDown(int keycode) {
		if(!pressed.containsKey(keycode))
			return false;
		if(!released.containsKey(keycode))
			return true;
		long p = pressed.get(keycode);
		long r = released.get(keycode);
		return p >= r || System.currentTimeMillis() - r < 100;
	}

	public void keyTyped(KeyEvent e) {}

	public void postRender() {
		J3DGraphics2D g3d = getGraphics2D();
		Roi roi = roiImagePlus.getRoi();
		if(roi != null) {
			roi.draw(g3d);
		}
		g3d.flush(true);
	}
} 

