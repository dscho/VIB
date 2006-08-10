package vib;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.SaveDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @date 07.08.2006
 * 
 * @author Benjamin Schmid
 */
class PointList implements Iterable<NamedPoint>{
	
	private List<NamedPoint> points;
	
	public PointList(){
		points = new ArrayList<NamedPoint>();
	}
		
	public void add(NamedPoint point){
		points.add(point);
	}
	
	public void remove(NamedPoint point){
		points.remove(point);
	}
	
	public void rename(NamedPoint point, String name){
		point.name = name;
	}
	
	public NamedPoint get(int index){
		return points.get(index);
	}
	
	public NamedPoint[] toArray(){
		return points.toArray(new NamedPoint[]{});
	}
	
	public int size(){
		return points.size();
	}
	
	public NamedPoint get(String name){
		for(NamedPoint p : points){
			if(p.name.equals(name)){
				return p;
			}
		}
		return null;
	}
	
	public Iterator<NamedPoint> iterator() {
		return points.iterator();
	}
	
	public static PointList load(ImagePlus imp){
		FileInfo info = imp.getOriginalFileInfo();
		if(info != null){
			PointList l =  load(info.directory,info.fileName + ".points",false);
			if(l == null){
				l = load(info.directory,info.fileName + ".points",true);
			}
			return l;
		}
		return null;
	}
	
	public static PointList load(String dir, String file, boolean showDialog){

		String openPath = dir + File.separatorChar + file;
		if(showDialog){
			OpenDialog od = new OpenDialog("Open points annotation file",
					dir,file);
			
			
			if(od.getFileName()==null)
				return null;
			else {
				openPath = od.getDirectory()+od.getFileName();
			}
		}
				
		PointList list = new PointList();	
		try {
			BufferedReader f = new BufferedReader(
							new FileReader(openPath));
			String line;
			while ((line=f.readLine())!=null) {
				NamedPoint p = NamedPoint.fromLine(line);
				if(p != null)
					list.add(p);
			}
			return list;
		} catch (FileNotFoundException e) {
			IJ.showMessage("Could not find file " + openPath);
		} catch (IOException e) {
			IJ.showMessage("Could not read file " + openPath);
		}
		return null;
	}
	
	public void save(String directory, String fileName ) {

		String suggestedSaveFilename = fileName+".points";

		SaveDialog sd = new SaveDialog("Save points annotation file as...",
					       directory,
					       suggestedSaveFilename,
					       ".points");

		if(sd.getFileName()==null)
			return;
		
		String savePath = sd.getDirectory()+sd.getFileName();
		File file = new File(savePath);
		if ((file != null) && file.exists()) {
			if (!IJ.showMessageWithCancel(
				    "Save points annotation file", "The file "+
				    savePath+" already exists.\n"+
				    "Do you want to replace it?"))
				return;
		}
		IJ.showStatus("Saving point annotations to "+savePath);

		try {
			PrintStream fos = new PrintStream(savePath);
			for(NamedPoint p : points){
				if(p.set) {
					fos.println(p.toYAML() + "\n");
				}
			}
			fos.close();
		} catch( IOException e ) {
			IJ.error("Error saving to: "+savePath+"\n"+e);
		}				
		IJ.showStatus("Saved point annotations.");
	}
	
	public static ArrayList<String> pointsInBothAsString(PointList points0,
		     PointList points1) {

		ArrayList<String> common = new ArrayList<String>();
		for(NamedPoint point0 : points0){
			for(NamedPoint point1 : points1){
				if(point0.name.equals(point1.name)){
					common.add(point0.name);
					break;
				}
			}
		}
		return common;
	}
	
	public static PointList pointsInBoth(PointList points0, PointList points1){
		
		PointList common = new PointList();
		for(NamedPoint point0 : points0){
			for(NamedPoint point1 : points1){
				if(point0.name.equals(point1.name)){
					common.add(point0);
					break;
				}
			}
		}
		return common;
	}
	
	public void print(){
		for(NamedPoint p : points){
			System.out.println(p.toString());
		}
	}
}