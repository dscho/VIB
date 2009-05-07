/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This is a helper class that you can create from an Image3DUniverse
   to allow "blocking" versions of the various Image3DUniverse.add[...]
   methods, i.e. those that wait until the Content has been added to
   both the scene and the contents Hashtable before returning. */

/* I haven't implemented all the corresponding add[...] methods from
   Image3DUniverse here, just the ones that I need at the moment for
   Simple_Neurite_Tracer... */

package ij3d;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public class BlockingAdapter implements UniverseListener {

	protected Image3DUniverse universe;

	public BlockingAdapter( Image3DUniverse universe ) {
		this.universe = universe;
		universe.addUniverseListener(this);
	}

	protected static class ContentWrapper {
		Content content;
	}

	HashSet<ContentWrapper> pendingAdditions = new HashSet<ContentWrapper>();
	HashMap<Content,ContentWrapper> contentToWrapper = new HashMap<Content,ContentWrapper>();

	/**
	 *
	 * [Documentation copied from Image3DUniverse.]
	 *
	 * Add a custom mesh, in particular a triangle mesh, to the universe.
	 *
	 * For more details on custom meshes, read the package API docs of
	 * the package customnode.
	 *
	 * @param mesh a list of points which make up the mesh. The number of
	 *        points must be devidable by 3. 3 successive points make up one
	 *        triangle.
	 * @param color the color in which the line is displayed
	 * @param name a name for the added Content
	 * @return the connected Content.
	 */

	public Content addTriangleMesh(List<Point3f> mesh,
				       Color3f color, String name) {
		Content contentAdded;
		ContentWrapper wrapper = new ContentWrapper();
		synchronized (wrapper) {
			synchronized(pendingAdditions) {
				contentAdded = universe.addTriangleMesh(mesh,color,name);
				wrapper.content = contentAdded;
				pendingAdditions.add(wrapper);
				contentToWrapper.put(contentAdded,wrapper);
			}
			try {
				wrapper.wait();
			} catch( InterruptedException e ) {
				return null;
			}
		}
		return contentAdded;
	}

	/**
	 *
	 * [Documentation copied from Image3DUniverse.]
	 *
	 * Add the specified Content to the universe. The is assumed that the
	 * specified Content is constructed correctly.
	 * @param c
	 * @return the added Content, or null if an error occurred.
	 */
	public Content addContent(Content c) {
		Content contentAdded;
		ContentWrapper wrapper = new ContentWrapper();
		synchronized (wrapper) {
			synchronized(pendingAdditions) {
				contentAdded = universe.addContent(c);
				wrapper.content = contentAdded;
				pendingAdditions.add(wrapper);
				contentToWrapper.put(contentAdded,wrapper);
			}
			try {
				wrapper.wait();
			} catch( InterruptedException e ) {
				return null;
			}
		}
		return contentAdded;
	}

	public void transformationStarted(View view) { }
	public void transformationUpdated(View view) { }
	public void transformationFinished(View view) { }
	public void contentAdded(Content c) {
		synchronized (pendingAdditions) {
			ContentWrapper wrapper = contentToWrapper.get(c);
			// This might not be one we're waiting for:
			if( wrapper == null )
				return;
			pendingAdditions.remove(wrapper);
			contentToWrapper.remove(c);
			synchronized (wrapper) {
				wrapper.notify();
			}
		}
	}
	public void contentRemoved(Content c) { }
	public void contentChanged(Content c) { }
	public void contentSelected(Content c) { }
	public void canvasResized() { }
	public void universeClosed() { }
}
