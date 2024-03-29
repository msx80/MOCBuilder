package BrickControlGuide;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import Builder.BuilderConfigurationManager;
import Builder.BrickSelectionManager;
import Builder.MainCamera;
import Command.LDrawPart;
import Common.Matrix4;
import Common.Vector3f;
import Connectivity.Axle;
import Connectivity.Ball;
import Connectivity.Connectivity;
import Connectivity.ConnectivityTestResultT;
import Connectivity.GlobalConnectivityManager;
import Connectivity.Hinge;
import Connectivity.IConnectivity;
import Connectivity.ICustom2DField;
import Connectivity.MatrixItem;
import Connectivity.Slider;

import com.jogamp.opengl.util.gl2.GLUT;

public class BrickMovementGuideRenderer {
	private static BrickMovementGuideRenderer _instance = null;

	private MainCamera camera;
	private boolean isVisible = true;
	private GLU glu;
	private GLUT glut;
	private LDrawPart part;
	private IGuideRenderer selectedGuide = null;
	private ArrayList<IGuideRenderer> guideList;
	private ArrayList<IGuideRenderer> defaultGuideList;
	private boolean isForGroup = false;

	public static BrickMovementGuideRenderer getInstance() {
		return _instance;
	}

	public synchronized static BrickMovementGuideRenderer getInstance(
			MainCamera cam) {
		if (_instance == null)
			_instance = new BrickMovementGuideRenderer(cam);
		return _instance;
	}

	public void clear() {
		synchronized (guideList) {
			guideList.clear();
		}
	}

	public void addMovementGuide(Vector3f directionVector, IConnectivity center) {
		MovementGuide movementGuide = new MovementGuide(glu);
		movementGuide.setColor3f(0, 0, 1);
		movementGuide.setAxisGuideType(AxisGuideTypeT.Custom);
		movementGuide.setAxisDirectionVector(directionVector);
		movementGuide.setConnectivity(center);
		synchronized (guideList) {
			guideList.add(movementGuide);
		}

	}

	public void addRotationGuide(Vector3f directionVector,
			IConnectivity rotationCenter) {
		RotationGuide rotationGuide = new RotationGuide(glu, glut);
		rotationGuide.setColor3f(0, 1, 0);
		rotationGuide.setAxisGuideType(AxisGuideTypeT.Custom);
		rotationGuide.setAxisDirectionVector(directionVector);
		rotationGuide.setConnectivity(rotationCenter);
		synchronized (guideList) {
			guideList.add(rotationGuide);
		}
	}

	private BrickMovementGuideRenderer(MainCamera cam) {
		camera = cam;
		part = null;
		glu = new GLU(); // get GL Utilities
		glut = new GLUT();
		guideList = new ArrayList<IGuideRenderer>();

		defaultGuideList = new ArrayList<IGuideRenderer>();
		MovementGuide movementGuide = new MovementGuide(glu);
		movementGuide.setColor3f(1, 0, 0);
		movementGuide.setAxisGuideType(AxisGuideTypeT.X_Movement);
		defaultGuideList.add(movementGuide);

		movementGuide = new MovementGuide(glu);
		movementGuide.setColor3f(0, 1, 0);
		movementGuide.setAxisGuideType(AxisGuideTypeT.Y_Movement);
		defaultGuideList.add(movementGuide);

		movementGuide = new MovementGuide(glu);
		movementGuide.setColor3f(0, 0, 1);
		movementGuide.setAxisGuideType(AxisGuideTypeT.Z_Movement);
		defaultGuideList.add(movementGuide);

		RotationGuide rotationGuide = new RotationGuide(glu, glut);
		rotationGuide.setColor3f(1, 0, 0);
		rotationGuide.setAxisGuideType(AxisGuideTypeT.X_Rotate);
		defaultGuideList.add(rotationGuide);

		rotationGuide = new RotationGuide(glu, glut);
		rotationGuide.setColor3f(0, 1, 0);
		rotationGuide.setAxisGuideType(AxisGuideTypeT.Y_Rotate);
		defaultGuideList.add(rotationGuide);

		rotationGuide = new RotationGuide(glu, glut);
		rotationGuide.setColor3f(0, 0, 1);
		rotationGuide.setAxisGuideType(AxisGuideTypeT.Z_Rotate);
		defaultGuideList.add(rotationGuide);
	}

	public void draw(GL2 gl2) {
		if (camera == null)
			return;
		if (isVisible == false)
			return;
		if (part == null)
			return;
		if (part.isSelected() == false)
			return;

		Vector3f pos = part.position();
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glUseProgram(0);

		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPushMatrix();
		gl2.glLoadMatrixf(camera.getProjection(), 0);
		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPushMatrix();
		gl2.glLoadMatrixf(camera.getModelView(), 0);

		if (selectedGuide == null && part != null) {
			if (guideList.size() != 0
					&& isForGroup == false
					&& BuilderConfigurationManager.getInstance()
							.isUseConnectivity()) {
				synchronized (guideList) {
					for (IGuideRenderer guideRenderer : guideList)
						guideRenderer.draw(gl2, camera, pos);
				}
			} else {
				for (IGuideRenderer guideRenderer : defaultGuideList)
					guideRenderer.draw(gl2, camera, pos);
			}
		} else if (part != null)
			selectedGuide.draw(gl2, camera, pos);

		gl2.glPopMatrix();

		gl2.glEnable(GL2.GL_LIGHTING);
	}

	public void setLDrawPart(LDrawPart part) {
		setLDrawPart(part, false);
	}

	public void setLDrawPart(LDrawPart part, boolean isForGroup) {
		clear();
		this.part = part;
		this.isForGroup = isForGroup;
		if (part != null && isForGroup == false) {
			addGuideForPart(part);
		}
	}

	private void addGuideForPart(LDrawPart part) {
		GlobalConnectivityManager connectivityManager = GlobalConnectivityManager
				.getInstance();
		if (part.isConnectivityInfoExist() == false)
			return;
		
		//add guide generated by matrixItem
		ArrayList<MatrixItem> centerItem = new ArrayList<MatrixItem>();
		if (connectivityManager.isRotatibleByCustom2dConn(part, centerItem)) {
			for (MatrixItem item : centerItem)
				BrickMovementGuideRenderer.getInstance().addRotationGuide(
						item.getDirectionVector(Matrix4.getIdentityMatrix4()),
						item);
		}

		//add guide generated by connectivity. 
		for (IConnectivity conn : part.getConnectivityList()) {
			if (connectivityManager.isConnectible_Connectivity(
					conn.getConnectivity(), part.transformationMatrix())
					.getResultType() != ConnectivityTestResultT.True)
				continue;

			if (conn instanceof Axle || conn instanceof Slider) {
				Vector3f directionVector = conn.getDirectionVector(Matrix4
						.getIdentityMatrix4());
				BrickMovementGuideRenderer.getInstance().addMovementGuide(
						directionVector, conn);
				if (conn instanceof Axle) {
					if (((Axle) conn).isRotatible())
						BrickMovementGuideRenderer.getInstance()
								.addRotationGuide(directionVector, conn);
				}
			} else if (conn instanceof Ball) {
				BrickMovementGuideRenderer.getInstance().addRotationGuide(
						new Vector3f(1, 0, 0), conn);
				BrickMovementGuideRenderer.getInstance().addRotationGuide(
						new Vector3f(0, 1, 0), conn);
				BrickMovementGuideRenderer.getInstance().addRotationGuide(
						new Vector3f(0, 0, 1), conn);
			} else if (conn instanceof Hinge) {
				Vector3f directionVector = conn.getDirectionVector(
						Matrix4.getIdentityMatrix4()).scale(-1);
				BrickMovementGuideRenderer.getInstance().addRotationGuide(
						directionVector, conn);
			}
		}
	}

	public LDrawPart getLDrawPart() {
		return this.part;
	}

	public IGuideRenderer getHittedAxisArrow(float screenX, float screenY) {
		if (part == null)
			return null;
		FloatBuffer distance = FloatBuffer.allocate(1);
		FloatBuffer distanceTemp = FloatBuffer.allocate(1);
		distance.put(0, Float.MAX_VALUE);
		IGuideRenderer resultGuideRenderer = null;

		if (guideList.size() != 0
				&& isForGroup == false
				&& BuilderConfigurationManager.getInstance()
						.isUseConnectivity()) {
			synchronized (guideList) {
				for (IGuideRenderer guideRenderer : guideList) {
					distanceTemp.put(0, Float.MAX_VALUE);
					if (guideRenderer.isHitted(camera, part.position(),
							screenX, screenY, distanceTemp))
						if (distanceTemp.get(0) < distance.get(0)) {
							distance.put(0, distanceTemp.get(0));
							resultGuideRenderer = guideRenderer;
						}
				}
			}
		} else {
			for (IGuideRenderer guideRenderer : defaultGuideList) {
				distanceTemp.put(0, Float.MAX_VALUE);

				if (guideRenderer.isHitted(camera, part.position(), screenX,
						screenY, distanceTemp))
					if (distanceTemp.get(0) < distance.get(0)) {
						distance.put(0, distanceTemp.get(0));
						resultGuideRenderer = guideRenderer;
					}
			}
		}

		return resultGuideRenderer;
	}

	public void axisSelectedType(IGuideRenderer guide) {
		this.selectedGuide = guide;
	}

	public IGuideRenderer getSelectedGuide() {
		return this.selectedGuide;
	}
}
