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
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;

/**
 * @author Fab
 */

class AviFile
{
	final FramePositioningControl fpc;
	final FrameGrabbingControl fg;
	final Player p;
	final VideoFormat vf;
	final BufferToImage bufferToImage;

	public AviFile(String path) throws NoPlayerException, CannotRealizeException, IOException, InterruptedException
	{
		final MediaLocator mediaLocator = new MediaLocator("file:" + path);

		p = Manager.createRealizedPlayer(mediaLocator);

		System.out.println("Player : " + p);
		System.out.println("Duration : " + p.getDuration().getSeconds());

		// create a frame positioner
		fpc = (FramePositioningControl) p.getControl("javax.media.control.FramePositioningControl");
		if (fpc == null)
			System.out.println("ERROR : Can't use a frame positioning control on this file.");

		// create a frame grabber
		fg = (FrameGrabbingControl) p.getControl("javax.media.control.FrameGrabbingControl");
		if (fg == null)
			System.out.println("ERROR : Can't use a frame grabbing control on this file.");

		// request that the player changes to a 'prefetched' state
		p.prefetch();
		// wait for player init
		Thread.sleep(3000);

		// get first image
		fpc.seek(0);

		final Buffer buf = fg.grabFrame(); // FIXME: problem: null pointer exception possible ici.

		// initialize video format
		vf = (VideoFormat) buf.getFormat();
		// initialize BufferToImage
		bufferToImage = new BufferToImage(vf);
	}

	public int getCurrentFrame()
	{
		return fpc.mapTimeToFrame(p.getMediaTime());
	}

	public int getTotalNumberOfFrame()
	{
		int result = (int) (p.getDuration().getSeconds() * vf.getFrameRate());

		return result;
	}

	public BufferedImage getImageAt(Time time)
	{
		return getImageAt(fpc.mapTimeToFrame(time));
	}

	public void seek(int frame)
	{
		fpc.seek(frame);
	}

	public BufferedImage getImageAt(int frameNumber)
	{
		seek(frameNumber);

		return getImage();
	}

	public BufferedImage getImage()
	{
		// grab buffer
		final Buffer buf = fg.grabFrame();

		// get image
		final Image image = bufferToImage.createImage(buf);
		final BufferedImage result = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);

		// convert the image to a BufferedImage
		final Graphics g = result.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return result;
	}

	
	public ArrayList<BufferedImage> getImages(Sequence sequence, int index, int num , boolean firstFrameLoadedToCenter )
	{
		final ArrayList<BufferedImage> result = new ArrayList<BufferedImage>();
		final int frameEnd = index + num;
				
		if ( num==0 ) return result;
		
		safeSeek(index);

		p.start();

		int frame = getCurrentFrame();
		while (frame < frameEnd)
		{			

			sequence.setImage( frame , 0 , getImage() );

			if ( firstFrameLoadedToCenter && frame == 0 ) // center viewport at load for first frame
			{
				for ( Viewer viewer : sequence.getViewers() )
				{
					if ( viewer.getCanvas() instanceof Canvas2D )
					{
						((Canvas2D)viewer.getCanvas()).centerImage();	
					}						
				}
			}

			// wait for next frame
			while (frame == getCurrentFrame())
			{
				ThreadUtil.sleep( 5 );
			}
			int newFrame = getCurrentFrame();

			safeSeek( frame+1 );
			
			frame = getCurrentFrame();
		}

		p.stop();

		return result;
	}
	
	public void safeSeek( int targetFrame )
	{
		int newFrame = getCurrentFrame();

		while ( newFrame != targetFrame )
		{
			seek(targetFrame);			

			newFrame = getCurrentFrame();							
			if ( newFrame != targetFrame )
			{
				System.out.println("Expected: " + (targetFrame) + " Current: " + newFrame + " Dif: " + (newFrame-(targetFrame)) );
			}
			
		}
	}
	
	public ArrayList<BufferedImage> getImages(int index, int num)
	{
		final ArrayList<BufferedImage> result = new ArrayList<BufferedImage>();
		final int frameEnd = index + num;

		// set position
		seek(index);

		p.start();

		int frame = getCurrentFrame();
		while (frame < frameEnd)
		{
			result.add(getImage());

			// wait for next frame
			while (frame == getCurrentFrame())
				;
			final int newFrame = getCurrentFrame();

			// frame miss ? 
			if (newFrame != (frame + 1))
				// reset position
				seek(frame + 1);

			frame = getCurrentFrame();
		}

		p.stop();

		return result;
	}

	public String getTimeForFrame(int frameNumber) {
		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, (int) ((float)frameNumber / vf.getFrameRate()) );
		cal.set(Calendar.MILLISECOND, 0);		  
		
		String returnValue = ""+ DateFormat.getTimeInstance().format( cal.getTime() );
		
		return returnValue;
	}
}
