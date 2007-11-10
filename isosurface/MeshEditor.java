/** Albert Cardona 2007
 *  Released under the terms of the latest edition of the General Public License.
 */
package isosurface;

import ij3d.Content;
import javax.vecmath.Point3f;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MeshEditor {

	/** If the content wraps a mesh, smooth it by the fraction K (0, 1). */
	static public void smooth(final Content c, final float K) {
		if (null == c || !(c instanceof MeshGroup)) return;
		final MeshGroup mg = (MeshGroup)c;
		final List triangles = mg.shape.getMesh();
		if (0 != triangles.size() % 3) {
			System.out.println("MeshEditor.smooth: need a list of points multiple of 3.");
			return;
		}
		// for each unique point, find which other points are linked by one edge to it.
		// In the traingles List, there are only points, but each sequence of 3 points makes a triangle.
		final Hashtable ht = new Hashtable();
		for (int i=0; i<triangles.size(); i+=3) {
			// process one triangle at a time
			Point3f p1 = (Point3f)triangles.get(i);
			Point3f p2 = (Point3f)triangles.get(i+1);
			Point3f p3 = (Point3f)triangles.get(i+2);
			build(p1, p2, p3, ht);
			build(p2, p3, p1, ht);
			build(p3, p1, p2, ht);
		}
		for (Iterator it = ht.values().iterator(); it.hasNext(); ) {
			PointGroup pg = (PointGroup)it.next();
			pg.smoothMembers(K);
		}
		// done!
		mg.shape.update();
	}

	/** Represents one point in 3D space that appears in multiple intances within the triangles list. */
	static private class PointGroup {
		Point3f first;
		HashSet edges = new HashSet();
		ArrayList members = new ArrayList(); // can't be a HashSet, because points will compare as equal

		PointGroup(Point3f first) {
			this.first = first;
			members.add(first);
		}
		void addMember(Point3f p) {
			members.add(p);
		}
		void addEdge(Point3f p) {
			edges.add(p);
		}
		void smoothMembers(float K) {
			// Compute a vector composed of all vectors to other points with which it shares an edge, and add some fraction of that vector to each member point.
			float vx = 0;
			float vy = 0;
			float vz = 0;
			for (Iterator it = edges.iterator(); it.hasNext(); ) {
				Point3f po = (Point3f)it.next();
				vx += po.x;
				vy += po.y;
				vz += po.z;
			}
			int size = edges.size();
			vx = (vx/size - first.x) * K;
			vy = (vy/size - first.y) * K;
			vz = (vz/size - first.z) * K;
			for (Iterator it = members.iterator(); it.hasNext(); ) {
				Point3f m = (Point3f)it.next();
				m.x += vx;
				m.y += vy;
				m.z += vz;
			}
		}
	}

	/** Build a list of points that are just one edge away from p1, and store them in the Hashtable.  */
	static private void build(Point3f p1, Point3f p2, Point3f p3, Hashtable ht) {
		PointGroup pg = (PointGroup)ht.get(p1);
		if (null != pg) {
			pg.addMember(p1);
		} else {
			pg = new PointGroup(p1);
			ht.put(p1, pg);
		}
		pg.addEdge(p2);
		pg.addEdge(p3);
	}
}
