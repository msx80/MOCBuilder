package Connectivity;

import java.util.ArrayList;

import Command.LDrawPart;
import Common.Matrix4;
import Common.Vector3f;
import LDraw.Files.LDrawMPDModel;
import LDraw.Files.LDrawModel;
import LDraw.Support.LDrawDirective;
import LDraw.Support.MatrixMath;
import LDraw.Support.type.LDrawGridTypeT;
import Window.MOCBuilder;

public abstract class Connectivity implements Cloneable, IConnectivity {
	public static enum TYPE {
		Axle, Ball, Hole, Stud, Fixed, Gear, Hinge, Rail, Slider, CollisionBox, BoundingAABB, Comment, Import
	}

	protected Matrix4 transformMatrix = Matrix4.getIdentityMatrix4();

	protected Vector3f currentPos = new Vector3f();
	protected Vector3f directionVector = null;
	protected Direction6T direction = Direction6T.Y_Minus;

	protected int type;
	protected String fileName;

	protected LDrawPart parent = null;

	private boolean isSelected = false;

	private ArrayList<Connectivity> connectedConnectivity;

	public boolean isSelected() {
		return this.isSelected;
	}

	public void setSelected(boolean flag) {
		this.isSelected = flag;
	}

	public Matrix4 getTransformMatrix() {
		return new Matrix4(transformMatrix);
	}

	public void setTransformMatrix(Matrix4 mat) {
		this.transformMatrix = new Matrix4(mat);
	}

	public Vector3f getTranslationVector() {
		return new Vector3f(transformMatrix.element[3][0],
				transformMatrix.element[3][1], transformMatrix.element[3][2]);
	}

	public int gettype() {
		return type;
	}

	public void settype(String type) {
		try {
			this.type = Integer.parseInt(type);
		} catch (Exception e) {
			System.out.println(getFileName());
		}
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int parseString(String line[]) {
		settype(line[1]);
		for (int column = 0; column < 4; column++) {
			for (int row = 0; row < 3; row++) {
				transformMatrix.element[column][row] = Float.parseFloat(line[2
						+ column * 3 + row]);
			}
		}
		fileName = line[line.length - 1];
		return 13;
	}

	public String toString(String details) {
		String transform = String.format(
				"%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f",
				transformMatrix.element[0][0], transformMatrix.element[0][1],
				transformMatrix.element[0][2], transformMatrix.element[1][0],
				transformMatrix.element[1][1], transformMatrix.element[1][2],
				transformMatrix.element[2][0], transformMatrix.element[2][1],
				transformMatrix.element[2][2], transformMatrix.element[3][0],
				transformMatrix.element[3][1], transformMatrix.element[3][2]);

		if (details != null) {
			return String.format("%d %d %s %s %s",
					TYPE.valueOf(getClass().getName().substring(13)).ordinal(),
					type, transform, details, fileName);
		} else {
			return String.format("%d %d %s %s",
					TYPE.valueOf(getClass().getName().substring(13)).ordinal(),
					type, transform, fileName);
		}
	}

	public abstract String getName();

	public Object clone() throws CloneNotSupportedException {
		Connectivity a = (Connectivity) super.clone();
		a.transformMatrix = new Matrix4(transformMatrix);
		a.parent = parent;
		a.currentPos = (Vector3f) currentPos.clone();

		return a;
	}

	public void setParent(LDrawPart parent) {
		this.parent = parent;
	}

	public LDrawPart getParent() {
		return this.parent;
	}

	public static boolean isConnectable(Connectivity conn1, Connectivity conn2) {
		if (conn1.gettype() % 2 == 1)
			if (conn1.gettype() - 1 == conn2.gettype())
				return true;
		if (conn2.gettype() % 2 == 1)
			if (conn2.gettype() - 1 == conn1.gettype())
				return true;

		return false;
	}

	public Direction6T getDirection() {
		return direction;
	}

	@Override
	public void updateConnectivityOrientationInfo() {
		if (parent == null)
			return;
		
		// update curentPos
		Vector3f newPos = getCurrentPos(parent.transformationMatrix());
		currentPos.set(newPos);

		// update direction
		this.direction = getDirection(parent.transformationMatrix());

		// update direction vector
		this.directionVector = getDirectionVector(parent.transformationMatrix());//
	}

	@Override
	public Direction6T getDirection(Matrix4 partTransformMatrix) {
		return Direction6T.getDirectionOfTransformMatrix(Matrix4.multiply(
				getTransformMatrix(), partTransformMatrix));
	}

	@Override
	public Vector3f getDirectionVector() {
		if (directionVector == null)
			updateConnectivityOrientationInfo();
		return directionVector;
	}

	@Override
	public Vector3f getDirectionVector(Matrix4 newTransformMatrix) {
		Vector3f directionVector = new Vector3f(0, 1, 0);
		directionVector = MatrixMath.V3RotateByTransformMatrix(directionVector,
				getTransformMatrix());
		directionVector = MatrixMath.V3RotateByTransformMatrix(directionVector,
				newTransformMatrix);

		return directionVector;
	}

	@Override
	public Vector3f getCurrentPos(Matrix4 partTransformMatrix) {
		Vector3f newPos = new Vector3f(0, 0, 0);
		newPos = getTransformMatrix().transformPoint(newPos);
		newPos = partTransformMatrix.transformPoint(newPos);
		return newPos;
	}

	@Override
	public Vector3f getCurrentPos() {
		return currentPos;
	}

	@Override
	public ConnectivityTestResultT isConnectable(IConnectivity connector) {
		if (this.getClass().isInstance(connector) == true) {
			if (this.gettype() % 2 == 1)
				if (this.gettype() - 1 == ((Connectivity) connector).gettype())
					return ConnectivityTestResultT.True;
			if (((Connectivity) connector).gettype() % 2 == 1)
				if (((Connectivity) connector).gettype() - 1 == this.gettype())
					return ConnectivityTestResultT.True;
		}
		return ConnectivityTestResultT.None;
	}

	@Override
	public ConnectivityTestResultT isConnectable(
			ArrayList<IConnectivity> connectors) {
		if (connectors == null)
			return ConnectivityTestResultT.None;
		if (connectors.size() == 0)
			return ConnectivityTestResultT.None;
		if (connectors.size() == 1)
			return isConnectable(connectors.get(0));
		return ConnectivityTestResultT.None;
	}

	@Override
	public ConnectivityTestResultT isConnectable(
			ArrayList<IConnectivity> connectors, Matrix4 partTransformMatrix) {
		return isConnectable(connectors);
	}

	@Override
	public ConnectivityTestResultT isConnectable(IConnectivity connector,
			Matrix4 partTransformMatrix) {
		ConnectivityTestResultT result = isConnectable(connector);
		if (result != ConnectivityTestResultT.True)
			return result;

		if (getTransformMatrixForSnapConnecting((Connectivity) connector,
				partTransformMatrix) != null)
			return result;

		return ConnectivityTestResultT.False;
	}

	@Override
	public float distance(Vector3f testingPos) {
		Vector3f distance = testingPos.sub(getCurrentPos());
		return distance.dot(distance);
	}

	public Matrix4 getTransformMatrixForSnapConnecting(
			Connectivity existingConn, Matrix4 initialTransformOfPart) {

		Matrix4 newTransform = getRotationMatrixForConnection(existingConn,
				initialTransformOfPart);

		if (newTransform != null) {
			newTransform.translate(initialTransformOfPart.element[3][0],
					initialTransformOfPart.element[3][1],
					initialTransformOfPart.element[3][2]);

			Vector3f realMatchingPosOfExistingConn = existingConn
					.getCurrentPos();
			Vector3f realPosOfTestingConn = getCurrentPos(newTransform);

			float distance = (float) (realMatchingPosOfExistingConn
					.sub(getCurrentPos(initialTransformOfPart)).length());

			if (MatrixMath.compareFloat(distance,
					LDrawGridTypeT.Coarse.getXZValue() * 2) > 0)
				return null;

			Vector3f newPos = realMatchingPosOfExistingConn
					.sub(realPosOfTestingConn);

			newTransform.translate(newPos.x, newPos.y, newPos.z);
		}
		return newTransform;
	}

	public Matrix4 getRotationMatrixForConnection(Connectivity existingConn,
			Matrix4 initialTransformMatrixOfPart) {
		Matrix4 newMatrix = new Matrix4(initialTransformMatrixOfPart);
		newMatrix.element[3][0] = newMatrix.element[3][1] = newMatrix.element[3][2] = 0;

		LDrawPart existingPart = existingConn.getParent();
		Matrix4 existingConnTransformMatrix = Matrix4.multiply(
				existingConn.getTransformMatrix(),
				existingPart.transformationMatrix());
		for (int i = 0; i < 3; i++)
			existingConnTransformMatrix.element[3][i] = 0;

		Matrix4 testingConnTransformMatrix = Matrix4.multiply(
				getTransformMatrix(), initialTransformMatrixOfPart);
		for (int i = 0; i < 3; i++)
			testingConnTransformMatrix.element[3][i] = 0;
		Matrix4 candidate = Matrix4.multiply(
				Matrix4.inverse(getTransformMatrix()),
				existingConnTransformMatrix);

		Direction6T direction6tCandidate = Direction6T
				.getDirectionOfTransformMatrix(candidate);
		Direction6T direction6tInitial = Direction6T
				.getDirectionOfTransformMatrix(initialTransformMatrixOfPart);
		if (getDirection().getValue() == direction6tCandidate.getValue()
				|| getDirection().getValue() == direction6tInitial.getValue())
			if (direction6tCandidate.getValue() != direction6tInitial
					.getValue())
				return null;

		for (int i = 0; i < 3; i++)
			candidate.element[3][i] = 0;

		Matrix4 initMatrix = new Matrix4(initialTransformMatrixOfPart);
		for (int i = 0; i < 3; i++)
			initMatrix.element[3][i] = 0;

		// System.out.println(getCurrentPos(initMatrix));
		// System.out.println(getCurrentPos(candidate));
		// System.out.println("#####################");

		ArrayList<Matrix4> candidateForRotation = new ArrayList<Matrix4>();
		candidateForRotation.add(candidate);
		for (int i = 0; i < 3; i++) {
			candidate = new Matrix4(candidate);
			candidate.rotate((float) Math.toRadians(90),
					existingConn.getDirectionVector());
			candidateForRotation.add(candidate);
		}

		float posDiff = -1;
		float posDiff2 = -1;
		for (Matrix4 matrix : candidateForRotation) {
			posDiff2 = matrix.getDifferentValueForRotation(initMatrix);
			if (posDiff < 0) {
				posDiff = posDiff2;
				newMatrix = matrix;
			} else if (posDiff > posDiff2 + 0.1f) {
				posDiff = posDiff2;
				newMatrix = matrix;
			}
		}
		return newMatrix;
	}

	@Override
	public Matrix4 transformationMatrixOfPart() {
		return parent.transformationMatrix();
	}

	@Override
	public void moveTo(Vector3f moveByInWorld) {
		transformMatrix.element[3][0] = moveByInWorld.x;
		transformMatrix.element[3][1] = moveByInWorld.y;
		transformMatrix.element[3][2] = moveByInWorld.z;
		updateConnectivityOrientationInfo();
	}

	@Override
	public void moveBy(Vector3f moveByInWorld) {
		transformMatrix.translate(moveByInWorld.x, moveByInWorld.y,
				moveByInWorld.z);
		updateConnectivityOrientationInfo();
	}

	@Override
	public void rotateBy(float angle, Vector3f rotationVector) {
		if (MatrixMath.compareFloat(angle, 0) == 0)
			return;
		Vector3f originalPos = transformMatrix.getDefaultTransformPos();
		transformMatrix.rotate(angle, rotationVector);
		moveTo(originalPos);
	}

	public Connectivity getConnectivity() {
		return this;
	}

	public ArrayList<Connectivity> getConnectedConnectivity() {
		if (connectedConnectivity == null || connectedConnectivity.size() == 0)
			return null;
		return connectedConnectivity;
	}

	public void addConnectedConnectivity(Connectivity connectedConnectivity) {
		if (this.connectedConnectivity == null)
			this.connectedConnectivity = new ArrayList<Connectivity>();
		this.connectedConnectivity.add(connectedConnectivity);
	}

	public void removeConnectedConnectivity(Connectivity connectedConnectivity) {
		if (this.connectedConnectivity == null)
			return;
		this.connectedConnectivity.remove(connectedConnectivity);
	}

	public void clearConnectedConnectivity() {
		if (this.connectedConnectivity == null)
			return;
		this.connectedConnectivity.clear();
	}
}
