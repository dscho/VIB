package ij3d;

import ij3d.shapes.CoordinateSystem;
import ij3d.shapes.Scalebar;
import ij3d.behaviors.InteractiveBehavior;
import ij.gui.Toolbar;
import ij.ImagePlus;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.j3d.utils.universe.SimpleUniverse;

import ij3d.behaviors.BehaviorCallback;

import ij3d.behaviors.Picker;
import ij3d.behaviors.WaitForNextFrameBehavior;
import ij3d.behaviors.ContentTransformer;
import ij3d.behaviors.InteractiveViewPlatformTransformer;
import java.util.BitSet;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.PointLight;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

public abstract class DefaultUniverse extends SimpleUniverse
						implements BehaviorCallback {

	/**
	 * The index of the transform group in the viewing platform's
	 * MultiTransformGroup which is responsible for centering
	 */
	public static final int CENTER_TG    = 0;
	/**
	 * The index of the transform group in the viewing platform's
	 * MultiTransformGroup which is responsible for zooming
	 */
	public static final int ZOOM_TG      = 4;
	/**
	 * The index of the transform group in the viewing platform's
	 * MultiTransformGroup which is responsible for translation
	 */
	public static final int TRANSLATE_TG = 1;
	/**
	 * The index of the transform group in the viewing platform's
	 * MultiTransformGroup which is responsible for animation
	 */
	public static final int ANIMATE_TG   = 3;
	/**
	 * The index of the transform group in the viewing platform's
	 * MultiTransformGroup which is responsible for rotation
	 */
	public static final int ROTATION_TG  = 2;





	/**
	 * Constant used in showAttribute() specifying whether a scalebar
	 * should be displayed or not.
	 */
	public static final int ATTRIBUTE_SCALEBAR     = 0;

	/**
	 * Constant used in showAttribute() specifying whether a global coordinate
	 * system should be displayed or not.
	 */
	public static final int ATTRIBUTE_COORD_SYSTEM = 1;





	/**
	 * Reference to root BranchGroup. This is the place to add global entries.
	 */
	protected BranchGroup scene;

	/**
	 * Reference to the optionally displayable scale bar.
	 */
	protected Scalebar scalebar;

	/**
	 * Reference to the global coordinate system, which is optionally
	 * displayable.
	 */
	protected CoordinateSystem globalCoord;

	/**
	 * The shared Bounds object which is needed for Java3D's scheduling
	 * mechanism.
	 */
	protected BoundingSphere bounds;

	/**
	 * A reference to the window in which this universe is shown.
	 */
	protected ImageWindow3D win;





	/** The global minimum point */
	protected final Point3d globalMin    = new Point3d();

	/** The global maximum point */
	protected final Point3d globalMax    = new Point3d();

	/** The global center point */
	protected final Point3d globalCenter = new Point3d();

	/**
	 * Reference to the InteractiveBehavior. This handles mouse and
	 * keyboard input.
	 */
	protected final InteractiveBehavior mouseBehavior;

	/**
	 * Reference to the ContentTransformer. This handles the mouse and
	 * keyboard input which addresses individual Contents and is used by
	 * the InteractiveBehavior.
	 */
	protected final ContentTransformer contentTransformer;

	/**
	 * Reference to the Picker. This handles the picking of Contents and
	 * of landmark points of individual objects. The picker is also used by
	 * the InteractiveBehavior.
	 */
	protected final Picker picker;

	/**
	 * Reference to the InteractiveViewPlatformTransformer. This handles
	 * mouse and keyboard input which transforms the whole view. It is
	 * used by the InteractiveBehavior.
	 */
	protected final InteractiveViewPlatformTransformer viewTransformer;

	/**
	 * Reference to the WaitForNextFrameBehavior. This provides a way to
	 * wait until the next frame is rendered.
	 */
	protected final WaitForNextFrameBehavior frameBehavior;


	/**
	 * Switch which holds the optionally displayable scalebar and coordinate
	 * system.
	 */
	protected final Switch attributesSwitch;
	private BitSet attributesMask = new BitSet(2);

	private List listeners = new ArrayList();
	private boolean transformed = false;

	public abstract Content getSelected();
	public abstract Iterator contents();

	/**
	 * Constructor.
	 * Sets up the universe, adds the switch for the attributes (scalebar,
	 * coordinate syste), and initializes some light sources.
	 * @param width
	 * @param height
	 */
	public DefaultUniverse(int width, int height) {
		super(new ImageCanvas3D(width, height), 5);
		getViewer().getView().setProjectionPolicy(UniverseSettings.projection);

		bounds = new BoundingSphere();
		bounds.setRadius(10000.0);

		scene = new BranchGroup();
		scene.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		scene.setCapability(Group.ALLOW_CHILDREN_READ);
		scene.setCapability(Group.ALLOW_CHILDREN_WRITE);

		attributesSwitch = new Switch();
		attributesSwitch.setWhichChild(Switch.CHILD_MASK);
		attributesSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		attributesSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		scene.addChild(attributesSwitch);

		scalebar = new Scalebar();
		attributesSwitch.addChild(scalebar);
		attributesMask.set(ATTRIBUTE_SCALEBAR, UniverseSettings.showScalebar);

		// ah, and maybe a global coordinate system
		globalCoord = new CoordinateSystem(100, new Color3f(1, 0, 0));
		attributesSwitch.addChild(globalCoord);
		attributesMask.set(ATTRIBUTE_COORD_SYSTEM, UniverseSettings.showGlobalCoordinateSystem);

		attributesSwitch.setChildMask(attributesMask);

		// Lightening
		BranchGroup lightBG = new BranchGroup();

		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(bounds);
		lightA.setEnable(false);
		lightBG.addChild(lightA);

		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(bounds);
		lightBG.addChild(lightD1);

		PointLight lightS = new PointLight();
		lightS.setPosition(-2, 0, -3);
		lightS.setInfluencingBounds(bounds);
		lightBG.addChild(lightS);

		getZoomTG().addChild(lightBG);

		// setup global mouse behavior
		viewTransformer = new InteractiveViewPlatformTransformer(this, this);
		contentTransformer = new ContentTransformer(this, this);
		picker = new Picker(this);
		mouseBehavior = new InteractiveBehavior(this);
		mouseBehavior.setSchedulingBounds(bounds);
		scene.addChild(mouseBehavior);

		// add frame behavior
		frameBehavior = new WaitForNextFrameBehavior();
		frameBehavior.setSchedulingBounds(bounds);
		frameBehavior.setEnable(true);
		scene.addChild(frameBehavior);

		// add the scene to the universe
		scene.compile();
		addBranchGraph(scene);

		getCanvas().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int id = Toolbar.getToolId();
				if(id == Toolbar.HAND || id == Toolbar.MAGNIFIER) {
					if(transformed)
						fireTransformationFinished();
					transformed = false;
				}
			}
		});
		getCanvas().addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int id = Toolbar.getToolId();
				if(id == Toolbar.HAND || id == Toolbar.MAGNIFIER) {
					if(!transformed)
						fireTransformationStarted();
					transformed = true;
				}
			}
		});

		getCanvas().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				fireCanvasResized();
			}
		});

		fireTransformationUpdated();
	}

	/**
	 * Returns the TransformGroup of the viewing platform's
	 * MultiTransformGroup which is responsible for zooming.
	 */
	public TransformGroup getZoomTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(ZOOM_TG);
	}

	/**
	 * Returns the TransformGroup of the viewing platform's
	 * MultiTransformGroup which is responsible for centering the universe.
	 */
	public TransformGroup getCenterTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(CENTER_TG);
	}

	/**
	 * Returns the TransformGroup of the viewing platform's
	 * MultiTransformGroup which is responsible for rotation.
	 */
	public TransformGroup getRotationTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(ROTATION_TG);
	}

	/**
	 * Returns the TransformGroup of the viewing platform's
	 * MultiTransformGroup which is responsible for translation.
	 */
	public TransformGroup getTranslateTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(TRANSLATE_TG);
	}

	/**
	 * Returns the TransformGroup of the viewing platform's
	 * MultiTransformGroup which is responsible for animation.
	 */
	public TransformGroup getAnimationTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(ANIMATE_TG);
	}

	/**
	 * Returns a reference to the optionally displayable scale bar.
	 */
	public Scalebar getScalebar() {
		return scalebar;
	}

	/**
	 * Returns a reference to the ContentTransformer which is used by
	 * this universe to handle mouse and keyboard input which aims to
	 * transform individual Contents.
	 */
	public ContentTransformer getContentTransformer() {
		return contentTransformer;
	}

	/**
	 * Returns a reference to the Picker which is used by
	 * this universe to handle mouse input which aims to pick
	 * Contents and change landmark sets.
	 */
	public Picker getPicker() {
		return picker;
	}

	/**
	 * Returns a reference to the InteractiveViewPlatformTransformer which
	 * is used by this universe to handle mouse and keyboard input which
	 * aims to transform the viewing platform.
	 */
	public InteractiveViewPlatformTransformer getViewPlatformTransformer() {
		return viewTransformer;
	}

	/**
	 * Show or hide the specified attribute.
	 * @param attribute, one of ATTRIBUTE_SCALEBAR or ATTRIBUTE_COORD_SYSTEM
	 * @param flag, indicating whether it should be displayed or hided.
	 */
	public void showAttribute(int attribute, boolean flag) {
		attributesMask.set(attribute, flag);
		attributesSwitch.setChildMask(attributesMask);
	}

	/**
	 * Returns whether the specified attribute is visible or not.
	 * @param attribute, one of ATTRIBUTE_SCALEBAR or ATTRIBUTE_COORD_SYSTEM.
	 */
	public boolean isAttributeVisible(int attribute) {
		return attributesMask.get(attribute);
	}

	/**
	 * Returns a reference to the root BranchGroup of this universe.
	 */
	public BranchGroup getScene() {
		return scene;
	}

	/**
	 * Implements the BehaviorCallback interface of the behavior objects
	 * used by this universe. This method is invoked to inform about
	 * transformation changes. This method simply calls
	 * fireTransformationUpdated().
	 */
	public void transformChanged(int type, Transform3D xf) {
		fireTransformationUpdated();
	}

	/**
	 * Waits until the next frame is rendered.
	 */
	public void waitForNextFrame() {
		frameBehavior.postId(WaitForNextFrameBehavior.TRIGGER_ID);
		synchronized(frameBehavior) {
			try {
				frameBehavior.wait();
			} catch(Exception e) {}
		}
	}

	/**
	 * Flag indicating if the window of this universe should be brought
	 * to front when
	 */
	protected boolean useToFront = true;

	/**
	 * For some interactive applications, the use of toFront() in
	 * ImageWindow3D creates usability problems, so these methods
	 * allow one to supress this behaviour by calling
	 * setUseToFront(false).  This will only have an effect when
	 * off-screen 3D rendering is not available.  You should be
	 * careful about using this - it will, for example, cause
	 * problems for scripted use of the viewer from macros if
	 * off-screen 3D rendering is not available.
	 */
	public void setUseToFront(boolean useToFront) {
		this.useToFront = useToFront;
	}

	/**
	 * Returns the value of the useToFront flag.
	 */
	public boolean getUseToFront() {
		return useToFront;
	}

	/**
	 * Returns the dimensions of this universe.
	 */
	public Dimension getSize() {
		if(win != null)
			return win.getSize();
		return null;
	}

	/**
	 * Set the dimensions of this universe.
	 * @param w the new width
	 * @param h the new height
	 */
	public void setSize(int w, int h) {
		if(win != null)
			win.setSize(w, h);
	}

	/**
	 * Show this universe in a new window.
	 */
	public void show() {
		win = new ImageWindow3D("ImageJ 3D Viewer", this);
	}

	/**
	 * Close this universe and cleanup resources.
	 */
	public void close() {
		UniverseSettings.save();
		if(win != null) {
			fireUniverseClosed();
			while(!listeners.isEmpty())
				listeners.remove(0);
			win.close();
			ImageWindow3D win2 = win;
			win = null;
			win2.destroy();
		}
		// Flush native resources used by this universe:
		super.removeAllLocales();
		super.cleanup();
	}

	/**
	 * Returns a reference to the window in which this universe is displayed.
	 */
	public ImageWindow3D getWindow() {
		return win;
	}

	/**
	 * Returns a snapshot of the current viewer image.
	 */
	public ImagePlus takeSnapshot() {
		win.updateImagePlusAndWait();
		return win.getImagePlus();
	}


	/**
	 * Register the specified UniverseListener
	 */
	public void addUniverseListener(UniverseListener l) {
		listeners.add(l);
	}

	/**
	 * Remove the specified UniverseListener
	 */
	public void removeUniverseListener(UniverseListener l) {
		listeners.remove(l);
	}

	/**
	 * Invokes the univeresClosed() method of all registered
	 * UniverseListeners.
	 */
	public void fireUniverseClosed() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.universeClosed();
		}
	}

	/**
	 * Invokes the transformationStarted() method of all registered
	 * UniverseListeners.
	 */
	public void fireTransformationStarted() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationStarted(getCanvas().getView());
		}
	}

	/**
	 * Invokes the transformationUpdated() method of all registered
	 * UniverseListeners.
	 */
	public void fireTransformationUpdated() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationUpdated(getCanvas().getView());
		}
	}

	/**
	 * Invokes the transformationFinished() method of all registered
	 * UniverseListeners.
	 */
	public void fireTransformationFinished() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationFinished(getCanvas().getView());
		}
	}

	/**
	 * Invokes the contentAdded() method of all registered
	 * UniverseListeners.
	 */
	public void fireContentAdded(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentAdded(c);
		}
	}

	/**
	 * Invokes the contentChanged() method of all registered
	 * UniverseListeners.
	 */
	public void fireContentChanged(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentChanged(c);
		}
	}

	/**
	 * Invokes the contentRemoved() method of all registered
	 * UniverseListeners.
	 */
	public void fireContentRemoved(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentRemoved(c);
		}
	}

	/**
	 * Invokes the contentSelected() method of all registered
	 * UniverseListeners.
	 */
	public void fireContentSelected(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentSelected(c);
		}
	}

	/**
	 * Invokes the canvasResized() method of all registered
	 * UniverseListeners.
	 */
	public void fireCanvasResized() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.canvasResized();
		}
	}
}
