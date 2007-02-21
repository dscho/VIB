package voltex;

import java.awt.event.*;
import java.awt.*;

import ij.IJ;

public class CanvasPopup extends PopupMenu 
						 implements ActionListener, ItemListener {

	private VolRend volrend;

	private MenuItem reset;
	private MenuItem fill;
	private CheckboxMenuItem coord_cb;

	public CanvasPopup(VolRend volr) {
		super();
		this.volrend = volr;

		fill = new MenuItem("Fill");
		fill.addActionListener(this);
		this.add(fill);

		coord_cb = new CheckboxMenuItem("Coordinate system", true);
		coord_cb.addItemListener(this);
		this.add(coord_cb);

		reset = new MenuItem("Reset view");
		reset.addActionListener(this);
		this.add(reset);

		volrend.getCanvas().add(this);
   		
		volrend.getCanvas().addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				showPopup(e);	
			}
			public void mouseReleased(MouseEvent e){
				showPopup(e);	
			}
			private void showPopup(MouseEvent e) {
				Polygon p = volrend.getCanvas().getPolygon();
				if(e.isPopupTrigger()
					/* && p != null && p.contains(e.getPoint())*/ ){
					show(volrend.getCanvas(), e.getX(),e.getY());
				}
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == fill) {
			int intensity = (int)IJ.getNumber("Intensity: [0..255]", 0);
			if(intensity == IJ.CANCELED) 
				return;
			intensity = intensity < 0 ? 0 : intensity;
			intensity = intensity > 255 ? 255 : intensity;
			final byte fillVal = (byte)intensity;
			new Thread(new Runnable() {
				public void run() {
					volrend.fillRoiBlack(fillVal);
					volrend.update();
				}
			}).start();
		} 

		if(e.getSource() == reset) {
			volrend.resetView();
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == coord_cb) {
			volrend.showCoordinateSystem(coord_cb.getState());
		}
	}
}

