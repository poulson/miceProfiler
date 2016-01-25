package plugins.fab.MiceProfiler;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

/**
 * Original code by Thomas Provoost.
 * @author Fab
 *
 */
public class XugglerUtil {

	/**
	 * Get IRational number with FPS as denominator.
	 * @param filename
	 * @return
	 */
	public static IRational getVideoFrameRate(String filename) {

		// Create a Xuggler container object
		final IContainer container = IContainer.make();

		// Open up the container
		if (container.open(filename, IContainer.Type.READ, null) < 0)
			throw new IllegalArgumentException("Could not open file.");

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// and iterate through the streams to find the first video
		// stream
		for (int i = 0; i < numStreams; ++i) {
			// Find the stream object
			IStream stream = container.getStream(i);

			// Get the pre-configured decoder that can decode this stream;
			IStreamCoder streamcoder = stream.getStreamCoder();

			if (streamcoder.getCodec().getType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				IRational res = stream.getTimeBase();
				container.close();
				return res;
			}
		}
		return null;
	}

	/**
	 * Returns all keyframes (with timestamps) in the file <code>filename</code>
	 * .
	 * 
	 * @param filename
	 * @return
	 */
	public static ArrayList<Long> getVideoKeyFrames(String filename) {
		ArrayList<Long> keyFrames = new ArrayList<Long>();

		// Create a Xuggler container object
		final IContainer container = IContainer.make();

		// Open up the container
		if (container.open(filename, IContainer.Type.READ, null) < 0)
			throw new IllegalArgumentException("Could not open file.");

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// and iterate through the streams to find the first video
		// stream
		int videoStreamId = -1;
		for (int i = 0; i < numStreams; ++i) {
			// Find the stream object
			IStream stream = container.getStream(i);

			// Get the pre-configured decoder that can decode this
			// stream;
			IStreamCoder streamcoder = stream.getStreamCoder();

			if (streamcoder.getCodec().getType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamId = i;
				break;
			}
		}

		if (videoStreamId == -1) {
			System.out.println("No Video in file");
			return null;
		}

		IPacket packet = IPacket.make();
		keyFrames = new ArrayList<Long>();

		while (container.readNextPacket(packet) >= 0) {
			if (packet.getStreamIndex() == videoStreamId && packet.isKey())
				keyFrames.add(packet.getTimeStamp());
		}
		container.close();
		return keyFrames;
	}

	/**
	 * As of now, only returns the keyframe <b>before</b> the wanted timestamp.
	 * 
	 * @param keyFrames
	 * @param timeStampWanted
	 * @return
	 */
	public static long findBestKeyFrame(ArrayList<Long> keyFrames, long timeStampWanted) {
		if (timeStampWanted < 0)
			return -1;
		long bestKeyFrame = 0;
		for (int i = 1; i < keyFrames.size(); ++i) {
			long currentKeyFrame = keyFrames.get(i);
			if (currentKeyFrame <= timeStampWanted)
				bestKeyFrame = currentKeyFrame;
		}
		return bestKeyFrame;
	}

	/**
	 * Get duration of video file (in timestamps).
	 * 
	 * @param filename
	 * @return
	 */
	public static long getVideoDuration(String filename) {
		// Create a Xuggler container object
		final IContainer container = IContainer.make();

		// Open up the container
		if (container.open(filename, IContainer.Type.READ, null) < 0)
			throw new IllegalArgumentException("Could not open file.");

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// and iterate through the streams to find the first video
		// stream
		long duration = -1;
		IStreamCoder videoCoder = null;
		for (int i = 0; i < numStreams; ++i) {
			// Find the stream object
			IStream stream = container.getStream(i);

			// Get the pre-configured decoder that can decode this
			// stream;
			IStreamCoder streamcoder = stream.getStreamCoder();

			if (streamcoder.getCodec().getType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoCoder = streamcoder;
				duration = stream.getDuration();
				break;
			}
		}
		if (videoCoder == null) {
			System.out.println("No Video in file");
			return -1;
		}
		container.close();
		return duration;
	}

	public static BufferedImage getImage(String filename, long keyFrame, long wantedImageAfterKeyFrame) {
		// Create a Xuggler container object
		IContainer container = IContainer.make();

		// Open up the container
		if (container.open(filename, IContainer.Type.READ, null) < 0)
			throw new IllegalArgumentException("Could not open file.");

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// and iterate through the streams to find the first video
		// stream
		int videoStreamId = -1;
		IStreamCoder videoCoder = null;
		for (int i = 0; i < numStreams; ++i) {
			// Find the stream object
			IStream stream = container.getStream(i);

			// Get the pre-configured decoder that can decode this
			// stream;
			IStreamCoder streamcoder = stream.getStreamCoder();

			if (streamcoder.getCodec().getType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamId = i;
				videoCoder = streamcoder;
				break;
			}
		}

		if (videoCoder == null) {
			System.out.println("null");
			return null;
		}

		/*
		 * Now we have found the video stream in this file. Let's open up our
		 * decoder so it can do work.
		 */
		if (videoCoder.open(null, null) < 0) {
			System.out.println("error open");
			return null;
		}
		IVideoResampler resampler = null;
		if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
			// if this stream is not in BGR24, we're going to need
			// to
			// convert it. The VideoResampler does that for us.
			resampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight(),
					videoCoder.getPixelType());
			if (resampler == null)
				throw new RuntimeException("could not create color space " + "resampler for: " + filename);
		}
		IPacket packet = IPacket.make();

		int res = container.seekKeyFrame(videoStreamId, keyFrame, IURLProtocolHandler.SEEK_SET);
		if (res < 0) {
			System.out.println("no keyframe found");
			return null;
		}
		int nbImagesDones = 0;
		while (container.readNextPacket(packet) >= 0) {
			/*
			 * Now we have a packet, let's see if it belongs to our video stream
			 */
			if (packet.getStreamIndex() == videoStreamId) {
				/*
				 * We allocate a new picture to get the data out of Xuggler
				 */
				IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());

				int offset = 0;
				while (offset < packet.getSize()) {
					/*
					 * Now, we decode the video, checking for any errors.
					 */
					int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
					if (bytesDecoded < 0)
						throw new RuntimeException("got error decoding video");
					offset += bytesDecoded;

					/*
					 * Some decoders will consume data in a packet, but will not
					 * be able to construct a full video picture yet. Therefore
					 * you should always check if you got a complete picture
					 * from the decoder
					 */
					if (picture.isComplete()) {
						++nbImagesDones;
						if (nbImagesDones == wantedImageAfterKeyFrame || wantedImageAfterKeyFrame == 0) {
							IVideoPicture newPic = picture;
							/*
							 * If the resampler is not null, that means we
							 * didn't get the video in BGR24 format and need to
							 * convert it into BGR24 format.
							 */
							if (resampler != null) {
								// we must resample
								newPic = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
								if (resampler.resample(newPic, picture) < 0)
									throw new RuntimeException("could not resample video");
							}
							if (newPic.getPixelType() != IPixelFormat.Type.BGR24)
								throw new RuntimeException("could not decode video" + " as BGR 24 bit data");

							// And finally, convert the BGR24 to an Java
							// buffered image
							IConverter converter = ConverterFactory.createConverter("XUGGLER-BGR-24", newPic);
							BufferedImage result = converter.toImage(newPic);
							container.close();
							return result;
						}
					}
				}
			} else {
				/*
				 * This packet isn't part of our video stream, so we just
				 * silently drop it.
				 */
				do {
				} while (false);
			}
		}
		container.close();
		return null;
	}
}
