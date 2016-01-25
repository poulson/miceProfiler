package plugins.fab.MiceProfiler;

import java.awt.event.MouseEvent;

import icy.canvas.IcyCanvas;
import icy.painter.Anchor2D;
import icy.painter.Overlay;
import icy.type.point.Point5D.Double;

public class MAnchor2D extends Anchor2D {

	/** if not enabled, can't move the anchor */
	boolean enabled = true;

	public MAnchor2D(double x, double y) {
		super(x, y);
	}

	@Override
	public void mouseDrag(MouseEvent e, Double imagePoint, IcyCanvas canvas) {
		if ( !enabled ) return;
		super.mouseDrag(e, imagePoint, canvas);
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	// TODO: sort the layer here.
	@Override
	public int compareTo(Overlay o) {
		return super.compareTo(o);
	}
}
