package voltex;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.util.Enumeration;
import com.sun.j3d.utils.behaviors.mouse.*;
import java.net.*;
import java.lang.String.*;
import ij.ImageStack;


public class VolRendEdit extends JFrame {

    VolRend	volRend;
    Canvas3D canvas;


    public VolRendEdit(ImageStack stack) {
		super("Java3D Volume Rendering Editor");
		volRend = new VolRend();
		volRend.initContext(stack); // initializes the renderers
	
		// Setup the frame
		getContentPane().setLayout(new BorderLayout());

		canvas = volRend.getCanvas();
		canvas.setSize(600, 600);
		getContentPane().add(canvas, BorderLayout.CENTER);
		
		volRend.update();
		pack();
		show();
    }
}
