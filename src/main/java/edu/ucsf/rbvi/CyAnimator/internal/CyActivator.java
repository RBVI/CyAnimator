package edu.ucsf.rbvi.CyAnimator.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.CyAnimator.internal.tasks.CyAnimatorDialogTaskFactory;

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
	//	CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
	//	CyNetworkManager networkManager = (CyNetworkManager) getService(context, CyNetworkManager.class);
		
		if (cyApplication == null) {
			haveGUI = false;
		}
		else {
			CyAnimatorDialogTaskFactory dialogTaskFactory = new CyAnimatorDialogTaskFactory(context);
			Properties dialogTaskProperties = new Properties();
			setStandardProperties(dialogTaskProperties, "CyAnimator", null, "1.0");
			registerService(context, dialogTaskFactory, TaskFactory.class, dialogTaskProperties);
		}
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
