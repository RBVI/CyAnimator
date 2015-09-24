/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.CyAnimator.internal.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CySession;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;

import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;
import edu.ucsf.rbvi.CyAnimator.internal.ui.CyAnimatorDialog;

public class LoadSessionListener implements SessionLoadedListener {
	final CyServiceRegistrar registrar;

	public LoadSessionListener(final CyServiceRegistrar registrar) {
		this.registrar = registrar;
	}

	public void handleEvent(SessionLoadedEvent e) {
		// Reset CyAnimator
		FrameManager.reset();

		// Get the session
		CySession session = e.getLoadedSession();
		/*
		System.out.println("Session: "+session);
		for (String s: session.getAppFileListMap().keySet()) {
			System.out.println(s+":");
			for (File f: session.getAppFileListMap().get(s)) {
				System.out.println("File: "+f);
			}
		}
		*/
		if (!session.getAppFileListMap().containsKey("CyAnimator"))
			return;

		// System.out.println("We have animation information!");

		// Ask the user if they want to load the frames?
		//
		try {
			File frameListFile = session.getAppFileListMap().get("CyAnimator").get(0);
			FileReader reader = new FileReader(frameListFile);
			BufferedReader bReader = new BufferedReader(reader);
			JSONParser parser = new JSONParser();
			Object jsonObject = parser.parse(bReader);
	
			// For each root network, get the 
			if (jsonObject instanceof JSONArray) {
				for (Object obj: (JSONArray)jsonObject) {
					try {
						FrameManager.restoreFramesFromSession(registrar, session, (JSONObject)obj);
					} catch (Exception ex) { ex.printStackTrace(); }
				}
			}
			bReader.close();
			reader.close();
		} catch(FileNotFoundException fnf) {
			System.out.println("File not found exception: "+fnf.getMessage());
		} catch(IOException ioe) {
			System.out.println("IO exception: "+ioe.getMessage());
		} catch(ParseException pe) {
			System.out.println("Unable to parse JSON file: "+pe.getMessage());
			pe.printStackTrace();
		}


	}
}
