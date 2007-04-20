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
		roiImageCanvas = new ImageCanvas(roiImagePlus);
		roiImageCanvas.disablePopupMenu(true);
		
		addListeners();
		addMouseListener(roiImageCanvas);
		addMouseMotionListener(roiImageCanvas);
	}

	private void addListeners() {
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				postRender();
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				render();
			}
			public void mouseReleased(MouseEvent e) {
				render();
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

	private void render() {
		stopRenderer();
		swap();
		startRenderer();
	}

	public void postRender(){
		J3DGraphics2D g3d = getGraphics2D();
		Roi roi = roiImagePlus.getRoi();
		if(roi != null) {
			roi.draw(g3d);
		}
		g3d.flush(true);
	}
} 

