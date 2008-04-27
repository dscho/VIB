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
public class PointList implements Iterable<BenesNamedPoint>{
	
	private List<BenesNamedPoint> points;
	private List<PointListListener> listeners;
	
	public PointList(){
		listeners = new ArrayList<PointListListener>();
		points = new ArrayList<BenesNamedPoint>();
	}
		
	public void add(BenesNamedPoint point){
		points.add(point);
		fireAdded(points.size() - 1);
	}
	
	public void remove(BenesNamedPoint point){
		for(int i = 0; i < points.size(); i++) {
			if(point == points.get(i)) {
				remove(i);
			}
		}
	}

	public void remove(int i) {
		if(i >= 0 && i < size()) {
			points.remove(i);
			fireRemoved(i);
		}
	}
	
	public void rename(BenesNamedPoint point, String name){
		point.name = name;
		fireRenamed(points.indexOf(point));
	}

	public void up(BenesNamedPoint point) {
		int size = points.size();
		int i = points.indexOf(point);
		points.remove(i);
		points.add((i - 1 + size) % size, point);
		fireReordered();
	}

	public void down(BenesNamedPoint point) {
		int i = points.indexOf(point);
		int size = points.size();
		points.remove(i);
		points.add((i + 1) % size, point);
		fireReordered();
	}

	public void highlight(int i) {
		fireHighlighted(i);
	}

	public void highlight(BenesNamedPoint p) {
		fireHighlighted(points.indexOf(p));
	}

	public void placePoint(BenesNamedPoint point, 
				double x, double y, double z) {
		point.x = x;
		point.y = y;
		point.z = z;
		fireMoved(points.indexOf(point));
	}
	
	public BenesNamedPoint get(int index){
		return points.get(index);
	}
	
	public BenesNamedPoint[] toArray(){
		return points.toArray(new BenesNamedPoint[]{});
	}
	
	public int size(){
		return points.size();
	}
	
	public BenesNamedPoint get(String name){
		for(BenesNamedPoint p : points){
			if(p.name.equals(name)){
				return p;
			}
		}
		return null;
	}
	
	public Iterator<BenesNamedPoint> iterator() {
		return points.iterator();
	}
	
	public static PointList load(ImagePlus imp){
		FileInfo info = imp.getOriginalFileInfo();
		if(info != null){
			PointList l =  load(info.directory,
					info.fileName + ".points",false);
			if(l == null){
				l = load(info.directory,
					info.fileName + ".points",true);
			}
			return l;
		}
		return null;
	}
	
	public static PointList load(String dir, String file, 
						boolean showDialog){

		String openPath = dir + File.separatorChar + file;
		if(showDialog) {
			OpenDialog od = new OpenDialog(
				"Open points annotation file", dir,file);
			
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
				BenesNamedPoint p = BenesNamedPoint.
							fromLine(line);
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
		SaveDialog sd = new SaveDialog(
					"Save points annotation file as...",
				       directory,
				       suggestedSaveFilename,
				       ".points");

		if(sd.getFileName() == null)
			return;
		
		String savePath = sd.getDirectory() + sd.getFileName();
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
			for(BenesNamedPoint p : points)
				if(p.set)
					fos.println(p.toYAML() + "\n");
			fos.close();
		} catch( IOException e ) {
			IJ.error("Error saving to: "+savePath+"\n"+e);
		}				
		IJ.showStatus("Saved point annotations.");
	}
	
	public static ArrayList<String> pointsInBothAsString(PointList points0,
		     PointList points1) {

		ArrayList<String> common = new ArrayList<String>();
		for(BenesNamedPoint point0 : points0){
			for(BenesNamedPoint point1 : points1){
				if(point0.name.equals(point1.name)){
					common.add(point0.name);
					break;
				}
			}
		}
		return common;
	}
	
	public static PointList pointsInBoth(
				PointList points0, PointList points1){
		
		PointList common = new PointList();
		for(BenesNamedPoint point0 : points0){
			for(BenesNamedPoint point1 : points1){
				if(point0.name.equals(point1.name)){
					common.add(point0);
					break;
				}
			}
		}
		return common;
	}
	
	public void print(){
		for(BenesNamedPoint p : points){
			System.out.println(p.toString());
		}
	}

	// listener stuff
	
	public void addPointListListener(PointListListener pll) {
		listeners.add(pll);
	}

	public void removePointListListener(PointListListener pll) {
		listeners.remove(pll);
	}

	private void fireAdded(int i) {
		for(PointListListener l : listeners)
			l.added(i);
	}

	private void fireRemoved(int i) {
		for(PointListListener l : listeners)
			l.removed(i);
	}

	private void fireRenamed(int i) {
		for(PointListListener l : listeners)
			l.renamed(i);
	}

	private void fireMoved(int i) {
		for(PointListListener l : listeners)
			l.moved(i);
	}

	private void fireHighlighted(int i) {
		for(PointListListener l : listeners)
			l.highlighted(i);
	}

	private void fireReordered() {
		for(PointListListener l : listeners)
			l.reordered();
	}


	public interface PointListListener {
		public void added(int i);
		public void removed(int i);
		public void renamed(int i);
		public void moved(int i);
		public void highlighted(int i);
		public void reordered();
	}
}
