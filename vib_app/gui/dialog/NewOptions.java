package vib_app.gui.dialog;

import vib_app.Options;
import vib_app.FileGroup;

import java.io.File;

import java.awt.Label;
import java.awt.Dialog;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.TextField;
import java.awt.TextArea;
import java.awt.Insets;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Choice;
import java.awt.Font;
import java.awt.FileDialog;
import java.awt.Panel;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

public class NewOptions extends Dialog implements ActionListener {
	
	private Button workingDirButton;
	private Button fileGroupButton;
	private Button okButton, cancelButton;
	private Choice templateChoice;
	private TextField workingDirTF;
	
	private Options origOptions;
	private Options copyOptions;

	private final FileDialog fd = new FileDialog(this);
	
	public NewOptions(Frame parent, Options project) {
		super(parent);
		setModal(true);
		setTitle("New project");
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		this.origOptions = project;
		this.copyOptions = project.clone();
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);
	
		
		Label l = new Label("Create a new project");
		l.setFont(new Font("Monospace", Font.BOLD, 16));
		l.setForeground(Color.ORANGE);
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5,5,5,5);
		gridbag.setConstraints(l, c);
		this.add(l);
	
		TextArea ta = new TextArea("Set the working directory. \nAll " +
									" produced output will be stored there:",
									2,50, TextArea.SCROLLBARS_NONE);
		ta.setEnabled(false);
		ta.setBackground(Color.LIGHT_GRAY);
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		gridbag.setConstraints(ta, c);
		this.add(ta);
		
		workingDirTF = new TextField(20);
		File wd = project.getWorkingDirectory();
		String text = wd == null ? "" : wd.getAbsolutePath();
		workingDirTF.setText(text);
		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(workingDirTF, c);
		this.add(workingDirTF);

		workingDirButton = new Button("...");
		workingDirButton.addActionListener(this);
		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(workingDirButton, c);
		this.add(workingDirButton);

				
		ta = new TextArea("Edit the file group. The file group contains \n" + 
							"all files which will be included in the " + 
							"calculations",
							2,50, TextArea.SCROLLBARS_NONE);
		ta.setEnabled(false);
		ta.setBackground(Color.LIGHT_GRAY);
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(ta, c);
		this.add(ta);

		l = new Label(copyOptions.getFileGroup().getName());
		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(l, c);
		this.add(l);

		fileGroupButton = new Button("Edit");
		fileGroupButton.addActionListener(this);
		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(fileGroupButton, c);
		this.add(fileGroupButton);
			
		ta = new TextArea("Select a template. The templatemust be \n" + 
				"one of the files contained in the file group",
							2,50, TextArea.SCROLLBARS_NONE);
		ta.setEnabled(false);
		ta.setBackground(Color.LIGHT_GRAY);
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(ta, c);
		this.add(ta);
		
		l = new Label("Template");
		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(l, c);
		this.add(l);

		templateChoice = new Choice();
		templateChoice.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String templatename = templateChoice.getSelectedItem();
				File template = copyOptions.getFileGroup().
									getFileForName(templatename);
				if(template != null) {
					copyOptions.setTemplate(template);
				}
			}
		});
		updateTemplateChoice();
		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(templateChoice, c);
		this.add(templateChoice);

		Panel buttons = new Panel(new FlowLayout());
		okButton = new Button("OK");
		okButton.addActionListener(this);
		buttons.add(okButton);
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener(this);
		buttons.add(cancelButton);
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(buttons, c);
		this.add(buttons);
		
		pack();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == okButton) {
			origOptions.copy(copyOptions);
			this.setModal(false);
			this.dispose();
		} else if( e.getSource() == cancelButton) {
			this.dispose();
		} else if(e.getSource() == fileGroupButton) {
			FileGroupDialog fgd = new FileGroupDialog(
								this, copyOptions.getFileGroup());
			updateTemplateChoice();
		} else if (e.getSource() == workingDirButton) {
			fd.setVisible(true);
			String file = fd.getDirectory();
			if(file != null){
				File f = new File(file);
				if(f.exists() && f.isDirectory()) {
					copyOptions.setWorkingDirectory(f);
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
		FileGroup fg = copyOptions.getFileGroup();
		if(fg.size() == 0) {
			templateChoice.add("<filegroup empty>");
		} else {
			for(int i = 0; i < fg.size(); i++) {
				templateChoice.add(fg.get(i).getName());
			}
			if(copyOptions.getTemplate() != null) {
				templateChoice.select(copyOptions.getTemplate().getName());
				copyOptions.setTemplate(fg.getFileForName(
						templateChoice.getSelectedItem()));
			}
		}
	}

	public static void main(String[] args) {

		Options project = new Options("/home/bene/tmp/");
		FileGroup fg = new FileGroup("myfilegroup");
		fg.add("/home/bene/tmp.java");
		fg.add("/home/bene/tmp.class");
		project.setFileGroup(fg);
		project.setNumChannels(2);
		project.setRefChannel(2);
		project.setTemplate("/home/bene/tmp.java");
		
		new NewOptions(new Frame(), project);

		project.debug();
	}
}
