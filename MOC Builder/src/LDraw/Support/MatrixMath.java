package LDraw.Support;

import java.nio.FloatBuffer;

import Common.Box2;
import Common.Box3;
import Common.Matrix3;
import Common.Matrix4;
import Common.Ray3;
import Common.Segment3;
import Common.Size2;
import Common.Vector2f;
import Common.Vector3f;
import Common.Vector4f;

public class MatrixMath {
	public static final float SMALL_NUMBER = 1.e-6f;

	public static float V2BoxWidth(Box2 box) {
		return (box.getSize().getWidth());
	}

	// ========== V2BoxHeight
	// =======================================================
	// ==============================================================================
	public static float V2BoxHeight(Box2 box) {
		return (box.getSize().getHeight());
	}

	// ========== V2Make
	// ============================================================
	//
	// Purpose: Make a 2D point.
	//
	// ==============================================================================
	public static Vector2f V2Make(float x, float y) {
		Vector2f point = new Vector2f(x, y);

		return point;
	}

	// ========== V2BoxMaxX
	// =========================================================
	// ==============================================================================
	public static float V2BoxMaxX(Box2 box) {
		return (box.getOrigin().getX() + box.getSize().getWidth());
	}

	// ========== V2BoxMaxY
	// =========================================================
	// ==============================================================================
	public static float V2BoxMaxY(Box2 box) {
		return (box.getOrigin().getY() + box.getSize().getHeight());
	}

	// ========== V2BoxMidX
	// =========================================================
	// ==============================================================================
	public static float V2BoxMidX(Box2 box) {
		return (box.getOrigin().getX() + V2BoxWidth(box) * 0.5f);
	}

	// ========== V2BoxMidY
	// =========================================================
	// ==============================================================================
	public static float V2BoxMidY(Box2 box) {
		return (box.getOrigin().getY() + V2BoxHeight(box) * 0.5f);
	}

	// ========== V2BoxMinX
	// =========================================================
	// ==============================================================================
	public static float V2BoxMinX(Box2 box) {
		return box.getOrigin().getX();
	}

	// ========== V2BoxMinY
	// =========================================================
	// ==============================================================================
	public static float V2BoxMinY(Box2 box) {
		return box.getOrigin().getY();
	}

	// ========== Matrix4CreateFromGLMatrix4()
	// ======================================
	//
	// Purpose: Returns a two-dimensional (row matrix) representation of the
	// given OpenGL transformation matrix.
	//
	// +- -+
	// +- -+ +- -+| a d g 0 |
	// |a d g 0 b e h 0 c f i 0 x y z 1| -. |x y z 1|| b e h 0 |
	// +- -+ +- -+| c f i 0 |
	// | x y z 1 |
	// +- -+
	// OpenGL Matrix Format Matrix4 Format
	// (flat column-major of transpose) (shown multiplied by a point)
	//
	// ==============================================================================
	public static Matrix4 Matrix4CreateFromGLMatrix4(float[] glMatrix) {
		int row, column;
		Matrix4 newMatrix = new Matrix4();
		float[][] elements = newMatrix.getElement();

		for (row = 0; row < 4; row++)
			for (column = 0; column < 4; column++)
				elements[row][column] = glMatrix[row * 4 + column];

		return newMatrix;

	}// end Matrix4CreateFromGLMatrix4

	// ========== Matrix4Invert()
	// ===================================================
	//
	// Purpose: calculate the inverse of a 4x4 matrix
	//
	// -1
	// A = ___1__ adjoint A
	// det A
	//
	// ==============================================================================
	public static Matrix4 Matrix4Invert(Matrix4 in) {
		Matrix4 out = Matrix4.getIdentityMatrix4();
		int i;
		int j;
		float det = 0.0f;

		MatrixMath.Matrix4Adjoint(in, out);

		// Calculate the 4x4 determinant
		// If the determinant is zero, then the inverse matrix is not unique.
		det = Matrix4x4Determinant(in);

		if (Math.abs(det) < SMALL_NUMBER) {
			// The result of attempting to derive the inverse of a
			// non-invertible
			// matrix is undefined in OpenGL:
			// http://www.opengl.org/documentation/specs/version1.1/glspec1.1/node26.html
			// However, it is NOT permitted to cause program termination or
			// corruption!
			// printf("Non-singular matrix, no inverse!\n");
			// exit(1);
		} else {
			// scale the adjoint matrix to get the inverse
			float elements[][] = out.getElement();

			for (i = 0; i < 4; i++)
				for (j = 0; j < 4; j++)
					elements[i][j] = elements[i][j] / det;

			out.setElements(elements);
		}

		return out;

	}// end Matrix4Invert

	// ========== Matrix4Adjoint()
	// ==================================================
	//
	// Purpose: calculate the adjoint of a 4x4 matrix
	//
	// Let a denote the minor determinant of matrix A obtained by
	// ij
	//
	// deleting the ith row and jth column from A.
	//
	// i+j
	// Let b = (-1) a
	// ij ji
	//
	// The matrix B = (b ) is the adjoint of A
	// ij
	//
	// ==============================================================================
	public static void Matrix4Adjoint(Matrix4 in, Matrix4 out) {
		float a1, a2, a3, a4, b1, b2, b3, b4;
		float c1, c2, c3, c4, d1, d2, d3, d4;

		/* assign to individual variable names to aid */
		/* selecting correct values */

		float elements_in[][] = in.getElement();

		a1 = elements_in[0][0];
		b1 = elements_in[0][1];
		c1 = elements_in[0][2];
		d1 = elements_in[0][3];

		a2 = elements_in[1][0];
		b2 = elements_in[1][1];
		c2 = elements_in[1][2];
		d2 = elements_in[1][3];

		a3 = elements_in[2][0];
		b3 = elements_in[2][1];
		c3 = elements_in[2][2];
		d3 = elements_in[2][3];

		a4 = elements_in[3][0];
		b4 = elements_in[3][1];
		c4 = elements_in[3][2];
		d4 = elements_in[3][3];

		/* row column labeling reversed since we transpose rows columns */

		float elements_out[][] = out.getElement();

		elements_out[0][0] = MatrixMath.Matrix3x3Determinant(b2, b3, b4, c2,
				c3, c4, d2, d3, d4);
		elements_out[1][0] = -MatrixMath.Matrix3x3Determinant(a2, a3, a4, c2,
				c3, c4, d2, d3, d4);
		elements_out[2][0] = MatrixMath.Matrix3x3Determinant(a2, a3, a4, b2,
				b3, b4, d2, d3, d4);
		elements_out[3][0] = -MatrixMath.Matrix3x3Determinant(a2, a3, a4, b2,
				b3, b4, c2, c3, c4);

		elements_out[0][1] = -MatrixMath.Matrix3x3Determinant(b1, b3, b4, c1,
				c3, c4, d1, d3, d4);
		elements_out[1][1] = MatrixMath.Matrix3x3Determinant(a1, a3, a4, c1,
				c3, c4, d1, d3, d4);
		elements_out[2][1] = -MatrixMath.Matrix3x3Determinant(a1, a3, a4, b1,
				b3, b4, d1, d3, d4);
		elements_out[3][1] = MatrixMath.Matrix3x3Determinant(a1, a3, a4, b1,
				b3, b4, c1, c3, c4);

		elements_out[0][2] = MatrixMath.Matrix3x3Determinant(b1, b2, b4, c1,
				c2, c4, d1, d2, d4);
		elements_out[1][2] = -MatrixMath.Matrix3x3Determinant(a1, a2, a4, c1,
				c2, c4, d1, d2, d4);
		elements_out[2][2] = MatrixMath.Matrix3x3Determinant(a1, a2, a4, b1,
				b2, b4, d1, d2, d4);
		elements_out[3][2] = -MatrixMath.Matrix3x3Determinant(a1, a2, a4, b1,
				b2, b4, c1, c2, c4);

		elements_out[0][3] = -MatrixMath.Matrix3x3Determinant(b1, b2, b3, c1,
				c2, c3, d1, d2, d3);
		elements_out[1][3] = MatrixMath.Matrix3x3Determinant(a1, a2, a3, c1,
				c2, c3, d1, d2, d3);
		elements_out[2][3] = -MatrixMath.Matrix3x3Determinant(a1, a2, a3, b1,
				b2, b3, d1, d2, d3);
		elements_out[3][3] = MatrixMath.Matrix3x3Determinant(a1, a2, a3, b1,
				b2, b3, c1, c2, c3);

		out.setElements(elements_out);

	}// end Matrix4Adjoint

	// ========== Matrix3x3Determinant
	// ==============================================
	//
	// Purpose: Calculate the determinant of a 3x3 matrix in the form
	//
	// | a1, b1, c1 |
	// | a2, b2, c2 |
	// | a3, b3, c3 |
	//
	// ==============================================================================
	public static float Matrix3x3Determinant(float a1, float a2, float a3,
			float b1, float b2, float b3, float c1, float c2, float c3) {
		float ans;

		ans = a1 * Matrix2x2Determinant(b2, b3, c2, c3) - b1
				* Matrix2x2Determinant(a2, a3, c2, c3) + c1
				* Matrix2x2Determinant(a2, a3, b2, b3);
		return ans;

	}// end Matrix3x3Determinant

	// ========== Matrix2x2Determinant
	// ==============================================
	//
	// Purpose: Calculate the determinant of a 2x2 matrix.
	//
	// ==============================================================================
	public static float Matrix2x2Determinant(float a, float b, float c, float d) {
		float ans;
		ans = a * d - b * c;
		return ans;

	}// end Matrix2x2Determinant

	// ========== Matrix4x4Determinant()
	// ============================================
	//
	// Purpose: calculate the determinant of a 4x4 matrix.
	//
	// Source: Graphic Gems II, Spencer W. Thomas
	//
	// ==============================================================================
	public static float Matrix4x4Determinant(Matrix4 m) {
		float ans;
		float a1, a2, a3, a4, b1, b2, b3, b4, c1, c2, c3, c4, d1, d2, d3, d4;

		/* assign to individual variable names to aid selecting */
		/* correct elements */

		float elements[][] = m.getElement();

		a1 = elements[0][0];
		b1 = elements[0][1];
		c1 = elements[0][2];
		d1 = elements[0][3];

		a2 = elements[1][0];
		b2 = elements[1][1];
		c2 = elements[1][2];
		d2 = elements[1][3];

		a3 = elements[2][0];
		b3 = elements[2][1];
		c3 = elements[2][2];
		d3 = elements[2][3];

		a4 = elements[3][0];
		b4 = elements[3][1];
		c4 = elements[3][2];
		d4 = elements[3][3];

		ans = a1 * Matrix3x3Determinant(b2, b3, b4, c2, c3, c4, d2, d3, d4)
				- b1 * Matrix3x3Determinant(a2, a3, a4, c2, c3, c4, d2, d3, d4)
				+ c1 * Matrix3x3Determinant(a2, a3, a4, b2, b3, b4, d2, d3, d4)
				- d1 * Matrix3x3Determinant(a2, a3, a4, b2, b3, b4, c2, c3, c4);

		return ans;

	}// end Matrix4x4Determinant

	// ========== V3Add
	// =============================================================
	//
	// Purpose: return vector sum c = a + b
	//
	// ==============================================================================
	public static Vector3f V3Add(Vector3f a, Vector3f b) {
		Vector3f result = new Vector3f();

		result.setX(a.getX() + b.getX());
		result.setY(a.getY() + b.getY());
		result.setZ(a.getZ() + b.getZ());

		return result;

	}// end V3Add

	// ========== V3Sub
	// =============================================================
	//
	// Purpose: return vector difference c = a-b
	//
	// ==============================================================================
	public static Vector3f V3Sub(Vector3f a, Vector3f b) {
		Vector3f result = new Vector3f();

		result.setX(a.getX() - b.getX());
		result.setY(a.getY() - b.getY());
		result.setZ(a.getZ() - b.getZ());

		return result;

	}// end V3Sub

	// ========== V3Dot
	// =============================================================
	//
	// Purpose: return the dot product of vectors a and b
	//
	// ==============================================================================
	public static float V3Dot(Vector3f a, Vector3f b) {
		return ((a.getX() * b.getX()) + (a.getY() * b.getY()) + (a.getZ() * b
				.getZ()));

	}// end V3Dot

	// ========== V3Lerp
	// ============================================================
	//
	// Purpose: linearly interpolate between vectors by an amount alpha and
	// return the resulting vector.
	//
	// When alpha=0, result=lo. When alpha=1, result=hi.
	//
	// ==============================================================================
	public static Vector3f V3Lerp(Vector3f lo, Vector3f hi, float alpha) {
		Vector3f result = new Vector3f();

		result.setX(LERP(alpha, lo.getX(), hi.getX()));
		result.setY(LERP(alpha, lo.getY(), hi.getY()));
		result.setZ(LERP(alpha, lo.getZ(), hi.getZ()));

		return (result);

	}// end V3Lerp

	public static float LERP(float t, float a, float b) {
		return (a) + (((b) - (a)) * (t));
	}

	// ========== V3Combine
	// =========================================================
	//
	// Purpose: make a linear combination of two vectors and return the result.
	//
	// result = (a * ascl) + (b * bscl)
	//
	// ==============================================================================
	public static Vector3f V3Combine(Vector3f a, Vector3f b, float ascl,
			float bscl) {
		Vector3f result = new Vector3f();

		result.setX((ascl * a.getX()) + (bscl * b.getX()));
		result.setY((ascl * a.getY()) + (bscl * b.getY()));
		result.setZ((ascl * a.getZ()) + (bscl * b.getZ()));

		return (result);

	}// end V3Combine

	// ========== V3Mul
	// =============================================================
	//
	// Purpose: Multiply two vectors together component-wise and return the
	// result.
	//
	// ==============================================================================
	public static Vector3f V3Mul(Vector3f a, Vector3f b) {
		Vector3f result = new Vector3f();

		result.setX(a.getX() * b.getX());
		result.setY(a.getY() * b.getY());
		result.setZ(a.getZ() * b.getZ());

		return (result);

	}// end V3Mul

	// ========== V3MulScalar
	// =======================================================
	//
	// Purpose: Returns (a * scalar).
	//
	// ==============================================================================
	public static Vector3f V3MulScalar(Vector3f a, float scalar) {
		Vector3f result = new Vector3f();

		result.setX(a.getX() * scalar);
		result.setY(a.getY() * scalar);
		result.setZ(a.getZ() * scalar);

		return (result);

	}// end V3Mul

	// ========== V2MakeBox
	// =========================================================
	//
	// Purpose: Makes a box from width and height.
	//
	// ==============================================================================
	public static Box2 V2MakeBox(float x, float y, float width, float height) {
		Box2 box = Box2.getZeroBox2();

		box.getOrigin().setX(x);
		box.getOrigin().setY(y);

		box.getSize().setWidth(width);
		box.getSize().setHeight(height);

		return box;
	}

	// ========== Matrix4Multiply
	// ==========================================================
	//
	// Purpose: multiply together matrices c = ab
	//
	// Notes: c must not point to either of the input matrices
	//
	// ==============================================================================
	public static Matrix4 Matrix4Multiply(Matrix4 a, Matrix4 b) {
		Matrix4 c = Matrix4.getIdentityMatrix4();
		int row;
		int column;
		int k;

		float[][] elements = c.getElement();
		float[][] elements_a = a.getElement();
		float[][] elements_b = b.getElement();

		for (row = 0; row < 4; row++) {
			for (column = 0; column < 4; column++) {
				elements[row][column] = 0;

				for (k = 0; k < 4; k++)
					elements[row][column] += elements_a[row][k]
							* elements_b[k][column];
			}
		}
		c.setElements(elements);

		return (c);

	}// end Matrix4Multiply

	// ========== V4MulPointByMatrix()
	// ==============================================
	//
	// Purpose: multiply a hom. point by a matrix and return the transformed
	// point
	//
	// Source: Graphic Gems II, Spencer W. Thomas
	//
	// ==============================================================================
	public static Vector4f V4MulPointByMatrix(Vector4f pin, Matrix4 m) {
		Vector4f pout = new Vector4f();
		float[][] elements = m.getElement();

		pout.setX((pin.getX() * elements[0][0]) + (pin.getY() * elements[1][0])
				+ (pin.getZ() * elements[2][0]) + (pin.getW() * elements[3][0]));

		pout.setY((pin.getX() * elements[0][1]) + (pin.getY() * elements[1][1])
				+ (pin.getZ() * elements[2][1]) + (pin.getW() * elements[3][1]));

		pout.setZ((pin.getX() * elements[0][2]) + (pin.getY() * elements[1][2])
				+ (pin.getZ() * elements[2][2]) + (pin.getW() * elements[3][2]));

		pout.setW((pin.getX() * elements[0][3]) + (pin.getY() * elements[1][3])
				+ (pin.getZ() * elements[2][3]) + (pin.getW() * elements[3][3]));

		return (pout);

	}// end V4MulPointByMatrix

	// ========== V3FromV4
	// ==========================================================
	//
	// Purpose: Create a new 3D vector whose components match the given 4D
	// vector. Using this function is really only sensible when the 4D
	// vector is really a 3D one being used for convenience in 4D math.
	//
	// ==============================================================================
	public static Vector3f V3FromV4(Vector4f originalVector) {
		Vector3f newVector = new Vector3f();

		// This is very bad.
		if (originalVector.getW() != 1 && originalVector.getW() != 0)
			System.out.println(String.format(
					"lossy 4D vector conversion: <%f, %f, %f, %f>\n",
					originalVector.getX(), originalVector.getY(),
					originalVector.getZ(), originalVector.getW()));

		newVector.setX(originalVector.getX());
		newVector.setY(originalVector.getY());
		newVector.setZ(originalVector.getZ());

		return newVector;

	}// end V3FromV4

	// ========== V3IsolateGreatestComponent
	// ========================================
	//
	// Purpose: Leaves unchanged the component of vector which has the greatest
	// absolute value, but zeroes the other components.
	// Example: <4, -7, 1> . <0, -7, 0>.
	// This is useful for figuring out the direction of input.
	//
	// ==============================================================================
	public static Vector3f V3IsolateGreatestComponent(Vector3f vector) {
		if (Math.abs(vector.getX()) > Math.abs(vector.getY())) {
			vector.setY(0);

			if (Math.abs(vector.getX()) > Math.abs(vector.getZ()))
				vector.setZ(0);
			else
				vector.setX(0);
		} else {
			vector.setX(0);

			if (Math.abs(vector.getY()) > Math.abs(vector.getZ()))
				vector.setZ(0);
			else
				vector.setY(0);
		}

		return vector;

	}// end V3IsolateGreatestComponent

	// ========== V3Normalize
	// =======================================================
	//
	// Purpose: normalizes the input vector and returns it
	//
	// ==============================================================================
	public static Vector3f V3Normalize(Vector3f v) {
		float len = V3Length(v);

		if (len != 0.0) {
			v.setX(v.getX() / len);
			v.setY(v.getY() / len);
			v.setZ(v.getZ() / len);
		}

		return (v);

	}// end V3Normalize
		// ========== V3Length
		// ==========================================================
		//
		// Purpose: returns length of input vector
		//
		// ==============================================================================

	public static float V3Length(Vector3f a) {
		return (float) Math.sqrt(V3SquaredLength(a));

	}// end V3Length
		// ========== V3SquaredLength
		// ===================================================
		//
		// Purpose: returns squared length of input vector
		//
		// Same as V3Dot(a,a)

	//
	// ==============================================================================

	public static float V3SquaredLength(Vector3f a) {
		return ((a.getX() * a.getX()) + (a.getY() * a.getY()) + (a.getZ() * a
				.getZ()));

	}// end V3SquaredLength

	// ========== V3Cross
	// ===========================================================
	//
	// Purpose: return the cross product c = a x b
	//
	// ==============================================================================
	public static Vector3f V3Cross(Vector3f a, Vector3f b) {
		Vector3f c = new Vector3f();

		c.setX((a.getY() * b.getZ()) - (a.getZ() * b.getY()));
		c.setY((a.getZ() * b.getX()) - (a.getX() * b.getZ()));
		c.setZ((a.getX() * b.getY()) - (a.getY() * b.getX()));

		return (c);

	}// end V3Cross

	// ========== V3Unproject
	// =======================================================
	//
	// Purpose: Given a point in viewport coordinates, returns the location in
	// object coordinates.
	//
	// Notes: viewportPoint.getZ() is the depth buffer location.
	//
	// (Drop-in replacement for gluUnProject)
	//
	// ==============================================================================
	public static Vector3f V3Unproject(Vector3f viewportPoint,
			Matrix4 modelview, Matrix4 projection, Box2 viewport) {
		Matrix4 inversePM = Matrix4.getIdentityMatrix4();
		Vector3f normalized = Vector3f.getZeroVector3f();
		Vector3f modelPoint = Vector3f.getZeroVector3f();

		normalized.setX(2
				* (viewportPoint.getX() - viewport.getOrigin().getX())
				/ V2BoxWidth(viewport) - 1);
		normalized.setY(2
				* (viewportPoint.getY() - viewport.getOrigin().getY())
				/ V2BoxHeight(viewport) - 1);
		normalized.setZ(2 * (viewportPoint.getZ()) - 1);

		inversePM = Matrix4Invert(Matrix4Multiply(modelview, projection));
		modelPoint = V3MulPointByProjMatrix(normalized, inversePM);

		return modelPoint;
	}

	// ========== V3MulPointByProjMatrix
	// ============================================
	//
	// Purpose: multiply a point by a projective matrix and return the
	// transformed point
	//
	// ==============================================================================
	public static Vector3f V3MulPointByProjMatrix(Vector3f pin, Matrix4 m) {
		Vector3f pout = Vector3f.getZeroVector3f();
		float w = 0.0f;

		float[][] elements = m.getElement();
		pout.setX((pin.getX() * elements[0][0]) + (pin.getY() * elements[1][0])
				+ (pin.getZ() * elements[2][0]) + elements[3][0]);

		pout.setY((pin.getX() * elements[0][1]) + (pin.getY() * elements[1][1])
				+ (pin.getZ() * elements[2][1]) + elements[3][1]);

		pout.setZ((pin.getX() * elements[0][2]) + (pin.getY() * elements[1][2])
				+ (pin.getZ() * elements[2][2]) + elements[3][2]);

		w = (pin.getX() * elements[0][3]) + (pin.getY() * elements[1][3])
				+ (pin.getZ() * elements[2][3]) + elements[3][3];

		if (w != 0.0) {
			pout.setX(pout.getX() / w);
			pout.setY(pout.getY() / w);
			pout.setZ(pout.getZ() / w);
		}

		return (pout);

	}// end V3MulPointByProjMatrix

	// ========== V3Make
	// ============================================================
	//
	// Purpose: create, initialize, and return a new vector
	//
	// ==============================================================================
	public static Vector3f V3Make(float x, float y, float z) {
		Vector3f v = new Vector3f(x, y, z);
		return (v);

	}// end V3Make
		// ========== V3UnionBox
		// ========================================================
		//
		// Purpose: Returns the smallest box that completely encloses both aBox
		// and
		// bBox.

	//
	// Notes: If you pass something stupid in as the parameter, you will get
	// an appropriately stupid answer.
	//
	// ==============================================================================

	public static Box3 V3UnionBox(Box3 aBox, Box3 bBox) {
		Box3 bounds = new Box3();

		if (aBox==null || aBox == Box3.getInvalidBox())			
			try {
				bounds = (Box3) bBox.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else if (bBox==null || bBox == Box3.getInvalidBox())
			try {
				bounds = (Box3) aBox.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else {
			bounds.getMin().setX(
					Math.min(aBox.getMin().getX(), bBox.getMin().getX()));
			bounds.getMin().setY(
					Math.min(aBox.getMin().getY(), bBox.getMin().getY()));
			bounds.getMin().setZ(
					Math.min(aBox.getMin().getZ(), bBox.getMin().getZ()));

			bounds.getMax().setX(
					Math.max(aBox.getMax().getX(), bBox.getMax().getX()));
			bounds.getMax().setY(
					Math.max(aBox.getMax().getY(), bBox.getMax().getY()));
			bounds.getMax().setZ(
					Math.max(aBox.getMax().getZ(), bBox.getMax().getZ()));
		}
		return bounds;

	}// end V3UnionBox
		// ========== Matrix4Rotate()
		// ===================================================
		//
		// Purpose: Rotates the given matrix by the given number of degrees
		// around
		// each axis, placing the rotated matrix into the Matrix specified

	// by the result parameter. Also returns result.
	//
	// Rotation order is first X, then Y, and lastly Z.
	//
	// ==============================================================================

	public static Matrix4 Matrix4Rotate(Matrix4 original,
			Vector3f degreesToRotate) {
		TransformComponents rotateComponents = TransformComponents
				.getIdentityComponents();
		Matrix4 addedRotation = Matrix4.getIdentityMatrix4();
		Matrix4 result = Matrix4.getIdentityMatrix4();

		// Create a new matrix that causes the rotation we want.
		// (start with identity matrix)
		rotateComponents.getRotate().setX(
				(float) Math.toRadians(degreesToRotate.getX()));
		rotateComponents.getRotate().setY(
				(float) Math.toRadians(degreesToRotate.getY()));
		rotateComponents.getRotate().setZ(
				(float) Math.toRadians(degreesToRotate.getZ()));
		addedRotation = MatrixMath
				.Matrix4CreateTransformation(rotateComponents);

		result = Matrix4Multiply(original, addedRotation); // rotate at
															// rotationCenter

		return result;

	}// end Matrix4Rotate

	// ========== Matrix4CreateTransformation()
	// =====================================
	//
	// Purpose: Given the scale, shear, rotation, translation, and perspective
	// paramaters, create a 4x4 transformation.element matrix used to
	// modify row-matrix points.
	//
	// To reverse the procedure, pass the returned matrix to
	// Matrix4DecomposeTransformation().
	//
	// Notes: This ignores perspective, which is not supported.
	//
	// Source: Allen Smith, after too much handwork.
	//
	// ==============================================================================
	public static Matrix4 Matrix4CreateTransformation(
			TransformComponents components) {
		Matrix4 transformation = Matrix4.getIdentityMatrix4(); // zero out the
																// whole thing.
		float rotation[][] = new float[3][3];

		// Create the rotation matrix.
		double sinX = Math.sin(components.getRotate().getX());
		double cosX = Math.cos(components.getRotate().getX());

		double sinY = Math.sin(components.getRotate().getY());
		double cosY = Math.cos(components.getRotate().getY());

		double sinZ = Math.sin(components.getRotate().getZ());
		double cosZ = Math.cos(components.getRotate().getZ());

		rotation[0][0] = (float) (cosY * cosZ);
		rotation[0][1] = (float) -(cosY * sinZ);
		rotation[0][2] = (float) sinY;

		rotation[1][0] = (float) (sinX * sinY * cosZ + cosX * sinZ);
		rotation[1][1] = (float) (-sinX * sinY * sinZ + cosX * cosZ);
		rotation[1][2] = (float) -(sinX * cosY);

		rotation[2][0] = (float) (-cosX * sinY * cosZ + sinX * sinZ);
		rotation[2][1] = (float) (cosX * sinY * sinZ + sinX * cosZ);
		rotation[2][2] = (float) (cosX * cosY);

		// Build the transformation.element matrix.
		// Seeing the transformation.element matrix in these terms helps to make
		// sense of Matrix4DecomposeTransformation().
		float elements[][] = transformation.getElement();

		elements[0][0] = components.getScale().getX() * rotation[0][0];
		elements[0][1] = components.getScale().getX() * rotation[0][1];
		elements[0][2] = components.getScale().getX() * rotation[0][2];

		elements[1][0] = components.getScale().getY()
				* (components.shear_XY * rotation[0][0] + rotation[1][0]);
		elements[1][1] = components.getScale().getY()
				* (components.shear_XY * rotation[0][1] + rotation[1][1]);
		elements[1][2] = components.getScale().getY()
				* (components.shear_XY * rotation[0][2] + rotation[1][2]);

		elements[2][0] = components.getScale().getZ()
				* (components.shear_XZ * rotation[0][0] + components.shear_YZ
						* rotation[1][0] + rotation[2][0]);
		elements[2][1] = components.getScale().getZ()
				* (components.shear_XZ * rotation[0][1] + components.shear_YZ
						* rotation[1][1] + rotation[2][1]);
		elements[2][2] = components.getScale().getZ()
				* (components.shear_XZ * rotation[0][2] + components.shear_YZ
						* rotation[1][2] + rotation[2][2]);

		// translation is so nice and easy.
		elements[3][0] = components.translate.getX();
		elements[3][1] = components.translate.getY();
		elements[3][2] = components.translate.getZ();

		// And lastly the corner.
		elements[3][3] = 1;

		return transformation;

	}// end Matrix4CreateTransformation

	// ========== Matrix4DecomposeXYZRotation
	// =======================================
	//
	// Purpose: Decomposes a rotation matrix into an X-Y-Z angle (in radians)
	// which would yield it, such that the X angle is applied first and
	// the Z angle last.
	//
	// The matrix must not have any affect other than rotation.
	//
	// ==============================================================================
	public static Vector3f Matrix4DecomposeXYZRotation(Matrix4 matrix) {
		Vector3f rotationAngle = Vector3f.getZeroVector3f();

		float[][] elements = matrix.getElement();

		// Y is easy.
		rotationAngle.setY((float) Math.asin(elements[0][2]));

		// cos(Y) != 0.
		// We can just use some simple algebra on the simplest components
		// of the rotation matrix.

		if (Math.abs(Math.cos(rotationAngle.getY())) > SMALL_NUMBER)// within a
																	// tolerance
																	// of zero.
		{
			rotationAngle.setX((float) Math.atan2(-elements[1][2],
					elements[2][2]));
			rotationAngle.setZ((float) Math.atan2(-elements[0][1],
					elements[0][0]));
		}
		// cos(Y) == 0; so Y = +/- PI/2
		// this is a "singularity" that zeroes out the information we would
		// usually use to determine X and Y.

		else if (rotationAngle.getY() < 0) // -PI/2
		{
			rotationAngle.setX((float) -Math.atan2(elements[2][1],
					elements[1][1]));
			rotationAngle.setZ(0);
		} else if (rotationAngle.getY() > 0) // +PI/2
		{
			rotationAngle.setX((float) Math.atan2(elements[2][1],
					elements[1][1]));
			rotationAngle.setZ(0);
		}

		return rotationAngle;

	}// end Matrix4DecomposeXYZRotation

	// ========== VolumeCanIntersectBox()
	// ===========================================
	//
	// Purpose: Checks whether a bounding box in model view space may intersect
	// a 2-d screen space bounding box, given a complete GL transform.
	//
	// This routine might return true even if there is no intersection,
	// but if it returns false, it is guaranteed that the bounding box
	// and screen space rect are disjoint.
	//
	// ==============================================================================
	public static boolean VolumeCanIntersectBox(Box3 bounds, Matrix4 transform,
			Box2 box) {
		if (bounds.getMin().getX() > bounds.getMax().getX()
				|| bounds.getMin().getY() > bounds.getMax().getY()
				|| bounds.getMin().getZ() > bounds.getMax().getZ())
			return false;

		float aabb_mv[] = { bounds.getMin().getX(), bounds.getMin().getY(),
				bounds.getMin().getZ(), bounds.getMax().getX(),
				bounds.getMax().getY(), bounds.getMax().getZ() };
		float aabb_ndc[] = new float[6];
		float m[] = new float[16];

		MatrixMath.Matrix4GetGLMatrix4(transform, m);

		GLMatrixMath.aabbToClipbox(aabb_mv, m, aabb_ndc);

		float x1 = V2BoxMinX(box);
		float x2 = V2BoxMaxX(box);
		float y1 = V2BoxMinY(box);
		float y2 = V2BoxMaxY(box);

		if (x1 > aabb_ndc[3] || x2 < aabb_ndc[0] || y1 > aabb_ndc[4]
				|| y2 < aabb_ndc[1]) {
			return false;
		}
		return true;
	}// end VolumeCanIntersectBox
		// ========== Matrix4GetGLMatrix4
		// ===============================================
		//
		// Purpose: Converts the row-major row-vector matrix into a flat column-
		// major column-vector matrix understood by OpenGL.

	//
	//
	// +- -+ +- -++- -+
	// +- -+| a d g 0 | | a b c x || x |
	// |x y z 1|| b e h 0 | | d e f y || y | +- -+
	// +- -+| c f i 0 | -. | g h i z || z | -. |a d g 0 b e h c f i 0 x y z 1|
	// | x y z 1 | | 0 0 0 1 || 1 | +- -+
	// +- -+ +- -++- -+
	// LDraw Matrix Transpose OpenGL Matrix Format
	// Format (flat column-major of transpose)
	// (also Matrix4 format)
	//
	// ==============================================================================

	public static void Matrix4GetGLMatrix4(Matrix4 matrix,
			float[] glTransformation) {
		int row, column;

		float[][] element = matrix.getElement();

		for (row = 0; row < 4; row++) {
			for (column = 0; column < 4; column++) {
				glTransformation[row * 4 + column] = MatrixMath
						.round(element[row][column]);
			}
		}

	}// end Matrix4GetGLMatrix4

	// ========== VolumeCanIntersectPoint()
	// =========================================
	//
	// Purpose: Checks whether a point in screen space might interact with a 3-d
	// bounding box. Since we do "fuzzy" point testing using a screen
	// space bounds (to add thickness to infinitely thin lines) we use
	// the point's "soft" bounds here too.
	//
	// Notes: testDepthSoFar represents a limit that culls everything behind
	// it. Thus if hit testing finds a near object, farther back
	// objects are excluded via a fast path.
	//
	// ==============================================================================
	public static boolean VolumeCanIntersectPoint(Box3 bounds,
			Matrix4 transform, Box2 box, float testDepthSoFar) {
		if (bounds.getMin().getX() > bounds.getMax().getX()
				|| bounds.getMin().getY() > bounds.getMax().getY()
				|| bounds.getMin().getZ() > bounds.getMax().getZ())
			return false;

		// We gotta use clipped conversion to NDC coordinates to get our
		// device-space
		// bounding box. If we don't, geometry behind the camera will mirror
		// around the
		// XZ and YZ planes and cause chaos.

		float aabb_mv[] = { bounds.getMin().getX(), bounds.getMin().getY(),
				bounds.getMin().getZ(), bounds.getMax().getX(),
				bounds.getMax().getY(), bounds.getMax().getZ() };
		float aabb_ndc[] = new float[6];
		float m[] = new float[16];

		MatrixMath.Matrix4GetGLMatrix4(transform, m);

		GLMatrixMath.aabbToClipbox(aabb_mv, m, aabb_ndc);

		float x1 = V2BoxMinX(box);
		float x2 = V2BoxMaxX(box);
		float y1 = V2BoxMinY(box);
		float y2 = V2BoxMaxY(box);

		if (x1 > aabb_ndc[3] || x2 < aabb_ndc[0] || y1 > aabb_ndc[4]
				|| y2 < aabb_ndc[1] || testDepthSoFar < aabb_ndc[2]) // If the
																		// test
																		// depth
																		// is
																		// LESS
																		// than
																		// our
																		// MIN Z
																		// then
																		// we
																		// ALERADY
																		// are
																		// closer
																		// than
																		// this
																		// entire
																		// test
																		// model
																		// -
																		// skip
																		// this
																		// model!
		{
			return false;
		}
		return true;
	}// end VolumeCanIntersectPoint

	// ========== Matrix4DecomposeZYXRotation
	// =======================================
	//
	// Purpose: Decomposes a rotation matrix into a Z-Y-X angle (in radians)
	// which would yield it, such that the Z angle is applied first and
	// the X angle last.
	//
	// The matrix must not have any affect other than rotation.
	//
	// Notes: Any given rotation (matrix) is unique, but the angles which can
	// produce it are not. We must make assumptions when decomposing.
	// One of those assumptions is the order in which we assume the
	// constituent angles were applied to produce the rotation. This
	// method assumes they were applied in ZYX order, which will yield
	// completely different numbers than the XYZ order would give for
	// the same matrix.
	//
	// ==============================================================================
	public static Vector3f Matrix4DecomposeZYXRotation(Matrix4 matrix) {
		Vector3f rotationAngle = Vector3f.getZeroVector3f();

		float[][] element = matrix.getElement();
		// Y is easy.
		rotationAngle.setY((float) Math.asin(element[2][0]));

		// cos(Y) != 0.
		// We can just use some simple algebra on the simplest components
		// of the rotation matrix.

		if (Math.abs(Math.cos(rotationAngle.getY())) > SMALL_NUMBER)// within a
																	// tolerance
																	// of zero.
		{
			rotationAngle.setX((float) Math
					.atan2(-element[2][1], element[2][2]));
			rotationAngle.setZ((float) Math
					.atan2(-element[1][0], element[0][0]));
		}
		// cos(Y) == 0; so Y = +/- PI/2
		// this is a "singularity" that zeroes out the information we would
		// usually use to determine X and Y.

		else if (rotationAngle.getY() < 0) // -PI/2
		{
			rotationAngle
					.setX((float) Math.atan2(element[1][2], element[0][2]));
			rotationAngle.setZ(0);
		} else if (rotationAngle.getY() > 0) // +PI/2
		{
			rotationAngle
					.setX((float) Math.atan2(element[0][1], element[1][1]));
			rotationAngle.setZ(0);
		}

		return rotationAngle;

	}// end Matrix4DecomposeZYXRotation

	// ========== FloatsApproximatelyEqual
	// ==========================================
	//
	// Purpose: Testing floating-point numbers for equality is horribly
	// difficult, owing to tiny little rounding errors. This method
	// attempts to determine approximate equality.
	//
	// It is insufficient to test with some tolerance value (such as
	// SMALL_NUMBER), because the minimum difference between two
	// floating-point values changes depending on how big the integer
	// component is.
	//
	// The trick is described here:
	// http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm
	//
	// Basically, the bits of floating-point values are guaranteed to
	// be ordered, so if we compare them as SIGN-MAGNITUDE integers,
	// the integer difference represents the true relative difference.
	// There are 0xFFFFFFFF possible floating-point values; a
	// difference of, say, 2 doesn't amount to much!
	//
	// The main issue now is converting from 2's compliment into
	// sign-magnitude ints.
	//
	// ==============================================================================
	public static boolean FloatsApproximatelyEqual(float float1, float float2) {
		// todo
		// �뜝�룞�삕�뜝�룞�삕�뜝�룞�삕�뜝�룞�삕�뜝�룞�삕 �뜝�뙗�눦�삕�뜝�룞�삕. �뜝�룞�삕�뜝�룞�삕黎귛떉�뜝占�
		// �뜝�룞�삕�뜝�룞�삕�뜝�룞�삕 �뜝�룜�뼸�뜝�룞�삕 �뜝�룞�삕�뜝�룞�삕?
		// Use a union; it's less scary than *(int*)point1.getZ();
		// union intFloat
		// {
		// int32_t intValue;
		// float floatValue;
		// };
		//
		// union intFloat value1;
		// union intFloat value2;
		boolean closeEnough = false;
		//
		// // First translate the floats into integers via the union.
		// value1.floatValue = float1;
		// value2.floatValue = float2;
		//
		// // Make value1.intValue lexicographically ordered as a
		// twos-complement int
		// // (Floating-point -0 == 0x80000000; the next number less than -0 is
		// // 0x80000001, etc.) So we do: value1.intValue = 0x80000000 -
		// value1.intValue;
		// if (value1.intValue < 0)
		// value1.intValue = (1 << (sizeof(float) * 8 - 1)) - value1.intValue;
		//
		// // ...and do the same for value2
		// if (value2.intValue < 0)
		// value2.intValue = (1 << (sizeof(float) * 8 - 1)) - value2.intValue;
		//
		// // Less than 5 integer positions different will be considered equal.
		// This
		// // number was pulled out of my hat. Each integer difference equals a
		// // different number depending on the magnitute of the float value.
		// if(abs(value1.intValue - value2.intValue) < 5)
		// {
		// closeEnough = true;
		// }
		// // The int method doesn't seem to work very well for numbers very
		// close to
		// // zero, where float values can have extremely precise
		// representations. So
		// // if we are trying to compare a float to 0, we fall back on the old
		// // precision threshold.
		// else if( float1 > -1 float1 < 1
		// float1 > -1 float1 < 1 )
		// {
		// if( fabs(float1 - float2) < SMALL_NUMBER)
		// {
		// closeEnough = true;
		// }
		// }

		if (Math.abs(float1 - float2) < SMALL_NUMBER)
			closeEnough = true;

		return closeEnough;

	}// end FloatsApproximatelyEqual

	// ========== V3EqualBoxes
	// ======================================================
	//
	// Purpose: Returns 1 (YES) if the two boxes are equal; 0 otherwise.
	//
	// ==============================================================================
	public static boolean V3EqualBoxes(Box3 box1, Box3 box2) {
		return (box1.getMin().getX() == box2.getMin().getX()
				&& box1.getMin().getY() == box2.getMin().getY()
				&& box1.getMin().getZ() == box2.getMin().getZ() &&

				box1.getMax().getX() == box2.getMax().getX()
				&& box1.getMax().getY() == box2.getMax().getY() && box1
				.getMax().getZ() == box2.getMax().getZ());

	}// end V3EqualBoxes
		// ========== V3Project
		// =========================================================
		//
		// Purpose: Projects the given object point into viewport coordinates.
		//
		// (Drop-in replacement for gluProject)

	//
	// ==============================================================================

	public static Vector3f V3Project(Vector3f objPoint, Matrix4 modelview,
			Matrix4 projection, Box2 viewport) {
		Vector3f transformedPoint = Vector3f.getZeroVector3f();
		Vector3f windowPoint = Vector3f.getZeroVector3f();

		transformedPoint = V3MulPointByProjMatrix(objPoint,
				Matrix4Multiply(modelview, projection));

		windowPoint.setX(viewport.getOrigin().getX()
				+ (V2BoxWidth(viewport) * (transformedPoint.getX() + 1)) / 2);
		windowPoint.setY(viewport.getOrigin().getY()
				+ (V2BoxHeight(viewport) * (transformedPoint.getY() + 1)) / 2);
		windowPoint.setZ((transformedPoint.getZ() + 1) / 2);

		return windowPoint;

	}// end V3Project

	// ========== V3UnionBoxAndPoint
	// ================================================
	//
	// Purpose: Returns the smallest box that completely encloses both box and
	// point.
	//
	// ==============================================================================
	public static Box3 V3UnionBoxAndPoint(Box3 box, Vector3f point) {
		Box3 bounds = new Box3();
		if (box==null || box == Box3.getInvalidBox()) {
			float min = point.getX();
			bounds.getMin().setX(min);
			min = point.getY();
			bounds.getMin().setY(min);
			min = point.getZ();
			bounds.getMin().setZ(min);

			float max = point.getX();
			bounds.getMax().setX(max);
			max = point.getY();
			bounds.getMax().setY(max);
			max = point.getZ();
			bounds.getMax().setZ(max);
		} else {
			float min = Math.min(box.getMin().getX(), point.getX());
			bounds.getMin().setX(min);
			min = Math.min(box.getMin().getY(), point.getY());
			bounds.getMin().setY(min);
			min = Math.min(box.getMin().getZ(), point.getZ());
			bounds.getMin().setZ(min);

			float max = Math.max(box.getMax().getX(), point.getX());
			bounds.getMax().setX(max);
			max = Math.max(box.getMax().getY(), point.getY());
			bounds.getMax().setY(max);
			max = Math.max(box.getMax().getZ(), point.getZ());
			bounds.getMax().setZ(max);
		}
		return bounds;

	}// end V3UnionBoxAndPoint

	// ========== Matrix4Scale()
	// ====================================================
	//
	// Purpose: Scales the given matrix by the given factors along
	// each axis. Returns result.
	//
	// ==============================================================================
	public static Matrix4 Matrix4Scale(Matrix4 original, Vector3f scaleFactors) {
		TransformComponents components = TransformComponents
				.getIdentityComponents();
		Matrix4 scalingMatrix = Matrix4.getIdentityMatrix4();
		Matrix4 result = Matrix4.getIdentityMatrix4();

		// Create a new matrix that causes the rotation we want.
		// (start with identity matrix)
		components.scale = scaleFactors;
		scalingMatrix = Matrix4CreateTransformation(components);

		result = Matrix4Multiply(original, scalingMatrix);

		return result;

	}// end Matrix4Scale

	// ========== Matrix4Translate()
	// ================================================
	//
	// Purpose: Translates the given matrix by the given displacement, placing
	// the translated matrix into the Matrix specified by the result
	// parameter. Also returns result.
	//
	// ==============================================================================
	public static Matrix4 Matrix4Translate(Matrix4 original,
			Vector3f displacement) {
		Matrix4 result = Matrix4.getIdentityMatrix4();

		// Copy original to result
		result = original;
		float[][] element = result.getElement();

		element[3][0] += displacement.getX(); // applied directly to
		element[3][1] += displacement.getY(); // the matrix because
		element[3][2] += displacement.getZ(); // that's easier here.

		return result;

	}// end Matrix4Translate
		// ========== V3Negate
		// ==========================================================
		//
		// Purpose: negates the input vector and returns it
		//
		// ==============================================================================

	public static Vector3f V3Negate(Vector3f v) {
		return V3MulScalar(v, -1);

	}// end V3Negate

	// ========== Matrix4DecomposeTransformation()
	// ==================================
	//
	// Purpose: Decompose a non-degenerate 4x4 transformation.element matrix
	// into the sequence of transformations that produced it.
	//
	// [Sx][Sy][Sz][Shearx/y][Sx/z][Sz/y][Rx][Ry][Rz][Tx][Ty][Tz][P(x,y,z,w)]
	//
	// The coefficient of each transformation.element is returned in
	// the corresponding element of the vector tran.
	//
	// Returns: 1 upon success, 0 if the matrix is singular.
	//
	// Source: Graphic Gems II, Spencer W. Thomas
	//
	// ==============================================================================
	public static int Matrix4DecomposeTransformation(Matrix4 originalMatrix,
			TransformComponents decomposed) {
		int counter = 0;
		int j = 0;
		Matrix4 localMatrix = new Matrix4();
		localMatrix.setElements(originalMatrix.getElement());
		Matrix4 pmat, invpmat, tinvpmat;

		Vector4f prhs, psol;
		Vector3f row[] = new Vector3f[3];
		for (int i = 0; i < 3; i++)
			row[i] = Vector3f.getZeroVector3f();

		prhs = new Vector4f();

		float[][] elements_local = localMatrix.getElement();

		// Normalize the matrix.
		if (elements_local[3][3] == 0)
			return 0;

		for (counter = 0; counter < 4; counter++)
			for (j = 0; j < 4; j++)
				elements_local[counter][j] /= elements_local[3][3];

		// ---------- Perspective
		// ---------------------------------------------------
		// Perspective is not used by Bricksmith.

		// pmat is used to solve for perspective, but it also provides an easy
		// way
		// to test for singularity of the upper 3x3 component.
		pmat = new Matrix4();
		pmat.setElements(localMatrix.getElement());
		for (counter = 0; counter < 3; counter++)
			pmat.getElement()[counter][3] = 0;
		pmat.getElement()[3][3] = 1;

		if (Matrix4x4Determinant(pmat) == 0.0f)
			return 0;

		// First, isolate perspective. This is the messiest.
		if (elements_local[0][3] != 0 || elements_local[1][3] != 0
				|| elements_local[2][3] != 0) {
			// prhs is the right hand side of the equation.
			prhs.setX(elements_local[0][3]);
			prhs.setY(elements_local[1][3]);
			prhs.setZ(elements_local[2][3]);
			prhs.setW(elements_local[3][3]);

			// Solve the equation by inverting pmat and multiplying prhs by the
			// inverse. (This is the easiest way, not necessarily the best.)
			// inverse function (and Matrix4x4Determinant, above) from the
			// Matrix
			// Inversion gem in the first volume.
			invpmat = Matrix4Invert(pmat);
			tinvpmat = Matrix4Transpose(invpmat);
			psol = V4MulPointByMatrix(prhs, tinvpmat);

			// Stuff the answer away.
			decomposed.perspective.setX(psol.getX());
			decomposed.perspective.setY(psol.getY());
			decomposed.perspective.setZ(psol.getZ());
			decomposed.perspective.setW(psol.getW());
			// Clear the perspective partition.
			elements_local[0][3] = 0;
			elements_local[1][3] = 0;
			elements_local[2][3] = 0;
			elements_local[3][3] = 1;
		}
		// No perspective
		else {
			decomposed.perspective.setX(0);
			decomposed.perspective.setY(0);
			decomposed.perspective.setZ(0);
			decomposed.perspective.setW(0);
		}

		// ---------- Translation
		// ---------------------------------------------------

		// This is really easy.
		decomposed.translate.setX(elements_local[3][0]);
		decomposed.translate.setY(elements_local[3][1]);
		decomposed.translate.setZ(elements_local[3][2]);

		// Zero out the translation as we continue to decompose.
		for (counter = 0; counter < 3; counter++) {
			elements_local[3][counter] = 0;
		}

		// ---------- Now get scale and shear.
		// --------------------------------------

		// First translate to vector format, because all our linear combination
		// functions expect vector datatypes.
		for (counter = 0; counter < 3; counter++) {
			row[counter].setX(elements_local[counter][0]);
			row[counter].setY(elements_local[counter][1]);
			row[counter].setZ(elements_local[counter][2]);
		}

		// Compute X scale factor and normalize first row.
		decomposed.scale.setX(V3Length(row[0]));
		row[0] = V3Scale(row[0], 1.0f);

		// Compute XY shear factor and make 2nd row orthogonal to 1st.
		decomposed.shear_XY = V3Dot(row[0], row[1]);
		row[1] = V3Combine(row[1], row[0], 1.0f, -decomposed.shear_XY);

		// Now, compute Y scale and normalize 2nd row.
		decomposed.scale.setY(V3Length(row[1]));
		row[1] = V3Scale(row[1], 1.0f);
		decomposed.shear_XY /= decomposed.scale.getY();

		// Compute XZ and YZ shears, orthogonalize 3rd row.
		decomposed.shear_XZ = V3Dot(row[0], row[2]);
		row[2] = V3Combine(row[2], row[0], 1.0f, -decomposed.shear_XZ);
		decomposed.shear_YZ = V3Dot(row[1], row[2]);
		row[2] = V3Combine(row[2], row[1], 1.0f, -decomposed.shear_YZ);

		// Next, get Z scale and normalize 3rd row.
		decomposed.scale.setZ(V3Length(row[2]));
		row[2] = V3Scale(row[2], 1.0f);
		decomposed.shear_XZ /= decomposed.scale.getZ();
		decomposed.shear_YZ /= decomposed.scale.getZ();

		// At this point, the matrix (in rows[]) is orthonormal.
		// Check for a coordinate system flip. If the determinant is -1, then
		// negate the matrix and the scaling factors.
		if (V3Dot(row[0], V3Cross(row[1], row[2])) < 0) {
			V3MulScalar(decomposed.scale, -1.0f);

			for (counter = 0; counter < 3; counter++) {
				V3MulScalar(row[counter], -1.0f);
			}

		}

		// ---------- Extract Rotation Angles
		// ---------------------------------------

		// Convert back to the matrix datatype, because that is what the
		// decomposition function expects.
		localMatrix = Matrix4.getIdentityMatrix4();
		elements_local = localMatrix.getElement();
		for (counter = 0; counter < 3; counter++) {
			elements_local[counter][0] = row[counter].getX();
			elements_local[counter][1] = row[counter].getY();
			elements_local[counter][2] = row[counter].getZ();
		}

		// extract rotation
		decomposed.rotate = Matrix4DecomposeXYZRotation(localMatrix);

		// All done!
		return 1;

	}// end Matrix4DecomposeTransformation
		// ========== Matrix4Transpose()
		// ================================================
		//
		// Purpose: transpose rotation portion of matrix a, return b
		//
		// Source: Graphic Gems II, Spencer W. Thomas
		//
		// ==============================================================================

	public static Matrix4 Matrix4Transpose(Matrix4 a) {
		Matrix4 transpose = Matrix4.getIdentityMatrix4();
		int i, j;
		float[][] elements = transpose.getElement();
		float[][] elements_a = a.getElement();
		for (i = 0; i < 4; i++)
			for (j = 0; j < 4; j++)
				elements[i][j] = elements_a[j][i];

		return transpose;

	}// end Matrix4Transpose
		// ========== V3Scale
		// ===========================================================
		//
		// Purpose: scales the input vector to the new length and returns it
		//
		// ==============================================================================

	public static Vector3f V3Scale(Vector3f v, float newlen) {
		float len = V3Length(v);

		if (len != 0.0) {
			v.setX(v.getX() * newlen / len);
			v.setY(v.getY() * newlen / len);
			v.setZ(v.getZ() * newlen / len);
		}

		return (v);

	}// end V3Scale

	// ========== Matrix3MakeNormalTransformFromProjMatrix
	// ==========================
	//
	// Purpose: Normal vectors (for lighting) cannot be transformed by the same
	// matrix which transforms vertexes. This method returns the
	// correct matrix to transform normals for the given vertex
	// transform (modelview) matrix.
	//
	// Notes: See "Matrices" notes in Bricksmith/Information for derivation.
	//
	// Also http://www.lighthouse3d.com/opengl/glsl/index.php?normalmatrix
	// and http://www.songho.ca/opengl/gl_normaltransform.html
	//
	// We only need a 3x3 matrix because the translation in the 4x4
	// transform (row 4) is undesirable anyway (a 4D vector should be
	// [x y z 0]), and column 4 isn't used.
	//
	// ==============================================================================
	public static Matrix3 Matrix3MakeNormalTransformFromProjMatrix(
			Matrix4 transformationMatrix) {
		Matrix4 normalTransform = Matrix4.getIdentityMatrix4();
		Matrix3 normalTransform3 = Matrix3.getIdentityMatrix3();
		int row = 0;
		int column = 0;

		// The normal transform is the inverse transpose of the vertex
		// transform.

		normalTransform = Matrix4Invert(transformationMatrix);
		normalTransform = Matrix4Transpose(normalTransform);

		float[][] element = normalTransform.getElement();
		// Convert to a 3x3 matrix, because row 4 and column 4 are unnecessary.
		for (row = 0; row < 3; row++) {
			for (column = 0; column < 3; column++)
				element[row][column] = element[row][column];
		}

		return normalTransform3;

	}// end Matrix3MakeNormalTransformFromProjMatrix

	// ========== V3RayIntersectsSegment
	// ============================================
	//
	// Purpose: Determines if the shortest distance between the ray and segment
	// is within the tolerance.
	//
	// Notes: The tolerance is necessary because two lines in 3D graphics will
	// almost never actually intersect. But they may be within 1 pixel
	// of each other!
	//
	// Adapted from
	// http://softsurfer.com/Archive/algorithm_0106/algorithm_0106.htm
	// The body contains commented-out code for determining the closest
	// points between two line segments, rather than an infinite ray
	// and a finite segment.
	//
	// Parameters: ray - selection ray
	// segment2 - line segment to test (in same coordinates as ray)
	// tolerance - max distance to consider intersection
	// intersectDepth - on return, distance from ray origin to segment
	// intersection
	//
	// ==============================================================================
	public static boolean V3RayIntersectsSegment(Ray3 segment1,
			Segment3 segment2, float tolerance, FloatBuffer intersectDepth) {
		Vector3f u = segment1.getDirection(); // V3Sub(segment1.point1,
												// segment1.point0);
		Vector3f v = V3Sub(segment2.getPoint1(), segment2.getPoint0());
		Vector3f w = V3Sub(segment1.getOrigin(), segment2.getPoint0());
		float a = V3Dot(u, u); // always >= 0
		float b = V3Dot(u, v);
		float c = V3Dot(v, v); // always >= 0
		float d = V3Dot(u, w);
		float e = V3Dot(v, w);
		float D = a * c - b * b; // always >= 0
		float sc = 0; // sc = sN / sD, default sD = D >= 0
		float sN = 0;
		float sD = 0;
		float tc = 0; // tc = tN / tD, default tD = D >= 0
		float tN = 0;
		float tD = 0;
		boolean intersects = false;

		// compute the line parameters of the two closest points
		if (D < SMALL_NUMBER) { // the lines are almost parallel
			sN = 0.0f; // force using point0 on segment S1
			sD = 1.0f; // to prevent possible division by 0.0 later

			tN = e;
			tD = c;
		} else {
			// get the closest points on the infinite lines

			sN = (b * e - c * d);
			sD = D;

			tN = (a * e - b * d);
			tD = D;

			if (sN < 0.0) // sc < 0 => the s=0 edge is visible
			{
				sN = 0.0f;
				tN = e;
				tD = c;
			}
			// else if (sN > sD) // sc > 1 => the s=1 edge is visible
			// {
			// I think this is the part needed if the ray had been a segment
			// instead of a ray
			// As it is, we only care that sN >= 0
			// sN = sD;
			// tN = e + b;
			// tD = c;
			// }
		}

		if (tN < 0.0) // tc < 0 => the t=0 edge is visible
		{
			tN = 0.0f;
			// recompute sc for this edge
			if (-d < 0.0) {
				sN = 0.0f;
			}
			// else if (-d > a)
			// {
			// I think this is the part needed if the ray had been a segment
			// instead of a ray
			// As it is, we only care that sN >= 0
			// sN = sD;
			// }
			else {
				sN = -d;
				sD = a;
			}
		} else if (tN > tD) // tc > 1 => the t=1 edge is visible
		{
			tN = tD;
			// recompute sc for this edge
			if ((-d + b) < 0.0) {
				sN = 0;
			}
			// else if ((-d + b) > a)
			// {
			// I think this is the part needed if the ray had been a segment
			// instead of a ray
			// As it is, we only care that sN >= 0
			// sN = sD;
			// }
			else {
				sN = (-d + b);
				sD = a;
			}
		}
		// finally do the division to get sc and tc
		sc = (float) (Math.abs(sN) < SMALL_NUMBER ? 0.0 : sN / sD);
		tc = (float) (Math.abs(tN) < SMALL_NUMBER ? 0.0 : tN / tD);

		// get the difference of the two closest points
		// distance = S1(sc) - S2(tc)
		//
		// Vector3f s1 = V3Add(segment1.point0, V3MulScalar(u, sc));
		// Vector3f s2 = V3Add(segment2.point0, V3MulScalar(v, tc));
		// Vector3f dP = V3Sub(s1, s2);
		// a more compact form: dP = w + (sc * u) - (tc * v) = S1(sc) - S2(tc)
		Vector3f dP = V3Add(w, V3Sub(V3MulScalar(u, sc), V3MulScalar(v, tc)));
		float minCloseness = V3Length(dP); // return the closest distance

		// printf("closeness = %f\n", minCloseness);

		if (minCloseness <= tolerance) {
			if (intersectDepth != null)
				intersectDepth.put(0, sc);
			intersects = true;
		}

		return intersects;
	}

	// ========== V3BoundsFromPoints
	// ================================================
	//
	// Purpose: Sorts the points into their minimum and maximum.
	//
	// ==============================================================================
	public static Box3 V3BoundsFromPoints(Vector3f point1, Vector3f point2) {
		Box3 bounds = new Box3();

		bounds.getMin().setX(Math.min(point1.getX(), point2.getX()));
		bounds.getMin().setY(Math.min(point1.getY(), point2.getY()));
		bounds.getMin().setZ(Math.min(point1.getZ(), point2.getZ()));

		bounds.getMax().setX(Math.max(point1.getX(), point2.getX()));
		bounds.getMax().setY(Math.max(point1.getY(), point2.getY()));
		bounds.getMax().setZ(Math.max(point1.getZ(), point2.getZ()));

		return bounds;

	}// end V3BoundsFromPoints

	// ========== V2BoxIntersectsPolygon
	// ============================================
	//
	// Purpose: tests whether any point on or in the polygon (as defined by
	// a point array) intersects the given axis-aligned bounding
	// box.
	//
	// ==============================================================================
	public static boolean V2BoxIntersectsPolygon(Box2 bounds, Vector2f poly[],
			int num_pts) {
		int i, j;

		// Easy case: selection box contains a polygon point. Do this first -
		// it's fastest.
		for (i = 0; i < num_pts; ++i)
			if (V2BoxContains(bounds, poly[i]))
				return true;

		// Next case: if any edge of the polygon hits the box edge, that's a
		// hit.
		for (i = 0; i < num_pts; ++i) {
			j = (i + 1) % num_pts;
			if (V2BoxIntersectsLine(bounds, poly[i], poly[j]))
				return true;
		}

		// Finally: for polygons (tri, quad, etc.) our selection box might be
		// entirely INSIDE the
		// poylgon. Test its centroid.
		if (num_pts < 3)
			return false;
		else
			// Final case: for non-degenerate case, marquee could be FULLY
			// inside - test one point to be sure.
			return V2PolygonContains(poly, num_pts,
					V2Make(V2BoxMidX(bounds), V2BoxMidY(bounds)));
	}

	// ========== V2BoxContains
	// =====================================================
	//
	// Purpose: simple containment test for points and boxes - on the line is
	// in.
	//
	// ==============================================================================
	public static boolean V2BoxContains(Box2 box, Vector2f pin) {
		return pin.getX() >= V2BoxMinX(box) && pin.getX() <= V2BoxMaxX(box)
				&& pin.getY() >= V2BoxMinY(box) && pin.getY() <= V2BoxMaxY(box);
	}

	// ========== V2BoxIntersectsLine
	// ===============================================
	//
	// Purpose: tests whether a given line segment intersects any of the four
	// edges of an axis-aligned bounding box.
	//
	// ==============================================================================
	public static boolean V2BoxIntersectsLine(Box2 box, Vector2f pin1,
			Vector2f pin2) {
		float x1 = V2BoxMinX(box);
		float x2 = V2BoxMaxX(box);
		float y1 = V2BoxMinY(box);
		float y2 = V2BoxMaxY(box);

		if (!(pin1.getX() < x1 && pin2.getX() < x1)
				&& !(pin1.getX() > x1 && pin2.getX() > x1)) {
			float yp = seg_y_at_x(pin1, pin2, x1);

			if (yp >= y1 && yp <= y2)
				return true;
		}

		if (!(pin1.getX() < x2 && pin2.getX() < x2)
				&& !(pin1.getX() > x2 && pin2.getX() > x2)) {
			float yp = seg_y_at_x(pin1, pin2, x2);

			if (yp >= y1 && yp <= y2)
				return true;
		}

		if (!(pin1.getY() < y1 && pin2.getY() < y1)
				&& !(pin1.getY() > y1 && pin2.getY() > y1)) {
			float xp = seg_x_at_y(pin1, pin2, y1);

			if (xp >= x1 && xp <= x2)
				return true;
		}

		if (!(pin1.getY() < y2 && pin2.getY() < y2)
				&& !(pin1.getY() > y2 && pin2.getY() > y2)) {
			float xp = seg_x_at_y(pin1, pin2, y2);

			if (xp >= x1 && xp <= x2)
				return true;
		}

		return false;
	}

	// ========== HELPER FUNCTIONS: horizontal/vertical line testing
	// ================
	//
	// Purpose: these two helper functions can be used to find the intercept
	// of a line (going through p1/p2) with a horizontal or vertical
	// line. We use this to do our seg-seg intersection with the AABB.
	//
	// ==============================================================================

	public static float seg_y_at_x(Vector2f p1, Vector2f p2, float x) {
		if (p1.getX() == p2.getX())
			return p1.getY();
		if (x == p1.getX())
			return p1.getY();
		if (x == p2.getX())
			return p2.getY();
		return p1.getY() + (p2.getY() - p1.getY()) * (x - p1.getX())
				/ (p2.getX() - p1.getX());
	}

	public static float seg_x_at_y(Vector2f p1, Vector2f p2, float y) {
		if (p1.getY() == p2.getY())
			return p1.getX();
		if (y == p1.getY())
			return p1.getX();
		if (y == p2.getY())
			return p2.getX();
		return p1.getX() + (p2.getX() - p1.getX()) * (y - p1.getY())
				/ (p2.getY() - p1.getY());
	}

	// ========== V2PolygonContains
	// =================================================
	//
	// Purpose: test whether a point is within a polygon, as define by an array
	// of points. "On the line" points are in if they are on a left
	// or bottom (but not right or top) edge.
	//
	// ==============================================================================
	public static boolean V2PolygonContains(Vector2f begin[], int num_pts,
			Vector2f pin) {
		int end = num_pts;
		int cross_counter = 0;
		Vector2f first_p = begin[0];
		Vector2f s_p1;
		Vector2f s_p2;

		s_p1 = begin[0];
		int index = 0;
		index++;

		while (index != end) {
			s_p2 = begin[index];
			if ((s_p1.getX() < pin.getX() && pin.getX() <= s_p2.getX())
					|| (s_p2.getX() < pin.getX() && pin.getX() <= s_p1.getX()))
				if (pin.getY() > seg_y_at_x(s_p1, s_p2, pin.getX()))
					++cross_counter;

			s_p1 = s_p2;
			index++;
		}
		s_p2 = first_p;
		if ((s_p1.getX() < pin.getX() && pin.getX() <= s_p2.getX())
				|| (s_p2.getX() < pin.getX() && pin.getX() <= s_p1.getX()))
			if (pin.getY() > seg_y_at_x(s_p1, s_p2, pin.getX()))
				++cross_counter;
		return (cross_counter % 2) == 1;

	}

	// ========== DepthOnLineSegment()
	// ==============================================
	//
	// Purpose: returns true if the screen-space point test_pt is on the line
	// segment v0..v1 in screen space. If true is returned, test_pt's
	// Z coordinate is set to the interpolated depth.
	//
	// Notes: All points are in screen-space. Tolerance^2 is the square of
	// the proximity distance we use.
	//
	// ==============================================================================
	public static boolean DepthOnLineSegment(Vector3f v0, Vector3f v1,
			float t2, // tolerance^2
			Vector3f test_pt) {
		float ldx = v1.getX() - v0.getX();
		float ldy = v1.getY() - v0.getY();
		float ldz = v1.getZ() - v0.getZ();
		float l2 = ldx * ldx + ldy * ldy;
		if (l2 == 0.0f) {
			if (PtsCloserThanT2InXY(v0, test_pt, t2)) {
				test_pt.setZ(v0.getZ());
				return true;
			}
			return false;
		}

		float dx = test_pt.getX() - v0.getX();
		float dy = test_pt.getY() - v0.getY();

		float t = (dx * ldx + dy * ldy) / l2;
		if (t < 0.0f) {
			if (PtsCloserThanT2InXY(v0, test_pt, t2)) {
				test_pt.setZ(v0.getZ());
				return true;
			}
			return false;
		} else if (t > 1.0f) {
			if (PtsCloserThanT2InXY(v1, test_pt, t2)) {
				test_pt.setZ(v1.getZ());
				return true;
			}
			return false;
		} else {
			Vector3f proj_pt = new Vector3f(v0.getX() + t * ldx, v0.getY() + t
					* ldy, v0.getZ() + t * ldz);

			if (PtsCloserThanT2InXY(proj_pt, test_pt, t2)) {
				test_pt.setZ(proj_pt.getZ());
				return true;
			}
			return false;
		}
	}// end DepthOnLineSegment

	public static boolean PtsCloserThanT2InXY(Vector3f p1, Vector3f p2,
			float dist_sqr) {
		// Returns true if p1 and p2 are closer than the sqrt of dist_sqr.
		float dx = p1.getX() - p2.getX();
		float dy = p1.getY() - p2.getY();
		return (dx * dx + dy * dy) < dist_sqr;
	}

	public static boolean V3RayIntersectsBox3(Ray3 ray, Box3 box,
			FloatBuffer intersectDepth, Vector2f intersectPointOut) {
		boolean intersects = false;
		Vector3f min, max;
		min = box.getMin();
		max = box.getMax();
		Vector3f verts[] = new Vector3f[4];
		verts[0] = max;
		verts[1] = new Vector3f(max.x, min.y, min.z);
		verts[2] = new Vector3f(min.x, min.y, min.z);
		verts[3] = new Vector3f(min.x, min.y, max.z);
		intersects = V3RayIntersectsQuadrilateral(ray, verts[0], verts[1],
				verts[2], verts[3], intersectDepth, intersectPointOut);

		if (intersects == false) {
			verts[0] = max;
			verts[1] = new Vector3f(max.x, max.y, min.z);
			verts[2] = new Vector3f(min.x, max.y, min.z);
			verts[3] = new Vector3f(min.x, max.y, max.z);
			intersects = V3RayIntersectsQuadrilateral(ray, verts[0], verts[1],
					verts[2], verts[3], intersectDepth, intersectPointOut);
		}

		if (intersects == false) {
			verts[0] = new Vector3f(max.x, min.y, min.z);
			verts[1] = new Vector3f(max.x, max.y, min.z);
			verts[2] = new Vector3f(max.x, max.y, max.z);
			verts[3] = new Vector3f(max.x, min.y, max.z);
			intersects = V3RayIntersectsQuadrilateral(ray, verts[0], verts[1],
					verts[2], verts[3], intersectDepth, intersectPointOut);
		}
		if (intersects == false) {
			verts[0] = new Vector3f(min.x, min.y, min.z);
			verts[1] = new Vector3f(min.x, max.y, min.z);
			verts[2] = new Vector3f(min.x, max.y, max.z);
			verts[3] = new Vector3f(min.x, min.y, max.z);
			intersects = V3RayIntersectsQuadrilateral(ray, verts[0], verts[1],
					verts[2], verts[3], intersectDepth, intersectPointOut);
		}
		if (intersects == false) {
			verts[0] = new Vector3f(min.x, min.y, max.z);
			verts[1] = new Vector3f(max.x, min.y, max.z);
			verts[2] = new Vector3f(max.x, max.y, max.z);
			verts[3] = new Vector3f(min.x, max.y, max.z);
			intersects = V3RayIntersectsQuadrilateral(ray, verts[0], verts[1],
					verts[2], verts[3], intersectDepth, intersectPointOut);
		}
		if (intersects == false) {
			verts[0] = new Vector3f(min.x, min.y, min.z);
			verts[1] = new Vector3f(max.x, min.y, min.z);
			verts[2] = new Vector3f(max.x, max.y, min.z);
			verts[3] = new Vector3f(min.x, max.y, min.z);
			intersects = V3RayIntersectsQuadrilateral(ray, verts[0], verts[1],
					verts[2], verts[3], intersectDepth, intersectPointOut);
		}

		return intersects;
	}

	public static boolean V3RayIntersectsQuadrilateral(Ray3 ray,
			Vector3f vert0, Vector3f vert1, Vector3f vert2, Vector3f vert3,
			FloatBuffer intersectDepth, Vector2f intersectPointOut) {
		boolean intersects = false;

		intersects = V3RayIntersectsTriangle(ray, vert0, vert1, vert2,
				intersectDepth, null);

		if (intersects == false) {
			intersects = V3RayIntersectsTriangle(ray, vert2, vert3, vert0,
					intersectDepth, null);
		}
		return intersects;
	}

	// ========== V3RayIntersectsTriangle
	// ===========================================
	//
	// Purpose: Returns whether the given (normalized) ray intersects the
	// triangle.
	// http://www.graphics.cornell.edu/pubs/1997/MT97.html
	//
	// Parameters: ray - selection ray
	// vert[0-2] - vertexes of triangle (in same coordinates as ray)
	// intersectDepth - on return, distance from ray origin to triangle
	// intersection
	// intersectPoint - on return, barycentric coordinates within
	// triangle of intersection point (can be NULL).
	//
	// ==============================================================================

	public static boolean V3RayIntersectsTriangle(Ray3 ray, Vector3f vert0,
			Vector3f vert1, Vector3f vert2, FloatBuffer intersectDepth,
			Vector2f intersectPointOut) {
		Vector3f edge1;
		Vector3f edge2;
		Vector3f tvec;
		Vector3f pvec;
		Vector3f qvec;
		double det = 0;
		double inv_det = 0;
		float distance = 0;
		float u = 0;
		float v = 0;

		// find vectors for two edges sharing vert0
		edge1 = V3Sub(vert1, vert0);
		edge2 = V3Sub(vert2, vert0);

		// begin calculating determinant - also used to calculate U parameter
		pvec = V3Cross(ray.getDirection(), edge2);

		// if determinant is near zero, ray lies in plane of triangle
		det = V3Dot(edge1, pvec);

		if (det > -SMALL_NUMBER && det < SMALL_NUMBER)
			return false;
		inv_det = 1.0f / det;

		// calculate distance from vert0 to ray origin
		tvec = V3Sub(ray.getOrigin(), vert0);

		// calculate U parameter and test bounds
		u = (float) (V3Dot(tvec, pvec) * inv_det);
		if (u < 0.0 || u > 1.0)
			return false;

		// prepare to test V parameter
		qvec = V3Cross(tvec, edge1);

		// calculate V parameter and test bounds
		v = (float) (V3Dot(ray.getDirection(), qvec) * inv_det);
		if (v < 0.0 || u + v > 1.0)
			return false;

		// calculate t, ray intersects triangle
		distance = (float) (V3Dot(edge2, qvec) * inv_det);
		// Intersects; return info
		if (intersectDepth != null)
			intersectDepth.put(0, distance);
		if (intersectPointOut != null)
			intersectPointOut.set(V2Make(u, v));

		return true;
	}

	// ========== V4FromVector3f
	// ======================================================
	//
	// Purpose: Create a new 4D point whose components match the given 3D
	// point, with a 1 in the 4th dimension.
	//
	// Notes: This method is not suitable for creating 4D vectors, whose w
	// value must be 0. (That will cause the translation part of a 4x4
	// transformation matrix to have no effect, which is what you want
	// for vectors.)
	//
	// ==============================================================================
	public static Vector4f V4FromVector3f(Vector3f originalPoint) {
		Vector4f newPoint = new Vector4f();
		;

		newPoint.setX(originalPoint.getX());
		newPoint.setY(originalPoint.getY());
		newPoint.setZ(originalPoint.getZ());
		newPoint.setW(1); // By setting this to 1, the returned value is a
							// point. Vectors would be set to 0.

		return newPoint;

	}// end V4FromVector3f
		// ========== DepthOnTriangle()
		// =================================================
		//
		// Purpose: Returns true if test_pt is in the triangle in XY space.
		//
		// Notes: All points are in screen-space; if true is returned, test_pt's
		// Z is the intersection depth.

	//
	// ==============================================================================

	public static boolean DepthOnTriangle(Vector3f v0, Vector3f v1,
			Vector3f v2, Vector3f test_pt) {
		float area = SignedAreaOfTriXY(v0, v1, v2);
		if (area == 0.0)
			return false;
		area = 1.0f / area;
		float A = SignedAreaOfTriXY(v1, v2, test_pt) * area;
		float B = SignedAreaOfTriXY(v2, v0, test_pt) * area;
		float C = SignedAreaOfTriXY(v0, v1, test_pt) * area;

		if (A >= 0 && B >= 0 && C >= 0) {
			test_pt.setZ(v0.getZ() * A + v1.getZ() * B + v2.getZ() * C);
			return true;
		}
		return false;
	}// end DepthOnTriangle

	public static float SignedAreaOfTriXY(Vector3f v0, Vector3f v1, Vector3f v2) {
		// This is the signed area of a triangle in XY space - used to calculate
		// bathymetric coordinates.
		return (v0.getX() - v2.getX()) * (v1.getY() - v2.getY())
				- (v1.getX() - v2.getX()) * (v0.getY() - v2.getY());
	}

	// ========== V3MulPointByMatrix
	// ================================================
	//
	// Purpose: multiply a point by a matrix and return the transformed point
	//
	// ==============================================================================
	public static Vector3f V3MulPointByMatrix(Vector3f pin, Matrix3 m) {
		Vector3f pout = Vector3f.getZeroVector3f();
		float[][] element = m.getElements();

		pout.setX((pin.getX() * element[0][0]) + (pin.getY() * element[1][0])
				+ (pin.getZ() * element[2][0]));

		pout.setY((pin.getX() * element[0][1]) + (pin.getY() * element[1][1])
				+ (pin.getZ() * element[2][1]));

		pout.setZ((pin.getX() * element[0][2]) + (pin.getY() * element[1][2])
				+ (pin.getZ() * element[2][2]));

		return pout;

	}// end V3MulPointByMatrix

	// ========== V3RotateByTransformMatrix
	// ================================================
	//
	// Purpose: multiply a point by a matrix and return the transformed point
	//
	// ==============================================================================
	public static Vector3f V3RotateByTransformMatrix(Vector3f pin, Matrix4 m) {
		Vector3f pout = Vector3f.getZeroVector3f();
		float[][] element = m.getElement();

		pout.setX((pin.getX() * element[0][0]) + (pin.getY() * element[1][0])
				+ (pin.getZ() * element[2][0]));

		pout.setY((pin.getX() * element[0][1]) + (pin.getY() * element[1][1])
				+ (pin.getZ() * element[2][1]));

		pout.setZ((pin.getX() * element[0][2]) + (pin.getY() * element[1][2])
				+ (pin.getZ() * element[2][2]));

		return pout;

	}// end V3RotateByTransformMatrix

	// ========== V3DistanceBetween2Points
	// ==========================================
	//
	// Purpose: return the distance between two points
	//
	// ==============================================================================
	public static float V3DistanceBetween2Points(Vector3f a, Vector3f b) {
		float dx = a.getX() - b.getX();
		float dy = a.getY() - b.getY();
		float dz = a.getZ() - b.getZ();

		float distance = (float) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));

		return distance;

	}// end V3DistanceBetween2Points

	// ========== V3EqualPoints()
	// ===================================================
	//
	// Purpose: Returns YES if point1 and point2 have the same coordinates..
	//
	// ==============================================================================
	public static boolean V3EqualPoints(Vector3f point1, Vector3f point2) {

		if (compareFloat(point1.getX(), point2.getX()) == 0
				&& compareFloat(point1.getY(), point2.getY()) == 0
				&& compareFloat(point1.getZ(), point2.getZ()) == 0)
			return true;
		else
			return false;
	}// end V3EqualPoints

	public static boolean V4EqualPoints(Vector4f point1, Vector4f point2) {
		if (compareFloat(point1.getX(), point2.getX()) == 0
				&& compareFloat(point1.getY(), point2.getY()) == 0
				&& compareFloat(point1.getZ(), point2.getZ()) == 0
				&& compareFloat(point1.getW(), point2.getW()) == 0)
			return true;
		else
			return false;
	}

	public static int compareFloat(float f1, float f2) {
		if (Math.abs(f1 - f2) < 0.01f)
			return 0;
		if (f1 > f2)
			return 1;
		return -1;
	}

	// ========== Matrix4RotateModelview()
	// ==========================================
	//
	// Purpose: Applies a rotation to a modelview matrix. Modelviews have
	// translations to incorporate the camera location; this method
	// maintains the camera location while rotating around the origin.
	//
	// Rotation order is first X, then Y, and lastly Z.
	//
	// ==============================================================================
	public static Matrix4 Matrix4RotateModelview(Matrix4 original,
			Vector3f degreesToRotate) {
		TransformComponents rotateComponents = TransformComponents
				.getIdentityComponents();
		Matrix4 addedRotation = Matrix4.getIdentityMatrix4();
		Matrix4 result = Matrix4.getIdentityMatrix4();
		Vector3f camera = Vector3f.getZeroVector3f();

		// Camera translation is in the bottom row of the matrix. Capture and
		// clear it so we can apply the rotation around the world origin.
		float[][] element = original.getElement();
		camera = V3Make(element[3][0], element[3][1], element[3][2]);
		element[3][0] = 0;
		element[3][1] = 0;
		element[3][2] = 0;

		// Create a new matrix that causes the rotation we want.
		// (start with identity matrix)
		rotateComponents.getRotate().setX(
				(float) Math.toRadians(degreesToRotate.getX()));
		rotateComponents.getRotate().setY(
				(float) Math.toRadians(degreesToRotate.getY()));
		rotateComponents.getRotate().setZ(
				(float) Math.toRadians(degreesToRotate.getZ()));
		addedRotation = Matrix4CreateTransformation(rotateComponents);

		result = Matrix4Multiply(original, addedRotation);
		result = Matrix4Translate(result, camera);

		return result;

	}// end Matrix4RotateModelview

	// ========== V2MakeSize
	// ========================================================
	// ==============================================================================
	public static Size2 V2MakeSize(float width, float height) {
		Size2 size = new Size2();

		size.setWidth(width);
		size.setHeight(height);

		return size;
	}

	public static void copy_vec3(float d[], float s[]) {
		copy_vecn(3, d, s);
	}

	public static void copy_vec3(float d[], int offset_d, float s[],
			int offset_s) {
		copy_vecn(3, d, offset_d, s, offset_s);
	}

	public static void copy_vec4(float d[], float s[]) {
		copy_vecn(4, d, s);
	}

	public static void copy_vec4(float d[], int offset_d, float s[],
			int offset_s) {
		copy_vecn(4, d, offset_d, s, offset_s);
	}

	public static final void copy_vecn(int n, float d[], float s[]) {
		copy_vecn(n, d, 0, s, 0);
	}

	public static final void copy_vecn(int n, float d[], int offset_d, float s[],
			int offset_s) {
		for (int i = 0; i < n; i++)
			d[i + offset_d] = s[i + offset_s];
	}

	// ========== V3RayIntersectsSphere
	// =============================================
	//
	// Purpose: Returns whether the given (normalized) ray intersects the
	// sphere.
	//
	// Notes: Derived from solving:
	// R(t) = O + td // ray starting at (xO, yO, zO) extending in direction (dx,
	// dy, dz)
	// r^2 = (x - xc)^2 + (y - yc)^2 + (z - zc)^2 // sphere radius r centered at
	// (xc, yc, zc)
	//
	// http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter1.htm
	//
	// ==============================================================================
	public static boolean V3RayIntersectsSphere(Ray3 ray,
			Vector3f sphereCenter, float radius, FloatBuffer intersectDepth) {
		float b = 0;
		float c = 0;
		float discriminant;
		float distance = 0;
		boolean intersects = false;

		// b and c stand for terms in the quadratic equation which solves for
		// the
		// depth of an intersection along the ray. (a is always 1 when the ray
		// is
		// normalized).
		Vector3f direction = ray.getDirection();
		Vector3f origin = ray.getOrigin();

		b = 2 * (direction.getX() * (origin.getX() - sphereCenter.getX())
				+ direction.getY() * (origin.getY() - sphereCenter.getY()) + direction
				.getZ() * (origin.getZ() - sphereCenter.getZ()));

		c = (float) (Math.pow(origin.getX() - sphereCenter.getX(), 2)
				+ Math.pow(origin.getY() - sphereCenter.getY(), 2)
				+ Math.pow(origin.getZ() - sphereCenter.getZ(), 2) - (radius * radius));

		// Find the discriminant (the part under the square root) of the
		// quadratic
		// formula to determine if there are solutions (intersections).
		discriminant = b * b - 4 * c;

		if (discriminant >= 0.0) {
			distance = (float) ((-b - Math.sqrt(discriminant)) / 2);

			if (distance <= 0) {
				distance = (float) ((-b + Math.sqrt(discriminant)) / 2);
			}
			intersects = true;

			if (intersectDepth != null)
				intersectDepth.put(0, distance);
		}
		return intersects;
	}

	public static float round(float value) {
		return Math.round(value / LDrawGlobalFlag.DecimalPoint)
				* LDrawGlobalFlag.DecimalPoint;
	}

	public static Vector3f round(Vector3f value) {
		return new Vector3f(round(value.x), round(value.y), round(value.z));
	}
}
