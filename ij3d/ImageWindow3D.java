package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.process.ColorProcessor;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Screen3D;

public class ImageWindow3D extends ImageWindow {
	SimpleUniverse universe;
	Canvas3D canvas3D;

	public ImageWindow3D(String title, int width, int height) {
		this(title, getCanvas3D(width, height));
	}

	private ImageWindow3D(String title, Canvas3D canvas3D) {
		this(title, canvas3D, new SimpleUniverse(canvas3D));
	}

	public ImageWindow3D(String title, Canvas3D canvas3D,
			SimpleUniverse universe) {
		super(title);
		imp = new ImagePlus();
		setSize(canvas3D.getWidth(), canvas3D.getHeight());
		this.universe = universe;
		this.canvas3D = canvas3D;

		WindowManager.addWindow(this);
		WindowManager.setCurrentWindow(this);
		add(canvas3D);
		setSize(canvas3D.getWidth(), canvas3D.getHeight());
		ImageJ ij = IJ.getInstance();
		if (ij != null)
			addKeyListener(ij);
		addFocusListener(this);
		addWindowListener(this);
		canvas3D.addKeyListener(ij);
		show();
	}

	private static Canvas3D getCanvas3D(int width, int height) {
		Canvas3D c;
		c = new Canvas3D( SimpleUniverse.getPreferredConfiguration());
		c.setSize(new Dimension(width, height));
		return c;
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

	public ImagePlus getImagePlus() {
		if (getWidth() <= 0 || getHeight() <= 0) {
			ColorProcessor cp = new ColorProcessor(1, 1);
			return new ImagePlus("3D", cp);
		}
		BufferedImage bImage = new BufferedImage(canvas3D.getWidth(),
				canvas3D.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		ImageComponent2D buffer =
			new ImageComponent2D(ImageComponent2D.FORMAT_RGBA,
					bImage);

		getOffScreenCanvas();
		offScreenCanvas3D.setOffScreenBuffer(buffer);
		offScreenCanvas3D.renderOffScreenBuffer();
		offScreenCanvas3D.waitForOffScreenRendering();
		bImage = offScreenCanvas3D.getOffScreenBuffer().getImage();

		// To release the reference of buffer inside Java 3D.
		offScreenCanvas3D.setOffScreenBuffer(null);

		ColorProcessor cp = new ColorProcessor(bImage);
		return new ImagePlus("3d", cp);
	}
}

