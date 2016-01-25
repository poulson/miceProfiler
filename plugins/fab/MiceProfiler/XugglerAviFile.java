package plugins.fab.MiceProfiler;

import icy.sequence.Sequence;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.media.Time;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IPixelFormat.Type;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class XugglerAviFile
{
    private final String filename;
    private IContainer container;
    private int streamId;
    private long startFrame;
    private long numFrames;
    private double frameRate;
    private final long[] keyFrames;
    private IStreamCoder videoCoder;
    private IVideoResampler resampler;
    private final IConverter converter;
    private final IPacket packet;
    private int offset;
    private final boolean fast;

    public XugglerAviFile(String filename, boolean fast) throws IOException, IllegalArgumentException
    {
        super();

        this.filename = filename;
        this.fast = fast;

        container = null;
        videoCoder = null;

        container = getContainerAndCoder();

        if (videoCoder.getPixelType() != Type.BGR24)
        {
            // if this stream is not in BGR24, we're going to need to convert it.
            // The VideoResampler does that for us.
            resampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), Type.BGR24,
                    videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
            if (resampler == null)
                throw new RuntimeException("Could not create color space " + "resampler for: " + filename);
        }

        converter = ConverterFactory.createConverter("XUGGLER-BGR-24", Type.BGR24, videoCoder.getWidth(),
                videoCoder.getHeight());
        packet = IPacket.make();

        // build key frames index (sorted)
        final ArrayList<Long> keys = new ArrayList<Long>();

        while (container.readNextPacket(packet) >= 0)
            if (packet.getStreamIndex() == streamId)
                if (packet.isKey())
                    keys.add(Long.valueOf(packet.getTimeStamp()));

        final int size = keys.size();
        keyFrames = new long[size];

        for (int i = 0; i < size; i++)
            keyFrames[i] = keys.get(i).longValue();

        if (size > 0)
            startFrame = keyFrames[0];
        else
            startFrame = 0;
    }

    private IContainer getContainerAndCoder() throws IOException
    {
        if (videoCoder != null)
            videoCoder.close();
        if (container != null)
            container.close();

        // Xuggler container object
        final IContainer result = IContainer.make();

        // Open up the container
        if (result.open(filename, IContainer.Type.READ, null) < 0)
            throw new IllegalArgumentException("Could not open file.");

        // query how many streams the call to open found
        final int numStreams = result.getNumStreams();

        // and iterate through the streams to find the first video stream
        videoCoder = null;
        for (int i = 0; i < numStreams; ++i)
        {
            // Find the stream object
            final IStream stream = result.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            final IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodec().getType() == ICodec.Type.CODEC_TYPE_VIDEO)
            {
                streamId = i;
                frameRate = stream.getFrameRate().getValue();
                numFrames = stream.getNumFrames();
                videoCoder = coder;
                break;
            }
        }

        if (videoCoder == null)
            throw new IllegalArgumentException("File format not supported.");

        /*
         * Now we have found the video stream in this file. Let's open up our
         * decoder so it can do work.
         */
        if (videoCoder.open(null, null) < 0)
            throw new IOException("Could not open the file.");

        return result;
    }

    private long getKeyFrame(long frame)
    {
        // no key frame --> juste return frame
        if (keyFrames.length == 0)
            return frame;

        final int index = Arrays.binarySearch(keyFrames, frame);

        if (index < 0)
        {
            if (index == -1)
                return keyFrames[0];

            return keyFrames[-(index + 2)];
        }

        return keyFrames[index];
    }

    /**
     * Seek to desired frame.
     */
    private boolean seek(long frame)
    {
        final long adjustedFrame = frame + startFrame;

        if (container.seekKeyFrame(streamId, getKeyFrame(adjustedFrame), IURLProtocolHandler.SEEK_SET) < 0)
        {
            // seek operation not supported --> just reset container
            try
            {
                container = getContainerAndCoder();
            }
            catch (IOException e)
            {
                return false;
            }
        }

        return true;
    }

    private boolean readNext()
    {
        if (container.readNextPacket(packet) < 0)
            return false;

        offset = 0;

        return true;
    }

    private boolean decodeNext(IVideoPicture picture)
    {
        if (offset >= packet.getSize())
            return readNext();

        /*
         * Some decoders will consume data in a packet, but will not
         * be able to construct a full video picture yet. Therefore
         * you should always check if you got a complete picture
         * from the decoder
         */
        final int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);

        if (bytesDecoded < 0)
            throw new RuntimeException("Error while decoding video");

        offset += bytesDecoded;

        return true;
    }

    private IVideoPicture getNextPicture()
    {
        final IVideoPicture result = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(),
                videoCoder.getHeight());

        while (!result.isComplete())
        {
            // we are on the video stream
            if (packet.getStreamIndex() == streamId)
            {
                if (!decodeNext(result))
                    return null;
            }
            else if (!readNext())
                return null;
        }

        return result;
    }

    private BufferedImage convertPicture(IVideoPicture picture)
    {
        if (picture == null)
            return null;

        final IVideoPicture newPicture;

        /*
         * If resampler is not null, that means we didn't get the video
         * in BGR24 format and need to convert it into BGR24 format.
         */
        if (resampler != null)
        {
            // we must resample
            newPicture = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
            if (resampler.resample(newPicture, picture) < 0)
                throw new RuntimeException("Could not resample video");
        }
        else
            newPicture = picture;

        if (newPicture.getPixelType() != IPixelFormat.Type.BGR24)
            throw new RuntimeException("Could not decode video" + " as BGR 24 bit data");

        // and finally, convert the BGR24 to an Java buffered image
        return converter.toImage(newPicture);
    }

    private synchronized BufferedImage getImageInternal(int frame)
    {
        if (!fast)
        {
            try
            {
                container = getContainerAndCoder();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }

            // seek
            if (!seek(frame))
                return null;
            // and init packet
            if (!readNext())
                return null;
        }

        IVideoPicture picture = null;
        final long adjustedFrame = frame + startFrame;

        boolean done = false;
        int retry = 0;
        while (!done && (retry < 50))
        {
            while (!done)
            {
                /*
                 * We allocate a new picture to get the data out of Xuggler
                 */
                picture = getNextPicture();

                // error --> interrupt
                if (picture == null)
                    break;

                // get picture frame
                final long pictureFrame = Math.round(picture.getTimeStamp() * picture.getTimeBase().getValue()
                        * frameRate);

                // too far --> interrupt
                if (pictureFrame > adjustedFrame)
                    break;

                done = (pictureFrame == adjustedFrame);
            }

            // that means we go too far --> seek needed
            if (!done)
            {
                // seek
                if (!seek(frame))
                    return null;
                // and init packet
                if (!readNext())
                    return null;

                retry++;
                //System.out.println("retry " + retry);
            }
        }

        return convertPicture(picture);
    }

    public long getTotalNumberOfFrame()
    {
        return numFrames;
    }

    public String getTimeForFrame(int frame)
    {
        final Calendar cal = new GregorianCalendar();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        if (frameRate != 0d)
            cal.set(Calendar.SECOND, (int) (frame / frameRate));
        else
            cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return "" + DateFormat.getTimeInstance().format(cal.getTime());
    }

    public BufferedImage getImage(Time time)
    {
        if (frameRate != 0d)
            return getImage((int) (time.getSeconds() * frameRate));

        return null;
    }

    public BufferedImage getImage(int frame)
    {
        return getImageInternal(frame);
    }

    public ArrayList<BufferedImage> getImages(int index, int num)
    {
        final ArrayList<BufferedImage> result = new ArrayList<BufferedImage>();

        final int frameEnd = index + num;

        for (int i = index; i < frameEnd; i++)
            result.add(getImageInternal(i));

        return result;
    }

    public ArrayList<BufferedImage> getImages(Sequence sequence, int index, int num)
    {
        final ArrayList<BufferedImage> result = getImages(index, num);

        sequence.beginUpdate();
        try
        {
            int i = index;
            for (BufferedImage img : result)
            {
                sequence.setImage(i, 0, img);
                i++;
            }
        }
        finally
        {
            sequence.endUpdate();
        }

        return result;
    }
}
