package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.MessageDialog;
import ij.process.ColorProcessor;
import ij.macro.Interpreter;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

import java.awt.event.WindowEvent;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Screen3D;

public class ImageWindow3D extends ImageWindow {
	Image3DUniverse universe;
	ImageCanvas3D canvas3D;
	private boolean noOffScreen = false;

	public ImageWindow3D(String title, Image3DUniverse universe) {
		super(title);
		String j3dNoOffScreen = System.getProperty("j3d.noOffScreen");
		if (j3dNoOffScreen != null && j3dNoOffScreen.equals("true"))
			noOffScreen = true;
		imp = new ImagePlus();
		this.universe = universe;
		this.canvas3D = (ImageCanvas3D)universe.getCanvas();


		WindowManager.addWindow(this);
		WindowManager.setCurrentWindow(this);

		add(canvas3D);
		pack();

		ImageJ ij = IJ.getInstance();
		if (ij != null)
			addKeyListener(ij);
		addFocusListener(this);
		setFocusTraversalKeysEnabled(false);
		addWindowListener(this);
		addWindowStateListener(this);
		canvas3D.addKeyListener(ij);
		show();
	}

	private static Canvas3D getCanvas3D(int width, int height) {
		return new ImageCanvas3D(width, height);
	}

	/* prevent ImageWindow from painting */
	public void drawInfo(Graphics g) { }
	public void paint(Graphics g) { }
	public Insets getInsets() { return new Insets(0, 0, 0, 0); }

	/* off-screen rendering stuff */
	private Canvas3D offScreenCanvas3D;
	private Canvas3D getOffScreenCanvas() {
		if (offScreenCanvas3D != null)
			return offScreenCanvas3D;

		GraphicsConfigTemplate3D templ = new GraphicsConfigTemplate3D();
		templ.setDoubleBuffer(GraphicsConfigTemplate3D.UNNECESSARY);
		GraphicsConfiguration gc =
			GraphicsEnvironment.getLocalGraphicsEnvironment().
			getDefaultScreenDevice().getBestConfiguration(templ);

		offScreenCanvas3D = new Canvas3D(gc, true);
		Screen3D sOn = canvas3D.getScreen3D();
		Screen3D sOff = offScreenCanvas3D.getScreen3D();
		sOff.setSize(sOn.getSize());
		sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth());
		sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight());

		universe.getViewer().getView().addCanvas3D(offScreenCanvas3D);

		return offScreenCanvas3D;
	}

	private static ImagePlus makeDummyImagePlus() {
		ColorProcessor cp = new ColorProcessor(1, 1);
		return new ImagePlus("3D", cp);
	}

	public void updateImagePlus() {
		this.imp = getNewImagePlus();
	}

	public ImagePlus getImagePlus() {
		if(imp == null)
			updateImagePlus();
		return imp;
	}

	private int top = 25, bottom = 4, left = 4, right = 4;
	private ImagePlus getNewImagePlus() {
		if (getWidth() <= 0 || getHeight() <= 0)
			return makeDummyImagePlus();
		if (noOffScreen) {
			toFront();
			Point p = canvas3D.getLocationOnScreen();
			int w = canvas3D.getWidth();
			int h = canvas3D.getHeight();
			Robot robot;
			try {
				robot = new Robot(getGraphicsConfiguration()
					.getDevice());
			} catch (AWTException e) {
				return makeDummyImagePlus();
			}
			Rectangle r = new Rectangle(p.x + left, p.y + top,
					w - left - right, h - top - bottom);
			BufferedImage bImage = robot.createScreenCapture(r);
			ColorProcessor cp = new ColorProcessor(bImage);
			ImagePlus result = new ImagePlus("3d", cp);
			result.setRoi(canvas3D.getRoi());
			return result;
		}
		BufferedImage bImage = new BufferedImage(canvas3D.getWidth(),
				canvas3D.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		ImageComponent2D buffer =
			new ImageComponent2D(ImageComponent2D.FORMAT_RGBA,
					bImage);

		try {
			getOffScreenCanvas();
			offScreenCanvas3D.setOffScreenBuffer(buffer);
			offScreenCanvas3D.renderOffScreenBuffer();
			offScreenCanvas3D.waitForOffScreenRendering();
			bImage = offScreenCanvas3D.getOffScreenBuffer()
				.getImage();
			// To release the reference of buffer inside Java 3D.
			offScreenCanvas3D.setOffScreenBuffer(null);
		} catch (Exception e) {
			noOffScreen = true;
			universe.getViewer().getView()
				.removeCanvas3D(offScreenCanvas3D);
			offScreenCanvas3D = null;
			new MessageDialog(this, "Java3D error",
				"Off-screen rendering not supported by this\n"
				 + "setup. Falling back to screen capturing");
			return getImagePlus();
		}


		ColorProcessor cp = new ColorProcessor(bImage);
		ImagePlus result = new ImagePlus("3d", cp);
		result.setRoi(canvas3D.getRoi());
		return result;
	}

	/**
	 * Override windowActivated() in ij.gui.ImageWindow. 
	 * The default implementation sets ImageJ's menubar to this 
	 * ImageWindow, however, we have our own menubar here.
	 */
	public void windowActivated(WindowEvent e) {
		ImageJ ij = IJ.getInstance();
		boolean quitting = ij!=null && ij.quitting();
		imp.setActivated(); // notify ImagePlus that image has been activated
		if (!closed && !quitting && !Interpreter.isBatchMode())
			WindowManager.setCurrentWindow(this);
	}
}

