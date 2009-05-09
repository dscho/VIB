package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.media.j3d.View;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.ImageCanvas;
import ij.gui.Toolbar;
import ij.process.ColorProcessor;
import ij.macro.Interpreter;

import java.awt.AWTException;
import java.awt.Label;
import java.awt.Color;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

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
import javax.vecmath.Color3f;

public class ImageWindow3D extends ImageWindow implements UniverseListener,
							WindowStateListener,
							KeyListener {
	DefaultUniverse universe;
	ImageCanvas3D canvas3D;
	Label status = new Label("");
	private boolean noOffScreen = true;
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

		add(canvas3D, -1);
		status.setText("");
		status.setForeground(Color.WHITE);
		Color3f c = UniverseSettings.defaultBackground;
		status.setBackground(new Color(c.x, c.y, c.z));
		status.setFont(new Font("Verdana", Font.PLAIN, 20));
		add(status, BorderLayout.SOUTH, -1);

		pack();

		addFocusListener(this);
		setFocusTraversalKeysEnabled(false);
		addWindowListener(this);
		addWindowStateListener(this);
		// this listener first, to interrupt events
		canvas3D.addKeyListener(this);
		ImageJ ij = IJ.getInstance();
		if (ij != null) {
			canvas3D.addKeyListener(ij);
			addKeyListener(ij);
		}
		universe.addUniverseListener(this);
		updateImagePlus();
		Toolbar.getInstance().setTool(Toolbar.HAND);
		show();
	}

	public boolean close() {
		boolean b = super.close();
		return b;
	}

	public DefaultUniverse getUniverse() {
		return universe;
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
	public Insets getInsets() {
		// pretend to have a canvas to avoid a NullPointerException
		// when calling the super method
		ic = getCanvas();
		Insets insets = super.getInsets();
		ic = null;
		double mag = 1;
		int extraWidth = (int)((MIN_WIDTH - imp.getWidth()*mag) / 2.0);
		if (extraWidth < 0) extraWidth = 0;
		int extraHeight = (int)((MIN_HEIGHT - imp.getHeight()*mag)/2.0);
		if (extraHeight < 0) extraHeight = 0;
		insets = new Insets(
			insets.top - 10 /* TEXT_GAP */ - extraHeight,
			insets.left - extraWidth,
			insets.bottom - extraHeight,
			insets.right - extraWidth);
		return insets;
	}

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
		//this.imp = getNewImagePlus();
		imp_updater.update();
	}

	public void updateImagePlusAndWait() {
		imp_updater.updateAndWait();
	}

	final ImagePlusUpdater imp_updater = new ImagePlusUpdater();

	private class ImagePlusUpdater extends Thread {
		boolean go = true;
		int update = 0;
		ImagePlusUpdater() {
			super("3D-V-IMP-updater");
			try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
			setPriority(Thread.NORM_PRIORITY);
			start();
		}
		void update() {
			synchronized (this) {
				update++;
				notify();
			}
		}
		void updateAndWait() {
			update();
			synchronized (this) {
				while (update > 0) {
					try { wait(); } catch (InterruptedException ie) { ie.printStackTrace(); }
				}
			}
		}
		public void run() {
			while (go) {
				final int u;
				synchronized (this) {
					if (0 == update) {
						try { wait(); } catch (InterruptedException ie) { ie.printStackTrace(); }
					}
					u = update;
				}
				ImageWindow3D.this.imp = getNewImagePlus();
				synchronized (this) {
					if (u != update) continue; // try again, there was a new request
					// Else, done:
					update = 0;
					notify(); // for updateAndWait
				}
			}
		}
		void quit() {
			go = false;
			synchronized (this) {
				update = -Integer.MAX_VALUE;
				notify();
			}
		}
	}

	public ImagePlus getImagePlus() {
		if(imp == null)
			imp_updater.updateAndWait(); //updateImagePlus();
		return imp;
	}

// 	private int top = 25, bottom = 4, left = 4, right = 4;
	private int top = 0, bottom = 0, left = 0, right = 0;
	private ImagePlus getNewImagePlus() {
		if (getWidth() <= 0 || getHeight() <= 0)
			return makeDummyImagePlus();
		if (noOffScreen) {
			if (universe.getUseToFront())
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
		destroy();
	}

	public void destroy() {
		if (null == universe) return;
		universe.removeUniverseListener(this);

		// Must remove the listener so this instance can be garbage
		// collected and removed from the Canvas3D, overcomming the limit
		// of 32 total Canvas3D instances.
		try {
			Method m = SimpleUniverse.class.getMethod(
					"removeRenderingErrorListener",
					new Class[]{RenderingErrorListener.class});
			if (null != m)
				m.invoke(universe, new Object[]{error_listener});
		} catch (Exception ex) {
			System.out.println(
					"Could NOT remove the RenderingErrorListener!");
			ex.printStackTrace();
		}

		if (null != universe.getWindow())
			universe.close();
		ImageJ ij = IJ.getInstance();
		if (null != ij) {
			removeKeyListener(ij);
			canvas3D.removeKeyListener(ij);
		}
		imp_updater.quit();
		canvas3D.flush();
		universe = null;
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
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			if (Toolbar.getToolId() == Toolbar.HAND)
				Toolbar.getInstance().setTool(lastToolID);
			else {
				lastToolID = Toolbar.getToolId();
				Toolbar.getInstance().setTool(Toolbar.HAND);
			}
		}
		// AVOID forwarding the x,y,z commands to ImageJ when manipulating
		// an orthoslice
		Content c = universe.getSelected();
		if (null != c) {
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

