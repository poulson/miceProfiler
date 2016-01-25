package plugins.fab.MiceProfiler;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.point.Point5D.Double;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class LockScrollHelperOverlay extends Overlay {

	public LockScrollHelperOverlay() {
		super("Lock Scroll Helper");
		setPriority( OverlayPriority.IMAGE_NORMAL );
	}

	boolean lockingPan=false;
	/** static font for absolute text */
	Font absoluteFont = new Font("Arial", Font.BOLD , 15 );
		
	void drawAbsoluteString( String string ,int x, int y , Graphics2D g , IcyCanvas2D canvas )
	{
		AffineTransform transform = g.getTransform();		
		g.transform( canvas.getInverseTransform() );		
		g.setFont( absoluteFont );
		g.drawString( string , x, y );
		g.setTransform( transform );
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
		
		if ( canvas instanceof IcyCanvas2D ){
			if ( lockingPan )
			{
				g.setColor( Color.red );			
				drawAbsoluteString("E: Mouse pan disabled (use numpad arrow keys)", 150, 20, g, (IcyCanvas2D)canvas );
			}else
			{				
				g.setColor( Color.gray );			
				drawAbsoluteString("E: Mouse pan enabled", 150, 20, g, (IcyCanvas2D)canvas );
			}
		}
	}
	
	@Override
	public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {
		if ( e.getKeyChar()=='e' || e.getKeyChar()=='E' )
		{
			lockingPan=!lockingPan;			
			e.consume();
			canvas.getSequence().overlayChanged( this );
		}
	}
	
	@Override
	public void mouseDrag(MouseEvent e, Double imagePoint, IcyCanvas canvas) {
		if ( lockingPan )
		{
			e.consume();
		}
	}
	
}
