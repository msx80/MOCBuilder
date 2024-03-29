package Connectivity;

import java.util.ArrayList;

import Command.LDrawPart;
import Common.Matrix4;
import Common.Vector3f;
import LDraw.Support.MatrixMath;
import LDraw.Support.type.LDrawGridTypeT;

public class Slider extends Connectivity {
	int startCapped;
	int endCapped;
	float length;
	int cylindrical;
	float[] spring;
	String tag;

	public int getstartCapped() {
		return startCapped;
	}

	public void setstartCapped(String startCapped) {
		this.startCapped = Integer.parseInt(startCapped);
	}

	public int getendCapped() {
		return endCapped;
	}

	public void setendCapped(String endCapped) {
		this.endCapped = Integer.parseInt(endCapped);
	}

	public float getlength() {
		return length;
	}

	public void setlength(String length) {
		this.length = Float.parseFloat(length) * 25;
	}

	public int getcylindrical() {
		return cylindrical;
	}

	public void setcylindrical(String cylindrical) {
		this.cylindrical = Integer.parseInt(cylindrical);
	}

	public float[] getspring() {
		return spring;
	}

	public void setspring(String spring) {
		String[] split = spring.split(",");
		this.spring = new float[split.length];
		for (int i = 0; i < split.length; i++) {
			this.spring[i] = Float.parseFloat(split[i]);
		}
	}

	public String gettag() {
		return tag;
	}

	public void settag(String tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		String str = String.format("%d %d %f %d", startCapped, endCapped,
				length/25, cylindrical);
		if (spring != null) {
			str += " " + spring[0];
			for (int i = 1; i < spring.length; i++) {
				str += "," + spring[i];
			}
		}
		if (tag != null) {
			str += " " + tag;
		}
		return super.toString(str);
	}

	@Override
	public int parseString(String[] line) {
		int size = super.parseString(line);
		setstartCapped(line[size + 1]);
		setendCapped(line[size + 2]);
		setlength(line[size + 3]);
		setcylindrical(line[size + 4]);
		if (line.length > size + 7) {
			setspring(line[size + 5]);
			settag(line[size + 6]);
		} else if (line.length > size + 6) {
			if (line[18].contains(",")) {
				setspring(line[size + 5]);
			} else {
				settag(line[size + 5]);
			}
		}
		return 0;
	}

	@Override
	public String getName() {
		return "Slider";
	}

	@Override
	public Matrix4 getTransformMatrixForSnapConnecting(
			Connectivity existingConn, Matrix4 initialTransformOfPart) {
		Matrix4 newTransform = getRotationMatrixForConnection(existingConn,
				initialTransformOfPart);
		if (newTransform != null) {
			newTransform.translate(initialTransformOfPart.element[3][0],
					initialTransformOfPart.element[3][1],
					initialTransformOfPart.element[3][2]);

			Slider existingSlider = (Slider) existingConn;

			Vector3f realMatchingPosOfExistingConn = existingConn
					.getCurrentPos();

			Vector3f realPosOfTestingConn = getCurrentPos(newTransform);

			Vector3f posDifferent = realPosOfTestingConn
					.sub(realMatchingPosOfExistingConn);
			Vector3f directionVector = getDirectionVector(newTransform);

			float endPointOfExsiting = existingSlider.getDirectionVector()
					.scale(existingSlider.getlength()).dot(directionVector);
			float minSlidingLength = 0;
			float maxSlidingLength = getlength();
			boolean isSameDirection = false;
			if (endPointOfExsiting > 0) {
				isSameDirection = true;
				minSlidingLength = -(getlength()+existingSlider.getlength());
				if (existingSlider.getstartCapped() != 0 || getendCapped() != 0)
					minSlidingLength = 0;

				maxSlidingLength = Math.max(existingSlider.getlength(),
						getlength());
				if (getstartCapped() != 0 || existingSlider.getendCapped() != 0) {
					maxSlidingLength = Math.min(existingSlider.getlength(),
							getlength());
				}

			} else {
				minSlidingLength = -(getlength() + existingSlider.getlength());
				if (existingSlider.getendCapped() != 0)
					minSlidingLength = Math.max(minSlidingLength,
							-existingSlider.getlength());
				if (getendCapped() != 0)
					minSlidingLength = Math.max(minSlidingLength, -getlength());

				maxSlidingLength = 0;
				if (existingSlider.getstartCapped() != 0) {
					maxSlidingLength = Math.min(maxSlidingLength, -getlength());
				}
				if (getstartCapped() != 0)
					maxSlidingLength = Math.min(maxSlidingLength, getlength());
			}

			// if start or end capped, restrict the axle sliding.
			if (maxSlidingLength < minSlidingLength)
				return null;

			float projectedDistance = posDifferent.dot(directionVector);

			float distance = posDifferent.dot(posDifferent) - projectedDistance
					* projectedDistance;
			distance = (float) Math.sqrt(distance);

			if (distance > LDrawGridTypeT.Coarse.getXZValue() * 2)
				return null;

			if (projectedDistance >= maxSlidingLength
					+ LDrawGridTypeT.Coarse.getXZValue())
				return null;
			else if (projectedDistance >= maxSlidingLength)
				projectedDistance = maxSlidingLength;
			else if (projectedDistance < minSlidingLength
					- LDrawGridTypeT.Coarse.getXZValue())
				return null;
			else if (projectedDistance < minSlidingLength)
				projectedDistance = minSlidingLength;

			Vector3f posAdjust = realMatchingPosOfExistingConn
					.sub(realPosOfTestingConn);
			posAdjust = posAdjust.add(directionVector.scale(projectedDistance));
			newTransform.translate(posAdjust.x, posAdjust.y, posAdjust.z);

			// System.out.println("isSameDirection: " + isSameDirection);
			// System.out.println("Connectible Range: " + minSlidingLength +
			// ", "
			// + maxSlidingLength);
			// System.out.println("ProjectedDistance: " + projectedDistance);

		}
		return newTransform;
	}

	@Override
	public Matrix4 getRotationMatrixForConnection(Connectivity existingConn,
			Matrix4 initialTransformMatrixOfPart) {
		Matrix4 newMatrix = new Matrix4(initialTransformMatrixOfPart);
		newMatrix.element[3][0] = newMatrix.element[3][1] = newMatrix.element[3][2] = 0;

		LDrawPart existingPart = existingConn.getParent();
		Matrix4 existingConnTransformMatrix = Matrix4.multiply(
				existingConn.getTransformMatrix(),
				existingPart.transformationMatrix());

		Matrix4 candidate = Matrix4.multiply(Matrix4.inverse(transformMatrix),
				existingConnTransformMatrix);
		for (int i = 0; i < 3; i++)
			candidate.element[3][i] = 0;

		Vector3f rotationVector = getRotationVector(candidate);

		Matrix4 inverseOfCandidate = new Matrix4(candidate);
		inverseOfCandidate.rotate((float) Math.toRadians(180), rotationVector);

		Matrix4 initMatrix = new Matrix4(initialTransformMatrixOfPart);
		for (int i = 0; i < 3; i++)
			initMatrix.element[3][i] = 0;

//		System.out.println(getDirectionVector(initMatrix));
//		System.out.println(getDirectionVector(candidate));
//		System.out.println(getDirectionVector(inverseOfCandidate));
//		System.out.println("#####################");

		Vector3f directionVector_init = getDirectionVector(initMatrix);
		Vector3f directionVector_candidate = getDirectionVector(candidate);
		Vector3f directionVector_icandidate = getDirectionVector(inverseOfCandidate);

		float dirDiff = (float) Math.acos(directionVector_init
				.dot(directionVector_candidate)
				/ directionVector_candidate.length()
				/ directionVector_init.length());
		float dirDiff2 = (float) Math.acos(directionVector_init
				.dot(directionVector_icandidate)
				/ directionVector_icandidate.length()
				/ directionVector_init.length());
		// System.out.println("#############################");
		// System.out.println(Direction6T
		// .getDirectionOfTransformMatrix(initialTransformMatrixOfPart));
		// System.out
		// .println(Direction6T.getDirectionOfTransformMatrix(candidate));
		// System.out.println(Direction6T
		// .getDirectionOfTransformMatrix(inverseOfCandidate));
//		System.out.println("DirectionVectorDiff: " + dirDiff + ", " + dirDiff2);

		if (directionVector_init.equals(directionVector_candidate))
			;
		else if (directionVector_init.equals(directionVector_icandidate))
			candidate = inverseOfCandidate;
		else if (dirDiff > dirDiff2)
			candidate = inverseOfCandidate;

		ArrayList<Matrix4> candidateForRotation = new ArrayList<Matrix4>();
		candidateForRotation.add(candidate);
		for (int i = 0; i < 1; i++) {
			candidate = new Matrix4(candidate);
			candidate.rotate((float) Math.toRadians(180),
					existingConn.getDirectionVector());
			candidateForRotation.add(candidate);
		}

		dirDiff = -1;
		// System.out.println("#########################");
		for (Matrix4 matrix : candidateForRotation) {
			dirDiff2 = matrix
					.getDifferentValueForRotation(initialTransformMatrixOfPart);
			if (dirDiff < 0) {
				dirDiff = dirDiff2;
				newMatrix = matrix;
			} else if (dirDiff > dirDiff2 + 0.1f) {
				dirDiff = dirDiff2;
				newMatrix = matrix;
			}
		}
		return newMatrix;
	}

	@Override
	public ConnectivityTestResultT isConnectable(IConnectivity connector,
			Matrix4 partTransformMatrix) {
		ConnectivityTestResultT result = isConnectable(connector);
		if (result != ConnectivityTestResultT.True)
			return result;

		Matrix4 rotationMatrix = getTransformMatrixForSnapConnecting(
				connector.getConnectivity(), partTransformMatrix);
		if (rotationMatrix == null)
			return ConnectivityTestResultT.False;
		if (partTransformMatrix.getDefaultTransformPos()
				.sub(rotationMatrix.getDefaultTransformPos()).length() > 1f) {
			return ConnectivityTestResultT.False;
		}

		return result;
	}
	
	private Vector3f getRotationVector(Matrix4 candidate) {
		Vector3f directionVector = getDirectionVector(candidate);
		// System.out.println("DirectionVector: "+directionVector);
		Vector3f rotationVector = null;
		switch (Direction6T.getDirectionOfTransformMatrix(Matrix4.multiply(
				transformMatrix, candidate))) {
		case X_Minus:
		case X_Plus:
			rotationVector = directionVector.cross(new Vector3f(0, 1, 0));
			if (rotationVector.equals(new Vector3f()))
				rotationVector = new Vector3f(0, 1, 0);
			break;
		case Y_Minus:
		case Y_Plus:
			rotationVector = directionVector.cross(new Vector3f(0, 0, 1));
			if (rotationVector.equals(new Vector3f()))
				rotationVector = new Vector3f(0, 0, 1);
			break;
		case Z_Minus:
		case Z_Plus:
			rotationVector = directionVector.cross(new Vector3f(1, 0, 0));
			if (rotationVector.equals(new Vector3f()))
				rotationVector = new Vector3f(1, 0, 0);
			break;
		}
		// System.out.println("Rotation Vector: "+rotationVector);
		return rotationVector;
	}
}
