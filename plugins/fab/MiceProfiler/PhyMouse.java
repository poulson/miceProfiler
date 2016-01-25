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
import icy.canvas.IcyCanvas2D;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.painter.Painter;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.DataType;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.phys2d.math.ROVector2f;
import net.phys2d.math.Vector2f;
import net.phys2d.raw.Body;
import net.phys2d.raw.DistanceJoint;
import net.phys2d.raw.FixedJoint;
import net.phys2d.raw.SlideJoint;
import net.phys2d.raw.World;
import net.phys2d.raw.shapes.Box;
import net.phys2d.raw.shapes.Circle;
import net.phys2d.raw.shapes.Polygon;
import net.phys2d.raw.shapes.Shape;
import net.phys2d.raw.strategies.QuadSpaceStrategy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import plugins.fab.MiceProfiler.PhyMouse.MouseInfoRecord;

/**
 * Physics model of the 2d mouse
 * 
 */
public class PhyMouse implements ActionListener, ChangeListener {

	public World world = null;
	ArrayList<Body> bodyList = new ArrayList<Body>();
	ArrayList<DistanceJoint> distanceJointList = new ArrayList<DistanceJoint>();
	ArrayList<SlideJoint> slideJointList = new ArrayList<SlideJoint>();

	public int currentFrame = 0;

	final float MASSE = 1;

	public Color color = new Color((float) Math.random(),
			(float) Math.random(), (float) Math.random());

	float SCALE = 0.22f;
	float SCALERAY = 1f;
	float GRADIENT_ENERGY_MULTIPLICATOR = 10000;
	float BINARY_ENERGY_MULTIPLICATOR = 10000;
	float EAR_ENERGY_MULTIPLICATOR = 10;
//	float GRADIENT_ENERGY_MULTIPLICATOR = 10;
//	float BINARY_ENERGY_MULTIPLICATOR = 10;
//	float EAR_ENERGY_MULTIPLICATOR = 10;


	JPanel panel = new JPanel();

	JCheckBox displayForceCheckBox = new JCheckBox("Forces", false);
	JCheckBox displayEnergyAreaCheckBox = new JCheckBox("Energy Area", false);
	JCheckBox displayBodyCenterCheckBox = new JCheckBox("Body Center", false);
	JCheckBox displayBodyShapeCheckBox = new JCheckBox("Body Shape", true);
	JCheckBox displayGlobalSplineCheckBox = new JCheckBox("Global Spline", true);
	JCheckBox displaySlideJointCheckBox = new JCheckBox("Slide Joint", true);
	JCheckBox displayDistanceJointCheckBox = new JCheckBox("Distance Joint",
			true);
	JCheckBox displayBinaryMapCheckBox = new JCheckBox("Binary Map", false);
	JCheckBox displayGradientMapCheckBox = new JCheckBox("Gradient Map", false);

	JCheckBox displayMemoryCheckBox = new JCheckBox("Track Memory", true);
	JCheckBox pauseTrackAllBox = new JCheckBox("pause track all", false);
	JCheckBox displayStepBox = new JCheckBox("display step", false);
	JTextField scaleTextField = new JTextField();

	public int SEUIL_BINARY_MAP = 30;
	public int SEUIL_EDGE_MAP = 32;

	JSpinner binaryThresholdSpinner = new JSpinner(new SpinnerNumberModel(
			SEUIL_BINARY_MAP, 0, 255, 10));

	JCheckBox useMotionPredictionCheckBox = new JCheckBox(
			"Use motion prediction", false);
	JLabel nbTotalIterationLabel = new JLabel("nb total iteration");

	JButton resultButton = new JButton("give me the results");

	HashMap<Integer, ArrayList<Body>> bodyHash = new HashMap<Integer, ArrayList<Body>>();

	JButton displayScaleBinaryButton = new JButton("display binary scales");
	JButton computeATestAnchorVectorMapButton = new JButton(
			"compute test anchor");
	//JButton applyNewScaleButton = new JButton("Apply new scale");
	
//	HashMap<Integer, Double> head1head2 = new HashMap<Integer, Double>();
//	HashMap<Integer, Double> bottom1head2 = new HashMap<Integer, Double>();
//	HashMap<Integer, Double> head1bottom2 = new HashMap<Integer, Double>();
//	HashMap<Integer, Double> bottom1head1 = new HashMap<Integer, Double>();
//	HashMap<Integer, Double> bottom2head2 = new HashMap<Integer, Double>();

	int nbTotalIteration = 0;

	/** used by painter */
	boolean motion_prediction_state = false;

	class MouseView {
		Point2D headPosition;
		Point2D bodyPosition;

		public float headAngle;
	}

	HashMap<Integer, MouseView> mouse1view = new HashMap<Integer, MouseView>();
	HashMap<Integer, MouseView> mouse2view = new HashMap<Integer, MouseView>();

	public PhyMouse(Sequence sequence) {
		this.sequence = sequence;
		if (world == null)
			world = new World(new Vector2f(0, 0), 10, new QuadSpaceStrategy(20,
					5));

		world.clear();
		world.setGravity(0, 0);

		panel = GuiUtil.generatePanel();
		panel.add(GuiUtil.besidesPanel(displayBinaryMapCheckBox,
				displayGradientMapCheckBox));
		panel.add(GuiUtil.besidesPanel(displayForceCheckBox,
				displayEnergyAreaCheckBox));
		panel.add(GuiUtil.besidesPanel(displayBodyCenterCheckBox,
				displayBodyShapeCheckBox));
		panel.add(GuiUtil.besidesPanel(displayGlobalSplineCheckBox,
				displaySlideJointCheckBox));
		panel.add(GuiUtil.besidesPanel(displayDistanceJointCheckBox,
				displayMemoryCheckBox));

		panel.add(GuiUtil.besidesPanel(useMotionPredictionCheckBox));

		panel.add(GuiUtil.besidesPanel(new JLabel("Binary Threshold:"),
				binaryThresholdSpinner));
		scaleTextField.setText("" + SCALE);
		JLabel mouseModelScaleLabel = new JLabel("Mouse Model Scale:");
		mouseModelScaleLabel.setToolTipText("Scale of the model.");
		
		
		panel.add(GuiUtil.besidesPanel(mouseModelScaleLabel, scaleTextField )); // applyNewScaleButton
		//applyNewScaleButton.addActionListener( this );

		binaryThresholdSpinner.addChangeListener(this);

		resultButton.addActionListener(this);

		computeATestAnchorVectorMapButton.addActionListener(this);
		displayScaleBinaryButton.addActionListener(this);

		displayForceCheckBox.addActionListener(this);
		displayEnergyAreaCheckBox.addActionListener(this);
		displayBodyCenterCheckBox.addActionListener(this);
		displayBodyShapeCheckBox.addActionListener(this);
		displayGlobalSplineCheckBox.addActionListener(this);
		displaySlideJointCheckBox.addActionListener(this);
		displayDistanceJointCheckBox.addActionListener(this);
		displayBinaryMapCheckBox.addActionListener(this);
		displayGradientMapCheckBox.addActionListener(this);
	}

	public void setScaleFieldEnable(boolean enable) {
		scaleTextField.setEnabled(enable);
	}

	public void generateMouse(float x, float y, float alpha) {
		SCALE = Float.parseFloat(scaleTextField.getText());
		
//		GRADIENT_ENERGY_MULTIPLICATOR = (float) (( 10f * SCALE ) / 0.22 );
//		BINARY_ENERGY_MULTIPLICATOR = (float) (( 10f * SCALE ) / 0.22 );
//		EAR_ENERGY_MULTIPLICATOR = (float) (( 10f * SCALE ) / 0.22 );
		
		mouseList.add(new Mouse(x, y, alpha));
	}

	Body copyBody(Body source) {
		Body target = null;
		if (source.getShape() instanceof Box) {
			final Box box = (Box) source.getShape();
			target = new Body("", new Box(box.getSize().getX(), box.getSize()
					.getY()), source.getMass());
		}

		if (source.getShape() instanceof Circle) {
			final Circle circle = (Circle) source.getShape();
			target = new Body("", new Circle(circle.getRadius()),
					source.getMass());
		}

		target.setDamping(source.getDamping());
		target.setPosition(source.getPosition().getX(), source.getPosition()
				.getY());
		target.setGravityEffected(false);
		target.setRotation(source.getRotation());
		target.adjustVelocity(new Vector2f(source.getVelocity().getX(), source
				.getVelocity().getY()));

		final EnergyInfo energyInfo = (EnergyInfo) source.getUserData();
		target.setUserData(energyInfo);

		return target;
	}

	public class Mouse {
		Body nose = null;

		Body earL = null;
		Body earR = null;
		Body neck = null;
		Body shoulderR = null;
		Body shoulderL = null;

		Body tommyL = null;
		Body tommyR = null;
		Body tommyB = null;
		Body assR = null;
		Body assL = null;

		Body tommyBody = null;

		Body tail1 = null;
		Body tail2 = null;
		Body tail3 = null;
		Body tail4 = null;
		Body tail5 = null;

		Body neckAttachBody = null;
		
		ArrayList<Body> bodyList = new ArrayList<Body>();

		void setPosition(Body body, float x, float y, float dx, float dy,
				float alpha) {
			x *= SCALE;
			y *= SCALE;

			Point2D point = new Point2D.Float(x, y);

			final float xx = (float) (Math.cos(alpha) * (x - 0)
					- Math.sin(alpha) * (y - 0) + 0);
			final float yy = (float) (Math.cos(alpha) * (y - 0)
					+ Math.sin(alpha) * (x - 0) + 0);

			point = new Point2D.Float(xx + dx, yy + dy);

			body.setPosition((float) point.getX(), (float) point.getY());
			body.setRotation(alpha);

		}

		ArrayList<Body> contourList = new ArrayList<Body>();

		public Body tail;
		public Body headBody;

		public Mouse(float x, float y, float alpha) {

			Body neckBody;

			tommyBody = generateBody2(BodyType.CIRCLE, 1, 60, 30, 30,
					EnergyMap.BINARY_MOUSE, false, true);
			bodyList.add(tommyBody);
			setPosition(tommyBody, 0f, 80f, x, y, alpha);

			final Body tommyR = generateBody2(BodyType.BOX, 1, 60, 20, 60,
					EnergyMap.NO_ENERGY, true, true);
			bodyList.add(tommyR);
			setPosition(tommyR, 20f, 80f, x, y, alpha);

			final Body tommyL = generateBody2(BodyType.BOX, 1, 60, 20, 60,
					EnergyMap.NO_ENERGY, true, true);
			bodyList.add(tommyL);
			setPosition(tommyL, -20f, 80f, x, y, alpha);

			final Body tommyBL = generateBody2(BodyType.BOX, 1, 20, 10, 10,
					EnergyMap.GRADIENT_MAP, true, true);
			bodyList.add(tommyBL);
			setPosition(tommyBL, -20f, 120f, x, y, alpha);

			final Body tommyBR = generateBody2(BodyType.BOX, 1, 20, 10, 10,
					EnergyMap.GRADIENT_MAP, true, true);
			bodyList.add(tommyBR);
			setPosition(tommyBR, 20f, 120f, x, y, alpha);

			final Body tommyBLC = generateBody2(BodyType.BOX, 1, 20, 10, 10,
					EnergyMap.GRADIENT_MAP, true, true);
			bodyList.add(tommyBLC);
			setPosition(tommyBLC, -20f, 60f, x, y, alpha);

			final Body tommyBRC = generateBody2(BodyType.BOX, 1, 20, 10, 10,
					EnergyMap.GRADIENT_MAP, true, true);
			bodyList.add(tommyBRC);
			setPosition(tommyBRC, 20f, 60f, x, y, alpha);

			tommyR.addExcludedBody(tommyBRC);
			tommyL.addExcludedBody(tommyBLC);
			tommyR.addExcludedBody(tommyBR);
			tommyL.addExcludedBody(tommyBL);
			tommyBody.addExcludedBody(tommyBL);
			tommyBody.addExcludedBody(tommyBR);
			tommyBody.addExcludedBody(tommyBLC);
			tommyBody.addExcludedBody(tommyBRC);

			tommyBody.addExcludedBody(tommyL);
			tommyBody.addExcludedBody(tommyR);

			world.add(new FixedJoint(tommyBody, tommyR));
			world.add(new FixedJoint(tommyBody, tommyL));

			world.add(new FixedJoint(tommyBody, tommyBL));
			world.add(new FixedJoint(tommyBody, tommyBR));

			world.add(new FixedJoint(tommyBody, tommyBLC));
			world.add(new FixedJoint(tommyBody, tommyBRC));

			// create neck

			neckBody = generateBody2(BodyType.BOX, 1, 0, 20, 60,
					EnergyMap.NO_ENERGY, true, true);
			bodyList.add(neckBody);
			setPosition(neckBody, 0f, 60f, x, y, alpha);

			neckBody.addExcludedBody(tommyBody);

			neckAttachBody = generateBody2(BodyType.CIRCLE, 1, 0, 5,
					5, EnergyMap.NO_ENERGY, true, true);
			bodyList.add(neckAttachBody);
			setPosition(neckAttachBody, 0f, 30f, x, y, alpha);

			neckAttachBody.addExcludedBody(tommyBody);
			neckAttachBody.addExcludedBody(neckBody);

			world.add(new FixedJoint(neckBody, neckAttachBody));

			generateSlideJoint(neckBody, tommyBody, 0, 4);

			headBody = generateBody2(BodyType.CIRCLE, 1, 50, 20, 20,
					EnergyMap.GRADIENT_MAP, false, true);
			bodyList.add(headBody);
			setPosition(headBody, 0f, 0f, x, y, alpha);
			headBody.setRotation((float) (Math.PI / 4f));

			generateSlideJoint(neckAttachBody, headBody, 0, 2);

			// Was no energy
			tail = generateBody2(BodyType.CIRCLE, 1, 0, 5, 5,
					EnergyMap.NO_ENERGY, true, true); // false
			bodyList.add(tail);
			setPosition(tail, 0f, 120f, x, y, alpha);
			world.add(new FixedJoint(tommyBody, tail));

			for (final Mouse mouse : mouseList) {
				// create link with existing mice
				if (mouse != this) {

					for (final Body bodyA : bodyList)
						for (final Body bodyB : mouse.bodyList) {

							final EnergyInfo eA = (EnergyInfo) bodyA
									.getUserData();
							final EnergyInfo eB = (EnergyInfo) bodyB
									.getUserData();

							if (eA.excludeFromOtherMouse
									|| eB.excludeFromOtherMouse) {
								bodyA.addExcludedBody(bodyB);
								bodyB.addExcludedBody(bodyA);

							}

						}

				}
			}

			for (final Body body : bodyList) {
				final EnergyInfo e = (EnergyInfo) body.getUserData();
				e.mouse = this;
			}

		}

	}

	enum BodyType {
		BOX, CIRCLE;
	}

	public Body generateBody2(BodyType bodyType, float mass, float ray,
			float width, float height, EnergyMap energyMap,
			boolean excludeFromOtherMouseContact,
			boolean excludeFromAttractiveMapOwner) {
		
		Body body = null;
		if (bodyType == BodyType.BOX) {
			body = new Body("", new Box(width * SCALE, height * SCALE), mass);
		}
		if (bodyType == BodyType.CIRCLE) {
			body = new Body("", new Circle(width * SCALE), mass);
		}

		body.setUserData(new EnergyInfo(ray * SCALE * SCALERAY, energyMap,
				excludeFromOtherMouseContact, excludeFromAttractiveMapOwner));
		body.setDamping(0.1f);

		body.setGravityEffected(false);
		bodyList.add(body);
		world.add(body);
		body.setCanRest(true);

		return body;
	}

	ArrayList<Mouse> mouseList = new ArrayList<Mouse>();

	class EnergyInfo {
		EnergyInfo(float ray, EnergyMap energyMap,
				boolean excludeFromOtherMouse,
				boolean excludeFromAttractiveMapOwner) {
			this.ray = ray;
			this.energyMap = energyMap;
			this.excludeFromOtherMouse = excludeFromOtherMouse;
			this.excludeFromAttractiveMapOwner = excludeFromAttractiveMapOwner;
		}

		public Object copy() {

			return null;
		}

		public boolean excludeFromAttractiveMapOwner;
		public boolean excludeFromOtherMouse;
		public float ray;
		public EnergyMap energyMap;

		/**
		 * position list for motion prediction list(0) should be speed Ã  t-1
		 * after each motion prediction, remove (0) from the list.
		 **/
		ArrayList<ROVector2f> previousPositionList = new ArrayList<ROVector2f>();

		/** speed */
		public float vx = 0;
		/** speed */
		public float vy = 0;
		/** mouse which own this energy */
		Mouse mouse;

	}

	public enum EnergyMap {
		NO_ENERGY, GRADIENT_MAP, BINARY_MOUSE, BINARY_EAR, SPECIAL_ENERGY, SPECIAL_ENERGY2;
	}

	ArrayList<Scale> binaryScaleMap = null;

	IcyBufferedImage binaryMap = null;
	BufferedImage earMap = null;
	IcyBufferedImage edgeMap = null;

	public void computeForcesMap(IcyBufferedImage imageSource) {

		//System.out.println( "refresh boolean: " + reverseThresholdBoolean );
		
		
		
		ROI2D clipROI = null;
		Sequence activeSequence = Icy.getMainInterface().getFocusedSequence();
		if (activeSequence != null)
			for (ROI roi : activeSequence.getROIs()) {
				clipROI = (ROI2D) roi;

			}

		final int imageSourceWidth = imageSource.getWidth();
		final int imageSourceHeight = imageSource.getHeight();

		if (binaryMap == null) {

			binaryMap = new IcyBufferedImage(imageSourceWidth,
					imageSourceHeight, 1, DataType.UBYTE);
		}
		if ((binaryMap.getWidth() != imageSourceWidth)
				|| (binaryMap.getHeight() != imageSourceHeight)) {
			binaryMap = new IcyBufferedImage(imageSourceWidth,
					imageSourceHeight, 1, DataType.UBYTE);
		}
		final byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);

		final byte[] imageSourceDataBuffer = imageSource.getDataXYAsByte(1);

		for (int x = 0; x < imageSourceWidth; x++)
			for (int y = 0; y < imageSourceHeight; y++) {

				int val = imageSourceDataBuffer[x + y * imageSourceWidth] & 0xFF;

				if (val < SEUIL_BINARY_MAP)
					val = 255;
				else
					val = 0;
				
				if ( reverseThresholdBoolean )
				{
					if ( val == 255 )
					{
						val = 0;
					}else
					{
						val = 255;
					}
				}
				
				if (clipROI != null) {
					if (!clipROI.contains(x, y))
						val = 0;
				}

				binaryMapDataBuffer[x + y * imageSourceWidth] = (byte) val;

			}

		// compute Edge Object EnergyMap
		if (edgeMap == null) {
			edgeMap = new IcyBufferedImage(imageSourceWidth, imageSourceHeight,
					1, DataType.UBYTE);
		}

		final int maxWidth = binaryMap.getWidth() - 1;
		final int maxHeight = binaryMap.getHeight() - 1;

		final byte[] edgeMapDataBuffer = edgeMap.getDataXYAsByte(0);

		for (int x = 1; x < maxWidth; x++)
			for (int y = 1; y < maxHeight; y++) {

				final int val1 = binaryMapDataBuffer[x + y * imageSourceWidth] & 0xFF;
				final int val2 = binaryMapDataBuffer[x + 1 + y
						* imageSourceWidth] & 0xFF;

				final int val4 = binaryMapDataBuffer[x + (y + 1)
						* imageSourceWidth] & 0xFF;

				int val = Math.abs(val1 - val2) + Math.abs(val1 - val4);

				if (val > SEUIL_EDGE_MAP) // avant 32 // avant 16 encore
					val = 255;
				else
					val = 0;

				edgeMapDataBuffer[x + y * imageSourceWidth] = (byte) val;
			}

	}

	/**
	 * True: ajoute False: remove
	 * 
	 * @param set255
	 */
	private void drawCircleInMaskMap(int centreX, int centreY, int ray,
			IcyBufferedImage maskImage, boolean set255) {
		byte val = 0;
		if (set255) {
			val = (byte) 255;
		}

		final byte[] maskMapData = maskImage.getDataXYAsByte(0);

		final int raySquare = ray * ray;
		final int width = maskImage.getWidth();
		final int height = maskImage.getHeight();

		for (int y = -ray; y <= ray; y++)
			for (int x = -ray; x <= ray; x++)
				if (x * x + y * y <= raySquare) {
					final int xx = centreX + x;
					final int yy = centreY + y;

					if (xx >= 0)
						if (yy >= 0)
							if (xx < width)
								if (yy < height) {
									maskMapData[xx + yy * width] = val;
								}

				}
	}

	public void computeForces() {

		// force location of the head
		if ( headForcedPosition[0] != null ){
			MAnchor2D pos = headForcedPosition[0];
			mouseList.get( 0 ).headBody.setPosition( (float) pos.getX() , (float)pos.getY() );
		}
		if ( headForcedPosition[1] != null ){
			MAnchor2D pos = headForcedPosition[1];
			mouseList.get( 1 ).headBody.setPosition( (float) pos.getX() , (float)pos.getY() );
		}

		
//		for (final Body body : bodyList) {
//			// first mouse
//			if( body == mouseList.get( 0 ).headBody )
//			{
//				if ( headForcedPosition[0] !=null )
//				{
//					vx = (float) (body.getPosition().getX() - headForcedPosition[0].getPositionX());
//					vy = (float) (body.getPosition().getY() - headForcedPosition[0].getPositionY());
//					vx*=BINARY_ENERGY_MULTIPLICATOR;
//					vy*=BINARY_ENERGY_MULTIPLICATOR;
//				}
//			}
//			// second mouse
//			if( body == mouseList.get( 1 ).headBody )
//			{
//				if ( headForcedPosition[1] !=null )
//				{
//					vx = (float) (body.getPosition().getX() - headForcedPosition[1].getPositionX());
//					vy = (float) (body.getPosition().getY() - headForcedPosition[1].getPositionY());
//					vx*=BINARY_ENERGY_MULTIPLICATOR;
//					vy*=BINARY_ENERGY_MULTIPLICATOR;
//				}
//			}
//		}
		
		// compute forces
		
		final IcyBufferedImage maskMap = new IcyBufferedImage(
				binaryMap.getWidth(), binaryMap.getHeight(), 1,
				DataBuffer.TYPE_BYTE);

		final byte[] maskMapData = maskMap.getDataXYAsByte(0);
		final int maskMapWidth = maskMap.getWidth();

		// apply energy.
		for (final Body body : bodyList) {

			{
				final int length = maskMapData.length;
				for (int i = 0; i < length; i++) {
					maskMapData[i] = 0;
				}
			}

			final EnergyInfo energyInfo = (EnergyInfo) body.getUserData();

			drawCircleInMaskMap((int) body.getLastPosition().getX(), (int) body
					.getLastPosition().getY(), (int) energyInfo.ray, maskMap,
					true);

			for (final Body body2 : bodyList)
				if (body != body2) {
					final EnergyInfo energyInfo2 = (EnergyInfo) body2
							.getUserData();
					if (energyInfo.energyMap == energyInfo2.energyMap)
						if (energyInfo2.excludeFromAttractiveMapOwner) {
							drawCircleInMaskMap((int) body2.getLastPosition()
									.getX(), (int) body2.getLastPosition()
									.getY(), (int) energyInfo2.ray, maskMap,
									false);

						}
				}

			if (energyInfo.energyMap == EnergyMap.BINARY_MOUSE) {
				float vx = 0;
				float vy = 0;
				float count = 0;

				final int maxX = (int) (body.getLastPosition().getX() + energyInfo.ray);
				final int maxY = (int) (body.getLastPosition().getY() + energyInfo.ray);

				final int imageWidth = binaryMap.getWidth();
				final int imageHeight = binaryMap.getHeight();
				final byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);

				for (int x = (int) (body.getLastPosition().getX() - energyInfo.ray); x < maxX; x++)
					for (int y = (int) (body.getLastPosition().getY() - energyInfo.ray); y < maxY; y++) {
						if (x >= imageWidth)
							continue;
						if (y >= imageHeight)
							continue;
						if (x < 0)
							continue;
						if (y < 0)
							continue;

						float factor = 0.5f;
						if (maskMapData[x + y * maskMapWidth] != 0)

						{
							factor = 1f;
						}

						if (maskMapData[x + y * maskMapWidth] != 0)

						{

							if ((binaryMapDataBuffer[x + y * imageWidth] & 0xFF) == 255) {
								vx += (x - body.getLastPosition().getX())
										* factor;
								vy += (y - body.getLastPosition().getY())
										* factor;

							}
							count++;
						}
					}

				if ( count > 0 )
				{
					vx /=count;
					vy /=count;
				}

				vx *= BINARY_ENERGY_MULTIPLICATOR;
				vy *= BINARY_ENERGY_MULTIPLICATOR;
				
//				vx *= BINARY_ENERGY_MULTIPLICATOR;
//				vy *= BINARY_ENERGY_MULTIPLICATOR;
//
//				energyInfo.vx = vx / 10f;
//				energyInfo.vy = vy / 10f;

				energyInfo.vx = vx ;
				energyInfo.vy = vy ;

				body.setForce(vx, vy);
			}

			if (energyInfo.energyMap == EnergyMap.GRADIENT_MAP) {
				float vx = 0;
				float vy = 0;
				float count = 0;

				final int imageWidth = edgeMap.getWidth();
				final int imageHeight = edgeMap.getHeight();

				final byte[] edgeMapDataBuffer = edgeMap.getDataXYAsByte(0);

				final int maxX = (int) (body.getLastPosition().getX() + energyInfo.ray);
				final int maxY = (int) (body.getLastPosition().getY() + energyInfo.ray);

				for (int x = (int) (body.getLastPosition().getX() - energyInfo.ray); x < maxX; x++)
					for (int y = (int) (body.getLastPosition().getY() - energyInfo.ray); y < maxY; y++) {
						// if ( mask.contains( x ,y ) )

						if (x >= imageWidth)
							continue;
						if (y >= imageHeight)
							continue;
						if (x < 0)
							continue;
						if (y < 0)
							continue;

						if (maskMapData[x + y * maskMapWidth] != 0) {

							if ((edgeMapDataBuffer[x + y * imageWidth] & 0xFF) != 0) {

								vx += x - body.getLastPosition().getX();
								vy += y - body.getLastPosition().getY();
							}
							count++;
						}
					}

				if ( count > 0 )
				{
					vx /=count;
					vy /=count;
				}
				
				vx *= GRADIENT_ENERGY_MULTIPLICATOR; // *50
				vy *= GRADIENT_ENERGY_MULTIPLICATOR;

//				energyInfo.vx = vx / 10f;
//				energyInfo.vy = vy / 10f;

				energyInfo.vx = vx ;
				energyInfo.vy = vy ;

				body.setForce(vx, vy);
			}

		}

		// remove all forced positions.
		headForcedPosition[0] = null;
		headForcedPosition[1] = null;
	}

	Sequence sequence = null;

	class MouseInfoRecord {
		Point2D headPosition;
		Point2D tailPosition;
		Point2D bodyPosition;
		Point2D neckPosition; // body that links to the head to compute head angle.
	}

	public void swapIdentityRecordFromTToTheEnd( int time )
	{
		// look for maximum time recorded
		int maxT = 0;
		for ( int i : mouseARecord.keySet() )
		{
			if ( i > maxT ) maxT = i;
		}
		for ( int i : mouseBRecord.keySet() )
		{
			if ( i > maxT ) maxT = i;
		}
		
		// swap data
		for ( int t = time ; t <= maxT ; t++ )
		{
			MouseInfoRecord recA = mouseARecord.get( t );
			MouseInfoRecord recB = mouseBRecord.get( t );
			
			mouseARecord.put( t , recB );
			mouseBRecord.put( t , recA );
		}
		
	}
	
	HashMap<Integer, MouseInfoRecord> mouseARecord = new HashMap<Integer, MouseInfoRecord>();
	HashMap<Integer, MouseInfoRecord> mouseBRecord = new HashMap<Integer, MouseInfoRecord>();

	public void recordMousePosition(int currentFrame) {

		// record mouse A
		{
			final MouseInfoRecord mouseAInfo = new MouseInfoRecord();
			mouseAInfo.headPosition = new Point2D.Float(
					mouseList.get(0).headBody.getPosition().getX(),
					mouseList.get(0).headBody.getPosition().getY());
			mouseAInfo.bodyPosition = new Point2D.Float(
					mouseList.get(0).tommyBody.getPosition().getX(),
					mouseList.get(0).tommyBody.getPosition().getY());
			mouseAInfo.tailPosition = new Point2D.Float(mouseList.get(0).tail
					.getPosition().getX(), mouseList.get(0).tail.getPosition()
					.getY());
			mouseAInfo.neckPosition = new Point2D.Float(mouseList.get(0).neckAttachBody
					.getPosition().getX(), mouseList.get(0).neckAttachBody.getPosition()
					.getY());
			mouseARecord.put(currentFrame, mouseAInfo);
		}
		// record mouse B
		if (mouseList.size() > 1) // is there 2 mice ?
		{
			final MouseInfoRecord mouseBInfo = new MouseInfoRecord();
			mouseBInfo.headPosition = new Point2D.Float(
					mouseList.get(1).headBody.getPosition().getX(),
					mouseList.get(1).headBody.getPosition().getY());
			mouseBInfo.bodyPosition = new Point2D.Float(
					mouseList.get(1).tommyBody.getPosition().getX(),
					mouseList.get(1).tommyBody.getPosition().getY());
			mouseBInfo.tailPosition = new Point2D.Float(mouseList.get(1).tail
					.getPosition().getX(), mouseList.get(1).tail.getPosition()
					.getY());
			mouseBInfo.neckPosition = new Point2D.Float(mouseList.get(1).neckAttachBody
					.getPosition().getX(), mouseList.get(1).neckAttachBody.getPosition()
					.getY());
			mouseBRecord.put(currentFrame, mouseBInfo);
		}
	}

	public void worldStep(int currentFrame) {

		motion_prediction_state = false;

		world.step();
		nbTotalIteration++;

		while (pauseTrackAllBox.isSelected()) {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

		recordMousePosition(currentFrame);

		if (displayStepBox.isSelected()) {
			sequence.painterChanged(null);

		}
	}

	/**
	 * Applique la motion prediction et passe l'inertie a 0.
	 */
	public void applyMotionPrediction() {
		if (!useMotionPredictionCheckBox.isSelected())
			return;

		motion_prediction_state = true;
		// record and motion prediction.

		for (final Mouse mouse : mouseList) {
			for (final Body body : mouse.bodyList) {
				// record current position.
				final EnergyInfo eInfo = (EnergyInfo) body.getUserData();
				// copy the object.
				final ROVector2f vCopy = new Vector2f(body.getLastPosition()
						.getX(), body.getLastPosition().getY());

				body.setForce(0, 0);

				eInfo.previousPositionList.add(vCopy);

				// compute la prediction (t) - (t-1)
				if (eInfo.previousPositionList.size() > 1) // no prediction for
															// first frame.
				{

					final Vector2f newVelocity = new Vector2f(
							10f * (eInfo.previousPositionList.get(1).getX() - eInfo.previousPositionList
									.get(0).getX()),
							10f * (eInfo.previousPositionList.get(1).getY() - eInfo.previousPositionList
									.get(0).getY()));

					body.adjustVelocity(newVelocity);
					eInfo.previousPositionList
							.remove(eInfo.previousPositionList.get(0));
				}

			}
		}

		for (int i = 0; i < 6; i++) {
			while (pauseTrackAllBox.isSelected()) {
				try {
					Thread.sleep(100);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}

			world.step();

			if (displayStepBox.isSelected()) {
				// sequence.refreshPainter();
				// try {
				// Thread.sleep( 200 );
				// } catch (InterruptedException e) {
				//
				// e.printStackTrace();
				// }

				// if ( i == 5 )
				// try {
				// Thread.sleep( 1000 );
				// } catch (InterruptedException e) {
				//
				// e.printStackTrace();
				// }
			}

		}

		for (final Body body : bodyList) {
			body.adjustVelocity(new Vector2f(0, 0));
		}

	}

	public SlideJoint generateSlideJoint(Body bodyA, Body bodyB, float min,
			float max) {
		final SlideJoint slideJoint = new SlideJoint(bodyA, bodyB,
				new Vector2f(0, 0), new Vector2f(0, 0f), min, max, 0);
		slideJointList.add(slideJoint);
		world.add(slideJoint);
		return slideJoint;
	}

	public DistanceJoint generateDistanceJoint(Body bodyA, Body bodyB) {
		final Point2D pointA = new Point2D.Float(
				bodyA.getLastPosition().getX(), bodyA.getLastPosition().getY());
		final Point2D pointB = new Point2D.Float(
				bodyB.getLastPosition().getX(), bodyB.getLastPosition().getY());
		final float distance = (float) pointA.distance(pointB);
		final DistanceJoint distanceJoint = new DistanceJoint(bodyA, bodyB,
				new Vector2f(0, 0), new Vector2f(0, 0), distance);
		distanceJointList.add(distanceJoint);
		world.add(distanceJoint);
		return distanceJoint;
	}

	public DistanceJoint generateDistanceJoint(Body bodyA, Body bodyB,
			float distance) {
		final DistanceJoint distanceJoint = new DistanceJoint(bodyA, bodyB,
				new Vector2f(0, 0), new Vector2f(0, 0), distance);
		distanceJointList.add(distanceJoint);
		world.add(distanceJoint);
		return distanceJoint;
	}

	public void paint(Graphics g, IcyCanvas canvas) {
		if (!(canvas instanceof IcyCanvas2D))
			return;
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setStroke(new BasicStroke(0.3f));

		// g2.setStroke( new BasicStroke((float) ROI2D.canvasToImageLogDeltaX(
		// canvas, 5 ) ) );

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.5f));
		if (displayBinaryMapCheckBox.isSelected()) {

			if (binaryMap != null)
				g2.drawImage(binaryMap.convertToBufferedImage(null), null, 0, 0);

		}

		if (displayGradientMapCheckBox.isSelected()) {
			if (edgeMap != null)
				g2.drawImage(edgeMap.convertToBufferedImage(null), null, 0, 0);
		}

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				1.0f));

		// paint SlideJoint
		if (displaySlideJointCheckBox.isSelected()) {
			g2.setColor(Color.orange);
			for (final SlideJoint slideJoint : slideJointList) {
				final Line2D line = new Line2D.Float(slideJoint.getBody1()
						.getLastPosition().getX(), slideJoint.getBody1()
						.getLastPosition().getY(), slideJoint.getBody2()
						.getLastPosition().getX(), slideJoint.getBody2()
						.getLastPosition().getY());
				g2.draw(line);
			}
		}

		// paint DistanceJoint
		if (displayDistanceJointCheckBox.isSelected()) {
			g2.setColor(Color.YELLOW);
			for (final DistanceJoint distanceJoint : distanceJointList) {
				final Line2D line = new Line2D.Float(distanceJoint.getBody1()
						.getLastPosition().getX(), distanceJoint.getBody1()
						.getLastPosition().getY(), distanceJoint.getBody2()
						.getLastPosition().getX(), distanceJoint.getBody2()
						.getLastPosition().getY());
				g2.draw(line);
			}
		}

		// paint Bodie's center
		if (displayBodyCenterCheckBox.isSelected()) {
			g2.setColor(Color.blue);
			for (final Body body : bodyList) {
				final Ellipse2D ellipse = new Ellipse2D.Float(body
						.getLastPosition().getX() - 1.5f, body
						.getLastPosition().getY() - 1.5f, 3, 3);
				g2.draw(ellipse);
			}
		}

		// paint Bodie's shape ( if any )
		if (displayBodyShapeCheckBox.isSelected()) {
			g2.setColor(Color.white);
			for (final Body body : bodyList) {
				final Shape shape = body.getShape();
				if (shape != null)
					if (shape instanceof Polygon) {
						final ROVector2f[] vert = ((Polygon) shape)
								.getVertices();
						// if ( vert != null )
						for (int i = 0; i < vert.length - 1; i++) {
							final AffineTransform transform = g2.getTransform();
							g2.translate(body.getLastPosition().getX(), body
									.getLastPosition().getY());
							g2.rotate(body.getRotation());

							final Line2D line2D = new Line2D.Float(
									vert[i].getX(), vert[i].getY(),
									vert[i + 1].getX(), vert[i + 1].getY());
							g2.draw(line2D);

							g2.setTransform(transform);
						}
						final AffineTransform transform = g2.getTransform();
						g2.translate(body.getLastPosition().getX(), body
								.getLastPosition().getY());
						g2.rotate(body.getRotation());
						final Line2D line2D = new Line2D.Float(vert[0].getX(),
								vert[0].getY(), vert[vert.length - 1].getX(),
								vert[vert.length - 1].getY());
						g2.draw(line2D);
						g2.setTransform(transform);

					}
				if (shape instanceof Box) {
					final Box maBox = (Box) shape;

					final AffineTransform transform = g2.getTransform();
					g2.translate(body.getLastPosition().getX(), body
							.getLastPosition().getY());
					g2.rotate(body.getRotation());
					final Rectangle2D rect2D = new Rectangle2D.Float(-maBox
							.getSize().getX() / 2f,
							-maBox.getSize().getY() / 2f, maBox.getSize()
									.getX(), maBox.getSize().getY());
					g2.draw(rect2D);
					g2.setTransform(transform);
				}
				if (shape instanceof Circle) {
					final Circle maCircle = (Circle) shape;

					final AffineTransform transform = g2.getTransform();
					g2.translate(body.getLastPosition().getX(), body
							.getLastPosition().getY());
					g2.rotate(body.getRotation());
					final Ellipse2D ellipse2D = new Ellipse2D.Double(
							-maCircle.getRadius(), -maCircle.getRadius(),
							maCircle.getRadius() * 2, maCircle.getRadius() * 2);

					g2.draw(ellipse2D);
					g2.setTransform(transform);
				}

			}
		}

		// paint Energy area
		{
			for (final Body body : bodyList) {
				g2.setStroke(new BasicStroke(0.5f));
				final EnergyInfo energyInfo = (EnergyInfo) body.getUserData();
				Color color = Color.black;
				if (energyInfo.energyMap == EnergyMap.BINARY_MOUSE)
					color = Color.green;
				if (energyInfo.energyMap == EnergyMap.BINARY_EAR)
					color = Color.red;
				if (energyInfo.energyMap == EnergyMap.GRADIENT_MAP)
					color = Color.pink;
				g2.setColor(color);

				if (displayEnergyAreaCheckBox.isSelected())
					if (energyInfo.energyMap != EnergyMap.NO_ENERGY) {
						final Ellipse2D ellipse = new Ellipse2D.Float(body
								.getLastPosition().getX() - energyInfo.ray,
								body.getLastPosition().getY() - energyInfo.ray,
								energyInfo.ray * 2 + 1, energyInfo.ray * 2 + 1);
						g2.draw(ellipse);
					}

				boolean displayForce = displayForceCheckBox.isSelected();
				if (displayBinaryMapCheckBox.isSelected()
						&& energyInfo.energyMap != EnergyMap.BINARY_MOUSE)
					displayForce = false;
				if (displayGradientMapCheckBox.isSelected()
						&& energyInfo.energyMap != EnergyMap.GRADIENT_MAP)
					displayForce = false;
				if (displayForce) {
					final Line2D energyVector = new Line2D.Float(body
							.getLastPosition().getX(), body.getLastPosition()
							.getY(), body.getLastPosition().getX()
							+ energyInfo.vx, body.getLastPosition().getY()
							+ energyInfo.vy);
					g2.draw(energyVector);
				}
			}
		}

		if (displayGlobalSplineCheckBox.isSelected()) {
			g2.setColor(Color.blue);
			for (final Mouse mouse : mouseList) {
				// if ( mouse.tommyBody != null )
				// {
				// g2.drawString( ""+ (mouseList.indexOf( mouse ) + 1),
				// mouse.tommyBody.getLastPosition().getX() -5,
				// mouse.tommyBody.getLastPosition().getY() -5
				// );
				// }

				// for ( int i = 0 ; i < mouse.contourList.size() ; i++ )
				// {
				// Body ba = mouse.contourList.get( i );
				// Body bb = mouse.contourList.get( ( i + 1 ) %
				// mouse.contourList.size() );
				// Line2D line = new Line2D.Float ( ba.getLastPosition().getX()
				// ,
				// ba.getLastPosition().getY() , bb.getLastPosition().getX() ,
				// bb.getLastPosition().getY() );
				// g2.draw( line );
				// }
			}

		}

		// ROVector2f maxVelocity = bodyList.get(0).getVelocity();
		// for ( Body body : bodyList )
		// {
		// if ( body.getVelocity().length() > maxVelocity.length() ) maxVelocity
		// =
		// body.getVelocity();
		// }
		// g2.drawString("speed = " + maxVelocity.length() ,
		// bodyList.get(0).getPosition().getX() ,
		// bodyList.get(0).getPosition().getY() );

		// paint body angle
		// for ( Body body : bodyList )
		// {
		// g2.setStroke( new BasicStroke( 0.5f ) );
		// g2.setColor( Color.cyan );
		//
		// // Ellipse2D ellipse = new Ellipse2D.Float(
		// body.getLastPosition().getX()-energyInfo.ray
		// , body.getLastPosition().getY()-energyInfo.ray , energyInfo.ray*2+1,
		// energyInfo.ray*2+1
		// );
		// // g2.draw( ellipse );
		// // Line2D energyVector = new Line2D.Float (
		// //
		// // body.getLastPosition().getX() , body.getLastPosition().getY() ,
		// // body.getLastPosition().getX() + (float)Math.cos(
		// body.getRotation() *10f ) ,
		// body.getLastPosition().getY() + (float)Math.sin( body.getRotation() *
		// 10f )
		// // );
		// // g2.draw( energyVector );
		// }

		// ***********************************************
		// *********************************************** PAINT CURRENT FRAME
		// ***********************************************

		g2.setColor(Color.blue);

		if (motion_prediction_state) {
			g2.setColor(Color.white);
			g2.drawString("Motion prediction (movie slowed)", 20, 200);
		}

		g2.setColor(Color.white);
		//g2.drawString("world E: " + world.getTotalEnergy(), 20, 220);

		// affichage au temps t (passe egalement) de mouseA et mouseB
		// Mouse A
		if (displayMemoryCheckBox.isSelected()) {
			{
				final MouseInfoRecord mouseAInfo = mouseARecord
						.get(currentFrame);
				if (mouseAInfo != null) {
					g2.setColor(Color.red);
					// g2.drawOval((int) mouseAInfo.headPosition.getX() - 5,
					// (int) mouseAInfo.headPosition.getY() - 5, 10, 10);
					// g2.drawOval((int) mouseAInfo.bodyPosition.getX() - 10,
					// (int) mouseAInfo.bodyPosition.getY() - 10, 20,
					// 20);
					// g2.drawOval((int) mouseAInfo.tailPosition.getX() - 2,
					// (int) mouseAInfo.tailPosition.getY() - 2, 4, 4);

					Ellipse2D ellipseHead = new Ellipse2D.Double(
							mouseAInfo.headPosition.getX() - 22 * SCALE,
							mouseAInfo.headPosition.getY() - 22 * SCALE,
							45 * SCALE, 45 * SCALE);

					Ellipse2D ellipseBody = new Ellipse2D.Double(
							(mouseAInfo.bodyPosition.getX() - 45 * SCALE),
							(mouseAInfo.bodyPosition.getY() - 45 * SCALE),
							(90 * SCALE), (90 * SCALE));

					Ellipse2D ellipseTail = new Ellipse2D.Double(
							(mouseAInfo.tailPosition.getX() - 10 * SCALE),
							(mouseAInfo.tailPosition.getY() - 10 * SCALE),
							(20 * SCALE), (20 * SCALE));

					g2.draw(ellipseHead);
					g2.draw(ellipseBody);
					g2.draw(ellipseTail);

					// g2.drawOval(
					// (int) (mouseAInfo.headPosition.getX() - 22* SCALE ),
					// (int) (mouseAInfo.headPosition.getY() - 22 *SCALE ),
					// (int) (45*SCALE ),
					// (int) (45*SCALE )
					// );
					// g2.drawOval(
					// (int) (mouseAInfo.bodyPosition.getX() - 45*SCALE ),
					// (int) (mouseAInfo.bodyPosition.getY() - 45*SCALE ),
					// (int) (90*SCALE ),
					// (int) (90*SCALE )
					// );
					// g2.drawOval(
					// (int) (mouseAInfo.tailPosition.getX() - 10*SCALE),
					// (int) (mouseAInfo.tailPosition.getY() - 10*SCALE),
					// (int) (20*SCALE),
					// (int) (20*SCALE)
					// );
				}
			}
			// Mouse B
			{
				final MouseInfoRecord mouseBInfo = mouseBRecord
						.get(currentFrame);
				if (mouseBInfo != null) {
					g2.setColor(Color.green);
					// g2.drawOval(
					// (int) mouseBInfo.headPosition.getX() - 5, (int)
					// mouseBInfo.headPosition.getY() - 5, 10, 10);
					// g2.drawOval(
					// (int) mouseBInfo.bodyPosition.getX() - 10, (int)
					// mouseBInfo.bodyPosition.getY() - 10, 20,20);
					// g2.drawOval(
					// (int) mouseBInfo.tailPosition.getX() - 2, (int)
					// mouseBInfo.tailPosition.getY() - 2, 4, 4
					// );

					Ellipse2D ellipseHead = new Ellipse2D.Double(
							(mouseBInfo.headPosition.getX() - 22 * SCALE),
							(mouseBInfo.headPosition.getY() - 22 * SCALE),
							(45 * SCALE), (45 * SCALE));

					Ellipse2D ellipseBody = new Ellipse2D.Double(
							(mouseBInfo.bodyPosition.getX() - 45 * SCALE),
							(mouseBInfo.bodyPosition.getY() - 45 * SCALE),
							(90 * SCALE), (90 * SCALE));

					Ellipse2D ellipseTail = new Ellipse2D.Double(
							(mouseBInfo.tailPosition.getX() - 10 * SCALE),
							(mouseBInfo.tailPosition.getY() - 10 * SCALE),
							(20 * SCALE), (20 * SCALE));

					g2.draw(ellipseHead);
					g2.draw(ellipseBody);
					g2.draw(ellipseTail);

					// g2.drawOval(
					// (int) (mouseBInfo.headPosition.getX() - 22* SCALE ),
					// (int) (mouseBInfo.headPosition.getY() - 22* SCALE ),
					// (int) (45*SCALE ),
					// (int) (45*SCALE )
					// );
					// g2.drawOval(
					// (int) (mouseBInfo.bodyPosition.getX() - 45*SCALE ),
					// (int) (mouseBInfo.bodyPosition.getY() - 45*SCALE ),
					// (int) (90*SCALE ),
					// (int) (90*SCALE )
					// );
					// g2.drawOval(
					// (int) (mouseBInfo.tailPosition.getX() - 10*SCALE),
					// (int) (mouseBInfo.tailPosition.getY() - 10*SCALE),
					// (int) (20*SCALE),
					// (int) (20*SCALE)
					// );
				}
			}
		}

		if (false)
			if (displayMemoryCheckBox.isSelected()) {

				g2.setColor(Color.white);
				// g2.fillRect( 0 , 0 , sequence.getWidth() ,
				// sequence.getHeight() );
				// g2.setColor( Color.black );

				// for ( int t = 0 ; t < sequence.getLength() ; t++ )
				{
					final ArrayList<Body> bodyCurrentList = bodyHash
							.get(currentFrame);
					// ArrayList<Body> bodyCurrentList = bodyHash.get( t );
					if (bodyCurrentList != null)
						for (final Body body : bodyCurrentList) {
							final EnergyInfo e = (EnergyInfo) body
									.getUserData();
							if (mouseList.get(0) == e.mouse) {
								g2.setColor(Color.red);
							} else {
								g2.setColor(Color.green);
							}

							final Shape shape = body.getShape();
							if (shape instanceof Box) {
								final Box maBox = (Box) shape;

								final AffineTransform transform = g2
										.getTransform();
								g2.translate(body.getLastPosition().getX(),
										body.getLastPosition().getY());
								g2.rotate(body.getRotation());
								final Rectangle2D rect2D = new Rectangle2D.Float(
										-maBox.getSize().getX() / 2f, -maBox
												.getSize().getY() / 2f, maBox
												.getSize().getX(), maBox
												.getSize().getY());
								g2.draw(rect2D);
								g2.setTransform(transform);
							}
							if (shape instanceof Circle) {
								final Circle maCircle = (Circle) shape;

								final AffineTransform transform = g2
										.getTransform();
								g2.translate(body.getLastPosition().getX(),
										body.getLastPosition().getY());
								g2.rotate(body.getRotation());
								final Ellipse2D ellipse2D = new Ellipse2D.Double(
										-maCircle.getRadius(),
										-maCircle.getRadius(),
										maCircle.getRadius() * 2,
										maCircle.getRadius() * 2);

								g2.draw(ellipse2D);
								g2.setTransform(transform);
							}
						}
				}

			}

	}

	public ArrayList<Body> getBodyList() {
		return bodyList;
	}

	public JPanel getPanel() {
		return panel;
	}

	public double convertScaleX(double x) {
		x = x - 150;
		x *= (35. / 100.);
		return x;
	}

	public double convertScaleY(double y) {
		y = y - 50;
		y *= (50. / 200.);
		return y;
	}

	public void writeXMLResult() {
		// Should save
		// head position
		// tommy position
		// tail position

		System.out.println();
		System.out.println("Body Mouse X 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleX(mouseView.bodyPosition.getX())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("Body Mouse Y 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleY(mouseView.bodyPosition.getY())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("head Mouse X 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleX(mouseView.headPosition.getX())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("head Mouse Y 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse1view.get(i);
			System.out.print(convertScaleY(mouseView.headPosition.getY())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("Head Mouse Angle 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse1view.get(i);
			System.out.print((int) (180 * mouseView.headAngle / 3.14f) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		// souris 2

		System.out.println();
		System.out.println("Body Mouse X 2:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleX(mouseView.bodyPosition.getX())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("Body Mouse Y 2:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleY(mouseView.bodyPosition.getY())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("head Mouse X 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleX(mouseView.headPosition.getX())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println("head Mouse Y 1:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse2view.get(i);
			System.out.print(convertScaleY(mouseView.headPosition.getY())
					+ ", ");
			if (i % 10 == 0)
				System.out.println();
		}

		System.out.println();
		System.out.println("Head Mouse Angle 2:");
		for (int i = 0; i < sequence.getLength(); i++) {
			final MouseView mouseView = mouse2view.get(i);
			System.out.print((int) (180 * mouseView.headAngle / 3.14f) + ", ");
			if (i % 10 == 0)
				System.out.println();
		}

	}

	class Scale implements Painter {
		String name;
		int height;
		int width;
		float value[];
		float barycenterX[];
		float barycenterY[];
		int scale;

		public Scale(int width, int height, int scale) {
			this.scale = scale;
			this.width = width;
			this.height = height;
			value = new float[width * height];
			barycenterX = new float[width * height];
			barycenterY = new float[width * height];
		}

		float getScaleFactor() {
			float scaleFactor = 1;
			for (int i = 0; i < scale; i++) {
				scaleFactor *= 2;
			}

			return (float) scaleFactor;
		}

		void sendToDisplay() {
			final IcyBufferedImage image = new IcyBufferedImage(width, height, 1, DataBuffer.TYPE_FLOAT);
			final float[] data = image.getDataXYAsFloat(0);

			for (int i = 0; i < value.length; i++) {
				data[i] = value[i];
			}

			final Sequence sequence = new Sequence( image);
			sequence.setName("Scale " + scale + " resol div par "
					+ getScaleFactor());
			sequence.addPainter(this);
			Icy.addSequence(sequence);

		}

		@Override
		public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mouseClick(MouseEvent e, Point2D p, IcyCanvas canvas) {
			final int x = (int) p.getX();
			final int y = (int) p.getY();
			System.out
					.println("Point : x:" + x + " y:" + y + " bx:"
							+ barycenterX[x + y * width] + " by:"
							+ barycenterY[x + y * width] + " v:"
							+ value[x + y * width]);
		}

		@Override
		public void mouseDrag(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void mouseMove(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			g.setStroke(new BasicStroke(0.1f));
			final Line2D line = new Line2D.Float();
			g.setColor(Color.red);

			// float squareScale = (scale-1) * (scale-1);

			final float scaleDivider = getScaleFactor();

			for (int x = 0; x < width; x += 1)
				for (int y = 0; y < height; y += 1) {
					final float xx = barycenterX[x + y * width]
							/ (float) scaleDivider;
					final float yy = barycenterY[x + y * width]
							/ (float) scaleDivider;

					if ((value[x + y * width] != 0)) {
						g.setColor(Color.yellow);
						line.setLine(0.5 + x + 0.25, 0.5 + y + 0.25,
								0.5 + x - 0.25, 0.5 + y - 0.25);
						g.draw(line);
						line.setLine(0.5 + x + 0.25, 0.5 + y - 0.25,
								0.5 + x - 0.25, 0.5 + y + 0.25);
						g.draw(line);

						g.setColor(Color.red);

						line.setLine(0.5 + x, 0.5 + y, xx + 0.25, yy + 0.25);

						g.draw(line);
					} else {

					}

				}

		}

		@Override
		public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		@Override
		public void mousePressed(MouseEvent e, Point2D imagePoint,
				IcyCanvas canvas) {

		}

		@Override
		public void mouseReleased(MouseEvent e, Point2D imagePoint,
				IcyCanvas canvas) {

		}

	}

	class Ancre2 implements Painter {

		int mapWidth = 0;
		int mapHeight = 0;
		float centerX;
		float centerY;
		float ray;
		IcyBufferedImage carteAncre = null;
		int minX;
		int maxX;
		int minY;
		int maxY;
		ArrayList<Rectangle2D> listRect = new ArrayList<Rectangle2D>();

		public Ancre2(int mapWidth, int mapHeight, float centerX,
				float centerY, float ray) {
			this.mapWidth = mapWidth;
			this.mapHeight = mapHeight;
			this.centerX = centerX;
			this.centerY = centerY;
			this.ray = ray;

			minX = (int) (centerX - ray);
			maxX = (int) (centerX + ray);
			minY = (int) (centerY - ray);
			maxY = (int) (centerY + ray);

			carteAncre = new IcyBufferedImage(mapWidth, mapHeight, 1,
					DataBuffer.TYPE_BYTE);

			// construction de la carte encre.
			final byte[] data = carteAncre.getDataXYAsByte(0);

			for (int x = minX; x < maxX; x++)
				for (int y = minY; y < maxY; y++) {

					final float dis = (x - centerX) * (x - centerX)
							+ (y - centerY) * (y - centerY);
					if (dis < ray * ray) {
						data[x + y * mapWidth] = (byte) 255;
					}

				}

		}

		Sequence sequence;

		public void displayAsSequence() {

			sequence = new Sequence(carteAncre);
			sequence.setName("Map Ancre");
			sequence.addPainter(this);
			Icy.addSequence(sequence);

		}

		public void refreshDisplay() {

			sequence.painterChanged(null);

		}

		@Override
		public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		}

		@Override
		public void mouseClick(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void mouseDrag(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void mouseMove(MouseEvent e, Point2D p, IcyCanvas canvas) {
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			g.setStroke(new BasicStroke(1f));

			g.setColor(new Color((float) Math.random(), (float) Math.random(),
					(float) Math.random(), (float) 0.5f));
			for (final Rectangle2D rect : listRect) {
				g.draw(rect);
			}
		}

		Color color = Color.yellow;

		public void setColor(Color color) {
			this.color = color;
		}

		@Override
		public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		@Override
		public void mousePressed(MouseEvent e, Point2D imagePoint,
				IcyCanvas canvas) {

		}

		@Override
		public void mouseReleased(MouseEvent e, Point2D imagePoint,
				IcyCanvas canvas) {

		}

	}

	public void computeATestAnchorVectorMap(boolean DISPLAY, Ancre2 ancre) {
		final Sequence sequenceFocused = Icy.getMainInterface()
				.getFocusedSequence();

		// creation d'une ancre avec sa carte

		final Ancre2 a1 = ancre;

		if (DISPLAY) {
			a1.displayAsSequence();
			sequenceFocused.addPainter(a1);
		}

		// calcul du vecteur moyen avec methode lente et basique

		{
			int count = 0;
			float vx = 0;
			float vy = 0;
			final byte[] dataAncre = a1.carteAncre.getDataXYAsByte(0);
			final byte[] dataCarte = sequenceFocused.getDataXYAsByte(0, 0, 0);

			for (int x = a1.minX; x < a1.maxX; x++)
				for (int y = a1.minY; y < a1.maxY; y++) {
					if (dataAncre[x + y * a1.mapWidth] != 0) {
						final float vectX = (x - a1.centerX)
								* (dataCarte[x + y * a1.mapWidth] & 0xFF);
						final float vectY = (y - a1.centerY)
								* (dataCarte[x + y * a1.mapWidth] & 0xFF);
						vx += vectX;
						vy += vectY;
						count++;
					}
				}

			System.out.println("nb iteration simple : " + count);
			System.out.println("Calcul du vecteur via methode simple : vx = "
					+ vx + " vy = " + vy);
		}

		// calcul du vecteur moyen avec methode scale et quadtrees

		{
			// Iteration sur les echelles ( plus grande vers + petite )
			float vx = 0;
			float vy = 0;
			int count = 0;

			for (int scaleNumber = binaryScaleMap.size() - 1; scaleNumber >= 0; scaleNumber--) {
				// System.out.println("scale : " + scaleNumber);
				// rajouter le clipping sur le parcours des map d'attraction en
				// echelle sur la ROI.

				final Scale currentScale = binaryScaleMap.get(scaleNumber);
				final int scaleWidth = currentScale.width;
				final int scaleHeight = currentScale.height;
				final int scaleMulti = (int) currentScale.getScaleFactor();
				final byte[] dataAncre = a1.carteAncre.getDataXYAsByte(0);
				final int widthDataAncre = a1.carteAncre.getWidth();

				for (int x = 0; x < scaleWidth; x++)
					for (int y = 0; y < scaleHeight; y++) {
						// first point to test:
						final int p1x = x * scaleMulti;
						final int p1y = y * scaleMulti;
						// second point to test:
						final int p2x = (x + 1) * scaleMulti - 1;
						final int p2y = (y + 1) * scaleMulti - 1;

						// test d'inclusion de l'echelle
						if ((dataAncre[p1x + p1y * widthDataAncre] != 0) // haut
																			// gauche
								&& (dataAncre[p2x + p2y * widthDataAncre] != 0) // bas
																				// droite
								&& (dataAncre[p1x + p2y * widthDataAncre] != 0)
								&& (dataAncre[p2x + p1y * widthDataAncre] != 0)) {
							// add a quad to watch
							// System.out.print("x");
							a1.listRect.add(new Rectangle2D.Float(p1x, p1y, p2x
									- p1x + 1, p2y - p1y + 1));

							// remove pixels from anchor

							for (int xx = p1x; xx <= p2x; xx++)
								for (int yy = p1y; yy <= p2y; yy++) {
									dataAncre[xx + yy * widthDataAncre] = 0; // 0
								}

							// calcul du vecteur

							final float vectX = (currentScale.barycenterX[x + y
									* scaleWidth] - a1.centerX)
									* currentScale.value[x + y * scaleWidth];
							final float vectY = (currentScale.barycenterY[x + y
									* scaleWidth] - a1.centerY)
									* currentScale.value[x + y * scaleWidth];
							vx += vectX;
							vy += vectY;

							count++;
						}
					}

				if (DISPLAY) {
					a1.refreshDisplay();
				}

			}
			System.out.println("nb iteration quad : " + count);
			System.out.println("Calcul du vecteur via methode quad : vx = "
					+ vx + " vy = " + vy);

		}

	}

	public void actionPerformed(ActionEvent e) {

		if ((e.getSource() == displayForceCheckBox)
				|| (e.getSource() == displayEnergyAreaCheckBox)
				|| (e.getSource() == displayBodyCenterCheckBox)
				|| (e.getSource() == displayBodyShapeCheckBox)
				|| (e.getSource() == displayGlobalSplineCheckBox)
				|| (e.getSource() == displaySlideJointCheckBox)
				|| (e.getSource() == displayBinaryMapCheckBox)
				|| (e.getSource() == displayGradientMapCheckBox)) {
			sequence.painterChanged(null);
		}

		if (e.getSource() == computeATestAnchorVectorMapButton) {
			final Sequence sequence = Icy.getMainInterface()
					.getFocusedSequence();
			final ArrayList<Ancre2> ancreListOut = new ArrayList<Ancre2>();
			final ArrayList<Ancre2> ancreListIn = new ArrayList<Ancre2>();

			final Ancre2 a1 = new Ancre2(Icy.getMainInterface()
					.getFocusedSequence().getWidth(), Icy.getMainInterface()
					.getFocusedSequence().getHeight(), 200, 160, 140 // ray =
																		// 140.
			);
			a1.setColor(Color.yellow);

			final Ancre2 a2 = new Ancre2(Icy.getMainInterface()
					.getFocusedSequence().getWidth(), Icy.getMainInterface()
					.getFocusedSequence().getHeight(), 300, 160, 140 // ray =
																		// 140.
			);
			a2.setColor(Color.orange);

			final Ancre2 a3 = new Ancre2(Icy.getMainInterface()
					.getFocusedSequence().getWidth(), Icy.getMainInterface()
					.getFocusedSequence().getHeight(), 250, 250, 100 // ray =
																		// 140.
			);
			a3.setColor(Color.blue);

			final Ancre2 aConflict = new Ancre2(Icy.getMainInterface()
					.getFocusedSequence().getWidth(), Icy.getMainInterface()
					.getFocusedSequence().getHeight(), 250, 160, 140 // ray =
			// 140.
			);
			aConflict.setColor(Color.pink);

			final byte[] aConflictdata = aConflict.carteAncre
					.getDataXYAsByte(0);
			final byte[] a1data = a1.carteAncre.getDataXYAsByte(0);
			final byte[] a2data = a2.carteAncre.getDataXYAsByte(0);
			final byte[] a3data = a3.carteAncre.getDataXYAsByte(0);

			for (int i = 0; i < aConflictdata.length; i++) {
				aConflictdata[i] = 0;
				if (a1data[i] != 0)
					aConflictdata[i]++;
				if (a2data[i] != 0)
					aConflictdata[i]++;
				if (a3data[i] != 0)
					aConflictdata[i]++;
			}

			ancreListIn.add(a1);
			ancreListIn.add(a2);
			ancreListIn.add(a3);

			// creation des zones ici nomme ancres
			for (final Ancre2 ancre : ancreListIn) {
				// cherche le max dans le masque de l ancre avec la map de
				// conflit
				int max = 0;
				final byte[] mapAncre = ancre.carteAncre.getDataXYAsByte(0);
				for (int i = 0; i < aConflictdata.length; i++) {
					if (mapAncre[i] != 0) {
						if (aConflictdata[i] > max)
							max = aConflictdata[i];
					}
				}

				System.out.println("max = " + max);

				// creation des ancres pour chaque indice de conflit
				for (int cumulIndex = max; cumulIndex > 0; cumulIndex--) {
					final Ancre2 newAncre = new Ancre2(sequence.getWidth(),
							sequence.getHeight(), 10, 10, 0);
					final byte[] ancreData = newAncre.carteAncre
							.getDataXYAsByte(0);
					for (int i = 0; i < aConflictdata.length; i++) {
						if (mapAncre[i] == (byte) 255) {
							if (aConflictdata[i] == cumulIndex) {
								ancreData[i] = (byte) 255;
							}
						}

					}
					System.out.println("computing.");
					computeATestAnchorVectorMap(true, newAncre);
					newAncre.setColor(new Color((float) Math.random(),
							(float) Math.random(), (float) Math.random()));
					sequence.addPainter(newAncre);

				}

			}

		}

		// generaliser a toutes les carte binaire et gradient

		if (e.getSource() == displayScaleBinaryButton) {

			binaryScaleMap = new ArrayList<Scale>();
			binaryScaleMap.clear();

			// construction de la carte binaire.

			// Echelle 1 = copie originale de la carte binarized

			final Scale binaryScale0 = new Scale(Icy.getMainInterface()
					.getFocusedSequence().getWidth(), Icy.getMainInterface()
					.getFocusedSequence().getHeight(), 0);

			final byte[] dataIn = Icy.getMainInterface().getFocusedSequence()
					.getDataXYAsByte(0, 0, 0);
			final float[] dataOut = binaryScale0.value;

			{
				final int maxWidth = Icy.getMainInterface()
						.getFocusedSequence().getWidth();
				final int maxHeight = Icy.getMainInterface()
						.getFocusedSequence().getHeight();
				int offset = 0;

				for (int y = 0; y < maxHeight; y++) {
					for (int x = 0; x < maxWidth; x++) {

						dataOut[offset] = dataIn[offset] & 0xFF;

						binaryScale0.barycenterX[offset] = x;
						binaryScale0.barycenterY[offset] = y;
						offset++;
					}

				}
			}
			binaryScale0.sendToDisplay();

			binaryScaleMap.add(binaryScale0);

			final int MAX_SCALE_TO_BUILD = 9;

			// construction des echelles version 2
			for (int scale = 1; scale < MAX_SCALE_TO_BUILD; scale++) {

				final Scale previousScale = binaryScaleMap.get(scale - 1);

				final Scale currentScale = new Scale(previousScale.width / 2,
						previousScale.height / 2, scale);

				final float[] in = previousScale.value;
				final float[] out = currentScale.value;

				final int maxHeight = previousScale.height / 2;
				final int maxWidth = previousScale.width / 2;

				for (int y = 0; y < maxHeight; y++) // balayage dans la nouvelle
													// echelle
				{
					for (int x = 0; x < maxWidth; x++) // balayage dans la
														// nouvelle echelle
					{
						// calcul de la valeur coefficient

						boolean LOG = false;

						if (x == 20 && y == 21 && scale == 2) {
							System.out.println("log true x: 20 y:21 s:2");
							LOG = true;
						}

						final int xx = x * 2;
						final int yy = y * 2;

						if (LOG) {
							System.out.println("recherche pour X s= " + x);
							System.out.println("recherche pour Y s= " + y);

							System.out.println("recherche pour X s-1= " + xx);
							System.out.println("recherche pour X s-1= "
									+ (xx + 1));
							System.out.println("recherche pour Y s-1= " + yy);
							System.out.println("recherche pour Y s-1= "
									+ (yy + 1));
						}

						out[x + y * currentScale.width] = in[xx + yy
								* previousScale.width]
								+ in[xx + 1 + yy * previousScale.width]
								+ in[xx + (yy + 1) * previousScale.width]
								+ in[xx + 1 + (yy + 1) * previousScale.width];

						if (LOG) {
							System.out.println("previous val 1: "
									+ in[xx + yy * previousScale.width]);
							System.out.println("previous val 2: "
									+ in[xx + 1 + yy * previousScale.width]);
							System.out.println("previous val 3: "
									+ in[xx + (yy + 1) * previousScale.width]);
							System.out.println("previous val 4: "
									+ in[xx + 1 + (yy + 1)
											* previousScale.width]);
							System.out.println("OUT destination value = "
									+ out[x + y * currentScale.width]);
						}

						// calcul des barycentres propagés

						if (out[x + y * currentScale.width] != 0) {

							if (LOG) {
								System.out.println("Entree dans barycentre.");

							}

							// calcul de X pour le barycentre

							currentScale.barycenterX[x + y * currentScale.width] =

							((float) (previousScale.value[xx + yy
									* previousScale.width]
									* previousScale.barycenterX[xx + yy
											* previousScale.width]
									+ previousScale.value[xx + 1 + yy
											* previousScale.width]
									* previousScale.barycenterX[xx + 1 + yy
											* previousScale.width]
									+ previousScale.value[xx + (yy + 1)
											* previousScale.width]
									* previousScale.barycenterX[xx + (yy + 1)
											* previousScale.width] + previousScale.value[xx
									+ 1 + (yy + 1) * previousScale.width]
									* previousScale.barycenterX[xx + 1
											+ (yy + 1) * previousScale.width]) / (float) out[x
									+ y * currentScale.width]);

							if (LOG) {
								System.out.println("Calcul de by: ");
								System.out.println("p1: "
										+ previousScale.barycenterY[xx + yy
												* previousScale.width]);
								System.out.println("p2: "
										+ previousScale.barycenterY[xx + 1 + yy
												* previousScale.width]);
								System.out.println("p3: "
										+ previousScale.barycenterY[xx
												+ (yy + 1)
												* previousScale.width]);
								System.out.println("p4: "
										+ previousScale.barycenterY[xx + 1
												+ (yy + 1)
												* previousScale.width]);

							}

							currentScale.barycenterY[x + y * currentScale.width] =

							((float) (previousScale.value[xx + yy
									* previousScale.width]
									* previousScale.barycenterY[xx + yy
											* previousScale.width]
									+ previousScale.value[xx + 1 + yy
											* previousScale.width]
									* previousScale.barycenterY[xx + 1 + yy
											* previousScale.width]
									+ previousScale.value[xx + (yy + 1)
											* previousScale.width]
									* previousScale.barycenterY[xx + (yy + 1)
											* previousScale.width] + previousScale.value[xx
									+ 1 + (yy + 1) * previousScale.width]
									* previousScale.barycenterY[xx + 1
											+ (yy + 1) * previousScale.width]) / (float) out[x
									+ y * currentScale.width]);

							if (LOG) {
								System.out.println("Bary x:"
										+ currentScale.barycenterX[x + y
												* currentScale.width]);
								System.out.println("Bary y:"
										+ currentScale.barycenterY[x + y
												* currentScale.width]);

							}

						} else {

						}

					}

				}

				currentScale.sendToDisplay();

				System.out.println("adding scale : " + scale);
				binaryScaleMap.add(currentScale);

			}

		}

		if (e.getSource() == resultButton) {
			writeXMLResult();
		}

//		if ( e.getSource() == applyNewScaleButton )
//		{
//			//applyNewScaleButton
//			SCALE = Float.parseFloat(scaleTextField.getText()); 
//			
//			for ( Mouse mouse : mouseList )
//			{
//				// set mouse again
//				
////				sdf
//			}
//		}
	}

	public boolean isStable() {

		final double seuil = 1000d;

		if (world.getTotalEnergy() > seuil)
			return false;

		return true;
	}

	public void loadXML(File currentFile) {

		// LOAD DOCUMENT

		mouseARecord.clear();
		mouseBRecord.clear();

		final File XMLFile = new File(currentFile.getAbsoluteFile() + ".xml");
		if (!XMLFile.exists())
			return;

		final Document XMLDocument = XMLTools.loadDocument(XMLFile);

		final XPath xpath = XPathFactory.newInstance().newXPath();

		{
			final String expression = "//MOUSEA/DET";
			NodeList nodes;
			try {
				nodes = (NodeList) xpath.evaluate(expression, XMLDocument,
						XPathConstants.NODESET);

				System.out.println("node size : " + nodes.getLength());

				for (int i = 0; i < nodes.getLength(); i++) {
					final Element detNode = (Element) nodes.item(i);

					final MouseInfoRecord mouseInfoRecord = new MouseInfoRecord();
					mouseInfoRecord.bodyPosition = new Point2D.Float(
							Float.parseFloat(detNode.getAttribute("bodyx")),
							Float.parseFloat(detNode.getAttribute("bodyy")));
					mouseInfoRecord.headPosition = new Point2D.Float(
							Float.parseFloat(detNode.getAttribute("headx")),
							Float.parseFloat(detNode.getAttribute("heady")));
					mouseInfoRecord.tailPosition = new Point2D.Float(
							Float.parseFloat(detNode.getAttribute("tailx")),
							Float.parseFloat(detNode.getAttribute("taily")));

					float neckX = 0;
					float neckY = 0;
					try
					{
						neckX = Float.parseFloat( detNode.getAttribute("neckx") );
						neckY = Float.parseFloat( detNode.getAttribute("necky") );
					}catch(Exception e){}
					mouseInfoRecord.neckPosition = new Point2D.Float( neckX , neckY );

					mouseARecord.put(
							Integer.parseInt(detNode.getAttribute("t")),
							mouseInfoRecord);

				}

			} catch (final XPathExpressionException e) {

				e.printStackTrace();
			}
		}

		// MOUSE B LOAD

		{
			final String expression = "//MOUSEB/DET";
			NodeList nodes;
			try {
				nodes = (NodeList) xpath.evaluate(expression, XMLDocument,
						XPathConstants.NODESET);

				System.out.println("node size : " + nodes.getLength());

				for (int i = 0; i < nodes.getLength(); i++) {
					final Element detNode = (Element) nodes.item(i);

					final MouseInfoRecord mouseInfoRecord = new MouseInfoRecord();
					mouseInfoRecord.bodyPosition = new Point2D.Float(
							Float.parseFloat(detNode.getAttribute("bodyx")),
							Float.parseFloat(detNode.getAttribute("bodyy")));
					mouseInfoRecord.headPosition = new Point2D.Float(
							Float.parseFloat(detNode.getAttribute("headx")),
							Float.parseFloat(detNode.getAttribute("heady")));
					mouseInfoRecord.tailPosition = new Point2D.Float(
							Float.parseFloat(detNode.getAttribute("tailx")),
							Float.parseFloat(detNode.getAttribute("taily")));
					
					float neckX = 0;
					float neckY = 0;
					try
					{
						neckX = Float.parseFloat( detNode.getAttribute("neckx") );
						neckY = Float.parseFloat( detNode.getAttribute("necky") );
					}catch(Exception e){}
					mouseInfoRecord.neckPosition = new Point2D.Float( neckX , neckY );

					mouseBRecord.put(
							Integer.parseInt(detNode.getAttribute("t")),
							mouseInfoRecord);

				}

			} catch (final XPathExpressionException e) {

				e.printStackTrace();
			}
		}

	}

	public void saveXML(File currentFile) {

		// CREATE DOCUMENT

		final File XMLFile = new File(currentFile.getAbsoluteFile() + ".xml");

		final DocumentBuilderFactory dbfac = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = dbfac.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		}
		final Document XMLDocument = docBuilder.newDocument();

		final Element child = XMLDocument.createElement("MOUSETRACK002Puppy");
		final Element child1 = XMLDocument.createElement("FILENAME");
		child1.setTextContent(currentFile.getAbsolutePath());
		final Element child2 = XMLDocument.createElement("PARAMETERS");
		final Element resultChild = XMLDocument.createElement("RESULT");

		XMLDocument.appendChild(child);
		child.appendChild(child1);
		child.appendChild(child2);
		child.appendChild(resultChild);

		final Element resultMouseA = XMLDocument.createElement("MOUSEA");
		final Element resultMouseB = XMLDocument.createElement("MOUSEB");

		resultChild.appendChild(resultMouseA);
		resultChild.appendChild(resultMouseB);

		// FILL DATA

		int maxT = 0;
		{
			Set<Integer> integerKey = mouseARecord.keySet();
			Iterator<Integer> it = integerKey.iterator();
			while (it.hasNext()) {
				Integer t = it.next();
				if (t > maxT)
					maxT = t;
			}
		}

		// MOUSE A

		for (int t = 0; t <= maxT; t++) {

			final MouseInfoRecord mouseAInfo = mouseARecord.get(t);
			if (mouseAInfo == null)
				continue;

			final Element newResultElement = XMLDocument.createElement("DET");
			newResultElement.setAttribute("t", "" + t);

			newResultElement.setAttribute("headx",
					"" + mouseAInfo.headPosition.getX());
			newResultElement.setAttribute("heady",
					"" + mouseAInfo.headPosition.getY());

			
			newResultElement.setAttribute("bodyx",
					"" + mouseAInfo.bodyPosition.getX());
			newResultElement.setAttribute("bodyy",
					"" + mouseAInfo.bodyPosition.getY());

			newResultElement.setAttribute("tailx",
					"" + mouseAInfo.tailPosition.getX());
			newResultElement.setAttribute("taily",
					"" + mouseAInfo.tailPosition.getY());

			newResultElement.setAttribute("neckx",
					"" + mouseAInfo.neckPosition.getX());
			newResultElement.setAttribute("necky",
					"" + mouseAInfo.neckPosition.getY());

			resultMouseA.appendChild(newResultElement);

		}

		// MOUSE B

		if (mouseList.size() > 1) // check if two mice are presents
		{

			for (int t = 0; t <= maxT; t++) {

				final MouseInfoRecord mouseBInfo = mouseBRecord.get(t);
				if (mouseBInfo == null)
					continue;

				final Element newResultElement = XMLDocument
						.createElement("DET");
				newResultElement.setAttribute("t", "" + t);

				newResultElement.setAttribute("headx", ""
						+ mouseBInfo.headPosition.getX());
				newResultElement.setAttribute("heady", ""
						+ mouseBInfo.headPosition.getY());

				newResultElement.setAttribute("bodyx", ""
						+ mouseBInfo.bodyPosition.getX());
				newResultElement.setAttribute("bodyy", ""
						+ mouseBInfo.bodyPosition.getY());

				newResultElement.setAttribute("tailx", ""
						+ mouseBInfo.tailPosition.getX());
				newResultElement.setAttribute("taily", ""
						+ mouseBInfo.tailPosition.getY());

				newResultElement.setAttribute("neckx",
						"" + mouseBInfo.neckPosition.getX());
				newResultElement.setAttribute("necky",
						"" + mouseBInfo.neckPosition.getY());

				resultMouseB.appendChild(newResultElement);

			}
		}

		// SAVE DOC

		XMLTools.saveDocument(XMLDocument, XMLFile);

	}

	@Override
	public void stateChanged(ChangeEvent e) {

		if (e.getSource() == binaryThresholdSpinner) {
			SEUIL_BINARY_MAP = Integer.parseInt(binaryThresholdSpinner
					.getValue().toString());
		}

	}

	public void divideTimePer2() {

		int max = 0;

		final Set<Integer> integerKey = mouseBRecord.keySet();
		final Iterator<Integer> it = integerKey.iterator();
		while (it.hasNext()) {
			final Integer t = it.next();
			if (t > max)
				max = t;
		}
		System.out.println("Max =" + max);
		for (int i = 0; i <= max; i += 2) {
			if (mouseARecord.containsKey(i)) {
				final MouseInfoRecord mouseRecordA = mouseARecord.get(i);
				mouseARecord.remove(i);
				mouseARecord.put(i / 2, mouseRecordA);
			}

			if (mouseBRecord.containsKey(i)) {
				final MouseInfoRecord mouseRecordB = mouseBRecord.get(i);
				mouseBRecord.remove(i);
				mouseBRecord.put(i / 2, mouseRecordB);
			}
		}

		for (int i = max / 2; i <= max; i++) {
			mouseARecord.remove(i);
			mouseBRecord.remove(i);
		}

		System.out.println("Split done.");

	}

	boolean reverseThresholdBoolean = false;
	
	public void setReverseThreshold(boolean reverseThresholdBoolean) {
		
		this.reverseThresholdBoolean = reverseThresholdBoolean;
		
	}

	MAnchor2D[] headForcedPosition = new MAnchor2D[2];
	/** force head location for the next compute force frame (1 frame only)*/
	public void setHeadLocation(int mouseNumber, MAnchor2D controlPoint) {
		headForcedPosition[mouseNumber] = controlPoint;		
	}

}
