/*
 * File: ProgressDialog.java
 * 
 * 
 */

package edu.ucsf.rbvi.CyAnimator.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import edu.ucsf.rbvi.CyAnimator.internal.model.Progress;

public class ProgressDialog extends JDialog {
	
	TimelinePanel parent;
	// JProgressBar progressBar;

	public ProgressDialog(TimelinePanel parent, String title, String message) {
		super();
		this.parent = parent;
		setLayout(new BorderLayout());
		setTitle(title);

		JLabel label = new JLabel(message);
		label.setFont(new Font("Sans", Font.BOLD, 12));
		label.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		Box b = Box.createHorizontalBox();
		b.add(Box.createHorizontalGlue());
		b.add(label, BorderLayout.CENTER);
		b.add(Box.createHorizontalGlue());
		add(b, BorderLayout.CENTER);
		label.setPreferredSize(new Dimension(200,50));

		/*
		progressBar = new JProgressBar(0,100);
		progressBar.setIndeterminate(true);
		progressBar.setBorderPainted(false);
		progressBar.setStringPainted(false);
		progressBar.setPreferredSize(new Dimension(200,40));
		progressBar.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		add(progressBar, BorderLayout.CENTER);
		*/

		pack();
		setLocationRelativeTo(parent);
	}

	/*
	public void frames(final int maxFrames) { 
		System.out.println("Updating maxFrames = "+maxFrames);
		this.maxFrames = maxFrames; 
		progressBar.setValue(0);
		progressBar.setIndeterminate(false);
		progressBar.setStringPainted(true);
	}

	public void progress(final int frame) { 
		System.out.println("Updating frame "+frame);
		if (!progressBar.isIndeterminate()) {
			System.out.println("Setting progress value to "+(frame*100/maxFrames));
			progressBar.setValue(frame*100/maxFrames);
			repaint();
			update(getGraphics());
		}
	}
	*/

}
