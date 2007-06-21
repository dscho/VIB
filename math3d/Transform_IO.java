package math3d;

import ij.IJ;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;

/**
 * 
 */

/**
 * This class provides simple IO for affine transforms.
 * These are available as float[16]
 * reading row-wise along the Affine Matrix
 * See Transform3D Java Doc for details
 * http://java.sun.com/products/java-media/3D/forDevelopers/J3D_1_3_API/j3dapi/javax/media/j3d/Transform3D.html
 * @author jefferis
 *
 */
public class Transform_IO implements PlugIn {
	
	// Determines whether the transformations that are 
	// supplied are normalised before being returned
	// Should probably happen elsewhere though
	public boolean normaliseScaleFactors=false;
	
	public static final int matRows=4;
	public static final int matCols=4;
	public static final int matSize=matRows*matCols;
	
	String tags="";
	
	String getTags() {return tags;}
	void setTags(String tags) { this.tags=tags;}
	boolean appendTag(String tag) {
		tag=tag.trim();
		if(tag.indexOf(":=")<1 || tag.length()<4) return false; 
		tags+=tag+"\n";
		return true;
	}
	
	float[] openAffineTransform(String path) {
		float[] mat = new float[matSize];
		setTags("");
		try {
			File f=new File (path);
			BufferedReader in=new BufferedReader(new FileReader(f));
			
			String s;
			int nLines=0;
			while((s = in.readLine()) != null && nLines<matRows){
				s=s.trim();
				if(s.startsWith("#")) continue;
				if(s.length()==0) continue;
				if(s.indexOf(":=")>-1) appendTag(s);
				if(s.indexOf(":")>-1) continue;
				
				String[] floatStrings = s.split("\\s+", matCols);
				if(floatStrings.length!=4) throw new 
					Exception("Could not read 4 floats from line "+nLines+" of file "+path);
				for(int i=0;i<floatStrings.length;i++){
					mat[nLines*matCols+i]=s2f(floatStrings[i]);
					if(IJ.debugMode) IJ.log("nLines = "+nLines+" i = "+i+" val = "+floatStrings[i]);
				}
				nLines++;
			}
		} catch (Exception e){
			IJ.log("Exception: "+e);
			return null;
		}
		return mat;
	}
	
	public float[] openAffineTransform(){
		OpenDialog od = new OpenDialog("Open Affine Transformation...", "");
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		return openAffineTransform((new File(directory,fileName)).getPath());
	}
	
	public boolean saveAffineTransform(String path, float[] mat){
		File f = new File(path);
		//if (!f.canWrite()) return false;
		if (mat.length!=matSize) return false;
		
		//SimpleDateFormat regDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
		
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			out.write("# Simple Affine Transformation written by Transform_IO\n");
			out.write("# at "+(new Date())+"\n");
			out.write(toString(mat));
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String toString(float[]  mat){
		StringBuffer sb=new StringBuffer();
		sb.append(getTags());
		for(int i=0;i<matRows;i++){
			sb.append(mat[i*matCols]+" "+mat[i*matCols+1]+" "+mat[i*matCols+2]+" "+mat[i*matCols+3]+"\n");
		}
		return sb.toString();
	}
	
	public boolean saveAffineTransform(float[] mat){
		SaveDialog sd = new SaveDialog("Save Affine Transformation ...", "", ".mat");
		String file = sd.getFileName();
		if (file == null) return false;
		String directory = sd.getDirectory();
		return saveAffineTransform((new File(directory,file)).getPath(), mat);
	}
	
	// Converts a string to a double. Returns NAN if the string does not contain a valid number. */
	float s2f(String s) {
		Float f = null;
		try {f = new Float(s);}
		catch (NumberFormatException e) {}
		return f!=null?f.floatValue():Float.NaN;
	}

	public void run(String arg) {
		// For testing really
		float[] mat;
		if(arg.equals("")) mat=openAffineTransform();
		else mat=openAffineTransform(arg);
		saveAffineTransform(mat);
	}

	
}
