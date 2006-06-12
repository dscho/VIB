package vib.segment;

import java.awt.Polygon;

public class Labelizer {

	ContainerPanel.MainPanel iMainPanel;
	Segmentation_Editor.CustomStackWindow csw;
	
	public Labelizer(ContainerPanel.MainPanel pan) {
/*
		iMainPanel = pan;
		csw = iMainPanel.win;
		csw.setLabelizer(this);
*/
	}
	
	public void addToMaterial() {
/*
		int materialIndex = iMainPanel.data.getSelectedIndex();
		Polygon poly;
		poly = csw.getImagePlus().getRoi().getPolygon();
//		csw.getImagePlus().getProcessor().fillPolygon(poly);
		csw.segmentedImage.getProcessor(csw.oldSlice).fill();
*/
	}

}
