/** Albert Cardona and Bene Schmid 20070614 at Janelia Farms*/
package isosurface;

import ij3d.Content;

import ij.IJ;
import ij.io.SaveDialog;
import ij.gui.YesNoCancelDialog;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;

import java.util.Collection;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Color3f;


public class MeshExporter {

	private MeshExporter() {}

	/** Accepts a collection of MeshGroup objects. */
	static public void saveAsWaveFront(final Collection contents) {
		if (null == contents || 0 == contents.size())
			return;
		SaveDialog sd = new SaveDialog(
				"Save WaveFront", "untitled", ".obj");
		String dir = sd.getDirectory();
		if (null == dir)
			return;
		String obj_filename = sd.getFileName();
		if (!obj_filename.toLowerCase().endsWith(".obj"))
			obj_filename += ".obj";

		File obj_file = new File(dir + "/" + obj_filename);
		// check if file exists
		if (!IJ.isMacOSX() && obj_file.exists()) {
			YesNoCancelDialog yn = new YesNoCancelDialog(
				IJ.getInstance(),
				"Overwrite?",
				"File  "+obj_filename+" exists!\nOverwrite?");
			if (!yn.yesPressed()) return;
		}

		String mtl_filename = obj_filename.substring(
			0, obj_filename.lastIndexOf('.')) + ".mtl";
		// generate file content
		String[] data = createWaveFront(contents, mtl_filename);
		//
		saveToFile(obj_file, data[0]);
		saveToFile(new File(dir + "/" + mtl_filename), data[1]);
	}

	static public void saveAsDXF(final Collection meshgroups) {
		if (null == meshgroups || 0 == meshgroups.size()) return;
		SaveDialog sd = new SaveDialog("Save as DXF", "untitled", ".dxf");
		String dir = sd.getDirectory();
		if (null == dir) return;
		String dxf_filename = sd.getFileName();
		if (!dxf_filename.toLowerCase().endsWith(".dxf")) dxf_filename += ".dxf";

		File dxf_file = new File(dir + "/" + dxf_filename);
		// check if file exists
		if (!IJ.isMacOSX()) {
			if (dxf_file.exists()) {
				YesNoCancelDialog yn = new YesNoCancelDialog(IJ.getInstance(), "Overwrite?", "File  " + dxf_filename + " exists!\nOverwrite?");
				if (!yn.yesPressed()) return;
			}
		}

		saveToFile(new File(dir + "/" + dxf_filename), createDXF(meshgroups));
	}

	static public String createDXF(final Collection contents) {
		StringBuffer sb_data = new StringBuffer("0\nSECTION\n2\nENTITIES\n");   //header of file
		for (Iterator it = contents.iterator(); it.hasNext(); ) {
			Content ob = (Content)it.next();
			if (ob.getType() != Content.SURFACE) continue;
			final MeshGroup mg = (MeshGroup)ob.getContent();
			List triangles = mg.getMesh().getMesh();
			String title = ob.getName().replaceAll(" ", "_").replaceAll("#", "--");
			Mtl mat = new Mtl(1 - ob.getTransparency(), mg.getMesh().getColor());
			writeTrianglesDXF(sb_data, triangles, title, "" + mat.getAsSingle());
		}
		sb_data.append("0\nENDSEC\n0\nEOF\n");         //TRAILER of the file
		return sb_data.toString();
	}


	static public void writeTrianglesDXF(final StringBuffer sb, final List triangles, final String the_group, final String the_color) {

		final char L = '\n';
		final String s10 = "10\n"; final String s11 = "11\n"; final String s12 = "12\n"; final String s13 = "13\n";
		final String s20 = "20\n"; final String s21 = "21\n"; final String s22 = "22\n"; final String s23 = "23\n";
		final String s30 = "30\n"; final String s31 = "31\n"; final String s32 = "32\n"; final String s33 = "33\n";
		final String triangle_header = "0\n3DFACE\n8\n" + the_group + "\n6\nCONTINUOUS\n62\n" + the_color + L;

		final int len = triangles.size();
		final Point3f[] vert = new Point3f[len];
		triangles.toArray(vert);
		for (int i=0; i<len; i+=3) {

			sb.append(triangle_header)

			.append(s10).append(vert[i].x).append(L)
			.append(s20).append(vert[i].y).append(L)
			.append(s30).append(vert[i].z).append(L)

			.append(s11).append(vert[i+1].x).append(L)
			.append(s21).append(vert[i+1].y).append(L)
			.append(s31).append(vert[i+1].z).append(L)

			.append(s12).append(vert[i+2].x).append(L)
			.append(s22).append(vert[i+2].y).append(L)
			.append(s32).append(vert[i+2].z).append(L)

			.append(s13).append(vert[i+2].x).append(L) // repeated point
			.append(s23).append(vert[i+2].y).append(L)
			.append(s33).append(vert[i+2].z).append(L);
		}
	}

	/**
	 * Expects a collection of MeshGroup objects, and the material file name to point to.
	 * Returns two String objects:
	 * - the contents of the .obj file with mesh data
	 * - the contents of the .mtl file with material data
	 */
	static public String[] createWaveFront(Collection contents, String mtl_filename) {
		final StringBuffer sb_obj = new StringBuffer("# OBJ File\n");
		sb_obj.append("mtllib ").append(mtl_filename).append('\n');

		final Hashtable ht_mat = new Hashtable();

		int j = 1; // Vert indices in .obj files are global, not reset for every object.
				// starting at '1' because vert indices start at one.

		for (Iterator it = contents.iterator(); it.hasNext(); ) {
			Content mob = (Content)it.next();
			if (mob.getType() != Content.SURFACE)
				continue;
			final MeshGroup mg = (MeshGroup)mob.getContent();

			List triangles = mg.getMesh().getMesh();
			// make material, and see whether it exists already
			Mtl mat = new Mtl(1 - mob.getTransparency(),
						mg.getMesh().getColor());
			Object mat2 = ht_mat.get(mat);
			if (null != mat2) mat = (Mtl)mat2; // recycling
			else ht_mat.put(mat, mat); // !@#$% Can't get the object in a HashSet easily
			// make list of vertices
			String title = mob.getName().replaceAll(" ", "_").replaceAll("#", "--");
			Hashtable ht_points = new Hashtable(); // because we like inefficiency
			sb_obj.append("o ").append(title).append('\n');
			final int len = triangles.size();
			int[] index = new int[len];
			if (0 != len % 3) System.out.println("WARNING: list of triangles not multiple of 3");
			int k = 0; // iterate over index array, to make faces later
			// j is tag for each new vert, which start at 1 (for some ridiculous reason)
			for (Iterator tt = triangles.iterator(); tt.hasNext(); ) {
				Point3f p = (Point3f)tt.next();
				// check if point already exists
				Object ob = ht_points.get(p);
				if (null != ob) {
					index[k] = ((Integer)ob).intValue();
				} else {
					// new point
					index[k] = j;
					// record
					ht_points.put(p, new Integer(j));
					// append vertex
					sb_obj.append('v').append(' ').append(p.x)
						      .append(' ').append(p.y)
						      .append(' ').append(p.z).append('\n');
					j++;
				}
				k++;
			}
			sb_obj.append("usemtl ").append(mat.name).append('\n');
			sb_obj.append("s 1\n");
			// print faces
			int len_p = ht_points.size();
			for (int i=0; i<len; i+=3) {
				sb_obj.append('f').append(' ').append(index[i])
					      .append(' ').append(index[i+1])
					      .append(' ').append(index[i+2]).append('\n');
				//if (index[i] > len_p) Utils.log2("WARNING: face vert index beyond range"); // range is from 1 to len_p inclusive
				//if (index[i+1] > len_p) Utils.log2("WARNING: face vert index beyond range");
				//if (index[i+2] > len_p) Utils.log2("WARNING: face vert index beyond range");
				//System.out.println("j: " + index[i]);
				// checks passed
			}
			sb_obj.append('\n');
		}
		// make mtl file
		StringBuffer sb_mtl = new StringBuffer("# MTL File\n");
		for (Iterator it = ht_mat.keySet().iterator(); it.hasNext(); ) {
			Mtl mat = (Mtl)it.next();
			mat.fill(sb_mtl);
		}

		return new String[]{sb_obj.toString(), sb_mtl.toString()};
	}

	/** A Material, but avoiding name colisions. Not thread-safe. */
	static private int mat_index = 1;
	static private class Mtl {
		float alpha = 1;
		float R = 1;
		float G = 1;
		float B = 1;
		String name;
		Mtl(float alpha, Color3f c) {
			this.alpha = alpha;
			float[] f = new float[3];
			c.get(f);
			this.R = f[0];
			this.G = f[1];
			this.B = f[2];
			name = "mat_" + mat_index;
			mat_index++;
		}
		Mtl(float alpha, float R, float G, float B) {
			this.alpha = alpha;
			this.R = R;
			this.G = G;
			this.B = B;
			name = "mat_" + mat_index;
			mat_index++;
		}
		public boolean equals(Object ob) {
			if (ob instanceof MeshExporter.Mtl) {
				Mtl mat = (Mtl)ob;
				if (mat.alpha == alpha
				 && mat.R == R
				 && mat.G == G
				 && mat.B == B) {
					return true;
				 }
			}
			return false;
		}
		void fill(StringBuffer sb) {
			sb.append("\nnewmtl ").append(name).append('\n')
			  .append("Ns 96.078431\n")
			  .append("Ka 0.0 0.0 0.0\n")
			  .append("Kd ").append(R).append(' ').append(G).append(' ').append(B).append('\n') // this is INCORRECT but I'll figure out the conversion later
			  .append("Ks 0.5 0.5 0.5\n")
			  .append("Ni 1.0\n")
			  .append("d ").append(alpha).append('\n')
			  .append("illum 2\n\n");
		}
		/** For DXF color */
		int getAsSingle() {
			return (int)((R + G + B / 3) * 255);
		}
	}

	/** Utility method to encode text data in 8859_1. */
	static public boolean saveToFile(final File f, final String data) {
		if (null == f) return false;
		try {
			OutputStreamWriter dos = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f), data.length()), "8859_1"); // encoding in Latin 1 (for macosx not to mess around
			dos.write(data, 0, data.length());
			dos.flush();
		} catch (Exception e) {
			e.printStackTrace();
			IJ.showMessage("ERROR: Most likely did NOT save your file.");
			return false;
		}
		return true;
	}

}
