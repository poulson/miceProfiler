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

import icy.canvas.IcyCanvas;
import icy.painter.Anchor2D;
import icy.painter.Anchor2D.Anchor2DListener;
import icy.painter.Overlay;
import icy.painter.PainterEvent;
import icy.sequence.Sequence;
import icy.type.point.Point5D;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

/**
 * Display mouse location editor
 * 
 * @author Fabrice de Chaumont
 */
public class MouseGuidePainter extends Overlay implements Anchor2DListener
{
    private Font font;

    Anchor2D M1h;
    Anchor2D M1b;
    Anchor2D M2h;
    Anchor2D M2b;

    private boolean visible;
    
    boolean trackOneMiceOnly ;

    public MouseGuidePainter(Sequence sequence, boolean trackOneMiceOnly )
    {
        //super(sequence);
    	super("Mouse Guide", OverlayPriority.SHAPE_NORMAL );

    	sequence.addOverlay( this );
//    	attachTo( sequence );

        this.trackOneMiceOnly = trackOneMiceOnly;
        font = new Font("Arial", Font.PLAIN, 5);

        M1h = new Anchor2D(100, 100);
        M1b = new Anchor2D(100, 200);
        M1h.addAnchorListener(this);
        M1b.addAnchorListener(this);               

        M2h = new Anchor2D(200, 100);
        M2b = new Anchor2D(200, 200);
        M2h.addAnchorListener(this);
        M2b.addAnchorListener(this);

        if ( trackOneMiceOnly )
        {
        	M2h.setVisible( false );
        	M2b.setVisible( false );
        	M2h.removeAnchorListener( this );
        	M2b.removeAnchorListener( this );
        }
        
        visible = true;
    }

    /**
     * @return the visible
     */
    public boolean isVisible()
    {
        return visible;
    }

    /**
     * @param visible
     *        the visible to set
     */
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    @Override
    public void mousePressed(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas) {
    	
    	//System.out.println("mouse pressed");
    	
    	if ( M1h.isSelected()
    			||
    			M1b.isSelected()
    			||
    			M2h.isSelected()
    			||
    			M2b.isSelected()    			
    			)
    		e.consume();
    }
    
      
    @Override
    public void mouseDrag( MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
    {

        if (!visible)
            return;
        
        // forward event        
        
        M1h.mouseDrag(e, imagePoint, canvas);
        M1b.mouseDrag(e, imagePoint, canvas);
        
        if ( !trackOneMiceOnly )
        {
        	M2h.mouseDrag(e, imagePoint, canvas);
        	M2b.mouseDrag(e, imagePoint, canvas);
        }
    }

    @Override
    public void mouseMove(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
    {
        if (!visible)
            return;

        // forward event
        M1h.mouseMove(e, imagePoint, canvas);
        M1b.mouseMove(e, imagePoint, canvas);
        if ( !trackOneMiceOnly )
        {
        M2h.mouseMove(e, imagePoint, canvas);
        M2b.mouseMove(e, imagePoint, canvas);
        }
    }

    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
        if (!visible)
            return;

        g.setColor(Color.RED);
        
//        g.drawLine((int) Math.round(M1h.getX()), (int) Math.round(M1h.getY()), (int) Math.round(M1b.getX()),
//                (int) Math.round(M1b.getY()));
        
        {
        	Line2D line = new Line2D.Double( M1h.getX() , M1h.getY() , M1b.getX() , M1b.getY() );
        	g.draw( line );
        }

        
        if ( !trackOneMiceOnly )
        {
        	g.setColor(Color.GREEN);

//        	g.drawLine((int) Math.round(M2h.getX()), (int) Math.round(M2h.getY()), (int) Math.round(M2b.getX()),
//        			(int) Math.round(M2b.getY()));

        	Line2D line = new Line2D.Double( M2h.getX() , M2h.getY() , M2b.getX() , M2b.getY() );
        	g.draw( line );
        }

        
        int fontSize = (int)canvas.canvasToImageDeltaX( 18 );
//        int fontSize = (int)ROI2D.canvasToImageLogDeltaX(canvas, 18 ) ;
        //int fontSize = (int)IcyCanvas.  canvasToImageLogDeltaX(canvas, 18 ) ;
		if ( fontSize < 1 ) fontSize = 1;
		Font font = new Font( "Arial" , Font.BOLD , fontSize );	
		g.setFont( font );		
        
        g.setFont(font);
        g.setColor(Color.RED);
        
        g.drawString("h", (float) M1h.getX(), (float) M1h.getY());
        g.drawString("", (float) M1b.getX(), (float) M1b.getY());

        if ( !trackOneMiceOnly )
        {
        	g.setColor(Color.GREEN);
        	g.drawString("h", (float) M2h.getX(), (float) M2h.getY());
        	g.drawString("", (float) M2b.getX(), (float) M2b.getY());
        }

        M1h.paint(g, sequence, canvas);
        M1b.paint(g, sequence, canvas);
        if ( !trackOneMiceOnly )
        {
        	M2h.paint(g, sequence, canvas);
        	M2b.paint(g, sequence, canvas);
        }
    }

    @Override
    public void positionChanged(Anchor2D source)
    {
        painterChanged();
    }
    
//    @Override
//    public void painterChanged() {
//    	super.painterChanged();
//    }


    @Override
    public void painterChanged(PainterEvent event)
    {
//    	painterChanged();
//        changed();
    }
    
    
}
