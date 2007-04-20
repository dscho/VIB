package ij3d;

import ij.gui.GenericDialog;
import ij.IJ;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;

import voltex.VoltexGroup;
import isosurface.MeshGroup;
import javax.vecmath.Color3f;
import javax.media.j3d.View;

public class Image3DMenubar extends MenuBar implements ActionListener, 
					 		ItemListener {

	private Image3DUniverse univ;

	private MenuItem mesh;
	private MenuItem voltex;
	private MenuItem color;
	private MenuItem fill;
	private MenuItem delete;
	private MenuItem startRecord;
	private MenuItem stopRecord;
	private CheckboxMenuItem perspective;

	public Image3DMenubar(Image3DUniverse universe) {
		super();
		this.univ = universe;

		Menu menu = new Menu("3D Viewer");
		this.add(menu);

		voltex = new MenuItem("Add volume");
		voltex.addActionListener(this);
		menu.add(voltex);

		mesh = new MenuItem("Add mesh");
		mesh.addActionListener(this);
		menu.add(mesh);

		delete = new MenuItem("Delete");
		delete.addActionListener(this);
		menu.add(delete);

		menu.addSeparator();

		fill = new MenuItem("Fill selection");
		fill.addActionListener(this);
		menu.add(fill);
		
		color = new MenuItem("Change color");
		color.addActionListener(this);
		menu.add(color);

		menu.addSeparator();

		startRecord = new MenuItem("Start recording");
		startRecord.addActionListener(this);
		menu.add(startRecord);

		stopRecord = new MenuItem("Stop recording");
		stopRecord.addActionListener(this);
		menu.add(stopRecord);

		menu.addSeparator();

		perspective = new CheckboxMenuItem(
					"Perspective Projection", true);
		perspective.addItemListener(this);
		menu.add(perspective);

/*
		univ.getCanvas().add(this);
   		
		univ.getCanvas().addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				showPopup(e);	
			}
			public void mouseReleased(MouseEvent e){
				showPopup(e);	
			}
			private void showPopup(MouseEvent e) {
				if(e.isPopupTrigger()){
					p = e.getPoint();
					show(univ.getCanvas(), 
							e.getX(),e.getY());
				}
			}
		});*/
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == color) {
			Content selected = univ.getSelected();
			if(selected == null) {
				IJ.error("Selection required");
				return;
			}	
			changeColor(selected);
			univ.clearSelection();
		} 

		if(e.getSource() == voltex) {
			VoltexGroup.addContent(univ, null);
		}
		
		if(e.getSource() == mesh) {
			MeshGroup.addContent(univ, null);
		}

		if(e.getSource() == delete) {
			Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			univ.removeContent(c.name);
		}
		
		if(e.getSource() == startRecord) {
			univ.recording = true;
		}

		if(e.getSource() == stopRecord) {
			univ.stopRecording().show();
		}

		if(e.getSource() == fill) {
			final Content c = univ.getSelected();
			if(c == null) {
				IJ.error("Selection required");
				return;
			}
			if(c instanceof VoltexGroup) {
				new Thread(new Runnable() {
					public void run() {
						((VoltexGroup)c).
							fillRoiBlack(univ, (byte)0);
					}
				}).start();
			}
		}

	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == perspective) {
			int policy = perspective.getState() 
						? View.PERSPECTIVE_PROJECTION 
						: View.PARALLEL_PROJECTION;
			univ.getViewer().getView().setProjectionPolicy(policy);
		}
	}
		
	public void changeColor(Content selected) {
		GenericDialog gd = new GenericDialog("Adjust color ...");
		gd.addChoice("Color", ColorTable.colorNames, 
							ColorTable.colorNames[0]);
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, new String[] {"red", "green", "blue"}, 
						new boolean[]{true, true, true});
		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		String color = gd.getNextChoice();
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
								gd.getNextBoolean(), 
								gd.getNextBoolean()};
		selected.setColor(color, channels);
	}
}

