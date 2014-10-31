package Connectivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import Builder.BrickSelectionManager;
import Builder.BuilderConfigurationManager;
import Command.LDrawPart;
import Command.PartTypeT;
import Common.Box3;
import Common.Matrix4;
import Common.Vector2f;
import Common.Vector3f;
import ConnectivityEditor.Connectivity.AxleT;
import Grouping.ConnectionPoint;
import LDraw.Support.MatrixMath;
import LDraw.Support.type.LDrawGridTypeT;
import Window.GlobalMousePosition;
import Window.MOCBuilder;

public class GlobalConnectivityManager {
	private static GlobalConnectivityManager _instance = null;

	public synchronized static GlobalConnectivityManager getInstance() {
		if (_instance == null)
			_instance = new GlobalConnectivityManager();
		return _instance;
	}

	private ArrayList<LDrawPart> allPartList;
	private HashMap<Vector3f, ArrayList<LDrawPart>> partListMap;

	private HashMap<Vector3f, ArrayList<MatrixItem>> studCoordinateMap;
	private HashMap<Vector3f, ArrayList<MatrixItem>> holeCoordinateMap;

	private HashMap<IConnectivity, Vector3f> oldPosMap;

	private GlobalConnectivityManager() {
		allPartList = new ArrayList<LDrawPart>();
		partListMap = new HashMap<Vector3f, ArrayList<LDrawPart>>();
		studCoordinateMap = new HashMap<Vector3f, ArrayList<MatrixItem>>();
		holeCoordinateMap = new HashMap<Vector3f, ArrayList<MatrixItem>>();
		oldPosMap = new HashMap<IConnectivity, Vector3f>();
	}

	public void addPart(LDrawPart part) {
		addPart(part, true);
	}

	public void addPart(LDrawPart part, boolean updateMatrix) {
		if (LDrawPart.class.isInstance(part)) {
			synchronized (allPartList) {
				allPartList.add(part);
			}

			if (updateMatrix)
				updateMatrix(part, false);
		}

	}

	private boolean checkBoundingBoxIntersection(LDrawPart part,
			Matrix4 testingTransform) {
		Box3 testPartBoundingBox = part.boundingBox3(testingTransform);

		boolean isIntersected = false;
		for (LDrawPart directive : getAdjacentPartList(testPartBoundingBox)) {
			if (directive == part)
				continue;
			if (directive.isDraggingPart())
				continue;
			if (directive.isSelected())
				continue;
			// if (directive.getConnectivityList().isEmpty()
			// && directive.getCacheType() != PartTypeT.PartTypeSubmodel)
			// continue;

			Box3 boundingBox = ((LDrawPart) directive).boundingBox3();
			if (boundingBox == null)
				continue;
			if (isIntersected(testPartBoundingBox, boundingBox)) {
				isIntersected = true;
				break;
			}
		}
		return isIntersected;
	}

	public boolean CheckCollisionBox(LDrawPart srcpart,
			Matrix4 testingPartTransformMatrix) {

		ArrayList<CollisionBox> srcboxes = srcpart.getCollisionBoxList();
		if (srcboxes == null || srcboxes.size() == 0)
			return false;

		Box3 boundingBox = srcpart.boundingBox3(testingPartTransformMatrix);

		for (int i = 0; i < srcboxes.size(); i++) {
			CollisionBox srcCollisionBox = srcboxes.get(i);

			Vector3f[] srcvAxisDir = srcCollisionBox
					.MakeDirection(testingPartTransformMatrix);
			float[] srcfAxisLen = srcCollisionBox.GetAxisLength();

			Vector3f srcCenter = srcCollisionBox
					.getCenter(testingPartTransformMatrix);

			for (LDrawPart destpart : getAdjacentPartList(boundingBox)) {
				if (srcpart == destpart)
					continue;
				if (destpart.isSelected())
					continue;

				ArrayList<CollisionBox> destboxes = destpart
						.getCollisionBoxList();
				if (destboxes == null || destboxes.size() == 0)
					continue;

				for (int k = 0; k < destboxes.size(); k++) {
					CollisionBox destCollisionBox = destboxes.get(k);

					Vector3f[] destvAxisDir = destCollisionBox
							.MakeDirection(destpart.transformationMatrix());
					float[] destfAxisLen = destCollisionBox.GetAxisLength();

					Vector3f destCenter = destCollisionBox.getCenter(destpart);

					if (CollisionBox.CheckOBBCollision(srcCollisionBox,
							srcCenter, srcvAxisDir, srcfAxisLen,
							destCollisionBox, destCenter, destvAxisDir,
							destfAxisLen) == true) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public void clear() {
		clear(true);
	}

	public void clear(boolean updateMatrix) {
		synchronized (allPartList) {
			allPartList.clear();
		}
		synchronized (studCoordinateMap) {
			studCoordinateMap.clear();
		}
		synchronized (holeCoordinateMap) {
			holeCoordinateMap.clear();
		}
		synchronized (partListMap) {
			partListMap.clear();
		}
		if (updateMatrix)
			updateMatrixAll();
	}

	public final ArrayList<LDrawPart> getAdjacentPartList(Box3 boundingBox) {
		ArrayList<LDrawPart> partList = new ArrayList<LDrawPart>();
		Vector3f coarseMin = LDrawGridTypeT.getSnappedPos(boundingBox.getMin(),
				LDrawGridTypeT.CoarseX10);
		Vector3f coarseMax = LDrawGridTypeT.getSnappedPos(boundingBox.getMax(),
				LDrawGridTypeT.CoarseX10);
		Vector3f partPos;
		for (int x = Math.round(coarseMin.x); x <= Math.round(coarseMax.x); x += LDrawGridTypeT.CoarseX10
				.getXZValue())
			for (int y = Math.round(coarseMin.y); y <= Math.round(coarseMax.y); y += LDrawGridTypeT.CoarseX10
					.getYValue())
				for (int z = Math.round(coarseMin.z); z <= Math
						.round(coarseMax.z); z += LDrawGridTypeT.CoarseX10
						.getXZValue()) {
					partPos = new Vector3f(x, y, z);
					if (partListMap.containsKey(partPos)) {
						for (LDrawPart part : this.partListMap.get(partPos)) {
							if (part.boundingBox3() == boundingBox)
								continue;
							if (isIntersected(boundingBox, part.boundingBox3()))
								if (partList.contains(part) == false)
									partList.add(part);
						}
					}
				}
		return partList;
	}

	// part 갖다 붙일 파츠.
	// testPosOfPart 갖다 붙일 파츠의 새로운 좌표.
	public Matrix4 getClosestConnectablePos(LDrawPart part,
			Vector3f testPosOfPart) {
		try {

			// hittedPos 마우스가 가리키는 좌표 .
			Vector2f mousePos = GlobalMousePosition.getInstance().getPos();
			Vector3f hittedWorldPos = MOCBuilder.getInstance().getHittedPos(
					mousePos.getX(), mousePos.getY(), true);

			ConnectivityTestResult testResult = null;
			if (BuilderConfigurationManager.getInstance().isUseConnectivity())
				testResult = isConnectable(part, testPosOfPart, hittedWorldPos);

			if (testResult != null
					&& testResult.getResultType() == ConnectivityTestResultT.True) {
				return testResult.getTransformMatrix();
			}

			if (MatrixMath.compareFloat(hittedWorldPos.y, 0) != 0) {
				LDrawPart minPart = BrickSelectionManager.getInstance()
						.getPartHavingMinY();

				float offsetY = -minPart.boundingBox3(
						minPart.getRotationMatrix()).getMax().y;

				testPosOfPart.y = hittedWorldPos.y + offsetY
						+ part.position().sub(minPart.position()).y;
				testPosOfPart = LDrawGridTypeT
						.getSnappedPos(testPosOfPart,
								BuilderConfigurationManager.getInstance()
										.getGridUnit());

				Matrix4 newTransform = BrickSelectionManager.getInstance()
						.getStartMoveTransformMatrix(part);
				
				newTransform.element[3][0] = testPosOfPart.x;
				newTransform.element[3][1] = testPosOfPart.y;
				newTransform.element[3][2] = testPosOfPart.z;
				return newTransform;

			}

			if (BuilderConfigurationManager.getInstance().isUseConnectivity()
					&& BuilderConfigurationManager.getInstance()
							.isUseDefaultBaseplate() && testResult == null) {

				MatrixItem minItem = BrickSelectionManager.getInstance()
						.getMatrixItemHavingMinY();
				if (minItem != null) {
					Vector3f newPosOfMinItem = minItem
							.getCurrentPos(
									minItem.getParent().getParent()
											.getRotationMatrix())
							.add(testPosOfPart)
							.add(LDrawGridTypeT.Coarse.getXZValue() / 2, 0,
									LDrawGridTypeT.Coarse.getXZValue() / 2);

					Vector3f snappedItemPosDiff = newPosOfMinItem
							.sub(LDrawGridTypeT.getSnappedPos(newPosOfMinItem,
									LDrawGridTypeT.Coarse));

					Matrix4 newTransform = BrickSelectionManager.getInstance()
							.getStartMoveTransformMatrix(part);
					newTransform.element[3][0] = testPosOfPart.x
							- snappedItemPosDiff.x;
					newTransform.element[3][1] = testPosOfPart.y;
					newTransform.element[3][2] = testPosOfPart.z
							- snappedItemPosDiff.z;
					return newTransform;
				}
			}

			// default
			LDrawPart minPart = BrickSelectionManager.getInstance()
					.getPartHavingMinY();
			Vector3f boundOfMinPart = new Vector3f();
			try {
				boundOfMinPart = minPart
						.boundingBox3(minPart.getRotationMatrix()).getMax()
						.add(testPosOfPart);
			} catch (Exception e) {
				e.printStackTrace();
			}

			Vector3f snappedItemPosDiff = boundOfMinPart.sub(LDrawGridTypeT
					.getSnappedPos(boundOfMinPart, BuilderConfigurationManager
							.getInstance().getGridUnit()));

			Matrix4 newTransform = BrickSelectionManager.getInstance()
					.getStartMoveTransformMatrix(part);
			newTransform.element[3][0] = testPosOfPart.x - snappedItemPosDiff.x;
			newTransform.element[3][1] = testPosOfPart.y;
			newTransform.element[3][2] = testPosOfPart.z - snappedItemPosDiff.z;
			return newTransform;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public ArrayList<LDrawPart> getPartList() {
		synchronized (allPartList) {
			ArrayList<LDrawPart> copy = new ArrayList<LDrawPart>(allPartList);
			return copy;
		}
	}

	private ArrayList<ConnectivityTestResult> getPotentialConnectibleList(
			final Vector3f hittedPos, LDrawPart part,
			Matrix4 initialTransformMatrixOfTestingPart) {

		ArrayList<ConnectivityTestResult> testResults = new ArrayList<ConnectivityTestResult>();

		ArrayList<IConnectivity> testingPartConnList = new ArrayList<IConnectivity>();
		testingPartConnList.addAll(BrickSelectionManager.getInstance()
				.getConnectivityManager(hittedPos).getConnectivityList());
		testingPartConnList.addAll(BrickSelectionManager.getInstance()
				.getConnectivityManager(hittedPos)
				.getConnectivityMatrixItemList());

		if (testingPartConnList.isEmpty())
			return null;

		ArrayList<Connectivity> existingConnList = new ArrayList<Connectivity>();
		ArrayList<MatrixItem> existingMatrixItemList = new ArrayList<MatrixItem>();
		for (LDrawPart adjacentPart : getAdjacentPartList(part
				.boundingBox3(initialTransformMatrixOfTestingPart))) {
			if (adjacentPart == part)
				continue;
			if (adjacentPart.isSelected())
				continue;
			if (adjacentPart.getConnectivityList() != null)
				for (Connectivity conn : adjacentPart.getConnectivityList())
					if (conn instanceof ICustom2DField) {
					} else
						existingConnList.add(conn);
			if (adjacentPart.getConnectivityMatrixItemList() != null)
				for (MatrixItem matrixItem : adjacentPart
						.getConnectivityMatrixItemList())
					if (matrixItem.getCurrentPos().sub(hittedPos).length() <= LDrawGridTypeT.Coarse
							.getYValue())
						existingMatrixItemList.add(matrixItem);
		}

		ArrayList<IConnectivity> existingTestibleItemList = new ArrayList<IConnectivity>();
		existingTestibleItemList.addAll(existingConnList);
		existingTestibleItemList.addAll(existingMatrixItemList);

		Collections.sort(existingTestibleItemList,
				new Comparator<IConnectivity>() {
					@Override
					public int compare(IConnectivity arg0, IConnectivity arg1) {
						return MatrixMath.compareFloat(
								arg0.distance(hittedPos),
								arg1.distance(hittedPos));
					}
				});

		// System.out.println("testingPos: "+testingPos);
		// System.out.println("testingMatrixItemList: "
		// + testingPartConnList.size());
		// System.out.println("existingMatrixItemList: "
		// + existingTestibleItemList.size());

		for (IConnectivity existingTestibleItem : existingTestibleItemList) {
			for (IConnectivity testingPartConn : testingPartConnList) {
				if (MatrixItem.class.isInstance(existingTestibleItem)
						&& MatrixItem.class.isInstance(testingPartConn) == true) {
					if (existingTestibleItem.isConnectable(testingPartConn) == ConnectivityTestResultT.True) {
						ConnectivityTestResult testResult = new ConnectivityTestResult();
						Matrix4 newTransform = ((MatrixItem) testingPartConn)
								.getTransformMatrixForConnecting(
										(MatrixItem) existingTestibleItem,
										initialTransformMatrixOfTestingPart);
						if (newTransform != null) {
							testResult
									.setResultType(ConnectivityTestResultT.True);
							testResult
									.setTestPos(initialTransformMatrixOfTestingPart
											.transformPoint(new Vector3f(0, 0,
													0)));
							if (testingPartConn.getConnectivity().getParent() != part) {
								Vector3f newPos = part.position().sub(
										testingPartConn.getConnectivity()
												.getParent().position());
								newTransform.translate(newPos.x, newPos.y,
										newPos.z);
							}

							testResult.setTransformMatrix(newTransform);
							testResults.add(testResult);
						}
					}

				} else if (Connectivity.class.isInstance(existingTestibleItem)
						&& Connectivity.class.isInstance(testingPartConn) == true) {
					if (existingTestibleItem.isConnectable(testingPartConn) == ConnectivityTestResultT.True) {

						ConnectivityTestResult testResult = new ConnectivityTestResult();
						testResult.setResultType(ConnectivityTestResultT.True);

						Matrix4 newTransform = ((Connectivity) testingPartConn)
								.getTransformMatrixForSnapConnecting(
										(Connectivity) existingTestibleItem,
										initialTransformMatrixOfTestingPart);
						if (newTransform != null) {
							if (testingPartConn.getConnectivity().getParent() != part) {
								Vector3f newPos = part.position().sub(
										testingPartConn.getConnectivity()
												.getParent().position());
								newTransform.translate(newPos.x, newPos.y,
										newPos.z);
							}
							testResult.setTransformMatrix(newTransform);
							testResults.add(testResult);
						}
					}

				}
			}
			if (testResults.size() != 0)
				break;
		}

		return testResults;
	}

	public ConnectivityTestResult isConnectable(LDrawPart part) {
		if (part == null)
			return new ConnectivityTestResult(ConnectivityTestResultT.False);

		ConnectivityTestResult testResult = isConnectable_Exact(part,
				part.transformationMatrix());

		return testResult;
	}

	private ConnectivityTestResult isConnectable(LDrawPart part,
			Vector3f testingPosOfPart, Vector3f hittedPos) {
		Matrix4 testingPartInitialTransforMatrix = null;
		testingPartInitialTransforMatrix = BrickSelectionManager.getInstance()
				.getStartMoveTransformMatrix(part);
		if (testingPartInitialTransforMatrix == null)
			testingPartInitialTransforMatrix = part.transformationMatrix();

		testingPartInitialTransforMatrix = new Matrix4(
				testingPartInitialTransforMatrix);

		testingPartInitialTransforMatrix.element[3][0] = testingPosOfPart.x;
		testingPartInitialTransforMatrix.element[3][1] = testingPosOfPart.y;
		testingPartInitialTransforMatrix.element[3][2] = testingPosOfPart.z;

		ConnectivityTestResult testResult = isConnectable_Potential(hittedPos,
				part, testingPartInitialTransforMatrix);

		return testResult;
	}

	public ConnectivityTestResult isConnectable_Exact(LDrawPart part,
			Matrix4 testingPartTransformMatrix) {

		ConnectivityTestResult testResult = null;

		// ArrayList<Connectivity> connectivityList =
		// part.getConnectivityList();
		// if (connectivityList.isEmpty()
		// && part.getCacheType() != PartTypeT.PartTypeSubmodel)
		// return new ConnectivityTestResult();

		// bounding box test
		if (testResult == null)
			testResult = new ConnectivityTestResult();

		if (checkBoundingBoxIntersection(part, testingPartTransformMatrix) == false) {
			testResult.setResultType(ConnectivityTestResultT.True);
			testResult.setTransformMatrix(testingPartTransformMatrix);
			// System.out.println("bounding Box Test Pass");
			return testResult;
		}
		// collision box test

		if (testResult.getResultType() != ConnectivityTestResultT.False) {
			if (BuilderConfigurationManager.getInstance().isUseCollision())
				if (CheckCollisionBox(part, testingPartTransformMatrix) == true) {
					testResult.setResultType(ConnectivityTestResultT.False);
					testResult.setMsg("Collision_2");
				}
		}
		return testResult;
	}

	private ConnectivityTestResult isConnectable_Potential(Vector3f hittedPos,
			LDrawPart part, Matrix4 testingPartInitialTransformMatrix) {

		ConnectivityTestResult testResult = null;
		ArrayList<Connectivity> testingPartConnectivityList = part
				.getConnectivityList();

		if (testingPartConnectivityList.isEmpty())
			return new ConnectivityTestResult();

		ArrayList<ConnectivityTestResult> potentialTestResults = new ArrayList<ConnectivityTestResult>();

		ArrayList<ConnectivityTestResult> potentialTestResultsTemp = getPotentialConnectibleList(
				hittedPos, part, testingPartInitialTransformMatrix);

		// System.out.println("potentialTestResultsTemp: "
		// + potentialTestResultsTemp.size());
		// connectivity test
		if (potentialTestResultsTemp != null) {
			for (ConnectivityTestResult tempResult : potentialTestResultsTemp) {
				boolean isContains = false;
				for (ConnectivityTestResult tempResult2 : potentialTestResults) {
					if (tempResult.getTransformMatrix().equals(
							tempResult2.getTransformMatrix())) {
						isContains = true;
						break;
					}
				}
				if (isContains == false)
					potentialTestResults.add(tempResult);
			}
		}

		// System.out.println("############################");
		// System.out
		// .println("TestTransform");
		// System.out.println(testingPartInitialTransformMatrix);
		// if (potentialTestResults.size() != 0)
		// System.out.println("Potential ListSize: "
		// + potentialTestResults.size());

		float minimumDistance = Float.MAX_VALUE;
		Vector3f distanceVector;
		float distance;
		boolean isMoreClosed = false;
		for (ConnectivityTestResult testResultTemp : potentialTestResults) {
			ConnectivityTestResultT isConnectible = ConnectivityTestResultT.True;
			isMoreClosed = false;

			distanceVector = testingPartInitialTransformMatrix
					.getDefaultTransformPos().sub(
							testResultTemp.getTransformMatrix().transformPoint(
									new Vector3f()));
			distance = distanceVector.dot(distanceVector);
			if (Direction6T
					.getDirectionOfTransformMatrix(testingPartInitialTransformMatrix) != Direction6T
					.getDirectionOfTransformMatrix(testResultTemp
							.getTransformMatrix()))
				distance *= 1.2f;
			if (distance < minimumDistance)
				isMoreClosed = true;
			else
				continue;

			if (isConnectible == ConnectivityTestResultT.True) {
				// System.out.println(testResultTemp.getTransformMatrix().getDefaultTransformPos());
				// collision box test
				if (BuilderConfigurationManager.getInstance().isUseCollision() == true
						&& checkBoundingBoxIntersection(part,
								testResultTemp.getTransformMatrix()) == true)
					if (CheckCollisionBox(part,
							testResultTemp.getTransformMatrix()) == true) {
						isConnectible = ConnectivityTestResultT.False;
						// System.out.println("Collistion!: "
						// + testResultTemp.getTransformMatrix()
						// .getDefaultTransformPos());
					}
			}
			if (isConnectible == ConnectivityTestResultT.True && isMoreClosed) {
				minimumDistance = distance;
				testResult = testResultTemp;
			}
		}

		return testResult;
	}

	public ConnectivityTestResult isConnectible_Connectivity(Connectivity conn,
			Matrix4 testingPartTransformMatrix) {
		ConnectivityTestResult testResult = new ConnectivityTestResult();

		ArrayList<Connectivity> existingConnList = new ArrayList<Connectivity>();

		// existingConnList 에 part.boundingBox3 속에 해당하는 Connectivity들을 넣어줌.
		for (LDrawPart adjacentPart : getAdjacentPartList(conn.getParent()
				.boundingBox3(testingPartTransformMatrix))) {
			if (adjacentPart == conn.getParent())
				continue;
			if (adjacentPart.isSelected())
				continue;
			if (adjacentPart.getConnectivityList() != null)
				for (Connectivity connTemp : adjacentPart.getConnectivityList())
					if (connTemp instanceof ICustom2DField) {
					} else
						existingConnList.add(connTemp);
		}
		for (Connectivity existItem : existingConnList) {
			if (existItem == conn)
				continue;

			if (conn.isConnectable(existItem, testingPartTransformMatrix) == ConnectivityTestResultT.True) {
				testResult.setResultType(ConnectivityTestResultT.True);
				break;
			}
		}
		return testResult;
	}

	private boolean isIntersected(Box3 box1, Box3 box2) {
		Vector3f max_box1 = box1.getMax();
		Vector3f min_box1 = box1.getMin();

		Vector3f max_box2 = box2.getMax();
		Vector3f min_box2 = box2.getMin();

		if (min_box1.y >= max_box2.y || max_box1.y <= min_box2.y)
			return false;
		if (max_box1.x - 1 <= min_box2.x || max_box1.z - 1 <= min_box2.z)
			return false;
		if (min_box1.x >= max_box2.x - 1 || min_box1.z >= max_box2.z - 1)
			return false;

		// System.out.println("####################");
		// System.out.println("max_box1: "+max_box1+", min_box1: "+min_box1);
		// System.out.println("max_box2: "+max_box2+", min_box1: "+min_box2);
		return true;

	}

	private void removeExistingConnInfo(LDrawPart part) {
		// remove partListMap
		Box3 boundingBox = part.boundingBox3();
		Vector3f coarseMin = LDrawGridTypeT.getSnappedPos(boundingBox.getMin(),
				LDrawGridTypeT.CoarseX10);
		Vector3f coarseMax = LDrawGridTypeT.getSnappedPos(boundingBox.getMax(),
				LDrawGridTypeT.CoarseX10);
		Vector3f partPos;
		for (int x = Math.round(coarseMin.x); x <= Math.round(coarseMax.x); x += LDrawGridTypeT.CoarseX10
				.getXZValue()) {
			for (int y = Math.round(coarseMin.y); y <= Math.round(coarseMax.y); y += LDrawGridTypeT.CoarseX10
					.getYValue()) {
				for (int z = Math.round(coarseMin.z); z <= Math
						.round(coarseMax.z); z += LDrawGridTypeT.CoarseX10
						.getXZValue()) {
					partPos = new Vector3f(x, y, z);
					if (partListMap.containsKey(partPos)) {
						partListMap.get(partPos).remove(part);
						// System.out.println("RemovePart from "+partPos);
					}
				}
			}
		}

		ArrayList<Connectivity> connList = part.getConnectivityList();
		ArrayList<MatrixItem> matrixItemList = part
				.getConnectivityMatrixItemList();
		if (connList.isEmpty() == false) {

			for (Connectivity conn : connList) {
				if (conn instanceof ICustom2DField)
					continue;
				if (conn.getConnectedConnectivity() == null)
					continue;
				for (Connectivity eConn : conn.getConnectedConnectivity()) {
					eConn.removeConnectedConnectivity(conn);
				}
				conn.clearConnectedConnectivity();
			}
		}

		if (matrixItemList != null) {
			for (MatrixItem item : matrixItemList) {
				if (oldPosMap.containsKey(item) == false)
					continue;

				Vector3f oldPos = oldPosMap.get(item);
				if (item.getParent() instanceof Stud) {
					if (studCoordinateMap.containsKey(oldPos))
						studCoordinateMap.get(oldPos).remove(item);

				} else if (item.getParent() instanceof Hole) {
					if (holeCoordinateMap.containsKey(oldPos))
						holeCoordinateMap.get(oldPos).remove(item);
				}

				if (item.getConnectedConnectivity() == null)
					continue;
				item.getConnectedConnectivity().setConnectedConnectivity(null);
				item.setConnectedConnectivity(null);
			}
		}
	}

	public void removePart(LDrawPart part) {
		removePart(part, false);
	}

	// public void updateMatrix() {
	// long nano = System.nanoTime();
	// updateMatrix2();
	// System.out.println("UpdateMatrix: " + (System.nanoTime() - nano));
	// }

	public void removePart(LDrawPart part, boolean updateMatrix) {
		synchronized (allPartList) {
			allPartList.remove(part);
		}

		if (updateMatrix)
			updateMatrix(part, true);
	}

	public void updateMatrix(LDrawPart part) {
		updateMatrix(part, false);
	}

	public void updateMatrix(LDrawPart part, boolean remove) {
		long nano = System.nanoTime();

		// if (remove) {
		// remove existing connectivity info
		// System.out.println("Remove Connectivity Matrix Info");
		removeExistingConnInfo(part);
		// return;
		// System.out.println("remove Connectivity Matrix: "
		// + (System.nanoTime() - nano));
		// }

		if (allPartList.contains(part) == false)
			return;

		// if (part.isDraggingPart()) {
		// return;
		// }

		// update partListMap
		Box3 boundingBox = part.boundingBox3();
		Vector3f coarseMin = LDrawGridTypeT.getSnappedPos(boundingBox.getMin(),
				LDrawGridTypeT.CoarseX10);
		Vector3f coarseMax = LDrawGridTypeT.getSnappedPos(boundingBox.getMax(),
				LDrawGridTypeT.CoarseX10);
		Vector3f partPos;
		for (int x = Math.round(coarseMin.x); x <= Math.round(coarseMax.x); x += LDrawGridTypeT.CoarseX10
				.getXZValue())
			for (int y = Math.round(coarseMin.y); y <= Math.round(coarseMax.y); y += LDrawGridTypeT.CoarseX10
					.getYValue())
				for (int z = Math.round(coarseMin.z); z <= Math
						.round(coarseMax.z); z += LDrawGridTypeT.CoarseX10
						.getXZValue()) {
					partPos = new Vector3f(x, y, z);
					if (partListMap.containsKey(partPos) == false)
						partListMap.put(partPos, new ArrayList<LDrawPart>());
					partListMap.get(partPos).add(part);
					// System.out.println("Add Part to "+partPos);
				}

		ArrayList<Connectivity> existingConnList = new ArrayList<Connectivity>();
		for (LDrawPart adjacentPart : getAdjacentPartList(part.boundingBox3())) {
			if (adjacentPart == part)
				continue;
			if (adjacentPart.isSelected())
				continue;
			if (adjacentPart.getConnectivityList() != null)
				for (Connectivity conn : adjacentPart.getConnectivityList())
					if (conn instanceof ICustom2DField) {
					} else
						existingConnList.add(conn);
		}

		// update position of connectivity and connected info.
		if (part.getConnectivityList() != null) {
			for (Connectivity conn : part.getConnectivityList()) {
				conn.updateConnectivityOrientationInfo();
				if (conn instanceof ICustom2DField)
					continue;
				for (Connectivity eConn : existingConnList) {
					if (conn.isConnectable(eConn, part.transformationMatrix()) == ConnectivityTestResultT.True) {
						conn.addConnectedConnectivity(eConn);
						eConn.addConnectedConnectivity(conn);
					}
				}
			}
		}

		Vector3f finePos = null;
		if (part.getConnectivityMatrixItemList() != null) {
			for (MatrixItem item : part.getConnectivityMatrixItemList()) {
				item.updateConnectivityOrientationInfo();
				finePos = LDrawGridTypeT.getSnappedPos(item.getCurrentPos(),
						LDrawGridTypeT.Fine);

				oldPosMap.put(item, finePos);
				if (item.getParent() instanceof Stud) {
					if (studCoordinateMap.containsKey(finePos) == false)
						studCoordinateMap.put(finePos,
								new ArrayList<MatrixItem>());
					studCoordinateMap.get(finePos).add(item);
					if (item.altitude == 29)
						continue;
					for (Vector3f testiblePos : getTestibleSnappedPos(
							item.getCurrentPos(), LDrawGridTypeT.Fine)) {
						if (holeCoordinateMap.containsKey(testiblePos)) {
							for (MatrixItem eItem : holeCoordinateMap
									.get(testiblePos)) {
								if (eItem.altitude == 29)
									continue;
								if (eItem.getRowIndex() % 2 != 1
										|| eItem.getColumnIndex() % 2 != 1)
									continue;
								if (eItem.getCurrentPos()
										.sub(item.getCurrentPos()).length() < 1) {

									if (eItem.getDirectionVector().equals(
											item.getDirectionVector())) {
										if (eItem.getConnectedConnectivity() != null) {
											System.out
													.println(item.getParent()
															.getParent()
															.displayName()
															+ ","
															+ eItem.getParent()
																	.getParent()
																	.displayName()
															+ ": "
															+ eItem.getCurrentPos()
																	.sub(item
																			.getCurrentPos())
																	.length());
										}

										item.setConnectedConnectivity(eItem);
										eItem.setConnectedConnectivity(item);
									}
								}
							}
						}
					}
				} else {
					if (holeCoordinateMap.containsKey(finePos) == false)
						holeCoordinateMap.put(finePos,
								new ArrayList<MatrixItem>());
					holeCoordinateMap.get(finePos).add(item);
					if (item.altitude == 29)
						continue;
					if (item.getRowIndex() % 2 != 1
							|| item.getColumnIndex() % 2 != 1)
						continue;
					for (Vector3f testiblePos : getTestibleSnappedPos(
							item.getCurrentPos(), LDrawGridTypeT.Fine)) {
						if (studCoordinateMap.containsKey(testiblePos)) {
							for (MatrixItem eItem : studCoordinateMap
									.get(testiblePos)) {
								if (eItem.altitude == 29)
									continue;
								if (eItem.getCurrentPos()
										.sub(item.getCurrentPos()).length() < 1) {
									if (eItem.getDirectionVector().equals(
											item.getDirectionVector())) {
										if (eItem.getConnectedConnectivity() != null) {
											System.out
													.println(item.getParent()
															.getParent()
															.displayName()
															+ ","
															+ eItem.getParent()
																	.getParent()
																	.displayName()
															+ ": "
															+ eItem.getCurrentPos()
																	.sub(item
																			.getCurrentPos())
																	.length());
										}

										item.setConnectedConnectivity(eItem);
										eItem.setConnectedConnectivity(item);
									}
								}
							}
						}
					}
				}
			}
		}
		// System.out.println("update Connectivity Matrix: "
		// + (System.nanoTime() - nano));
	}

	public void updateMatrixAll() {
		long nano = System.nanoTime();
		studCoordinateMap.clear();
		holeCoordinateMap.clear();
		partListMap.clear();
		oldPosMap.clear();
		for (int i = 0; i < allPartList.size(); i++) {
			ArrayList<Connectivity> connList = allPartList.get(i)
					.getConnectivityList();
			ArrayList<MatrixItem> matrixItemList = allPartList.get(i)
					.getConnectivityMatrixItemList();
			if (connList.isEmpty() == false) {

				for (Connectivity conn : connList) {
					if (conn instanceof ICustom2DField)
						continue;
					if (conn.getConnectedConnectivity() == null)
						continue;
					for (Connectivity eConn : conn.getConnectedConnectivity()) {
						eConn.removeConnectedConnectivity(conn);
					}
					conn.clearConnectedConnectivity();
				}
			}

			if (matrixItemList != null) {
				for (MatrixItem item : matrixItemList) {
					if (item.getConnectedConnectivity() == null)
						continue;
					item.getConnectedConnectivity().setConnectedConnectivity(
							null);
					item.setConnectedConnectivity(null);
				}
			}
		}

		for (int i = 0; i < allPartList.size(); i++) {
			updateMatrix((LDrawPart) allPartList.get(i));
		}
		// System.out.println("update All Connectivity Matrix: "
		// + (System.nanoTime() - nano));
	}

	public ArrayList<LDrawPart> getConnectedPart(LDrawPart part,
			IConnectivity centerConn, Vector3f directionVector,
			boolean isForRotation) {

		ArrayList<LDrawPart> resultList = new ArrayList<LDrawPart>();
		getConnectedPart(part, centerConn, directionVector, isForRotation,
				resultList);
		return resultList;
	}

	private ArrayList<LDrawPart> getConnectedPartBy(LDrawPart part,
			IConnectivity conn) {

		boolean isPartSelected = part.isSelected();
		ArrayList<LDrawPart> resultList = new ArrayList<LDrawPart>();
		if (conn == null)
			return resultList;
		if (resultList.contains(part))
			return resultList;
		if (BuilderConfigurationManager.getInstance().isUseConnectivity() == false)
			return resultList;

		part.setSelected(true);
		resultList.add(part);

		if (conn instanceof MatrixItem
				&& ((MatrixItem) conn).getConnectedConnectivity() != null)
			getConnectedPart(((MatrixItem) conn).getConnectedConnectivity()
					.getParent().getParent(), null, null, true, resultList);
		else
			for (Connectivity tempConn : conn.getConnectivity()
					.getConnectedConnectivity())
				getConnectedPart(tempConn.getParent(), null, null, true,
						resultList);
		resultList.remove(part);

		part.setSelected(isPartSelected);
		return resultList;
	}

	private void getConnectedPart(LDrawPart part, IConnectivity centerConn,
			Vector3f directionVector, boolean isForRotation,
			ArrayList<LDrawPart> resultList) {
		if (resultList.contains(part))
			return;
		resultList.add(part);

		if (part.isConnectivityInfoExist() == false)
			return;

		if (BuilderConfigurationManager.getInstance().isUseConnectivity() == false)
			return;
		for (Connectivity conn : part.getConnectivityList()) {
			if (conn instanceof ICustom2DField)
				continue;
			if (centerConn != null) {
				if (centerConn.getConnectivity() == conn)
					continue;
				if (isForRotation) {
					boolean isRotatable = false;

					if (conn instanceof Hinge || conn instanceof Ball)
						isRotatable = true;

					if (conn instanceof Axle)
						isRotatable = ((Axle) conn).isRotatible();

					if (isRotatable) {
						Vector3f testVector = conn.getCurrentPos().sub(
								centerConn.getCurrentPos());
						if (Math.abs(Math.abs(testVector.dot(directionVector))
								- testVector.length()) < 1.0f) {
							if (directionVector.equals(conn
									.getDirectionVector()))
								continue;
							if (directionVector.equals(conn
									.getDirectionVector().scale(-1)))
								continue;
						}
					}

				} else {
					boolean isMovable = false;
					if (conn instanceof Axle || conn instanceof Slider)
						isMovable = true;

					if (isMovable) {
						if (directionVector.equals(conn.getDirectionVector()))
							continue;
						if (directionVector.equals(conn.getDirectionVector()
								.scale(-1)))
							continue;
					}
				}
			}

			if (conn.getConnectedConnectivity() != null)
				for (Connectivity eItem : conn.getConnectedConnectivity()) {
					if (resultList.contains(eItem.getParent()))
						continue;
					if (eItem.getParent().isSelected())
						continue;
					getConnectedPart(eItem.getParent(), centerConn,
							directionVector, isForRotation, resultList);
				}
		}

		if (part.getConnectivityMatrixItemList() == null)
			return;

		for (MatrixItem matrixItem : part.getConnectivityMatrixItemList()) {
			MatrixItem eItem = matrixItem.getConnectedConnectivity();
			if (centerConn != null) {
				if (centerConn == matrixItem)
					continue;
				if (eItem == null)
					continue;
				if (isForRotation) {
					Vector3f testVector = centerConn.getCurrentPos().sub(
							matrixItem.getCurrentPos());
					if (Math.abs(Math.abs(testVector.dot(directionVector))
							- testVector.length()) < 1.0f) {
						if (directionVector.equals(eItem.getDirectionVector()))
							continue;
						if (directionVector.equals(eItem.getDirectionVector()
								.scale(-1)))
							continue;
					}
				}
			}

			if (eItem != null) {
				if (eItem.getParent().getParent().isSelected())
					continue;
				if (resultList.contains(eItem.getParent().getParent()))
					continue;
				getConnectedPart(eItem.getParent().getParent(), centerConn,
						directionVector, isForRotation, resultList);
			}
		}
	}

	public ArrayList<LDrawPart> getDirectlyConnectedParts(LDrawPart part) {
		ArrayList<LDrawPart> retList = new ArrayList<LDrawPart>();
		for (ConnectionPoint point : getConnectionPoints(part))
			if (retList.contains(point.getPart()) == false)
				retList.add(point.getPart());
		return retList;
	}

	public ArrayList<LDrawPart> getDirectlyConnectedPartsExceptCustom2d(
			LDrawPart part) {
		ArrayList<LDrawPart> retList = new ArrayList<LDrawPart>();
		for (ConnectionPoint point : getConnectionPoints(part))
			if (point.getFrom() instanceof ICustom2DField == false)
				retList.add(point.getPart());
		return retList;
	}

	//
	// public float getLowestConnectionPos(LDrawPart part,
	// ArrayList<LDrawPart> partList) {
	// float lowestPos = -Float.MAX_VALUE;
	// for (Connectivity conn : part.getConnectivityList()) {
	// Vector3f pos_Medium = null;
	// Vector3f pos_Fine = null;
	//
	// if (conn instanceof ICustom2DField) {
	// ConcurrentHashMap<Vector3f, ConcurrentHashMap<Vector3f,
	// ArrayList<MatrixItem>>> coordinateMap = null;
	// ConcurrentHashMap<Vector3f, ArrayList<MatrixItem>> fineCoordinateMap =
	// null;
	// if (conn instanceof Hole)
	// coordinateMap = studCoordinateMap;
	// else
	// coordinateMap = holeCoordinateMap;
	//
	// MatrixItem[][] matrixItems = ((ICustom2DField) conn)
	// .getMatrixItem();
	// for (int column = 1; column < matrixItems.length - 1; column += 2) {
	// for (int row = 1; row < matrixItems[column].length - 1; row += 2) {
	// pos_Medium = LDrawGridTypeT.getSnappedPos(
	// matrixItems[column][row].getCurrentPos(),
	// LDrawGridTypeT.Medium);
	// pos_Fine = LDrawGridTypeT.getSnappedPos(
	// matrixItems[column][row].getCurrentPos(),
	// LDrawGridTypeT.Fine);
	//
	// fineCoordinateMap = coordinateMap.get(pos_Medium);
	// if (fineCoordinateMap == null)
	// continue;
	// if (fineCoordinateMap.containsKey(pos_Fine) == false)
	// continue;
	//
	// for (MatrixItem eItem : fineCoordinateMap.get(pos_Fine)) {
	// if (partList
	// .contains(eItem.getParent().getParent()))
	// continue;
	//
	// if (matrixItems[column][row].isConnectable(eItem) ==
	// ConnectivityTestResultT.True) {
	// if (eItem.getDirection() != Direction6T.Y_Minus)
	// continue;
	// if (conn instanceof Stud) {
	// if (lowestPos < matrixItems[column][row]
	// .getCurrentPos().y + 0.5f)
	// lowestPos = matrixItems[column][row]
	// .getCurrentPos().y + 0.5f;
	// } else {
	// if (lowestPos < matrixItems[column][row]
	// .getCurrentPos().y)
	// lowestPos = matrixItems[column][row]
	// .getCurrentPos().y;
	// }
	// break;
	// }
	// }
	// }
	// }
	//
	// } else {
	// pos_Medium = LDrawGridTypeT.getSnappedPos(conn.getCurrentPos(),
	// LDrawGridTypeT.Medium);
	// pos_Fine = LDrawGridTypeT.getSnappedPos(conn.getCurrentPos(),
	// LDrawGridTypeT.Fine);
	//
	// ConcurrentHashMap<Vector3f, ArrayList<Connectivity>> fineConnectivityMap
	// = connectivityCoordinateMap
	// .get(pos_Medium);
	// if (fineConnectivityMap == null)
	// continue;
	// ArrayList<Connectivity> eConnList = fineConnectivityMap
	// .get(pos_Fine);
	// if (eConnList == null)
	// continue;
	//
	// for (Connectivity eItem : eConnList) {
	// if (partList.contains(eItem.getParent()))
	// continue;
	//
	// if (conn.isConnectable(eItem) == ConnectivityTestResultT.True) {
	// if (lowestPos < conn.getCurrentPos().y)
	// lowestPos = conn.getCurrentPos().y;
	// }
	// }
	// }
	// }
	//
	// return lowestPos;
	// }

	public ArrayList<ConnectionPoint> getConnectionPoints(LDrawPart part) {
		ArrayList<ConnectionPoint> retList = new ArrayList<ConnectionPoint>();

		if (part.getConnectivityMatrixItemList() != null)
			for (MatrixItem matrixItem : part.getConnectivityMatrixItemList()) {
				if (matrixItem.getConnectedConnectivity() != null) {
					if (matrixItem.isConnectable(matrixItem
							.getConnectedConnectivity()) == ConnectivityTestResultT.True) {
						ConnectionPoint point = new ConnectionPoint(matrixItem
								.getConnectedConnectivity().getParent()
								.getParent(), matrixItem,
								matrixItem.getConnectedConnectivity());
						if (retList.contains(point) == false)
							retList.add(point);
					}
				}
			}
		for (Connectivity conn : part.getConnectivityList()) {
			if (conn.getConnectedConnectivity() == null)
				continue;
			for (Connectivity eItem : conn.getConnectedConnectivity()) {
				if (conn instanceof ICustom2DField == false
						&& eItem instanceof ICustom2DField == false) {
					if (conn.isConnectable(eItem) == ConnectivityTestResultT.True) {
						ConnectionPoint point = new ConnectionPoint(
								eItem.getParent(), conn, eItem);
						if (retList.contains(point) == false)
							retList.add(point);
					}
				}
			}
		}
		return retList;
	}

	public boolean isRotatibleByCustom2dConn(LDrawPart part,
			ArrayList<MatrixItem> retRotationCenterList) {

		ArrayList<MatrixItem> connectingItemList = new ArrayList<MatrixItem>();
		for (ConnectionPoint point : getConnectionPoints(part)) {
			if (point.getFrom() instanceof MatrixItem)
				connectingItemList.add((MatrixItem) (point.getFrom()));
		}

		if (connectingItemList.size() == 0)
			return false;

		ArrayList<MatrixItem> victimList = new ArrayList<MatrixItem>();
		for (int i = 0; i < connectingItemList.size() - 1; i++) {
			for (int j = i + 1; j < connectingItemList.size(); j++) {
				Vector3f testVector = connectingItemList.get(i).getCurrentPos()
						.sub(connectingItemList.get(j).getCurrentPos());
				if (Math.abs(Math.abs(testVector.dot(connectingItemList.get(i)
						.getDirectionVector())) - testVector.length()) <= 1.0f) {
					victimList.add(connectingItemList.get(i));
				}
			}
		}
		connectingItemList.removeAll(victimList);

		HashMap<MatrixItem, ArrayList<LDrawPart>> connectedGroupMap = new HashMap<MatrixItem, ArrayList<LDrawPart>>();
		for (MatrixItem item : connectingItemList) {
			connectedGroupMap.put(item, getConnectedPartBy(part, item));
		}

		HashMap<MatrixItem, Boolean> isRotationalMap = new HashMap<MatrixItem, Boolean>();
		for (int i = 0; i < connectingItemList.size() - 1; i++) {
			boolean isRotatable = true;
			if (isRotationalMap.containsKey(connectingItemList.get(i)))
				continue;
			for (int j = i; j < connectingItemList.size(); j++) {
				if (i == j)
					continue;

				for (LDrawPart tempPart : connectedGroupMap
						.get(connectingItemList.get(i))) {
					if (connectedGroupMap.get(connectingItemList.get(j))
							.contains(tempPart)) {
						isRotatable = false;
						isRotationalMap.put(connectingItemList.get(i), false);
						isRotationalMap.put(connectingItemList.get(j), false);
					}
					break;
				}
				if (isRotatable == false)
					break;
			}
		}
		for (MatrixItem item : connectingItemList) {
			if (isRotationalMap.containsKey(item) == false)
				retRotationCenterList.add(item);
		}

		if (retRotationCenterList.size() == 0)
			return false;

		return true;

		// if (connectingItemList.size() == 1)
		// isRotatible = true;
		// else if (connectingItemList.size() == 2) {
		// MatrixItem item0 = connectingItemList.get(0);
		// MatrixItem item1 = connectingItemList.get(1);
		// // System.out.println(item0.getCurrentPos());
		// // System.out.println(item1.getCurrentPos());
		// //
		// System.out.println(item0.getCurrentPos().sub(item1.getCurrentPos())
		// // .cross(item0.getDirectionVector()).length());
		//
		// if (item0.getDirectionVector().equals(item1.getDirectionVector())
		// || item0.getDirectionVector().equals(
		// item1.getDirectionVector().scale(-1))) {
		// if (MatrixMath.compareFloat(
		// item0.getCurrentPos().sub(item1.getCurrentPos())
		// .cross(item0.getDirectionVector()).length(),
		// 0.1f) < 0)
		// isRotatible = true;
		// }
		// }
		// // System.out.println(connectingItemList.size());
		// // System.out.println(isRotatible);
		//
		// if (rotationCenter != null && isRotatible == true)
		// rotationCenter.addAll(connectingItemList);
		// return isRotatible;
	}

	public ArrayList<Vector3f> getTestibleSnappedPos(Vector3f originalPos,
			LDrawGridTypeT unit) {
		ArrayList<Vector3f> retList = new ArrayList<Vector3f>();

		Vector3f newPos;
		for (int x = Math.round(originalPos.x - unit.getXZValue() / 2); x <= Math
				.round(originalPos.x + unit.getXZValue() / 2); x += unit
				.getXZValue())
			for (int y = Math.round(originalPos.y - unit.getYValue() / 2); y <= Math
					.round(originalPos.y + unit.getYValue() / 2); y += unit
					.getYValue())
				for (int z = Math.round(originalPos.z - unit.getXZValue() / 2); z <= Math
						.round(originalPos.z + unit.getXZValue() / 2); z += unit
						.getXZValue()) {
					newPos = LDrawGridTypeT.getSnappedPos(
							new Vector3f(x, y, z), unit);
					if (retList.contains(newPos) == false)
						retList.add(newPos);
				}
		return retList;
	}
}
