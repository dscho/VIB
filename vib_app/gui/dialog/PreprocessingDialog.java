package vib_app.gui.dialog;

import java.awt.event.*;
import java.awt.*;

import vib_app.gui.VIBTextArea;
import vib_app.module.MessageReceiver;

public class PreprocessingDialog extends Panel 
						implements ActionListener, MessageReceiver {

	private VIBTextArea messageTA;
	private Button cancelButton;

	public PreprocessingDialog(String message) {
		super();
		messageTA = new VIBTextArea(message);
		messageTA.setFont(new Font("Monospace", Font.BOLD, 18));
		messageTA.setForeground(Color.RED);
		messageTA.setBackground(Color.LIGHT_GRAY);
		this.setBackground(Color.BLUE);
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);
		
		c.gridx = c.gridy = 0;
		c.weightx = 0.5;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(messageTA, c);
		this.add(messageTA);
	}

	public void setMessage(String message) {
		StringBuffer buf = new StringBuffer(message);
		if(message.length() > 10) {
			buf.insert(10, "<br>");
		}
		messageTA.setText(buf.toString());
	}

	public void actionPerformed(ActionEvent e) {
	}
}

