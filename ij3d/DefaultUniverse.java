package ij3d;

import ij3d.shapes.CoordinateSystem;
import ij3d.shapes.Scalebar;
import ij3d.behaviors.MouseBehavior;
import ij.gui.Toolbar;

import java.awt.Dimension;
import java.awt.event.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.j3d.utils.picking.behaviors.PickingCallback;

import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.*;

import ij3d.behaviors.BehaviorCallback;
import javax.media.j3d.*;
import javax.vecmath.*;

import ij3d.behaviors.Picker;
import ij3d.behaviors.ContentTransformer;
import ij3d.behaviors.InteractiveViewPlatformTransformer;
import java.util.BitSet;
import javax.media.j3d.Switch;

public abstract class DefaultUniverse extends SimpleUniverse implements 
					BehaviorCallback, PickingCallback {

	public static final int CENTER_TG    = 0;
	public static final int ZOOM_TG      = 1;
	public static final int TRANSLATE_TG = 2;
	public static final int ANIMATE_TG   = 3;
	public static final int ROTATION_TG  = 4;

	public static final int SCALEBAR = 0;
	public static final int COORD_SYSTEM = 1;

	protected BranchGroup scene;
	protected Scalebar scalebar;
	protected CoordinateSystem globalCoord;
	protected BoundingSphere bounds;
	protected ImageWindow3D win;

	protected final MouseBehavior mouseBehavior;
	protected final ContentTransformer contentTransformer;
	protected final Picker picker;
	protected final InteractiveViewPlatformTransformer viewTransformer;
	protected final Switch attributesSwitch;
	private BitSet attributesMask = new BitSet(2);

	private List listeners = new ArrayList();
	private boolean transformed = false;

	public abstract Content getSelected();
	public abstract Iterator contents();

	public TransformGroup getZoomTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(ZOOM_TG);
	}

	public TransformGroup getCenterTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(CENTER_TG);
	}

	public TransformGroup getRotationTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(ROTATION_TG);
	}

	public TransformGroup getTranslateTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(TRANSLATE_TG);
	}

	public TransformGroup getAnimationTG() {
		return getViewingPlatform().getMultiTransformGroup().getTransformGroup(ANIMATE_TG);
	}

	public Scalebar getScalebar() {
		return scalebar;
	}

	public ContentTransformer getRotator() {
		return contentTransformer;
	}

	public Picker getPicker() {
		return picker;
	}

	public InteractiveViewPlatformTransformer getViewPlatformTransformer() {
		return viewTransformer;
	}

	public DefaultUniverse(int width, int height) {
		super(new ImageCanvas3D(width, height), 5);
//		getViewingPlatform().setNominalViewingTransform();
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
		attributesMask.set(SCALEBAR, UniverseSettings.showScalebar);

		// ah, and maybe a global coordinate system
		globalCoord = new CoordinateSystem(100, new Color3f(1, 0, 0));
		attributesSwitch.addChild(globalCoord);
		attributesMask.set(COORD_SYSTEM, UniverseSettings.showGlobalCoordinateSystem);

		attributesSwitch.setChildMask(attributesMask);

		// Lightening
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(bounds);
		lightA.setEnable(false);
		scene.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(bounds);
		scene.addChild(lightD1);

		SpotLight lightS = new SpotLight();
		lightS.setInfluencingBounds(bounds);
		scene.addChild(lightS);

		// setup global mouse behavior
		viewTransformer = new InteractiveViewPlatformTransformer(this, this);
		contentTransformer = new ContentTransformer(this, this);
		picker = new Picker(this);
		mouseBehavior = new MouseBehavior(this);
		mouseBehavior.setSchedulingBounds(bounds);
		scene.addChild(mouseBehavior);

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

	public void showAttribute(int attribute, boolean flag) {
		attributesMask.set(attribute, flag);
		attributesSwitch.setChildMask(attributesMask);
	}

	public boolean isAttributeVisible(int attribute) {
		return attributesMask.get(attribute);
	}

	public BranchGroup getScene() {
		return scene;
	}

	public void transformChanged(int type, TransformGroup tg) {
		fireTransformationUpdated();
	}

	public void transformChanged(int type, Transform3D xf) {
		TransformGroup tg = null;
		transformChanged(type, tg);
	}

	public void show() {
		win = new ImageWindow3D("ImageJ 3D Viewer", this);
	}

	public Dimension getSize() {
		if(win != null)
			return win.getSize();
		return null;
	}

	public void setSize(int w, int h) {
		if(win != null)
			win.setSize(w, h);
	}

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

	public ImageWindow3D getWindow() {
		return win;
	}

	public void addUniverseListener(UniverseListener l) {
		listeners.add(l);
	}

	public void removeUniverseListener(UniverseListener l) {
		listeners.remove(l);
	}

	public void fireUniverseClosed() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.universeClosed();
		}
	}

	public void fireTransformationStarted() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationStarted(getCanvas().getView());
		}
	}

	public void fireTransformationUpdated() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationUpdated(getCanvas().getView());
		}
	}

	public void fireTransformationFinished() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationFinished(getCanvas().getView());
		}
	}

	public void fireContentAdded(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentAdded(c);
		}
	}

	public void fireContentChanged(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentChanged(c);
		}
	}

	public void fireContentRemoved(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentRemoved(c);
		}
	}

	public void fireContentSelected(Content c) {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.contentSelected(c);
		}
	}

	public void fireCanvasResized() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.canvasResized();
		}
	}
} 
