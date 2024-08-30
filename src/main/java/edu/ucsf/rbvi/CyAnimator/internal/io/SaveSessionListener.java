/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.cytoscape.session.CySessionManager;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;

import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

public class SaveSessionListener implements SessionAboutToBeSavedListener {

	public SaveSessionListener() {
	}

	public void handleEvent(SessionAboutToBeSavedEvent e) {
		CySessionManager sessionManager = e.getSource();

		try {
			// Write out our frame list in a temporary file
			String tmpDir = System.getProperty("java.io.tmpdir");
			File frameFile = new File(tmpDir, "CyAnimator.frames");
			FileOutputStream fos = new FileOutputStream(frameFile);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
			BufferedWriter writer = new BufferedWriter(osw);
			writer.write("[\n");
			boolean first = true;
			if (FrameManager.getAllFrameManagers() != null) {
				for (FrameManager frameManager: FrameManager.getAllFrameManagers()) {
					frameManager.writeFrames(writer, first);
					if (first) first = false;
				}
			}
			writer.write("\n]\n");
			writer.close();
			osw.close();
			fos.close();

			e.addAppFiles("CyAnimator", Collections.singletonList(frameFile));
		} catch (IOException ioe) {
			System.out.println("IO exception: "+ioe.getMessage());
		} catch (Exception ex) {
			System.out.println("Exception: "+ex.getMessage());
		}

	}
}
