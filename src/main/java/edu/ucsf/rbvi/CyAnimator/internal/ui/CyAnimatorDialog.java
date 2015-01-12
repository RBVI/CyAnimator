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

import edu.ucsf.rbvi.CyAnimator.internal.model.CyFrame;
import edu.ucsf.rbvi.CyAnimator.internal.model.FrameManager;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.io.IOException;
import java.io.File;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.cytoscape.service.util.CyServiceRegistrar;



public class CyAnimatorDialog extends JDialog 
                              implements ActionListener, java.beans.PropertyChangeListener, FocusListener, WindowListener {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6650485843548244554L;
	private JButton captureButton;
	private JButton playButton;
	private JButton stopButton;
	private JButton pauseButton;
	private JButton forwardButton;
	private JButton backwardButton;
	private JButton recordButton;
        private JButton browseButton;
        private JButton saveButton;
        
        private JComboBox choicesList;
        private JComboBox resolutionsList;
        private JComboBox frameCountList;
	
	private JMenuItem menuItem;
        private JTabbedPane tabbedPane;
	private JPanel mainPanel;
        private JPanel settingPanel;
        private JPanel frameSettingPanel;
        private JPanel directorySettingPanel;
        private JPanel outputSettingPanel;
        private JTextField directoryText;
	private JPopupMenu thumbnailMenu;
	private JSlider speedSlider;
	final JFileChooser fc = new JFileChooser();
	
	private ArrayList<JPopupMenu> menuList;
	
	private JScrollPane framePane;
	private JPanel framePanel;
	
	private DragAndDropManager dragnDrop;
	private FrameManager frameManager;

	private CyServiceRegistrar bc;
//	private CyLogger logger;
	
        private String filePath = "";
	int thumbnailPopupIndex = 0;
	ArrayList<CyFrame> frameList;
	       
	
	public CyAnimatorDialog(CyServiceRegistrar bundleContext/*CyLogger logger*/){
	/*	Cytoscape.getSwingPropertyChangeSupport().addPropertyChangeListener(this);
		
		//add as listener to CytoscapeDesktop
		Cytoscape.getDesktop().getSwingPropertyChangeSupport().addPropertyChangeListener(this); */
	
		this.setTitle("CyAnimator");
		bc = bundleContext;
		frameManager = new FrameManager(bc);
		
		
		frameList = frameManager.getKeyFrameList();
		
		menuList = new ArrayList<JPopupMenu>();
		
		framePane = new JScrollPane();
		framePanel = new JPanel();
		
		dragnDrop = new DragAndDropManager();
		addWindowListener(this);
		
		initialize();
	}
	
	/**
	 * Create the control buttons, panels, and initialize the main JDialog.
	 */
	public void initialize(){
		tabbedPane = new JTabbedPane();
		mainPanel = new JPanel();
		mainPanel.addPropertyChangeListener(this);
		
		BoxLayout mainbox = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
		mainPanel.setLayout(mainbox);		
		
		captureButton = new JButton("Add Frame");
		captureButton.addActionListener(this);
		captureButton.setActionCommand("capture");
		
		ImageIcon playIcon = createImageIcon("/images/play.png", "Play Button");
		playButton = new JButton(playIcon);
		playButton.addActionListener(this);
		playButton.setActionCommand("play");    

		ImageIcon stopIcon = createImageIcon("/images/stop.png", "Stop Button");
		stopButton = new JButton(stopIcon);
		stopButton.addActionListener(this);
		stopButton.setActionCommand("stop");

		ImageIcon pauseIcon = createImageIcon("/images/pause.png", "Pause Button");
		pauseButton = new JButton(pauseIcon);
		pauseButton.addActionListener(this);
		pauseButton.setActionCommand("pause");
		pauseButton.addPropertyChangeListener("Text", this);
		
		ImageIcon forwardIcon = createImageIcon("/images/fastForward.png", "Step Forward Button");
		forwardButton = new JButton(forwardIcon);
		forwardButton.addActionListener(this);
		forwardButton.setActionCommand("step forward");
		forwardButton.setToolTipText("Step Forward One Frame");
		
		
		ImageIcon backwardIcon = createImageIcon("/images/reverse.png", "Step Backward Button");
		backwardButton = new JButton(backwardIcon);
		backwardButton.addActionListener(this);
		backwardButton.setActionCommand("step backward");
		backwardButton.setToolTipText("Step Backward One Frame");
		
		ImageIcon recordIcon = createImageIcon("/images/record.png", "Record Animation");
		recordButton = new JButton(recordIcon);
		recordButton.addActionListener(this);
		recordButton.setActionCommand("record");
		recordButton.setToolTipText("Record Animation");

                browseButton = new JButton("Browse");
                browseButton.setSize(new Dimension(100, 30));
		browseButton.addActionListener(this);
		browseButton.setActionCommand("browse");
		browseButton.setToolTipText("Browse Directory to Save Video");
                
                saveButton = new JButton("Save");
                saveButton.setSize(new Dimension(100, 30));
		saveButton.addActionListener(this);
		saveButton.setActionCommand("save");
		saveButton.setToolTipText("Save Settings");
                
		speedSlider = new JSlider(1,60);

		speedSlider.addChangeListener(new SliderListener());

		JPanel controlPanel = new JPanel();
		BoxLayout box = new BoxLayout(controlPanel, BoxLayout.X_AXIS);
		controlPanel.setLayout(box);

		controlPanel.add(captureButton);
		controlPanel.add(playButton);
		controlPanel.add(pauseButton);
		controlPanel.add(stopButton);
		controlPanel.add(backwardButton);
		controlPanel.add(forwardButton);
		controlPanel.add(recordButton);
		controlPanel.add(speedSlider);

		mainPanel.add(controlPanel);

		updateThumbnails(); 
		mainPanel.add(framePane);
                                
                String[] choices = { "Frames" , "GIF", "MP4"};
                String[] resolutions = { "100", "200", "300", "400", "500"};
                String[] frameCount = { "10", "20", "30", "40", "50"};

                choicesList = new JComboBox(choices);
                resolutionsList = new JComboBox(resolutions);
                frameCountList = new JComboBox(frameCount);
                choicesList.setSelectedIndex(1);
                resolutionsList.setSelectedIndex(0);
                frameCountList.setSelectedIndex(2);
                
                choicesList.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        saveButton.setEnabled(true);
                    }
                });
                resolutionsList.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        saveButton.setEnabled(true);
                    }
                });
                frameCountList.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        saveButton.setEnabled(true);
                    }
                });

                settingPanel = new JPanel();
                outputSettingPanel = new JPanel();
                outputSettingPanel.setBorder(BorderFactory.createTitledBorder("Video Options"));
                outputSettingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                outputSettingPanel.add(new JLabel("Video Type: "));
                outputSettingPanel.add(choicesList);
                
                directorySettingPanel = new JPanel();
                directorySettingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                directorySettingPanel.add(new JLabel("Video location: "));
                directoryText = new JTextField(40);
                directoryText.setText("");
                directorySettingPanel.add(directoryText);
                directorySettingPanel.add(browseButton);
                
                frameSettingPanel = new JPanel();
                frameSettingPanel.setBorder(BorderFactory.createTitledBorder("Frame Options"));
                frameSettingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                frameSettingPanel.add(new JLabel("Frame Per Second: "));
                frameSettingPanel.add(frameCountList);
                frameSettingPanel.add(new JLabel("Resolution: "));
                frameSettingPanel.add(resolutionsList);
                
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.add(saveButton);
                
                settingPanel.setLayout(new GridLayout(4,1));
                settingPanel.add(directorySettingPanel);
                settingPanel.add(outputSettingPanel);
                settingPanel.add(frameSettingPanel);
                settingPanel.add(buttonPanel);
		this.setSize(new Dimension(700,300));
		this.setLocation(900, 100);

                tabbedPane.addTab("Home",mainPanel);
                tabbedPane.addTab("Settings", settingPanel);
		setContentPane(tabbedPane);
	}

	
	public void actionPerformed(ActionEvent e){
		
		String command = "";
		if (e != null)
			command = e.getActionCommand();
		
		//add current frame to key frame list
		if(command.equals("capture")){
			try {
				frameManager.addKeyFrame();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			updateThumbnails();
		}
		
		
		if(command.equals("play")){
			frameManager.play();
		} 
		
		
		if(command.equals("stop")){
			frameManager.stop();
		}
		
		
		if(command.equals("pause")){
			frameManager.pause();
		}
		
		
		//move forward one frame in the animation
		if(command.equals("step forward")){
			frameManager.stepForward();
		}
		
		
	    //move backwards one frame in the animation
		if(command.equals("step backward")){
			frameManager.stepBackward();
		}
		
		
		if(command.equals("record")){
                        if( ! new File(filePath).exists() ){
                            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
                            int returnVal = fc.showSaveDialog(new JPanel());
                            if (returnVal == JFileChooser.APPROVE_OPTION) {
                                    File file = fc.getSelectedFile();
                                    filePath = file.getPath();
                                    directoryText.setText(filePath);
                            }
                        }
                        try {
                            frameManager.recordAnimation(filePath);
                        } catch (Exception excp) {
                            //	logger.error("Record of animation failed",excp);
                        }
		}
                
                if(command.equals("browse")){
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
			int returnVal = fc.showSaveDialog(new JPanel());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				filePath = file.getPath();
                                directoryText.setText(filePath);
			}
                        saveButton.setEnabled(true);
		}
                
                if(command.equals("save")){                    
                    int choice = choicesList.getSelectedIndex();
                    int resolution = (resolutionsList.getSelectedIndex() + 1)*100;
                    int frameCount = (frameCountList.getSelectedIndex() + 1)*10;
                    frameManager.updateSettings(frameCount, choice, resolution);
                    saveButton.setEnabled(false);
                }
		
		//If event is fired from interpolation menu this matches the interpolation count.
		Pattern interpolateCount = Pattern.compile("interpolate([0-9]+)_$");
		Matcher iMatch = interpolateCount.matcher(command);
				
		if(iMatch.matches()){
			//Get the matched integer
			int inter = Integer.parseInt(iMatch.group(1));
			
			//If integer is a 0 pop up a dialog to enter a custom interpolation count
			if(inter == 0){
				String input = JOptionPane.showInputDialog("Enter the number of frames to interpolate over: ");
				try {
					inter = Integer.parseInt(input);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "Frame interpolation count must be an integer", 
							"Integer parse error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			
			//thumbnailPopupIndex is set in PopupListener to the most recent thumbnail that was rightclicked
			frameList.get(thumbnailPopupIndex).setInterCount(inter);
			frameManager.updateTimer();
		}
		
		
		if(command.equals("delete")){
			//thumbnailPopupIndex is set in PopupListener to the most recent thumbnail that was rightclicked
			frameManager.deleteKeyFrame(thumbnailPopupIndex);
			
			updateThumbnails();		
		}
		
		
		if(command.equals("move right")){	
			//thumbnailPopupIndex is set in PopupListener to the most recent thumbnail that was rightclicked
			int i = thumbnailPopupIndex;

			//swap the current thumbnail with the one following it
			if(i != frameList.size()-1){
				CyFrame tmp = frameList.get(i+1);
				frameList.set(i+1, frameList.get(i));
				frameList.set(i, tmp);
			}

			frameManager.setKeyFrameList(frameList);
			updateThumbnails();		
		}
		
		
		if(command.equals("move left")){
			int i = thumbnailPopupIndex;

			//swap the current thumbnail with the one preceeding it
			if(i != 0){
				CyFrame tmp = frameList.get(i-1);
				frameList.set(i-1, frameList.get(i));
				frameList.set(i, tmp);	
			}	
			
			frameManager.setKeyFrameList(frameList);
			updateThumbnails();
		}	
		setVisible(true);
	}

	
	/**
	 * Takes the current frameList and cycles through it to create a JButton
	 * for each frame with the corresponding thumbnail image.  It also creates
	 * a JPopupMenu for each frame in the frameList so that the right click controls
	 * for interpolate and delete can be tied to each JButton.
	 * 
	 */
	public void updateThumbnails(){

		//remove the framePane so that a new one can be made with the updated thumbnails
		mainPanel.remove(framePane);
		framePanel = new JPanel();
		
		//make a horizontal box layout
		BoxLayout box = new BoxLayout(framePanel, BoxLayout.X_AXIS);
		framePanel.setLayout(box);

		MouseListener popupListener = new PopupListener();
		
		//update the frameList
 		frameList = frameManager.getKeyFrameList();
		int i = 0;
		
		//drag and drop code, not fully implemented
		int totalFrameWidth = 0;
		//dragnDrop = new DragAndDropManager();
		dragnDrop.setFrameCount(frameList.size());
	
		
		
		for(CyFrame frame: frameList){

			//get the thumbnail image
			while (frame.getFrameImage() == null)
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};
			ImageIcon ic = new ImageIcon(frame.getFrameImage());

			//put the image on the thumbnail button
			JButton thumbnailButton = new JButton(ic);
			thumbnailButton.addMouseListener(dragnDrop);
			thumbnailButton.addActionListener(this);
			thumbnailButton.setActionCommand(frame.getID());
		
			
		    //for some reason thumbnailButton.getWidth() returns 0 so I had
			//to improvise and use the icon width plus 13 pixels for the border. 
			totalFrameWidth = totalFrameWidth + ic.getIconWidth() + 13;
			dragnDrop.setFrameHeight(ic.getIconHeight());
			
			
			//create a popupmenu for each thumbnail and add the menu items
			thumbnailMenu = new JPopupMenu();
			JMenu interpolateMenu = new JMenu("Interpolate");
			menuItem = new JMenuItem("10 Frames");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("interpolate10_");
			interpolateMenu.add(menuItem);

			menuItem = new JMenuItem("20 Frames");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("interpolate20_");
			interpolateMenu.add(menuItem);

			menuItem = new JMenuItem("50 Frames");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("interpolate50_");
			interpolateMenu.add(menuItem);

			menuItem = new JMenuItem("100 Frames");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("interpolate100_");
			interpolateMenu.add(menuItem);

			menuItem = new JMenuItem("Custom...");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("interpolate0_");
			interpolateMenu.add(menuItem);
	
			thumbnailMenu.add(interpolateMenu);
			
			menuItem = new JMenuItem("Delete");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("delete");
			menuItem.addMouseListener(popupListener);
			thumbnailMenu.add(menuItem);
			
			
			menuItem = new JMenuItem("Move Right");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("move right");
			menuItem.addMouseListener(popupListener);
			thumbnailMenu.add(menuItem);
			
			
			
			menuItem = new JMenuItem("Move Left");
			menuItem.addActionListener(this);
			menuItem.setActionCommand("move left");
			menuItem.addMouseListener(popupListener);
			thumbnailMenu.add(menuItem);
			
			
			menuList.add(thumbnailMenu);
			
			//set the name of the button to the integer position of the frame in the list
			thumbnailButton.setName(i+"");
			i++;
		
			thumbnailButton.addMouseListener(popupListener);
			framePanel.add(thumbnailButton);
			
		}
		
		
		dragnDrop.setFrameWidth(totalFrameWidth);
		
		//recreate the scrollpane with the updated framePanel
		framePane = new JScrollPane(framePanel);
		
		//add it to the main dialog panel
		mainPanel.add(framePane);
		
	}
	

	public void propertyChange ( PropertyChangeEvent e ) {
		if(e.getPropertyName().equals("ATTRIBUTES_CHANGED")){
			//initialize();
			setVisible(true);
		}
	}
	
	public void focusGained(FocusEvent e){
		updateThumbnails();
	}
	
	public void focusLost(FocusEvent e){
		
	}
	
	/**
	 * Listens for changes to the slider which then adjusts the speed of animation.
	 */
	class SliderListener implements ChangeListener {
	    public void stateChanged(ChangeEvent e) {
	        JSlider source = (JSlider)e.getSource();
	        if (!source.getValueIsAdjusting()) {
	        	
	        	//fps is frames per second
	            int fps = (int)source.getValue();
	            //fps = fps/60;
	            int  f = fps*fps;
	            if(frameManager.timer == null){ return; }
	            // System.out.println("FPS: "+fps);
	            
	            //timer delay is set in milliseconds, so 1000/fps gives delay per frame
	            frameManager.timer.setDelay(1000/fps);
	            
	        }    
	    }
	}

	/*
	 * Contains all of the code for managing mouse selections in relation to the drag and drop features.
	 * 
	 * 
	 */
	public class DragAndDropManager extends MouseMotionAdapter implements MouseListener, MouseMotionListener{
		private int frameCount = 0;
		private int totalFrameWidth = 0;
		private int frameWidth = 0;
		private int frameHeight = 0;
		private int currFrameIndex = 0;
		private Component clickedComponent;
		private int startX = 0;
		private int startY = 0;
		private int endX = 0;
		private int endY = 0;
		
		public DragAndDropManager(){}
		
		public void mousePressed(MouseEvent e) {
			
			//get starting point of the drag and drop
			startX = e.getX();
			startY = e.getY();
				
			//this.currFrameIndex = Integer.parseInt(e.getComponent().getName());	
		}
		
		
		public void mouseReleased(MouseEvent e) {
			
			//get ending point of the drag and drop
			endX = e.getX();
			endY = e.getY();
			
			//check to make sure the drag and drop is within 1.5 times the height 
			//of the frame in either direction of the y coordinate
			if(endY >= 0 && endY < frameHeight*1.5){
				
			}
			else{ 
				if(endY < 0 && endY > frameHeight*-1.5){}
				else{ return; }
			}
			int curr = currFrameIndex; //Integer.parseInt(clickedComponent.getName());
			
			//align the start position to the right or leftmost point of the clicked frame
			//based upon whether it is a forward or backwards drag respectively.
			int xgap = endX - startX;
			if(xgap >= 0){ xgap = xgap - (frameWidth - startX); }
			else{ xgap = xgap + (frameWidth - startX); }
			
			//divide xgap by framewidth to determine the number of frame positions which should be shifted
			double shiftCount = xgap/frameWidth;
			//round this same number
			int shiftInt = Math.round(xgap/frameWidth);
			
			//compare the raw number to the rounded number to see if it was rounded up or down
			//if it is rounded up then subtract 1 from the shift count
			if(shiftInt > shiftCount){
				shuffleList(curr, curr+shiftInt-1);
			}else{
				shuffleList(curr, curr+shiftInt);
			}

			//System.out.println("release   X: "+e.getX()+"  Y: "+e.getY());
		}
		
		public void mouseDragged(MouseEvent e) {
				System.out.println("hey");
		}
		
		 
		/*
		 * Shuffles the list by inserting the frame which was clicked (curr) into
		 * the position where the user released the mouse which is the insert
		 * position.
		 * 
		 * @param curr is the index of the frame that was clicked by the user
		 * @param insertPos is the index in the frame list where the frame will be inserted
		 */ 
		public void shuffleList(int curr, int insertPos){
			
			ArrayList<CyFrame> temp = new ArrayList<CyFrame>();
			
			for(int i=0; i<frameList.size(); i++){
				
				
				if(i == insertPos){ 
					temp.add(frameList.get(curr));
					continue;
				}
	
				
				if(i >= curr && curr < insertPos && i < insertPos && i < frameList.size()-1){ 
					temp.add(frameList.get(i+1)); 
					continue;
				}else{
					if(i <= curr && curr > insertPos && i > insertPos){
						temp.add(frameList.get(i-1));
						continue;
					}
				}

				temp.add(frameList.get(i));
			
			}
			
			
			frameList = temp;
			
			//This is where I feel like updateThumbnails() should go, however when this is done it freezes
			//until another button is pressed (i.e. Add Frame, pause, etc..) at which point the thumbnails update.
			
			//updateThumbnails();
		}

		public void mouseEntered(MouseEvent e) { 
			//e.getComponent().requestFocus();
			
		}

		public void mouseExited(MouseEvent e) {	}

		public void mouseClicked(MouseEvent e) {
			
			//Pattern frameID = Pattern.compile(".*([0-9]+)$");
			//Matcher fMatch = frameID.matcher(e.getComponent().getName());
			
			this.currFrameIndex = Integer.parseInt(e.getComponent().getName());
			//if(fMatch.matches()){
				frameList.get(currFrameIndex).display();
			//}
		}
		
		/*
		 * Sets the number of frames which is the number of thumbnail buttons
		 * 
		 * @param number of frames before interpolation
		 */
		public void setFrameCount(int frameCount){
			this.frameCount = frameCount;
		}
		/*
		 * set the total width(in pixels) of all of the frames combined
		 * 
		 * @param the total frame width
		 */
		public void setFrameWidth(int totalFrameWidth){
			this.totalFrameWidth = totalFrameWidth;
			if(frameCount == 0){ frameWidth = 0; return; }
			frameWidth = totalFrameWidth/frameCount;
		}
		
		/*
		 * set the frame height
		 * 
		 * @param frame height
		 */
		public void setFrameHeight(int frameHeight){
			this.frameHeight = frameHeight;
		}
		
		
	}
	
	
	
	class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
	      
			maybeShowPopup(e);
		}

	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {

			//menuList.get(menuIndex)

			//System.out.println(e.getComponent().getName());
			int currentIndex = Integer.parseInt(e.getComponent().getName());
			thumbnailPopupIndex = currentIndex;
			// System.out.println("CI: "+currentIndex);
			menuList.get(currentIndex).show(e.getComponent(), e.getX(), e.getY());
		}
		}
	}
	
	
	
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
		//	logger.error("Couldn't find file: " + path);
			return null;
		}
	}

	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosing(WindowEvent e) {
		frameManager.stop();
	}

	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	
}
