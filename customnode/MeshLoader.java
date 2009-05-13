package customnode;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.loaders.objectfile.ObjectFile;
import com.sun.j3d.loaders.Scene;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import java.io.FileNotFoundException;

public class MeshLoader {

	public static Map<String,CustomMesh> load(String file) {

		ObjectFile of = new ObjectFile();
		Scene scene = null;

		try {
			scene = of.load(file);
		} catch(FileNotFoundException e) {
			System.out.println("Could not find " + file);
			return null;
		}

		Map<String,CustomMesh> meshes = new TreeMap<String,CustomMesh>();

		BranchGroup root = scene.getSceneGroup();
		int n = root.numChildren();

		for(int i = 0; i < n; i++) {
			Node node = root.getChild(i);
			if(!(node instanceof Shape3D))
				continue;

			Shape3D shape = (Shape3D)node;
			Geometry geom = shape.getGeometry();
			if(!(geom instanceof GeometryArray)) {
				System.out.println("Skipping node " + i + ", since " +
					"geometry is not a GeometryArray.");
				continue;
			}

			GeometryArray ga = (GeometryArray)geom;
			int fmt = ga.getVertexFormat();
			if((fmt & GeometryArray.INTERLEAVED) == 0) {
				System.out.println("Skipping node " + i + ", since " +
					"geometry data is not in interleaved format.");
				continue;
			}

			String name = shape.getName();
			if (null == name)
				name = "Mesh-" + (1 + meshes.size());

			if(ga instanceof TriangleArray)
				meshes.put(name, new CustomTriangleMesh(
					readCoordinatesFromInterleaved(ga)));
			else if(ga instanceof QuadArray)
				meshes.put(name, new CustomQuadMesh(
					readCoordinatesFromInterleaved(ga)));
			else if(ga instanceof PointArray)
				meshes.put(name, new CustomPointMesh(
					readCoordinatesFromInterleaved(ga)));
			else if(ga instanceof LineArray)
				meshes.put(name, new CustomLineMesh(
					readCoordinatesFromInterleaved(ga),
					CustomLineMesh.PAIRWISE));
			// TODO LineStripArray
			else
				System.out.println("Skipping node " + i + ", since " +
					"geometry data is not one of TriangleArray, " +
					"QuadArray, PointArray or LineArray.");
		}
		return meshes;
	}

	private static List<Point3f> readCoordinatesFromInterleaved(
								GeometryArray geom) {
		List<Point3f>vertices = new ArrayList<Point3f>();
		int valid = geom.getValidVertexCount();
		float[] data = geom.getInterleavedVertices();
		int dataPerVertex = data.length / valid;

		for(int v = 0; v < valid; v++) {
			int offs = v * dataPerVertex;
			vertices.add(new Point3f(
				data[offs],
				data[offs + 1],
				data[offs + 2]));
		}

		return vertices;
	}
}

