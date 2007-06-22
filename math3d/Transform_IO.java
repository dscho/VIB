package math3d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedHashMap;
import ij.IJ;
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
	
	LinkedHashMap tags;
	LinkedHashMap fields;
	
	String getTags() {return tags.toString();}
	//void setTags(String tags) { this.tags=tags;}
	boolean appendTag(String tag) {
		//tag=tag.trim();
		int sepPos=tag.indexOf(":=");
		if(sepPos<1 || tag.length()<3) return false;
		String tagKey=tag.substring(0, sepPos);		
		String tagValue=tag.substring(sepPos+2,tag.length());
		tags.put(tagKey,tagValue);
		return true;
	}
	
	String getFields() {return fields.toString();}
	
	boolean appendField(String fieldspec){
		// Separate the field spec
		int sepIndex=fieldspec.indexOf(": ");
		String fieldName=standardFieldName(fieldspec.substring(0, sepIndex));
		String allFieldVals=fieldspec.substring(sepIndex+2,fieldspec.length());
		String[] fieldVals;

		if(fieldName.equals("content")){
			// this field contains a string that should not be split
			fieldVals=new String[1];
			fieldVals[0]=allFieldVals;
			return true;
		}
		allFieldVals=allFieldVals.trim();
		// Now split field vals
		if(allFieldVals.startsWith("\"") && allFieldVals.endsWith("\"")){
			// Remove the first and last quote
			allFieldVals=allFieldVals.substring(1, allFieldVals.length()-1);
			fieldVals=allFieldVals.split("\"\\s+\"");
			if(fieldVals.length<1) return false;
		} else fieldVals=allFieldVals.split("\\s+");
		fields.put(fieldName, fieldVals);
		return true;
	}
	
	String standardFieldName(String fieldName){
		fieldName=fieldName.toLowerCase();
		if(fieldName.equals("centerings")) return "centers";		
		if(fieldName.equals("axismaxs")) return "axis maxs";
		if(fieldName.equals("axismins")) return "axis mins";
		if(fieldName.equals("lineskip")) return "line skip";
		if(fieldName.equals("byteskip")) return "byte skip";
		if(fieldName.equals("datafile")) return "data file";
		if(fieldName.equals("oldmax")) return "old max";
		if(fieldName.equals("oldmin")) return "old min";
		return fieldName;
	}
	
	float[] openAffineTransform(String path) {
		float[] mat = new float[matSize];
		tags=new LinkedHashMap();
		fields=new LinkedHashMap();
		try {
			File f=new File (path);
			LineNumberReader in=new LineNumberReader(new FileReader(f));
			
			String s;
			int nLines=0;
			while((s = in.readLine()) != null && nLines<matRows){
				if(IJ.debugMode) IJ.log("Processing line: "+in.getLineNumber());
				if(s.startsWith("#")) continue;
				if(s.indexOf(":=")>-1) {
					appendTag(s);
					continue;
				}
				if(s.indexOf(": ")>-1){
					appendField(s);
					continue;
				}
				if(s.startsWith("NRRD")) continue;
				
				// Otherwise process as data
				s=s.trim();
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
		} catch (Exception e) {
			IJ.error("Unable to write transformation to file: "+f.getAbsolutePath());
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
	
	// Converts a string to a float. Returns NAN if the string does not contain a valid number. */
	float s2f(String s) {
		Float f = null;
		try {f = new Float(s);}
		catch (NumberFormatException e) {}
		return f!=null?f.floatValue():Float.NaN;
	}

	public void run(String arg) {
		// Only for testing
		float[] mat;
		if(arg.equals("")) mat=openAffineTransform();
		else mat=openAffineTransform(arg);
		IJ.log("fields:="+fields);
		IJ.log("tags:="+tags);
		IJ.log("mat = "+mat);
		//saveAffineTransform(mat);
	}
}
