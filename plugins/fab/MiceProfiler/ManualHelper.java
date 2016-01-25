package plugins.fab.MiceProfiler;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.painter.Anchor2D;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.type.point.Point5D.Double;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JSlider;

/** this helper is dedicated to help manual correction. */
public class ManualHelper extends Overlay implements SequenceListener {

	enum MODE {
		NO_ACTION_MODE, RECORD_MODE
	}
	
	/** list of anchors created by this overlay. */
	ArrayList<MAnchor2D> activeAnchorList = new ArrayList<MAnchor2D>();

	ArrayList<MAnchor2D> controlPointList = new ArrayList<MAnchor2D>();
	HashMap<Integer, MAnchor2D> time2controlPointMap = new HashMap<Integer, MAnchor2D>();
	Color color;
	/** Previous time position of the canvas. Stored to update the painter */
	int previousTPosition =-1;
	MODE currentMode = MODE.NO_ACTION_MODE;
	int mouseNumber ;
	/** static font for absolute text */
	Font absoluteFont = new Font("Arial", Font.BOLD , 15 );
	/** switchModeKeyCode is the key to switch from edit to record mode. -1 if more than 10 mice are loaded. */
	int switchModeKeyCode;
	
	/** list of all manual helper to disable certain action like record mode when an other engage it.*/
	static ArrayList<ManualHelper> manualHelperList = new ArrayList<ManualHelper>();
	Sequence sequence ;
	/** slider in the MiceProfilerInterfaceTracker. Kept for automatic seeking*/
	//JSlider sliderTime;
	
	public ManualHelper(String name, Color color , int mouseNumber , Sequence sequence ) {
		super(name);
		
		this.sequence = sequence;
		sequence.addListener( this );
		
		this.color = color;
		this.mouseNumber = mouseNumber;
		//this.sliderTime = sliderTime;
		
		if ( mouseNumber < 10 )
		{
			switchModeKeyCode = mouseNumber+48;
		}
		
		// record itself
		manualHelperList.add( this );
		
		// create some sample points
		
//		for ( int t = 0 ; t< 100 ; t++)
//		{
//			setControlPoint( t*5 + Math.random() * 5, t*5 + Math.random() * 5, t );
//		}
		setPriority( OverlayPriority.TEXT_NORMAL );
		
	}

	void setControlPoint( double x, double y , int t )
	{
		MAnchor2D a = time2controlPointMap.get( t );
		if ( a == null )
		{
			a = new MAnchor2D( x, y);
			time2controlPointMap.put( t , a );
		}else
		{
			a.setPosition(x, y);
		}
	}
	
	MAnchor2D getControlPoint( int t )
	{		
		return time2controlPointMap.get( t );
	}
	
	void drawAbsoluteString( String string ,int x, int y , Graphics2D g , IcyCanvas2D canvas )
	{
		AffineTransform transform = g.getTransform();		
		g.transform( canvas.getInverseTransform() );		
		g.setFont( absoluteFont );
		g.drawString( string , x, y );
		g.setTransform( transform );
	}
	
	@Override
	public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {

		if ( e.getKeyCode() == switchModeKeyCode )
		{
			if ( currentMode == MODE.NO_ACTION_MODE )
			{				
				currentMode = MODE.RECORD_MODE ;
				for ( ManualHelper m : manualHelperList )
				{
					if ( m!=this)
					{
						m.setMode ( MODE.NO_ACTION_MODE );
					}
				}
				
			}else
			{
				currentMode = MODE.NO_ACTION_MODE;
			}
			e.consume();
			canvas.getSequence().overlayChanged( this );
		}
				
	}
	
	double lastFrameUpdate = 0; 
	
	private void advanceOneFrame(IcyCanvas canvas) {

		if ( System.currentTimeMillis() - lastFrameUpdate > 200 )
		{
			lastFrameUpdate = System.currentTimeMillis();
			canvas.getSequence().getFirstViewer().setPositionT( canvas.getSequence().getFirstViewer().getPositionT()+1 );
		}
				
	}
	
	@Override
	public void mouseClick(MouseEvent e, Double imagePoint, IcyCanvas canvas) {
		
		if (!( canvas instanceof IcyCanvas2D )) return;
		
		if ( currentMode == MODE.RECORD_MODE )
		{
			updatePoint( imagePoint.getX() , imagePoint.getY() , (int) imagePoint.getT() );
			e.consume();
			advanceOneFrame( canvas );
		}
		
	}

	@Override
	public void mouseDrag(MouseEvent e, Double imagePoint, IcyCanvas canvas) {
		
		if (!( canvas instanceof IcyCanvas2D )) return;
		
		if ( currentMode == MODE.RECORD_MODE )
		{
			updatePoint( imagePoint.getX() , imagePoint.getY() , (int) imagePoint.getT() );
			e.consume();
			advanceOneFrame( canvas );
		}
		
	}
	
	private void updatePoint(double x, double y, int t) {
				
			setControlPoint( x , y, t );
		
	}

	private void setMode(MODE mode) {
		currentMode = mode;		
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		if (!( canvas instanceof IcyCanvas2D )) return;
				
		// set color, stroke
		g.setColor( color );

		// draw in absolute the current mode.
		String modeString = "";
		switch (currentMode)
		{
			case NO_ACTION_MODE: 
				modeString = "Play mode";
				break;
			case RECORD_MODE:
				modeString = "Record mode";
				break;
		
		}
		drawAbsoluteString( mouseNumber + " : " + modeString , 20,20*mouseNumber , g, (IcyCanvas2D)canvas );
		
		int currentT = canvas.getPositionT();
		/* The time window to display the controls */
		int timeWindow = 10;
		

		// check if the time cursor has been shifted

		if ( false )
		{
			if ( currentT != previousTPosition )
			{
				previousTPosition = currentT;

				try
				{
					sequence.beginUpdate();

					// display anchors for the minus -10 frame to +10 frame						

					ArrayList<MAnchor2D> newAnchorList = new ArrayList<MAnchor2D>();

					for ( int t = currentT-timeWindow ; t < currentT+timeWindow ; t++ )
					{
						// check time bounding of the sequence
						if ( t < 0 ) continue;
						if ( t > sequence.getSizeT() ) continue;

						// retreive anchor
						//Point2D p = getControlPoint( t );
						MAnchor2D anchor = getControlPoint( t );
						if ( anchor==null) return;

						// create control point

						// MAnchor2D anchor = new MAnchor2D( p.getX() , p.getY() );

						// set the editability of the anchor
						boolean editAnchor = false;

						anchor.setColor( Color.gray );
						anchor.setSelectedColor( Color.gray );
						anchor.setPriority( OverlayPriority.TEXT_LOW );
						if ( currentT == t )
						{
							editAnchor = true;
							anchor.setColor( this.color );
							anchor.setSelectedColor( this.color );
							anchor.setPriority( OverlayPriority.TEXT_HIGH );
						}
						anchor.setCanBeRemoved( editAnchor );
						anchor.setEnabled( editAnchor );

						// add anchor
						newAnchorList.add( anchor );
						sequence.addOverlay( anchor );
					}

					// removes previous anchor point that are not in the newAnchorList
					for ( Anchor2D anchor : activeAnchorList )
					{		
						if ( !newAnchorList.contains( anchor ) )
						{
							sequence.removeOverlay( anchor );
						}
					}
					// place the created anchor list as the active list
					activeAnchorList = newAnchorList;

				}finally{
					sequence.endUpdate();
				}

			}
		}
		
		// paint the links between anchors.
		
		for ( int t = currentT-timeWindow ; t < currentT+timeWindow ; t++ )
		{
			Anchor2D a1 = getControlPoint( t );
			Anchor2D a2 = getControlPoint( t+1 );
			
			if ( a1!=null && a2!=null)
			{
				Line2D line = new Line2D.Double( a1.getPosition(), a2.getPosition() );
				g.draw( line );
			}
		}
		
		// draw an ellipse for the current T
		Anchor2D a = getControlPoint( currentT );
		if ( a!=null )
		{
			Ellipse2D ellipse = new Ellipse2D.Double( a.getPositionX()-3, a.getPositionY()-3, 7,7 );
			g.fill( ellipse );
		}
		
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {		
		
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		manualHelperList.remove( this );
		
	}
	
}
