package vib_app.gui.dialog;

import java.awt.event.*;
import java.awt.*;

import vib_app.App;

import vib_app.gui.VIBTextArea;
import vib_app.module.MessageReceiver;

public class PreprocessingDialog extends Panel 
						implements ActionListener, MessageReceiver {

	private VIBTextArea messageTA;
	private Button cancelButton;

	public PreprocessingDialog(String message) {
		super();
		messageTA = new VIBTextArea(message, 0);
		messageTA.setFont(new Font("Monospace", Font.BOLD, 18));
		messageTA.setForeground(Color.RED);
		messageTA.setBackground(Color.ORANGE);
		messageTA.setMarginX(20);
		messageTA.setMarginY(100);
		this.setBackground(Color.ORANGE);
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);
		
		c.gridx = c.gridy = 0;
		c.weightx = 0.5; 
		c.weighty = 0.5;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(messageTA, c);
		this.add(messageTA);
	}

	public void setMessage(String message) {
		messageTA.setText(message);
	}

	public void setState(boolean busy) {
		Frame f = App.instance().getFrame();
		int c = busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR;
		f.setCursor(c);
	}

	public void actionPerformed(ActionEvent e) {
	}
}

