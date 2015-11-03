/*
 * File: CyFrame.java
 * Google Summer of Code
 * Written by Steve Federowicz with help from Scooter Morris
 * 
 * This contains all of the swing code for the GUI which is essentially just a thumbnail viewer
 * accompanied by stop, play, pause, forwards, and backwards buttons.  It also contains a slider
 * which is used to adjust the speed of the animations.  Each thumbnail is created by putting an
 * ImageIcon onto a JButton.
 * 
 */

package edu.ucsf.rbvi.CyAnimator.internal.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.ucsf.rbvi.CyAnimator.internal.model.TimeBase;
import edu.ucsf.rbvi.CyAnimator.internal.model.VideoType;

public class RecordPanel extends JPanel {
	JComboBox<VideoType> choicesList;
	JComboBox<String> resolutionsList;
	JComboBox<TimeBase> frameCountList;
	JTextField directoryText;
	final JFileChooser fc = new JFileChooser();
	private String defaultPath;

	/**
	 * 
	 */
	public RecordPanel() {
		super();

		String[] resolutions = { "100", "200", "300", "400", "500"};

		defaultPath = System.getProperty("user.home");	// Set a reasonable default
		defaultPath += System.getProperty("file.separator")+"video."+VideoType.MP4.getExt();	// Set a reasonable default

		choicesList = new JComboBox<VideoType>(VideoType.supportedValues());
		resolutionsList = new JComboBox<>(resolutions);
		frameCountList = new JComboBox<>(TimeBase.values());
		choicesList.setSelectedItem(VideoType.MP4);
		resolutionsList.setSelectedIndex(0);
		frameCountList.setSelectedItem(TimeBase.THIRTY); // Set to 30 FPS by default

		choicesList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VideoType type = (VideoType)choicesList.getSelectedItem();
				// No frames/second in just images
				if (type == VideoType.FRAMES)
					frameCountList.setEnabled(false);
				else
					frameCountList.setEnabled(true);

				// Update filePath if the user hasn't changed it...
				if (!directoryText.getText().equals(defaultPath))
					return;

				// No, we're still the default
				defaultPath = System.getProperty("user.home");	// Set a reasonable default
				if (type.equals(VideoType.FRAMES))
					defaultPath += System.getProperty("file.separator")+"Frames";	// Set a reasonable default
				else
					defaultPath += System.getProperty("file.separator")+"video."+type.getExt();	// Set a reasonable default
				directoryText.setText(defaultPath);
			}
		});

		JButton browseButton = new JButton("Browse");
		browseButton.setSize(new Dimension(100, 30));
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				int returnVal = fc.showSaveDialog(new JPanel());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					directoryText.setText(file.getPath());
				}
			}
		});
		browseButton.setActionCommand("browse");
		browseButton.setToolTipText("Select output file (or directory)");

		JPanel directorySettingPanel = new JPanel();
		directorySettingPanel.setPreferredSize(new Dimension(600, 50));
		directorySettingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		directorySettingPanel.add(new JLabel("Video location: "));
		directoryText = new JTextField(30);
		directoryText.setText(defaultPath);
		directorySettingPanel.add(directoryText);
		directorySettingPanel.add(browseButton);

		JPanel outputSettingPanel = new JPanel();
		outputSettingPanel.setPreferredSize(new Dimension(600, 100));
		outputSettingPanel.setBorder(BorderFactory.createTitledBorder("Video Options"));
		outputSettingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		outputSettingPanel.add(new JLabel("Video Type: "));
		outputSettingPanel.add(choicesList);

		JPanel frameSettingPanel = new JPanel();
		frameSettingPanel.setPreferredSize(new Dimension(600, 100));
		frameSettingPanel.setBorder(BorderFactory.createTitledBorder("Frame Options"));
		frameSettingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		frameSettingPanel.add(new JLabel("Frame rate: "));
		frameSettingPanel.add(frameCountList);
		frameSettingPanel.add(new JLabel("Resolution: "));
		frameSettingPanel.add(resolutionsList);
		frameSettingPanel.add(new JLabel("%"));

		JPanel settingButtonPanel = new JPanel();
		settingButtonPanel.setPreferredSize(new Dimension(600, 50));
		settingButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		setPreferredSize(new Dimension(600, 200));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(directorySettingPanel);
		add(outputSettingPanel);
		add(frameSettingPanel);
		add(settingButtonPanel);
	}

	public VideoType getOutputType() {
		return (VideoType)choicesList.getSelectedItem();
	}

	public int getResolution() {
		return (resolutionsList.getSelectedIndex() + 1)*100;
	}

	public TimeBase getFrameCount() {
		return (TimeBase)frameCountList.getSelectedItem();
	}

	public String getFilePath() {
		return directoryText.getText();
	}

}
