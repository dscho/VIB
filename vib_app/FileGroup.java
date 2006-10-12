package vib_app;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class FileGroup {
	
	private List<File> files = new ArrayList<File> ();
	private String name;

	public FileGroup(){}

	public FileGroup(String name) {
		this.name = name;
	}

	public boolean add(File f) {
		if(!f.exists())
			return false;
		return files.add(f);
	}

	public boolean add(String path) {
		File f = new File(path);
		return add(f);
	}

	public boolean remove(File f) {
		return files.remove(f);
	}

	public File remove(int index) {
		return files.remove(index);
	}

	public int size() {
		return files.size();
	}

	public void copy(FileGroup fg) {
		this.name = fg.name;
		this.files.clear();
		this.files.addAll(fg.files);
	}

	public String toString() {
		return name;
	}

	public File get(int index) {
		return files.get(index);
	}

	public File getFileForName(String name) {
		for(int i = 0; i < files.size(); i++) {
			if(files.get(i).getName().equals(name)){
				return files.get(i);
			}
		}
		return null;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public void debug() {
		System.out.println(name + ": ");
		System.out.println(files);
	}

	public FileGroup clone() {
		FileGroup clone = new FileGroup(this.name);
		clone.files = new ArrayList<File> (this.files);
		return clone;
	}
}
