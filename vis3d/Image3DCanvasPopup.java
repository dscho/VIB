package vis3d;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;

import isosurface.MeshGroup;
import javax.vecmath.Color3f;

public class Image3DCanvasPopup extends PopupMenu 
						 implements ActionListener {

	private Image3DUniverse univ;

	private MenuItem mesh;
	private MenuItem paint;
	private MenuItem delete;
	private MenuItem startRecord;
	private MenuItem stopRecord;

	private Point p = null;

	public Image3DCanvasPopup(Image3DUniverse universe) {
		super();
		this.univ = universe;

		mesh = new MenuItem("Add mesh");
		mesh.addActionListener(this);
		this.add(mesh);

		delete = new MenuItem("Delete");
		delete.addActionListener(this);
		this.add(delete);
		
		paint = new MenuItem("Painting");
		paint.addActionListener(this);
		this.add(paint);

		this.addSeparator();

		startRecord = new MenuItem("Start recording");
		startRecord.addActionListener(this);
		this.add(startRecord);

		stopRecord = new MenuItem("Stop recording");
		stopRecord.addActionListener(this);
		this.add(stopRecord);

		this.addSeparator();

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
		});
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == paint) {
			System.out.println("painting");
		} 

		if(e.getSource() == mesh) {
			MeshGroup.addContent(univ, null);
		}

		if(e.getSource() == delete) {
			if(p != null) {
				univ.removeContentAt(p.x, p.y);
				p = null;
			}
		}
		
		if(e.getSource() == startRecord) {
			univ.recording = true;
		}

		if(e.getSource() == stopRecord) {
			univ.stopRecording().show();
		}
	}
}

