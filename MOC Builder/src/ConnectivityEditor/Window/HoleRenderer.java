package ConnectivityEditor.Window;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import Builder.MainCamera;
import Common.Ray3;
import Common.Vector3f;
import Connectivity.Connectivity;
import Connectivity.Hole;
import LDraw.Support.MatrixMath;
import LDraw.Support.type.LDrawGridTypeT;

public class HoleRenderer extends DefaultConnectivityRenderer {
	private Hole hole;

	public HoleRenderer(MainCamera camera, Connectivity conn) {
		super(camera, conn);

		hole = (Hole) conn;
	}

	private Vector3f[] getVertices() {
		ArrayList<Vector3f> vertices = new ArrayList<Vector3f>();
		Vector3f vertex;
		for (int column = 0; column + 1 < hole.getheight(); column += 2) {
			for (int row = 0; row + 1 < hole.getwidth(); row += 2) {
				if (hole.getMatrixItem()[column + 1][row + 1].getAltitude() == 29)
					continue;
				if (hole.getMatrixItem()[column][row].getAltitude() != 29) {
					vertex = new Vector3f(LDrawGridTypeT.Coarse.getXZValue()
							* row / 2, 0, -LDrawGridTypeT.Coarse.getXZValue()
							* column / 2);
					vertices.add(vertex);
				}
				if (hole.getMatrixItem()[column + 2][row].getAltitude() != 29) {
					vertex = new Vector3f(LDrawGridTypeT.Coarse.getXZValue()
							* (row / 2), 0, -LDrawGridTypeT.Coarse.getXZValue()
							* (column / 2 + 1));
					vertices.add(vertex);
				}
				if (hole.getMatrixItem()[column + 2][row + 2].getAltitude() != 29) {
					vertex = new Vector3f(LDrawGridTypeT.Coarse.getXZValue()
							* (row / 2 + 1), 0,
							-LDrawGridTypeT.Coarse.getXZValue()
									* (column / 2 + 1));
					vertices.add(vertex);
				}
				if (hole.getMatrixItem()[column][row + 2].getAltitude() != 29) {
					vertex = new Vector3f(LDrawGridTypeT.Coarse.getXZValue()
							* (row / 2 + 1), 0,
							-LDrawGridTypeT.Coarse.getXZValue() * (column / 2));
					vertices.add(vertex);

				}
			}
		}

		Vector3f[] retArray = new Vector3f[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			retArray[i] = hole.getTransformMatrix().transformPoint(
					vertices.get(i));
		}
		return retArray;
	}

	@Override
	public void draw(GL2 gl2) {
		if (conn.isSelected())
			gl2.glColor3f(0.2f, 0.2f, 0.2f);
		else
			gl2.glColor3f(0, 1f, 1f);

		// draw base
		gl2.glLoadMatrixf(camera.getModelView(), 0);
		gl2.glPointSize(10);
		gl2.glBegin(GL2.GL_POINTS);
		for (Vector3f vertex : getVertices())
			gl2.glVertex3f(vertex.x, vertex.y, vertex.z);
		gl2.glEnd();
		gl2.glPointSize(1);

		// Draw Stud Disk (possible styles: FILL, LINE, POINT).
		gl2.glBegin(GL2.GL_QUADS); // draw using triangles
		for (Vector3f vertex : getCylinderVertices())
			gl2.glVertex3f(vertex.x, vertex.y, vertex.z);
		gl2.glEnd();

		gl2.glBegin(GL2.GL_TRIANGLES);
		for (Vector3f vertex : getDiskVertices())
			gl2.glVertex3f(vertex.x, vertex.y, vertex.z);
		gl2.glEnd();

		gl2.glBegin(GL2.GL_TRIANGLES);
		for (Vector3f vertex : getGuideVertices())
			gl2.glVertex3f(vertex.x, vertex.y, vertex.z);
		gl2.glEnd();
	}

	private Vector3f[] getGuideVertices() {
		ArrayList<Vector3f> vertices = new ArrayList<Vector3f>();

		final float radius = LDrawGridTypeT.Medium.getXZValue() / 1.2f;

		Vector3f vertex;
		for (int column = 0; column < hole.getheight(); column += 2) {
			for (int row = 0; row < hole.getwidth(); row += 2) {
				if (hole.getMatrixItem()[column + 1][row + 1].getAltitude() == 29)
					continue;
				vertex = new Vector3f(LDrawGridTypeT.Coarse.getXZValue() / 2
						+ LDrawGridTypeT.Coarse.getXZValue() * (row / 2), 0,
						-LDrawGridTypeT.Coarse.getXZValue() / 2
								- LDrawGridTypeT.Coarse.getXZValue()
								* (column / 2));
				vertices.add(vertex.add(-radius / 2, 0, 0));
				vertices.add(vertex.add(+radius / 2, 0, 0));
				vertices.add(vertex.add(0, 0, -radius));

				vertices.add(vertex.add(-radius / 2, 0, 0));
				vertices.add(vertex.add(+radius / 2, 0, 0));
				vertices.add(vertex.add(0, 0, +radius));

				vertices.add(vertex.add(0, 0, -radius / 2));
				vertices.add(vertex.add(0, 0, radius / 2));
				vertices.add(vertex.add(-radius, 0, 0));

				vertices.add(vertex.add(0, 0, -radius / 2));
				vertices.add(vertex.add(0, 0, radius / 2));
				vertices.add(vertex.add(radius, 0, 0));
			}
		}
		Vector3f[] retArray = new Vector3f[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			retArray[i] = hole.getTransformMatrix().transformPoint(
					vertices.get(i));
		}
		return retArray;
	}

	private Vector3f[] getDiskVertices() {
		ArrayList<Vector3f> vertices = new ArrayList<Vector3f>();

		final float diskRadius = LDrawGridTypeT.Medium.getXZValue() / 1.6f;
		final float diskHeight = -LDrawGridTypeT.Fine.getYValue() * 2;

		Vector3f vertex;
		int slice = 24;
		for (int column = 0; column < hole.getheight(); column += 2) {
			for (int row = 0; row < hole.getwidth(); row += 2) {
				if (hole.getMatrixItem()[column + 1][row + 1].getAltitude() == 29)
					continue;
				vertex = new Vector3f(LDrawGridTypeT.Coarse.getXZValue() / 2
						+ LDrawGridTypeT.Coarse.getXZValue() * (row / 2),
						diskHeight, -LDrawGridTypeT.Coarse.getXZValue() / 2
								- LDrawGridTypeT.Coarse.getXZValue()
								* (column / 2));

				for (int sliceIndex = 0; sliceIndex < slice - 1; sliceIndex++) {
					vertices.add(vertex);
					vertices.add(vertex.add(
							(float) (diskRadius * Math.cos((sliceIndex)
									* Math.PI * 2 / (slice - 1))),
							0,
							(float) (diskRadius * Math.sin((sliceIndex)
									* Math.PI * 2 / (slice - 1)))));
					vertices.add(vertex.add(
							(float) (diskRadius * Math.cos((sliceIndex + 1)
									* Math.PI * 2 / (slice - 1))),
							0,
							(float) (diskRadius * Math.sin((sliceIndex + 1)
									* Math.PI * 2 / (slice - 1)))));
				}
			}
		}
		Vector3f[] retArray = new Vector3f[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			retArray[i] = hole.getTransformMatrix().transformPoint(
					vertices.get(i));
		}
		return retArray;
	}

	private Vector3f[] getCylinderVertices() {
		ArrayList<Vector3f> vertices = new ArrayList<Vector3f>();

		final float cylinderRadius = LDrawGridTypeT.Medium.getXZValue() / 1.6f;
		final float cylinderHeight = -LDrawGridTypeT.Fine.getYValue() * 2;

		Vector3f vertex;
		int slice = 24;
		for (int column = 0; column < hole.getheight(); column += 2) {
			for (int row = 0; row < hole.getwidth(); row += 2) {
				if (hole.getMatrixItem()[column + 1][row + 1].getAltitude() == 29)
					continue;
				vertex = new Vector3f(LDrawGridTypeT.Coarse.getXZValue() / 2
						+ LDrawGridTypeT.Coarse.getXZValue() * (row / 2), 0,
						-LDrawGridTypeT.Coarse.getXZValue() / 2
								- LDrawGridTypeT.Coarse.getXZValue()
								* (column / 2));

				for (int sliceIndex = 0; sliceIndex < slice - 1; sliceIndex++) {
					vertices.add(vertex.add(
							(float) (cylinderRadius * Math.cos(sliceIndex
									* Math.PI * 2 / (slice - 1))),
							0,
							(float) (cylinderRadius * Math.sin(sliceIndex
									* Math.PI * 2 / (slice - 1)))));
					vertices.add(vertex.add(
							(float) (cylinderRadius * Math.cos((sliceIndex + 1)
									* Math.PI * 2 / (slice - 1))),
							0,
							(float) (cylinderRadius * Math.sin((sliceIndex + 1)
									* Math.PI * 2 / (slice - 1)))));
					vertices.add(vertex.add(
							(float) (cylinderRadius * Math.cos((sliceIndex + 1)
									* Math.PI * 2 / (slice - 1))),
							cylinderHeight,
							(float) (cylinderRadius * Math.sin((sliceIndex + 1)
									* Math.PI * 2 / (slice - 1)))));
					vertices.add(vertex.add(
							(float) (cylinderRadius * Math.cos(sliceIndex
									* Math.PI * 2 / (slice - 1))),
							cylinderHeight,
							(float) (cylinderRadius * Math.sin(sliceIndex
									* Math.PI * 2 / (slice - 1)))));
				}
			}
		}
		Vector3f[] retArray = new Vector3f[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			retArray[i] = hole.getTransformMatrix().transformPoint(
					vertices.get(i));
		}
		return retArray;
	}

	@Override
	public boolean isHitted(MainCamera camera, float screenX, float screenY,
			FloatBuffer distance) {

		Ray3 ray = camera.getRay(screenX, screenY);
		FloatBuffer distanceTemp = FloatBuffer.allocate(1);
		distanceTemp.put(Float.MAX_VALUE);

		Vector3f[] vertices = getCylinderVertices();
		boolean isHitted = false;
		for (int i = 0; i < vertices.length; i += 4) {
			if (MatrixMath.V3RayIntersectsTriangle(ray, vertices[i],
					vertices[(i + 1)], vertices[(i + 2)], distanceTemp, null)) {
				if (distance != null)
					if (distanceTemp.get(0) < distance.get(0))
						distance.put(0, distanceTemp.get(0));
				isHitted = true;
			}
			if (MatrixMath.V3RayIntersectsTriangle(ray, vertices[i + 2],
					vertices[(i + 3)], vertices[i], distanceTemp, null)) {
				if (distance != null)
					if (distanceTemp.get(0) < distance.get(0))
						distance.put(0, distanceTemp.get(0));
				isHitted = true;
			}
		}

		vertices = getDiskVertices();
		for (int i = 0; i < vertices.length; i += 3) {
			if (MatrixMath.V3RayIntersectsTriangle(ray, vertices[i],
					vertices[(i + 1)], vertices[(i + 2)], distanceTemp, null)) {
				if (distance != null)
					if (distanceTemp.get(0) < distance.get(0))
						distance.put(0, distanceTemp.get(0));
				isHitted = true;
			}
		}

		vertices = getGuideVertices();
		for (int i = 0; i < vertices.length; i += 3) {
			if (MatrixMath.V3RayIntersectsTriangle(ray, vertices[i],
					vertices[(i + 1)], vertices[(i + 2)], distanceTemp, null)) {
				if (distance != null)
					if (distanceTemp.get(0) < distance.get(0))
						distance.put(0, distanceTemp.get(0));
				isHitted = true;
			}
		}
		lastHittedDistance = distance.get(0);
		return isHitted;
	}
}
