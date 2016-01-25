/*
 * Copyright 2011, 2012 Institut Pasteur.
 * 
 * This file is part of MiceProfiler.
 * 
 * MiceProfiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MiceProfiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MiceProfiler. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.fab.MiceProfiler;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.file.FileUtil;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.painter.Painter;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginImageAnalysis;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.fab.MiceProfiler.PhyMouse.MouseInfoRecord;

/**
 * @author Fabrice de Chaumont
 */
public class MiceProfilerTracker extends Plugin implements Painter, PluginImageAnalysis, ActionListener,
        ChangeListener, ViewerListener
{

    IcyFrame mainFrame = null;
    JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();

    PhyMouse phyMouse01A = null;

    JButton startButton = new JButton("Start");
    JButton trackAllButton = new JButton("<html><br><b>Track All Start</b><br><br></html>");
    JButton stopTrackAllButton = new JButton("<html><br><b>Track All Stop</b><br><br></html>");
    JButton start2Button = new JButton("Start (step)");
    Point mousePoint = new Point(100, 100);
    Animator animator = new Animator();
    JButton startThreadStepButton = new JButton("Start Step Anim");
    JButton stopThreadStepButton = new JButton("Stop Step Anim");
    JButton readPositionFromROIButton = new JButton("Read starting position from Line ROI.");
    JCheckBox limitTrackingSpeedCheckBox = new JCheckBox("Limit tracking speed to 15fps");
        
    JComboBox mouseColorComboBox = new JComboBox( new String[] { "Track black mice", "Track white mice" } );

    JCheckBox updatePhysicsGuidesCheckBox = new JCheckBox("update phys. guides", true);
    // JCheckBox MouseTrackingCheckBox = new JCheckBox("Track 1 mouse", true);

    JButton previousFrame = new JButton("Previous Frame");
    JButton nextFrame = new JButton("Next Frame");
    JButton previous10Frame = new JButton("Previous 10 Frame");
    JButton next10Frame = new JButton("Next 10 Frame");

    JButton divPer2Button = new JButton("Div per 2 fps");

    JCheckBox useTotalSystemEnergyStopConditionBox = new JCheckBox("use total system energy stop condition", true);
    JCheckBox useImageBufferOptimisation = new JCheckBox("Use image load optimisation", true);
    JTextField numberOfImageForBufferTextField = new JTextField("200");
    JLabel bufferValue = new JLabel("0%");

    JButton setVideoSourceButton = new JButton("Click to set/change video source");
    JLabel currentTimeLabel = new JLabel("current time");
    JLabel lastFrameLoadTime = new JLabel("lastFrameLoadTime");
    JLabel lastFramePhysicTime = new JLabel("lastFramePhysicTime");
    JLabel lastFrameForceMapTime = new JLabel("lastFrameForceMapTime");
    JLabel totalImageTime = new JLabel("time last image");
    
    JButton reverseTrackFromTButton = new JButton("Reverse Identity (from now to end of sequence)");

    JSlider sliderTime = new JSlider();

    JButton saveXMLButton = new JButton("Save XML Data");
    JButton loadXMLButton = new JButton("Load XML Data");

    int ITERATION = 50;

    Sequence sequenceOut;

    XugglerAviFile aviFile;
    int currentFrame = 0;

    MouseGuidePainter mouseGuidePainter;

    Timer checkBufferTimer = new Timer(1000, this);
    
    ManualHelper manualHelperA;
    ManualHelper manualHelperB;
    
    @Override
    public void compute()
    {
        // singleMouseTrackingCheckBox.setSelected( false );
        stopTrackAllButton.setEnabled(false);
        
        BufferedImage bImage = new BufferedImage(400, 400, BufferedImage.TYPE_3BYTE_BGR);

        sequenceOut = new Sequence();
        sequenceOut.setImage(0, 0, bImage);

        addSequence(sequenceOut);

        sequenceOut.removeAllImages();

        // Start Physics Engines

        System.out.println("----------");
        System.out.println("Mice Profiler / Fab / Version 7");
        System.out.println("Red mice: occupante /// Green: visiteur");

        phyMouse01A = new PhyMouse(sequenceOut);

        mainFrame = new IcyFrame("Mice Profiler", true, true, true, true);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(mainPanel, BorderLayout.CENTER);

        final JPanel videoPanel = GuiUtil.generatePanel("Video Settings");

        videoPanel.add(GuiUtil.besidesPanel(setVideoSourceButton));
        videoPanel.add(GuiUtil.besidesPanel(sliderTime));
        videoPanel.add(GuiUtil.besidesPanel(useImageBufferOptimisation, numberOfImageForBufferTextField));

        videoPanel.add(GuiUtil.besidesPanel(new JLabel("Current buffer:"), bufferValue));

        videoPanel.add(GuiUtil.besidesPanel(previousFrame, nextFrame));
        videoPanel.add(GuiUtil.besidesPanel(previous10Frame, next10Frame));
        videoPanel.add(GuiUtil.besidesPanel(updatePhysicsGuidesCheckBox /*
                                                                         * ,
                                                                         * singleMouseTrackingCheckBox
                                                                         */));
        previousFrame.addActionListener(this);
        nextFrame.addActionListener(this);
        previous10Frame.addActionListener(this);
        next10Frame.addActionListener(this);

        videoPanel.add(GuiUtil.besidesPanel(currentTimeLabel));

        sliderTime.setMajorTickSpacing(1000);
        sliderTime.setPaintTicks(true);
        sliderTime.setPaintTrack(true);
        sliderTime.addChangeListener(this);

        mainPanel.add(GuiUtil.besidesPanel(videoPanel));

        mainPanel.add(GuiUtil.besidesPanel(trackAllButton, stopTrackAllButton));
        mainPanel.add(GuiUtil.besidesPanel(readPositionFromROIButton));
        mainPanel.add(GuiUtil.besidesPanel(saveXMLButton));
        mainPanel.add(GuiUtil.besidesPanel(limitTrackingSpeedCheckBox ) );
        mainPanel.add(GuiUtil.besidesPanel(mouseColorComboBox ) );        
        mainPanel.add( GuiUtil.besidesPanel( reverseTrackFromTButton ));
        mainPanel.add(GuiUtil.besidesPanel(phyMouse01A.getPanel()));
        mainPanel.add(GuiUtil.besidesPanel(startThreadStepButton, stopThreadStepButton));
        mainPanel.add(GuiUtil.besidesPanel(lastFramePhysicTime, lastFrameLoadTime));

        mainPanel.add(GuiUtil.besidesPanel(lastFrameForceMapTime, totalImageTime));
        
        mouseColorComboBox.addActionListener( this );
        
        saveXMLButton.addActionListener(this);

        loadXMLButton.setEnabled(false);

        stopTrackAllButton.addActionListener(this);
        setVideoSourceButton.addActionListener(this);
        readPositionFromROIButton.addActionListener(this);
        startButton.addActionListener(this);
        start2Button.addActionListener(this);
        trackAllButton.addActionListener(this);
        startThreadStepButton.addActionListener(this);
        stopThreadStepButton.addActionListener(this);
        stopThreadStepButton.setEnabled(false);
        reverseTrackFromTButton.addActionListener( this );

        mainFrame.pack();
        mainFrame.center();
        mainFrame.setVisible(true);
        mainFrame.addToMainDesktopPane();
        
        sequenceOut.addPainter(this);

        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                final Viewer v = Icy.getMainInterface().getFirstViewer(sequenceOut);

                if (v != null)
                    v.addListener(MiceProfilerTracker.this);
            }
        });

        checkBufferTimer.start();
    }

    private void readPositionFromROI()
    {
        synchronized (phyMouse01A)
        {
            phyMouse01A.mouseList.clear();
            phyMouse01A.bodyList.clear();
            phyMouse01A.distanceJointList.clear();
            phyMouse01A.slideJointList.clear();
            phyMouse01A.world.clear();

            //System.out.println("debug: Read position from roi step 1");

            // mouse 1
            {
                final float alpha = (float) Math.atan2(mouseGuidePainter.M1h.getY() - mouseGuidePainter.M1b.getY(),
                        mouseGuidePainter.M1h.getX() - mouseGuidePainter.M1b.getX());

                phyMouse01A.generateMouse((float) mouseGuidePainter.M1h.getX(), (float) mouseGuidePainter.M1h.getY(),
                        alpha + (float) Math.PI / 2f);
            }
            // mouse 2
            // if ( !singleMouseTrackingCheckBox.isSelected() ) // a second mouse is present
            {
                // System.out.println("READ POSITION FROM ROI");

                final float alpha = (float) Math.atan2(mouseGuidePainter.M2h.getY() - mouseGuidePainter.M2b.getY(),
                        mouseGuidePainter.M2h.getX() - mouseGuidePainter.M2b.getX());

                phyMouse01A.generateMouse((float) mouseGuidePainter.M2h.getX(), (float) mouseGuidePainter.M2h.getY(),
                        alpha + (float) Math.PI / 2f);
            }

            phyMouse01A.recordMousePosition(sliderTime.getValue());

            //System.out.println("nb souris dans mouse model : " + phyMouse01A.mouseList.size());

        }
        sequenceOut.painterChanged(null);
    }

    public void keyPressed(Point p, KeyEvent e)
    {
    	
    	//System.out.println("Key pressed is " + e );
    	
    }

    public void mouseClick(Point p, MouseEvent e)
    {
    	//System.out.println(" mouse click");
    }

    public void mouseDrag(Point p, MouseEvent e)
    {
    }

    public void mouseMove(Point p, MouseEvent e)
    {
        mousePoint = p;
    }

    TrackAllThread trackAllThread = null;

    class TrackAllThread extends Thread
    {
        public boolean shouldRun = true;

        @Override
        public void run()
        {

            Calendar cal = Calendar.getInstance();
            final double msStart = cal.getTimeInMillis();

            for (int t = sliderTime.getValue(); t < sliderTime.getMaximum(); t++)

            {

                currentFrame = t;

                final double totalImageTimerStart = Calendar.getInstance().getTimeInMillis();

                if (shouldRun == false)
                {
                	ThreadUtil.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							stopTrackAllButton.setEnabled(false);
							trackAllButton.setEnabled(true);
							startThreadStepButton.setEnabled( true );		
							readPositionFromROIButton.setEnabled( true );
						}
					});
                    
                    return;
                }

                sliderTime.setValue(t); // image should be updated in viewer here.

                IcyBufferedImage imageSourceR = sequenceOut.getImage(t, 0);

                while (imageSourceR == null)
                {

                    imageSourceR = sequenceOut.getImage(t, 0);

                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }

                    // test arret

                    if (shouldRun == false)
                    {
                        stopTrackAllButton.setEnabled(false);
                        trackAllButton.setEnabled(true);
                        startThreadStepButton.setEnabled( true );
                        readPositionFromROIButton.setEnabled( true );
                        return;
                    }

                }

                // Convert input image.
                IcyBufferedImage imageSource = null;
                imageSource = imageSourceR; // ASSUME 3 byte

                final double frameForceMapTimerStart = Calendar.getInstance().getTimeInMillis();

                
                
//                boolean reverseThresholdBoolean = ( mouseColorComboBox.getSelectedIndex() == 1 );
//                System.out.println("Setting mouse color: " + mouseColorComboBox.getSelectedIndex()  );
//                phyMouse01A.setReverseThreshold ( reverseThresholdBoolean );
//                
                // use the record to initialize the head position of the mouse.
                phyMouse01A.setHeadLocation( 0 ,  manualHelperA.getControlPoint( sliderTime.getValue() ) );
                phyMouse01A.setHeadLocation( 1 ,  manualHelperB.getControlPoint( sliderTime.getValue() ) );                
                
                phyMouse01A.computeForcesMap(imageSource);

                for (int i = 0; i < ITERATION; i++)
                {

                    if (useTotalSystemEnergyStopConditionBox.isSelected())
                    {
                        if ((i > 3))
                            if (phyMouse01A.isStable())
                            {
                                // System.out.println("iteration : " + i );
                                break;
                            }
                    }

                    phyMouse01A.computeForces();
                    phyMouse01A.worldStep(t);
                }

                phyMouse01A.applyMotionPrediction();

                updateMouseGuidePainter();

                final double totalImageMs = (Calendar.getInstance().getTimeInMillis() - totalImageTimerStart);
                totalImageTime.setText("total image time: " + totalImageMs + " ms / FPS: "
                        + ((int) (10 * 1000d / totalImageMs)) / 10d);

                if ( limitTrackingSpeedCheckBox.isSelected() )
                {
                	try {
						Thread.sleep( (int)(1000d / 15d) );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
            }
            // System.out.print("Ok.");

            cal = Calendar.getInstance();
            final double msEnd = cal.getTimeInMillis();

//            System.out.println("ms : " + (msEnd - msStart));

            final double fps = 0;

//            System.out.println("Tracking Frame rate: " + fps);

            stopTrackAllButton.setEnabled(false);
            trackAllButton.setEnabled(true);
            startThreadStepButton.setEnabled( true );
            readPositionFromROIButton.setEnabled( true );
            
        }

        private void updateMouseGuidePainter()
        {

            mouseGuidePainter.setVisible(updatePhysicsGuidesCheckBox.isSelected());

            mouseGuidePainter.M1h.moveTo((int) phyMouse01A.mouseList.get(0).headBody.getPosition().getX(),
                    (int) phyMouse01A.mouseList.get(0).headBody.getPosition().getY());
            mouseGuidePainter.M1b.moveTo((int) phyMouse01A.mouseList.get(0).tail.getPosition().getX(),
                    (int) phyMouse01A.mouseList.get(0).tail.getPosition().getY());

            if (phyMouse01A.mouseList.size() > 1)
            {
                mouseGuidePainter.M2h.moveTo((int) phyMouse01A.mouseList.get(1).headBody.getPosition().getX(),
                        (int) phyMouse01A.mouseList.get(1).headBody.getPosition().getY());
                mouseGuidePainter.M2b.moveTo((int) phyMouse01A.mouseList.get(1).tail.getPosition().getX(),
                        (int) phyMouse01A.mouseList.get(1).tail.getPosition().getY());
            }

        }
    }

    StepThread stepThread = null;

    class StepThread extends Thread
    {
        public boolean shouldRun = true;

        @Override
        public void run()
        {

            while (shouldRun)
            {

                IcyBufferedImage imageSourceR = null;
                while (imageSourceR == null)
                {
                    imageSourceR = sequenceOut.getImage(currentFrame, 0); // .getAsRenderedImage(

                }

                final IcyBufferedImage imageSource = imageSourceR; // assume good format.

                phyMouse01A.computeForcesMap(imageSource);

                {

                    synchronized (phyMouse01A)
                    {
                        phyMouse01A.computeForces();
                        phyMouse01A.worldStep(currentFrame);
                    }
                }

                sequenceOut.painterChanged(null);
            }
        }

    }

    File currentFile = null;

    private void saveXML()
    {
        // save a backup in the backup directory
        // Check if the folder exists
        String directory = FileUtil.getDirectory(currentFile.getAbsolutePath().toString());
        directory += FileUtil.separator + "xml tracking backup";
        FileUtil.createDir(directory);

        String backupFileName = FileUtil.getFileName(currentFile.getAbsolutePath().toString(), false);
        backupFileName += " " + new Date().toString();

        backupFileName = backupFileName.replace(":", "_"); // remove time : incompatible with
                                                           // fileName.

        String backupFullName = directory + FileUtil.separator + backupFileName;

        phyMouse01A.saveXML(new File(backupFullName));

        // update the file
        phyMouse01A.saveXML(currentFile);
    }

    private void displayImageAt(int frameNumber)
    {
        if (aviFile != null)
        {
            currentFrame = frameNumber;
            phyMouse01A.currentFrame = frameNumber;

            if (sequenceOut.getImage(frameNumber, 0) == null)
            {
                final boolean wasEmpty = sequenceOut.getNumImage() == 0;

                sequenceOut.setImage(frameNumber, 0, aviFile.getImage(frameNumber));

                if (wasEmpty)
                {
                    for (Viewer viewer : sequenceOut.getViewers())
                        if (viewer.getCanvas() instanceof Canvas2D)
                            ((Canvas2D) viewer.getCanvas()).centerImage();
                }
            }

            String timeString = "";
            timeString += "(#frame): " + frameNumber + "/" + aviFile.getTotalNumberOfFrame() + " "
                    + aviFile.getTimeForFrame(frameNumber);

            currentTimeLabel.setText(timeString);
        }
    }

    // AviFile aviFile;

    public void displayRelativeFrame( int nbFrame )
    {
    	sliderTime.setValue(sliderTime.getValue() + nbFrame );
    }
    
    public void startTrackAll()
    {
    	readPositionFromROI();
        trackAllThread = new TrackAllThread();
        trackAllThread.start();
        stopTrackAllButton.setEnabled(true);
        trackAllButton.setEnabled(false);
        startThreadStepButton.setEnabled( false );
        readPositionFromROIButton.setEnabled( false );
    }
    
    public void stopTrackAll()
    {
    	trackAllThread.shouldRun = false;
    }
        
    public void actionPerformed(ActionEvent e)
    {

        if (e.getSource() == checkBufferTimer)
        {
            if (bufferThread != null)
            {
                int bufferPercent = bufferThread.getCurrentBufferLoadPercent();
                bufferValue.setText(bufferPercent + " %");
            }
        }

        if (e.getSource() == previousFrame)
        {
        	displayRelativeFrame( -1 );
            //sliderTime.setValue(sliderTime.getValue() - 1);
        }

        if (e.getSource() == nextFrame)
        {
        	displayRelativeFrame( 1 );
            //sliderTime.setValue(sliderTime.getValue() + 1);
        }

        if (e.getSource() == saveXMLButton)
        {
            saveXML();
        }

        if (e.getSource() == previous10Frame)
        {
        	displayRelativeFrame( -10 );
            //sliderTime.setValue(sliderTime.getValue() - 10);
        }

        if (e.getSource() == next10Frame)
        {
        	displayRelativeFrame( 10 );
//            sliderTime.setValue(sliderTime.getValue() + 10);
        }

        if (e.getSource() == startButton)
        {
            sliderTime.setValue(1 + sliderTime.getValue());

            final IcyBufferedImage imageSourceR = sequenceOut.getImage(0, 0); // .getAsRenderedImage(

            final IcyBufferedImage imageSource = imageSourceR;

            synchronized (phyMouse01A)
            {
                phyMouse01A.computeForcesMap(imageSource);
            }

            for (int i = 0; i < ITERATION; i++)
            {

                synchronized (phyMouse01A)
                {
                    phyMouse01A.computeForces();
                    phyMouse01A.worldStep(currentFrame);
                }
            }

            sequenceOut.painterChanged(null);

        }

        if (e.getSource() == divPer2Button)
        {

            phyMouse01A.divideTimePer2();
        }

        if (e.getSource() == setVideoSourceButton)
        {
            // load last preferences for loader.
            final JFileChooser fileChooser = new JFileChooser();

            final String node = "plugins/PhysicTracker/browser";

            final Preferences preferences = Preferences.userRoot().node(node);
            final String path = preferences.get("path", "");
            fileChooser.setCurrentDirectory(new File(path));

            final int x = preferences.getInt("x", 0);
            final int y = preferences.getInt("y", 0);
            final int width = preferences.getInt("width", 400);
            final int height = preferences.getInt("height", 400);

            fileChooser.setLocation(x, y);
            fileChooser.setPreferredSize(new Dimension(width, height));

            final int returnValue = fileChooser.showDialog(null, "Load");
            if (returnValue == JFileChooser.APPROVE_OPTION)
            {
                preferences.put("path", fileChooser.getCurrentDirectory().getAbsolutePath());
                preferences.putInt("x", fileChooser.getX());
                preferences.putInt("y", fileChooser.getY());
                preferences.putInt("width", fileChooser.getWidth());
                preferences.putInt("height", fileChooser.getHeight());

                try
                {
                    aviFile = new XugglerAviFile(fileChooser.getSelectedFile().getAbsolutePath(), true);
                }
                catch (Exception exc)
                {
                	//new MessageDialog( null, "File error" , "File type or video-codec not supported." , null, true );
                	MessageDialog.showDialog( "File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE );
                	                	
                	//new FailedAnnounceFrame("File type or video-codec not supported.");
                    //exc.printStackTrace();
                    aviFile = null;
                    return;
                }

                sequenceOut.removeAllImages();
                sequenceOut.setName( fileChooser.getSelectedFile().getName() );
                displayImageAt(0);

                sliderTime.setMaximum((int) aviFile.getTotalNumberOfFrame());
                sliderTime.setValue(0);

                setVideoSourceButton.setText(fileChooser.getSelectedFile().getName());

                currentFile = fileChooser.getSelectedFile();
                phyMouse01A.loadXML(currentFile);

                if (bufferThread != null)
                {
                    bufferThread.pleaseStop = true;
                    try
                    {
                        bufferThread.join();
                    }
                    catch (final InterruptedException e1)
                    {

                        e1.printStackTrace();
                    }
                }
                bufferThread = new ImageBufferThread();
                bufferThread.setName("Buffer Thread");
                bufferThread.setPriority(Thread.NORM_PRIORITY);
                bufferThread.start();

                useImageBufferOptimisation.setEnabled(false);
                numberOfImageForBufferTextField.setEnabled(false);
                // singleMouseTrackingCheckBox.setEnabled( false );

                synchronized (phyMouse01A)
                {
                    phyMouse01A.generateMouse(246, 48, 0); // Mouse 1

                    // if ( !singleMouseTrackingCheckBox.isSelected() )
                    {
                        phyMouse01A.generateMouse(238, 121, (float) (Math.PI)); // Mouse 2
                    }
                }

                manualHelperA =  new ManualHelper("Manual Helper" , Color.red , 1 , sequenceOut ) ;
                manualHelperB =  new ManualHelper("Manual Helper" , Color.green , 2 , sequenceOut ) ;
                sequenceOut.addOverlay( manualHelperA );
                sequenceOut.addOverlay( manualHelperB );

                sequenceOut.addOverlay( new LockScrollHelperOverlay() );
                
                // mouseGuidePainter = new MouseGuidePainter(sequenceOut ,
                // singleMouseTrackingCheckBox.isSelected() );
                mouseGuidePainter = new MouseGuidePainter(sequenceOut, false);

                //phyMouse01A.setScaleFieldEnable( false );            
            }
        }

        if (e.getSource() == trackAllButton)
        {
        	startTrackAll();
//            readPositionFromROI();
//            trackAllThread = new TrackAllThread();
//            trackAllThread.start();
//            stopTrackAllButton.setEnabled(true);
//            trackAllButton.setEnabled(false);
//            startThreadStepButton.setEnabled( false );
//            readPositionFromROIButton.setEnabled( false );
        }

        if (e.getSource() == stopTrackAllButton)
        {
        	stopTrackAll();
        	//trackAllThread.shouldRun = false;

        }

        if (e.getSource() == startThreadStepButton)
        {
            startThreadStepButton.setEnabled(false);
            trackAllButton.setEnabled( false );
            stopThreadStepButton.setEnabled(true);
            readPositionFromROIButton.setEnabled( false );
            stepThread = new StepThread();
            stepThread.start();
        }

        if (e.getSource() == stopThreadStepButton)
        {
            stepThread.shouldRun = false;
            startThreadStepButton.setEnabled(true);
            trackAllButton.setEnabled( true );
            stopThreadStepButton.setEnabled(false);
            readPositionFromROIButton.setEnabled( true );
        }

        if ( e.getSource() == mouseColorComboBox )
        {        	
        	//System.out.println( "mouse color check box clicked: " + mouseColorComboBox.getSelectedIndex() );
          boolean reverseThresholdBoolean = ( mouseColorComboBox.getSelectedIndex() == 1 );
          System.out.println("Setting mouse color: " + mouseColorComboBox.getSelectedIndex()  );
          phyMouse01A.setReverseThreshold ( reverseThresholdBoolean );
        }
        
        if (e.getSource() == readPositionFromROIButton)
        {
            readPositionFromROI();
        }

        if (e.getSource() == start2Button)
        {
            final IcyBufferedImage imageSourceR = sequenceOut.getImage(currentFrame, 0);

            final IcyBufferedImage imageSource = imageSourceR;

            phyMouse01A.computeForcesMap(imageSource);
            phyMouse01A.computeForces();
            phyMouse01A.worldStep(currentFrame);

            sequenceOut.painterChanged(null);

        }
        
        if ( e.getSource() == reverseTrackFromTButton )
        {
        	phyMouse01A.swapIdentityRecordFromTToTheEnd( sliderTime.getValue() );
        	sequenceOut.painterChanged( null );
        }

    }

    class Animator extends Thread
    {
        @Override
        public void run()
        {

            while (true)
            {
                final Point bodyPoint = new Point((int) phyMouse01A.getBodyList().get(0).getLastPosition().getX(),
                        (int) phyMouse01A.getBodyList().get(0).getLastPosition().getY());

                phyMouse01A.getBodyList().get(0)
                        .setForce(100 * (mousePoint.x - bodyPoint.x), 100 * (mousePoint.y - bodyPoint.y));
                phyMouse01A.worldStep(currentFrame);
                sequenceOut.painterChanged(null);
                try
                {

                    Thread.sleep(10);
                }
                catch (final InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas quiaCanvas)
    {
    	// shortcuts:
    	
    	if ( e.getKeyCode()== KeyEvent.VK_LEFT )
    	{
   			displayRelativeFrame( -1 );

   			e.consume();
    	}
    	
    	if ( e.getKeyCode()== KeyEvent.VK_RIGHT )
    	{
   			displayRelativeFrame( 1 );

    		e.consume();
    	}

    	if ( e.getKeyCode()== KeyEvent.VK_DOWN )
    	{
   			displayRelativeFrame( -10 );

   			e.consume();
    	}
    	
    	if ( e.getKeyCode()== KeyEvent.VK_UP )
    	{
   			displayRelativeFrame( 10 );

    		e.consume();
    	}
    	
    	if ( e.getKeyCode() == KeyEvent.VK_SPACE )
    	{
    		if ( trackAllThread == null )
    		{
    			startTrackAll();    			
    		}else
    		{
    			if ( trackAllThread.shouldRun == false )
    			{    			
    				startTrackAll();   			
    			}else
    			{
    				stopTrackAll();    			
    			}
    		}
    		e.consume();
    	}
    	
    	if ( e.getKeyChar()=='r' )
    	{
    		readPositionFromROI();
    		displayRelativeFrame( 1 );
    		readPositionFromROI();
    		e.consume();
    	}
    	
    	
    	
    }

    @Override
    public void mouseClick(MouseEvent e, Point2D p, IcyCanvas quiaCanvas)
    {
    }

    @Override
    public void mouseDrag(MouseEvent e, Point2D p, IcyCanvas quiaCanvas)
    {
    }

    @Override
    public void mouseMove(MouseEvent e, Point2D p, IcyCanvas quiaCanvas)
    {
    }

    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
        synchronized (phyMouse01A)
        {
            phyMouse01A.paint(g, canvas);
        }

    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() == sliderTime)
        {
            final Viewer v = Icy.getMainInterface().getFirstViewer(sequenceOut);

            if (v != null)
                v.setT(sliderTime.getValue());

            displayImageAt(sliderTime.getValue());
            
            if ( trackAllButton.isEnabled() ) // should be something better...
            {
            	// set mouseA
            	{
            		MouseInfoRecord record = phyMouse01A.mouseARecord.get( sliderTime.getValue() );
            		if ( record != null )
            		{
            			mouseGuidePainter.M1h.setPosition( record.headPosition );
            			mouseGuidePainter.M1b.setPosition( record.tailPosition );
            		}
            	}

            	// set mouseB
            	{
            		MouseInfoRecord record = phyMouse01A.mouseBRecord.get( sliderTime.getValue() );
            		if ( record != null )
            		{
            			mouseGuidePainter.M2h.setPosition( record.headPosition );
            			mouseGuidePainter.M2b.setPosition( record.tailPosition );
            		}
            	}
            }
            
            
        }
    }

    ImageBufferThread bufferThread;

    /**
     * centre l image active sur le currentFrame avec une valeur encadrante. Enleve les images
     * inutiles
     * 
     * @author Administrator
     */
    class ImageBufferThread extends Thread
    {

        public boolean pleaseStop = false;
        boolean bufferOn = true;

        int fenetre = Integer.parseInt(numberOfImageForBufferTextField.getText());

        int getCurrentBufferLoadPercent()
        {
            int currentBufferPercent = 0;
            final int frameStart = currentFrame - 10;
            final int frameEnd = currentFrame + fenetre;
            float nbImage = 0;
            float nbImageLoaded = 0;

            for (int t = frameStart; t < frameEnd; t++)
            {
                if (t >= 0 && t < aviFile.getTotalNumberOfFrame())
                {
                    nbImage++;
                    if (sequenceOut.getImage(t, 0) != null)
                        nbImageLoaded++;
                }
            }

            currentBufferPercent = (int) (nbImageLoaded * 100f / nbImage);

            return currentBufferPercent;
        }

        @Override
        public void run()
        {
            try
            {
                while (pleaseStop == false)
                {
                    ThreadUtil.sleep(100);

                    if (bufferOn)
                    {
                        final int cachedCurrentFrame = currentFrame;
                        int frameStart = currentFrame - 10;
                        int frameEnd = currentFrame + fenetre;

                        if (frameStart < 0)
                            frameStart = 0;
                        if (frameEnd > aviFile.getTotalNumberOfFrame())
                            frameEnd = (int) aviFile.getTotalNumberOfFrame();

                        // enleve les images hors fenetre (excepté la dernière)
                        for (int t = 0; t < sequenceOut.getSizeT() - 1; t++)
                        {
                            if (Math.abs(t - currentFrame) > fenetre + 10)
                            {
                                // current frame changed --> interrupt
                                if (cachedCurrentFrame != currentFrame)
                                    break;
                                if (pleaseStop)
                                    return;

                                sequenceOut.removeImage(t, 0);
                            }
                        }

                        for (int t = frameStart; t < frameEnd; t++)
                        {
                            if (sequenceOut.getImage(t, 0) == null)
                                sequenceOut.setImage(t, 0, aviFile.getImage(t));

                            // current frame changed --> interrupt
                            if (cachedCurrentFrame != currentFrame)
                                break;
                            if (pleaseStop)
                                return;
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas)
    {

    }

    @Override
    public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas)
    {

    }

    @Override
    public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas)
    {

    }

    @Override
    public void viewerChanged(ViewerEvent event)
    {
        if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
            sliderTime.setValue(event.getSource().getPositionT() );
        
    }

    @Override
    public void viewerClosed(Viewer viewer)
    {
        // ignore
    }

}
