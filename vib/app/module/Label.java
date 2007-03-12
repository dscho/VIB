package vib.app.module;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import vib.app.Options;

import vib.segment.CustomStackWindow;

import ij.ImagePlus;

public class Label extends Module {
	protected final String name = "Label";
	protected final String message = "Labelling";

	protected static void run(State state, int index) {
		SplitChannels.runOnOneImage(state, index);

		int refChannel = state.options.refChannel - 1;
		String imagePath = state.getImagePath(refChannel, index);
		String labelPath = state.getImagePath(-1, index);
		if (state.upToDate(imagePath, labelPath))
			return;

		ImagePlus image = state.getImage(imagePath);

		ActionListener a = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("Ok")) {
					synchronized (this) {
						notifyAll();
					}
				}
			}
		};

		
		CustomStackWindow csw = new CustomStackWindow(image);
		csw.addActionListener(a);
		synchronized (a) {
			try {
				a.wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		ImagePlus labels = csw.getLabels();
		csw.cleanUp();
		state.save(labels, labelPath);
	}
}
