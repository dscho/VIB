package vib_app.gui.dialog;

import vib_app.Options;
import vib_app.FileGroup;
import vib_app.App;

import java.awt.event.*;
import java.awt.*;

import java.io.File;

public class FilesPanel extends Panel implements ActionListener {

	private Options options;

	private TextField workingDirTF;
	private Button workingDirButton;
	private Button fileGroupButton;
	private Choice templateChoice;
	
	private FileDialog fd;

	public FilesPanel(Options op) {

		this.options = op;
		this.fd = new FileDialog(new Frame());

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5,5,5,5);

		TextArea ta = new TextArea("All produced output will be stored in " + 
				"working directory. Specify an empty folder there. The file " + 
				"group contains all this which will be included in the " + 
				"registration calculations, the template file is the image " + 
				"stack the other this are registered to.", 
				5, 50, TextArea.SCROLLBARS_NONE);
		ta.setEnabled(false);
		ta.setBackground(new Color(255,255,100));
		c.gridy++;
		c.weightx = 0.5;
		gridbag.setConstraints(ta, c);
		this.add(ta);
		
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		
		Panel panel = new Panel(new FlowLayout());		
		Label l = new Label("Working directory");
		panel.add(l);
		workingDirTF = new TextField(20);
		workingDirTF.setBackground(new Color(255,255,100));
		panel.add(workingDirTF);
		workingDirButton = new Button("...");
		workingDirButton.addActionListener(this);
		panel.add(workingDirButton);
		c.gridy++;
		gridbag.setConstraints(panel, c);
		this.add(panel);

		panel = new Panel(new FlowLayout());
		l = new Label(options.getFileGroup().getName());
		panel.add(l);
		fileGroupButton = new Button("Edit");
		fileGroupButton.addActionListener(this);
		panel.add(fileGroupButton);
		c.gridy++;
		gridbag.setConstraints(panel, c);
		this.add(panel);
			
		panel = new Panel(new FlowLayout());
		l = new Label("Template");
		panel.add(l);
		templateChoice = new Choice();
		templateChoice.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String templatename = templateChoice.getSelectedItem();
				File template = options.getFileGroup().
									getFileForName(templatename);
				if(template != null) {
					options.setTemplate(template);
				}
			}
		});
		panel.add(templateChoice);
		c.gridy++;
		gridbag.setConstraints(panel, c);
		this.add(panel);

		// just to fill the rest of the space, otherwise all 
		// components are centered, which looks ugly
		panel = new Panel();
		c.gridy++;
		c.weighty = 0.5;
		gridbag.setConstraints(panel, c);
		this.add(panel);

		this.fillForms();
	}

	public void fillForms() {
		File wd = options.getWorkingDirectory();
		String text = wd == null ? "" : wd.getAbsolutePath();
		workingDirTF.setText(text);
		updateTemplateChoice();
	}

	public void setOptions(Options options) {
		this.options = options;
		fillForms();
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == fileGroupButton) {
			FileGroupDialog fgd = new FileGroupDialog(
						App.instance().getFrame(), options.getFileGroup());
			updateTemplateChoice();
		} else if (e.getSource() == workingDirButton) {
			fd.setVisible(true);
			String file = fd.getDirectory();
			if(file != null){
				File f = new File(file);
				if(f.exists() && f.isDirectory()) {
					options.setWorkingDirectory(f);
					workingDirTF.setText(file);
				} else {
					System.out.println(f + " either doesn't exist or is not " + 
							"a directory");
				}
			}
		}
	}

	private void updateTemplateChoice() {
		templateChoice.removeAll();
		FileGroup fg = options.getFileGroup();
		if(fg.size() == 0) {
			templateChoice.add("<filegroup empty>");
		} else {
			for(int i = 0; i < fg.size(); i++) {
				templateChoice.add(fg.get(i).getName());
			}
			if(options.getTemplate() != null) {
				templateChoice.select(options.getTemplate().getName());
				options.setTemplate(fg.getFileForName(
						templateChoice.getSelectedItem()));
			}
		}
	}
}
