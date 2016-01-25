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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.media.Buffer;
import javax.media.util.BufferToImage;

public class FrameAccess
{

    // FramePositioningControl fpc;
    // FrameGrabbingControl fg;
    // VideoFormat vf;
    // Player p;
    XugglerAviFile aviFile;

    public FrameAccess(File file)
    {
        try
        {
            aviFile = new XugglerAviFile(file.getAbsolutePath(), false);

            // try
            // {
            // p = Manager.createRealizedPlayer(mediaLocator);
            // }
            // // catch all as we can have OutOfMemory Error here
            // catch (Throwable t)
            // {
            // System.out.println("Problem accessing file : " + file.getAbsolutePath());
            // t.printStackTrace();
            // }
            //
            // System.out.println("Player : " + p);
            // System.out.println("Duration : " + p.getDuration().getSeconds());
            //
            // // create a frame positioner
            // fpc = (FramePositioningControl)
            // p.getControl("javax.media.control.FramePositioningControl");
            //
            // long mem = SystemUtil.getJavaMaxMemory() - ( SystemUtil.getJavaTotalMemory() -
            // SystemUtil.getJavaFreeMemory() );
            // System.out.println( "Memory: " + mem/1000/1000 + " MB");
            //
            // if (fpc == null)
            // {
            // System.out.println("ERROR : Can't use a frame positioning control on this file.");
            // }
            //
            // // create a frame grabber
            // fg = (FrameGrabbingControl) p.getControl("javax.media.control.FrameGrabbingControl");
            //
            // // move to a particular frame, e.g. frame 100
            // fpc.seek(100);
            //
            // // take a snap of the current frame
            // Buffer buf = fg.grabFrame();
            //
            // // get its video format details
            // vf = (VideoFormat) buf.getFormat();
            //
            // // initialize BufferToImage with video format
            // BufferToImage bufferToImage = new BufferToImage(vf);

            // // convert the buffer to an image
            // Image im = bufferToImage.createImage(buf);
            //
            // // specify the format of desired BufferedImage
            // BufferedImage formatImg = new BufferedImage(360, 360, BufferedImage.TYPE_3BYTE_BGR);
            //
            // // convert the image to a BufferedImage
            // Graphics g = formatImg.getGraphics();
            // g.drawImage(im, 0, 0, 360, 360, null);
            // g.dispose();
        }
        catch (Exception e)
        {
            System.out.println("failed on file : " + file);
            //e.printStackTrace();
        }

    }

    // public Time getTimeForFrame(int frame)
    // {
    // return fpc.mapFrameToTime(frame);
    // }

    public int getTotalNumberOfFrame()
    {
        return (int) aviFile.getTotalNumberOfFrame();
        // int result = (int) (p.getDuration().getSeconds() * vf.getFrameRate());
        //
        // return result;
    }

    BufferToImage bufferToImage;
    BufferedImage formatImg;
    Buffer buf;
    Image im;

    // public BufferedImage getImageAt(Time time)
    // {
    //
    // return getImageAt(fpc.mapTimeToFrame(time));
    //
    // }

    public BufferedImage getImageAt(int frameNumber)
    {
    	
    	BufferedImage bi = null;
    	
    	try
    	{
    		bi = aviFile.getImage(frameNumber);
    	}
    	catch( NullPointerException e )
    	{
    		// avi file is not loaded.
    	}
    	
    	
    	return bi; //aviFile.getImage(frameNumber);
    	
        //return ImageUtil.scaleImage(aviFile.getImage(frameNumber), 360, 360);

        // fpc.seek(frameNumber);
        //
        // buf = fg.grabFrame();
        //
        // // get its video format details
        // // if videoFormat already exists, skip it !
        // if ( vf == null )
        // {
        // vf = (VideoFormat) buf.getFormat();
        //
        // }
        //
        // // initialize BufferToImage with video format
        //
        // if (bufferToImage == null)
        // {
        // bufferToImage = new BufferToImage(vf);
        // }
        //
        // // convert the buffer to an image
        //
        // im = bufferToImage.createImage(buf);
        //
        // float scaleCoef = 1f;
        //
        // // specify the format of desired BufferedImage
        // if (formatImg == null)
        // {
        // formatImg = new BufferedImage((int) (vf.getSize().width * scaleCoef),
        // (int) (vf.getSize().height * scaleCoef), BufferedImage.TYPE_3BYTE_BGR);
        // }
        //
        // // convert the image to a BufferedImage
        // Graphics g = formatImg.getGraphics();
        // g.drawImage(im, 0, 0, (int) (vf.getSize().width * scaleCoef), (int) (vf.getSize().height
        // * scaleCoef), null);
        //
        // return formatImg;

    }

    // public Time getDuration()
    // {
    // return p.getDuration();
    // }

}