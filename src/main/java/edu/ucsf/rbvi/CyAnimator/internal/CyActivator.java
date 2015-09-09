package edu.ucsf.rbvi.CyAnimator.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.CyAnimator.internal.io.SaveSessionListener;
import edu.ucsf.rbvi.CyAnimator.internal.io.LoadSessionListener;
import edu.ucsf.rbvi.CyAnimator.internal.tasks.CyAnimatorDialogTaskFactory;
import edu.ucsf.rbvi.CyAnimator.internal.model.AnnotationLexicon;

public class CyActivator extends AbstractCyActivator {
	private static Logger logger = LoggerFactory
			.getLogger(edu.ucsf.rbvi.CyAnimator.internal.CyActivator.class);
	
	public CyActivator() {
		super();
	}

	public void start(BundleContext context) throws Exception {
		// See if we have a graphics console or not
		boolean haveGUI = true;
		CySwingApplication cyApplication = getService(context, CySwingApplication.class);
		AnnotationLexicon lex = new AnnotationLexicon();

	//	CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
	//	CyNetworkManager networkManager = (CyNetworkManager) getService(context, CyNetworkManager.class);
		CyServiceRegistrar registrar = getService(context, CyServiceRegistrar.class);
		
		if (cyApplication == null) {
			haveGUI = false;
		} else {
			CyAnimatorDialogTaskFactory dialogTaskFactory = new CyAnimatorDialogTaskFactory(registrar);
			Properties dialogTaskProperties = new Properties();
			setStandardProperties(dialogTaskProperties, "CyAnimator", null, "1.0");
			registerService(context, dialogTaskFactory, NetworkViewTaskFactory.class, dialogTaskProperties);
		}

		SaveSessionListener ssl = new SaveSessionListener();
		registerService(context, ssl, SessionAboutToBeSavedListener.class, new Properties());

		LoadSessionListener lsl = new LoadSessionListener(registrar);
		registerService(context, lsl, SessionLoadedListener.class, new Properties());
	}
	
	private void setStandardProperties(Properties p, String title, String command, String gravity) {
		if (title != null) {
			p.setProperty(TITLE, title);
			p.setProperty(PREFERRED_MENU, "Apps");
			p.setProperty(IN_MENU_BAR,"true");
			p.setProperty(MENU_GRAVITY, gravity);
		}
		if (command != null) {
			p.setProperty(COMMAND,command);
			p.setProperty(COMMAND_NAMESPACE,"CyAnimator");
		}
	}


}
