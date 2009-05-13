package customnode;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.loaders.objectfile.ObjectFile;
import com.sun.j3d.loaders.Scene;

import java.util.List;
import java.util.ArrayList;

import java.io.FileNotFoundException;

public class MeshLoader {

	public static List<CustomMesh> load(String file) {

		ObjectFile of = new ObjectFile();
		Scene scene = null;

		try {
			scene = of.load(file);
		} catch(FileNotFoundException e) {
			System.out.println("Could not find " + file);
			return null;
		}

		List<CustomMesh> meshes = new ArrayList<CustomMesh>();

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
			if(ga instanceof TriangleArray)
				meshes.add(new CustomTriangleMesh(
					readCoordinatesFromInterleaved(ga)));
			else if(ga instanceof QuadArray)
				meshes.add(new CustomQuadMesh(
					readCoordinatesFromInterleaved(ga)));
			else if(ga instanceof PointArray)
				meshes.add(new CustomPointMesh(
					readCoordinatesFromInterleaved(ga)));
			else if(ga instanceof LineArray)
				meshes.add(new CustomLineMesh(
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

