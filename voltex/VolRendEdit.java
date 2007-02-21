package voltex;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.picking.*;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.util.Enumeration;
import com.sun.j3d.utils.behaviors.mouse.*;
import java.net.*;
import java.lang.String.*;
import ij.ImagePlus;
import com.sun.j3d.utils.geometry.*;

public class VolRendEdit extends Frame {

    VolRend	volRend;
    Canvas3D canvas;


    public VolRendEdit(ImagePlus imp) {
		super("Java3D Volume Rendering Editor");
		volRend = new VolRend();
		volRend.initContext(imp); // initializes the renderers
	
		// Setup the frame
		setLayout(new BorderLayout());

		canvas = volRend.getCanvas();

		canvas.setSize(600, 600);
		add(canvas, BorderLayout.CENTER);
		
		volRend.update();
		pack();
		show();
    }
}
