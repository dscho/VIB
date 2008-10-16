package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.media.j3d.View;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.ImageCanvas;
import ij.gui.MessageDialog;
import ij.gui.Toolbar;
import ij.process.ColorProcessor;
import ij.macro.Interpreter;

import java.awt.AWTException;
import java.awt.Label;
import java.awt.Color;
import java.awt.Font;
import java.awt.BorderLayout;
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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowStateListener;
import java.awt.event.WindowEvent;

import java.lang.reflect.Method;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.RenderingError;
import javax.media.j3d.RenderingErrorListener;
import javax.media.j3d.Screen3D;

public class ImageWindow3D extends ImageWindow implements UniverseListener, 
							WindowStateListener,
							KeyListener {
	DefaultUniverse universe;
	ImageCanvas3D canvas3D;
	Label status = new Label("");
	private boolean noOffScreen = false;
	private ErrorListener error_listener;

	public ImageWindow3D(String title, DefaultUniverse universe) {
		super(title);
		String j3dNoOffScreen = System.getProperty("j3d.noOffScreen");
		if (j3dNoOffScreen != null && j3dNoOffScreen.equals("true"))
			noOffScreen = true;
		imp = new ImagePlus();
		this.universe = universe;
		this.canvas3D = (ImageCanvas3D)universe.getCanvas();

		error_listener = new ErrorListener();
		error_listener.addTo(universe);

		WindowManager.addWindow(this);
		WindowManager.setCurrentWindow(this);

		add(canvas3D);
		status.setText("");
		status.setForeground(Color.WHITE);
		status.setBackground(Color.BLACK);
		status.setFont(new Font("Verdana", Font.PLAIN, 20));
		add(status, BorderLayout.SOUTH);

		pack();

		addFocusListener(this);
		setFocusTraversalKeysEnabled(false);
		addWindowListener(this);
		addWindowStateListener(this);
		// this listener first, to interrupt events
		canvas3D.addKeyListener(this);
		canvas3D.addKeyListener(ij);
		ImageJ ij = IJ.getInstance();
		if (ij != null)
			addKeyListener(ij);
		universe.addUniverseListener(this);
		updateImagePlus();
		Toolbar.getInstance().setTool(Toolbar.HAND);
		show();

		/*
		 * Fixes a problem occurring on some machines: It happened
		 * from time to time that the window has a size of zero or
		 * at least very very small. Circumvent this behaviour by
		 * the following lines from Albert Cardona:
		 *
		 * Must be run after calls to pack() and show() above.
		 */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				Rectangle box = ImageWindow3D.this.canvas3D.getBounds();
				int min_width = box.width < 512 ? 512 : box.width;
				int min_height = box.height < 512 ? 512 : box.height;
				ImageWindow3D.this.setMinimumSize(new Dimension(min_width, min_height));
				ImageWindow3D.this.setPreferredSize(new Dimension(min_width, min_height));
			}
		});
	}

	public boolean close() {
		boolean b = super.close();
		return b;
	}

	public ImageCanvas getCanvas() {
		return new ImageCanvas(getImagePlus());
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
// 			new MessageDialog(this, "Java3D error",
// 				"Off-screen rendering not supported by this\n"
// 				 + "setup. Falling back to screen capturing");
			System.err.println("Java3D error: " +
 				"Off-screen rendering not supported by this\n" +
				"setup. Falling back to screen capturing");
			return getNewImagePlus();
		}


		ColorProcessor cp = new ColorProcessor(bImage);
		ImagePlus result = new ImagePlus("3d", cp);
		result.setRoi(canvas3D.getRoi());
		return result;
	}

	public Label getStatusLabel() {
		return status;
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

	public void windowClosing(WindowEvent e) {
		super.windowClosing(e);
		universe.removeUniverseListener(this);

		// Must remove the listener so this instance can be garbage collected and removed from the Canvas3D, overcomming the limit of 32 total Canvas3D instances.
		try {
			Method m = SimpleUniverse.class.getMethod("removeRenderingErrorListener", new Class[]{RenderingErrorListener.class});
			if (null != m) m.invoke(universe, new Object[]{error_listener});
		} catch (Exception ex) {
			System.out.println("Could NOT remove the RenderingErrorListener!");
			ex.printStackTrace();
		}

		universe.close();
		ImageJ ij = IJ.getInstance();
		if (null != ij) {
			removeKeyListener(ij);
			canvas3D.removeKeyListener(ij);
		}
	}

	/*
	 * The UniverseListener interface
	 */
	public void universeClosed() {}
	public void transformationStarted(View view) {}
	public void transformationUpdated(View view) {}
	public void contentSelected(Content c) {}
	public void transformationFinished(View view) {
		updateImagePlus();
	}

	public void contentAdded(Content c){
		updateImagePlus();
	}

	public void contentRemoved(Content c){
		updateImagePlus();
	}

	public void contentChanged(Content c){
		updateImagePlus();
	}

	public void canvasResized() {
		updateImagePlus();
	}

	private int lastToolID = Toolbar.HAND;

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == e.VK_ESCAPE) {
			if (Toolbar.getToolId() == Toolbar.HAND)
				Toolbar.getInstance().setTool(lastToolID);
			else {
				lastToolID = Toolbar.getToolId();
				Toolbar.getInstance().setTool(Toolbar.HAND);
			}
		}
		// AVOID forwarding the x,y,z commands to ImageJ when manipulating an orthoslice
		Content c = universe.getSelected();
		if (null != c && c.getType() == Content.ORTHO) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_X:
				case KeyEvent.VK_Y:
				case KeyEvent.VK_Z:
					e.consume();
					break;
			}
		}
	}

	private class ErrorListener implements RenderingErrorListener {
		public void errorOccurred(RenderingError error) {
			throw new RuntimeException(error.getDetailMessage());
		}

		/*
		 * This is a slightly ugly workaround for DefaultUniverse not
		 * having addRenderingErrorListener() in Java3D 1.5.
		 * The problem, of course, is that Java3D 1.5 just exit(1)s
		 * on error by default, _unless_ you add a listener!
		 */
		public void addTo(DefaultUniverse universe) {
			try {
				Class[] params = {
					RenderingErrorListener.class
				};
				Class c = universe.getClass();
				String name = "addRenderingErrorListener";
				Method m = c.getMethod(name, params);
				Object[] list = { this };
				m.invoke(universe, list);
			} catch (Exception e) {
				/* method not found -> Java3D 1.4 */
				System.err.println("Java3D < 1.5 detected");
				//e.printStackTrace();
			}
		}
	}

	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
}

