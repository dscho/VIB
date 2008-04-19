package ij3d;

import ij.gui.Toolbar;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.IJ;

import ij3d.ImageWindow3D;
import ij3d.ImageCanvas3D;

import isosurface.Triangulator;
import isosurface.MeshGroup;

import voltex.VoltexGroup;

import marchingcubes.MCTriangulator;

import java.awt.Panel;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;

import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import com.sun.j3d.utils.picking.behaviors.PickingCallback;
import com.sun.j3d.utils.picking.behaviors.PickRotateBehavior;
import com.sun.j3d.utils.picking.behaviors.PickTranslateBehavior;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

import com.sun.j3d.utils.behaviors.keyboard.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;

public abstract class DefaultUniverse extends SimpleUniverse implements 
					MouseBehaviorCallback, PickingCallback {

	protected BranchGroup root;
	protected BranchGroup scene;
	protected Scalebar scalebar;
	protected TransformGroup centerTG;
	protected TransformGroup translateTG;
	protected TransformGroup rotationsTG;
	protected TransformGroup scaleTG;
	protected BoundingSphere bounds;
	protected ImageWindow3D win;
	protected MouseBehavior mouseBehavior;

	private List listeners = new ArrayList();
	private boolean transformed = false;

	public abstract Content getSelected();
	public abstract Iterator contents();

	public TransformGroup getGlobalRotate() {
		return rotationsTG;
	}

	public TransformGroup getGlobalScale() {
		return scaleTG;
	}

	public TransformGroup getGlobalTranslate() {
		return translateTG;
	}

	public TransformGroup getCenterTG() {
		return centerTG;
	}

	public Scalebar getScalebar() {
		return scalebar;
	}

	public DefaultUniverse(int width, int height) {
		super(new ImageCanvas3D(width, height));
		getViewingPlatform().setNominalViewingTransform();
		getViewer().getView().setProjectionPolicy(
					View.PARALLEL_PROJECTION);

		bounds = new BoundingSphere();
		bounds.setRadius(10.0);

		root = new BranchGroup();
		
		scaleTG = new TransformGroup();
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		root.addChild(scaleTG);

		scalebar = new Scalebar();
		scaleTG.addChild(scalebar);

		rotationsTG = new TransformGroup();
		rotationsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		rotationsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		scaleTG.addChild(rotationsTG);

		translateTG = new TransformGroup();
		translateTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		translateTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		rotationsTG.addChild(translateTG);

		centerTG = new TransformGroup();
		centerTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		centerTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		translateTG.addChild(centerTG);

		scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		centerTG.addChild(scene);

		// Lightening
		AmbientLight lightA = new AmbientLight();
		lightA.setInfluencingBounds(bounds);
		lightA.setEnable(false);
		root.addChild(lightA);
		DirectionalLight lightD1 = new DirectionalLight();
		lightD1.setInfluencingBounds(bounds);
		root.addChild(lightD1);

		SpotLight lightS = new SpotLight();
		lightS.setInfluencingBounds(bounds);
		root.addChild(lightS);

		// setup global mouse rotation
		mouseBehavior = new MouseBehavior(this);
		mouseBehavior.setSchedulingBounds(bounds);
		mouseBehavior.setupCallback(this);
		root.addChild(mouseBehavior);

		getCanvas().addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if(Toolbar.getToolId() == Toolbar.HAND) {
					if(transformed) 
						fireTransformationFinished();
					transformed = false;
				}
			}
		});
		getCanvas().addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if(Toolbar.getToolId() == Toolbar.HAND) {
					if(!transformed)
						fireTransformationStarted();
					transformed = true;
				}
			}
		});

		getCanvas().addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				fireCanvasResized();
			}
		});

		fireTransformationUpdated();

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
		if(win != null) {
			while(!listeners.isEmpty())
				listeners.remove(0);
			win.close();
			win = null;
		}
	}

	public void addUniverseListener(UniverseListener l) {
		listeners.add(l);
	}

	public void removeUniverseListener(UniverseListener l) {
		listeners.remove(l);
	}

	public void fireTransformationStarted() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationStarted();
		}
	}

	public void fireTransformationUpdated() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationUpdated();
		}
	}

	public void fireTransformationFinished() {
		for(int i = 0; i < listeners.size(); i++) {
			UniverseListener l = (UniverseListener)listeners.get(i);
			l.transformationFinished();
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
			l.contentRemoved(c);;
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
