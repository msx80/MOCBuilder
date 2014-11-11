package Command;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.media.opengl.GL2;
import javax.swing.undo.UndoManager;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import Common.Box2;
import Common.Box3;
import Common.Matrix3;
import Common.Matrix4;
import Common.Ray3;
import Common.Vector2f;
import Common.Vector3f;
import Common.Vector4f;
import Connectivity.CollisionBox;
import Connectivity.Connectivity;
import Connectivity.ConnectivityManager;
import Connectivity.GlobalConnectivityManager;
import Connectivity.Hole;
import Connectivity.ICustom2DField;
import Connectivity.MatrixItem;
import Connectivity.Stud;
import LDraw.Files.LDrawContainer;
import LDraw.Files.LDrawFile;
import LDraw.Files.LDrawModel;
import LDraw.Files.LDrawStep;
import LDraw.Support.ConnectivityLibrary;
import LDraw.Support.DispatchGroup;
import LDraw.Support.ILDrawObservable;
import LDraw.Support.ILDrawObserver;
import LDraw.Support.LDrawDirective;
import LDraw.Support.LDrawGlobalFlag;
import LDraw.Support.LDrawUtilities;
import LDraw.Support.LDrawVertices;
import LDraw.Support.MatrixMath;
import LDraw.Support.ModelManager;
import LDraw.Support.NSLock;
import LDraw.Support.PartLibrary;
import LDraw.Support.PartReport;
import LDraw.Support.Range;
import LDraw.Support.TransformComponents;
import LDraw.Support.type.CacheFlagsT;
import LDraw.Support.type.LDrawGridTypeT;
import LDraw.Support.type.MessageT;
import Notification.ILDrawSubscriber;
import Notification.INotificationMessage;
import Notification.NotificationMessageT;
import Renderer.ILDrawRenderer;
import Renderer.LDrawRenderColorT;

public class LDrawPart extends LDrawDrawableElement implements ILDrawObserver,
		ILDrawSubscriber {
	public static final float SHRINK_AMOUNT = 0.125f; // in LDU
	private boolean isConnectivityInfoExist = true;
	/**
	 * @uml.property name="displayName"
	 */
	String displayName;
	/**
	 * @uml.property name="referenceName"
	 */
	String referenceName; // lower-case version of display name
	/**
	 * @uml.property name="part data exist"
	 */
	boolean isPartDataExist = true;

	/**
	 * @uml.property name="glTransformation"
	 */
	float glTransformation[];

	/**
	 * @uml.property name="cacheDrawable"
	 * @uml.associationEnd
	 */
	LDrawDirective cacheDrawable; // The drawable is the model we link to OR a
									// VBO that represents it from the part
									// library -- a drawable proxy.
	/**
	 * @uml.property name="cacheModel"
	 * @uml.associationEnd
	 */
	LDrawModel cacheModel; // The model is the real model we link to.
	/**
	 * @uml.property name="cacheType"
	 * @uml.associationEnd
	 */
	PartTypeT cacheType;
	/**
	 * @uml.property name="drawLock"
	 * @uml.associationEnd
	 */
	NSLock drawLock;

	/**
	 * @uml.property name="cacheBounds"
	 * @uml.associationEnd
	 */
	Box3 cacheBounds = null; // Cached bonuding box of resolved parts, in part's
								// coordinate (that is, _not_ in the coordinates
								// of the
								// underlying model.
	Vector3f[] cachedOOB;
	Box3 boundsInIndentityTransform = null;
	Vector3f verticesForBoundsInIndentityTransform[] = null;
	float boundingBoxSize = 0;

	ArrayList<Connectivity> connectivityList = null;
	ArrayList<MatrixItem> connectivityMatrixItemList = null;
	ArrayList<CollisionBox> collisionBoxList = null;
	private boolean isDragingPart = false;

	public LDrawPart() {
		init();
	}

	// ========== defaultIconName
	// ===================================================
	//
	// Purpose: The default icon name for this class.
	//
	// ==============================================================================
	public static String defaultIconName() {
		return "Brick";
	} // end defaultIconName
		//
		// #pragma mark -
		// #pragma mark INITIALIZATION
		// #pragma mark -

	// ========== init
	// ==============================================================
	//
	// Purpose: Creates an empty part.
	//
	// ==============================================================================
	public LDrawPart init() {
		super.init();
		glTransformation = new float[16];
		setDisplayName("");
		setIconName("Brick");
		setTransformComponents(TransformComponents.getIdentityComponents());
		color = new LDrawColor();
		// drawLock = [[NSLock alloc] init);

		return this;

	}// end init

	// ========== initWithLines:inRange:parentGroup:
	// ================================
	//
	// Purpose: Returns the LDraw directive based on lineFromFile, a single line
	// of LDraw code from a file.
	//
	// Line format:
	// 1 colour x y z a b c d e f g h i part.dat
	//
	// Matrix format:
	// +- -+
	// | a d g 0 |
	// | b e h 0 |
	// | c f i 0 |
	// | x y z 1 |
	// +- -+
	//
	// ==============================================================================
	public LDrawPart initWithLines(ArrayList<String> lines, Range range,
			DispatchGroup parentGroup) throws Exception {
		String workingLine = lines.get(range.getLocation());
		String parsedField = null;
		Matrix4 transformation = Matrix4.getIdentityMatrix4();
		LDrawColor parsedColor = null;

		float[][] elements = transformation.getElement();

		try {
			super.initWithLines(lines, range, parentGroup);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// A malformed part could easily cause a string indexing error, which
		// would
		// raise an exception. We don't want this to happen here.
		try {

			// Read in the line code and advance past it.
			StringTokenizer strTokenizer = new StringTokenizer(workingLine);
			parsedField = strTokenizer.nextToken();
			// Only attempt to create the part if this is a valid line.
			if (Integer.parseInt(parsedField) == 1) {
				// Read in the color code.
				// (color)
				parsedField = strTokenizer.nextToken();

				parsedColor = LDrawUtilities.parseColorFromField(parsedField);
				setLDrawColor(parsedColor);
				// Read position.
				// (x)
				parsedField = strTokenizer.nextToken();
				elements[3][0] = MatrixMath
						.round(Float.parseFloat(parsedField));

				// (y)
				parsedField = strTokenizer.nextToken();
				elements[3][1] = MatrixMath
						.round(Float.parseFloat(parsedField));

				// (z)
				parsedField = strTokenizer.nextToken();
				elements[3][2] = MatrixMath
						.round(Float.parseFloat(parsedField));

				// Read Transformation X.
				// (a)

				parsedField = strTokenizer.nextToken();
				elements[0][0] = MatrixMath
						.round(Float.parseFloat(parsedField));
				// (b)

				parsedField = strTokenizer.nextToken();
				elements[1][0] = MatrixMath
						.round(Float.parseFloat(parsedField));
				// (c)

				parsedField = strTokenizer.nextToken();
				elements[2][0] = MatrixMath
						.round(Float.parseFloat(parsedField));

				// Read Transformation Y.
				// (d)

				parsedField = strTokenizer.nextToken();
				elements[0][1] = MatrixMath
						.round(Float.parseFloat(parsedField));
				// (e)

				parsedField = strTokenizer.nextToken();
				elements[1][1] = MatrixMath
						.round(Float.parseFloat(parsedField));
				// (f)

				parsedField = strTokenizer.nextToken();
				elements[2][1] = MatrixMath
						.round(Float.parseFloat(parsedField));

				// Read Transformation Z.
				// (g)

				parsedField = strTokenizer.nextToken();
				elements[0][2] = MatrixMath
						.round(Float.parseFloat(parsedField));
				// (h)

				parsedField = strTokenizer.nextToken();
				elements[1][2] = MatrixMath
						.round(Float.parseFloat(parsedField));
				// (i)

				parsedField = strTokenizer.nextToken();
				elements[2][2] = MatrixMath
						.round(Float.parseFloat(parsedField));

				// finish off the corner of the matrix.
				elements[3][3] = 1;

				setTransformationMatrix(transformation);

				// Read Part Name
				// (part.dat) -- It can have spaces (for MPD models), so we just
				// use the whole
				// rest of the line.

				if (strTokenizer.hasMoreTokens()) {
					String partName = strTokenizer.nextToken();
					while (strTokenizer.hasMoreTokens()) {
						partName += " " + strTokenizer.nextToken();
					}
					setDisplayName(partName, true, parentGroup);

				}

				// Debug check: full part resolution isn't thread-safe so make
				// sure we haven't run it by accident here!
				assert (cacheType == PartTypeT.PartTypeUnresolved);
			} else
				throw new Exception("BricksmithParseException: "
						+ "Bad part syntax");
		} catch (IOException e) {
			System.out.println(String.format("the part %s was fatally invalid",
					lines.get(range.getLocation())));

			System.out.println(String.format(" raised exception %s",
					e.getMessage()));
		}

		return this;

	}// end initWithLines:inRange:

	// ========== drawSelf:
	// ===========================================================
	//
	// Purpose: Draw this directive and its subdirectives by calling APIs on
	// the passed in renderer, then calling drawSelf on children.
	//
	// Notes: Parts draw by pushing the matrix and color instancing info they
	// contain into the renderer, then passing drawSelf to the model
	// backing the part, if it exists.
	//
	// ================================================================================
	public void drawSelf(GL2 gl2, ILDrawRenderer renderer) {
		if (hidden == false) {
			boolean isPushWire = false;
			resolvePart();
			if (cacheModel != null) {
				LDrawColorT colorCode = color.getColorCode();
				if (colorCode != LDrawColorT.LDrawCurrentColor) {
					// Old rendering code did not actually support
					// pushing the edge color as the new current
					// color - and it's probably against spec. But
					// it's not really the place of drawSelf to go
					// slappign wrists, so pass it to the render,
					// which actually DOES know how to get this case
					// right.
					if (colorCode == LDrawColorT.LDrawEdgeColor)
						renderer.pushColor(LDrawRenderColorT.LDrawRenderComplimentColor
								.getValue());
					else {
						float c[] = new float[4];
						color.getColorRGBA(c);
						renderer.pushColor(c);
					}
				}

				if (isSelected() == true) {
					renderer.pushWireFrame(gl2);
					isPushWire = true;
				}

				renderer.pushMatrix(glTransformation);

				if (cacheModel != null)
					cacheModel.drawSelf(gl2, renderer);

				renderer.popMatrix();
				if (LDrawGlobalFlag.SHRINK_SEAMS != false)
					renderer.popMatrix();

				if (colorCode != LDrawColorT.LDrawCurrentColor)
					renderer.popColor();
				if (isPushWire)
					renderer.popWireFrame(gl2);

			}
		}
	}// end drawSelf:

	public void getRange(Matrix4 transform, float[] range) {
		if (hidden == false) {
			Box3 bounds = boundingBox3();
			if (bounds == null)
				return;
			compareRange(range, bounds.getMax());
			compareRange(range, bounds.getMin());

			// Matrix4 partTransform = transformationMatrix();
			// Matrix4 combinedTransform = MatrixMath.Matrix4Multiply(
			// partTransform, transform);
			//
			// resolvePart();
			// // If we are doing a bounds test, for now get the model, not the
			// VBO
			// // - VBO bounds test does not yet exist
			// // (which is not so good). For parent-color parts we HAVE to get
			// the
			// // model - there is no VBO!
			// LDrawDirective modelToDraw = (cacheDrawable == null) ? cacheModel
			// : cacheDrawable;
			//
			// if (modelToDraw != null) {
			// modelToDraw.getRange(combinedTransform, range);
			// }
		}
	}

	// ========== hitTest:transform:viewScale:creditObject:hits: =======
	//
	// Purpose: Hit-test the geometry.
	//
	// ==============================================================================
	public void hitTest(Ray3 pickRay, Matrix4 transform,
			LDrawDirective creditObject, HashMap<LDrawDirective, Float> hits) {
		if (hidden == false) {
			Matrix4 partTransform = transformationMatrix();
			Matrix4 combinedTransform = MatrixMath.Matrix4Multiply(
					partTransform, transform);

			Box3 bounds = boundingBox3(combinedTransform);
			if (bounds == null)
				return;

			// if (boundingBoxSize > 100)
			if (MatrixMath.V3RayIntersectsBox3(pickRay, bounds, null, null) == false) {
				// System.out.println(displayName + ": " + boundingBoxSize);
				return;
			}

			// Credit all subgeometry to ourselves (unless we are already a
			// child part)
			if (creditObject == null) {
				creditObject = this;
			}

			resolvePart();
			// If we are doing a bounds test, for now get the model, not the VBO
			// - VBO bounds test does not yet exist
			// (which is not so good). For parent-color parts we HAVE to get the
			// model - there is no VBO!
			LDrawDirective modelToDraw = (cacheDrawable == null) ? cacheModel
					: cacheDrawable;

			if (modelToDraw != null) {
				modelToDraw.hitTest(pickRay, combinedTransform, creditObject,
						hits);
			}
		}
	}// end hitTest:transform:viewScale:creditObject:hits:

	// ========== boxTest:transform:creditObject:hits: ===================
	//
	// Purpose: Check for intersections with screen-space geometry.
	//
	// ==============================================================================
	public boolean boxTest(Box2 bounds, Matrix4 transform, boolean boundsOnly,
			LDrawDirective creditObject, TreeSet<LDrawDirective> hits) {
		if (hidden == false) {
			if (!MatrixMath.VolumeCanIntersectBox(boundingBox3(), transform,
					bounds)) {
				return false;
			}

			Matrix4 partTransform = transformationMatrix();
			Matrix4 combinedTransform = MatrixMath.Matrix4Multiply(
					partTransform, transform);
			LDrawDirective modelToDraw = null;

			// Credit all subgeometry to ourselves (unless we are already a
			// child part)
			if (creditObject == null) {
				creditObject = this;
			}

			resolvePart();
			modelToDraw = cacheModel;

			if (boundsOnly == false) {
				if (modelToDraw.boxTest(bounds, combinedTransform, false,
						creditObject, hits))
					if (creditObject != null)
						return true;
			} else {
				// Hit test the bounding cube
				LDrawVertices unitCube = LDrawUtilities.boundingCube();
				Box3 bounds_3d = modelToDraw.boundingBox3();
				Vector3f extents = MatrixMath.V3Sub(bounds_3d.getMax(),
						bounds_3d.getMin());
				Matrix4 boxTransform = Matrix4.getIdentityMatrix4();

				// Expand and position the unit cube to match the model
				boxTransform = MatrixMath.Matrix4Scale(boxTransform, extents);
				boxTransform = MatrixMath.Matrix4Translate(boxTransform,
						bounds_3d.getMin());

				combinedTransform = MatrixMath.Matrix4Multiply(boxTransform,
						combinedTransform);

				if (unitCube.boxTest(bounds, combinedTransform, false,
						creditObject, hits))
					if (creditObject != null)
						return true;
			}
		}
		return false;
	}// end boxTest:transform:creditObject:hits:

	// ========== depthTest:transform:creditObject:bestObject:bestDepth:=======
	//
	// Purpose: depthTest finds the closest primitive (in screen space)
	// overlapping a given point, as well as its device coordinate
	// depth.
	//
	// ==============================================================================
	public void depthTest(Vector2f testPt, Box2 bounds, Matrix4 transform,
			LDrawDirective creditObject, ArrayList<LDrawDirective> bestObject,
			FloatBuffer bestDepth) {
		if (hidden == false) {
			if (!MatrixMath.VolumeCanIntersectPoint(boundingBox3(), transform,
					bounds, bestDepth.get(0)))
				return;

			Matrix4 partTransform = transformationMatrix();
			Matrix4 combinedTransform = MatrixMath.Matrix4Multiply(
					partTransform, transform);
			LDrawModel modelToDraw = null;

			// Credit all subgeometry to ourselves (unless we are already a
			// child part)
			if (creditObject == null) {
				creditObject = this;
			}

			resolvePart();
			modelToDraw = cacheModel;

			modelToDraw.depthTest(testPt, bounds, combinedTransform,
					creditObject, bestObject, bestDepth);
		}
	}// end depthTest:transform:creditObject:bestObject:bestDepth:

	// ========== write
	// =============================================================
	//
	// Purpose: Returns a line that can be written out to a file.
	//
	// Line format:
	// 1 colour x y z a b c d e f g h i part.dat
	//
	// Matrix format:
	// +- -+
	// | a d g 0 |
	// | b e h 0 |
	// | c f i 0 |
	// | x y z 1 |
	// +- -+
	//
	// ==============================================================================
	public String write() {
		Matrix4 transformation = transformationMatrix();

		float[][] elements = transformation.getElement();

		return String.format("1 %s %s %s %s %s %s %s %s %s %s %s %s %s %s",
				LDrawUtilities.outputStringForColor(color),

				LDrawUtilities.outputStringForFloat(elements[3][0]), // position.x,
																		// (x)
				LDrawUtilities.outputStringForFloat(elements[3][1]), // position.y,
																		// (y)
				LDrawUtilities.outputStringForFloat(elements[3][2]), // position.z,
																		// (z)

				LDrawUtilities.outputStringForFloat(elements[0][0]), // transformationX.x,
																		// (a)
				LDrawUtilities.outputStringForFloat(elements[1][0]), // transformationX.y,
																		// (b)
				LDrawUtilities.outputStringForFloat(elements[2][0]), // transformationX.z,
																		// (c)

				LDrawUtilities.outputStringForFloat(elements[0][1]), // transformationY.x,
																		// (d)
				LDrawUtilities.outputStringForFloat(elements[1][1]), // transformationY.y,
																		// (e)
				LDrawUtilities.outputStringForFloat(elements[2][1]), // transformationY.z,
																		// (f)

				LDrawUtilities.outputStringForFloat(elements[0][2]), // transformationZ.x,
																		// (g)
				LDrawUtilities.outputStringForFloat(elements[1][2]), // transformationZ.y,
																		// (h)
				LDrawUtilities.outputStringForFloat(elements[2][2]), // transformationZ.z,
																		// (i)

				displayName);
	}// end write

	// #pragma mark -
	// #pragma mark DISPLAY
	// #pragma mark -

	// ========== browsingDescription
	// ===============================================
	//
	// Purpose: Returns a representation of the directive as a short string
	// which can be presented to the user.
	//
	// Here we want the part name displayed.
	//
	// ==============================================================================
	public String browsingDescription() {
		return PartLibrary.sharedPartLibrary().descriptionForPart(this);

	}// end browsingDescription

	// ========== inspectorClassName
	// ================================================
	//
	// Purpose: Returns the name of the class used to inspect this one.
	//
	// ==============================================================================
	public String inspectorClassName() {
		return "InspectionPart";

	}// end inspectorClassName

	// #pragma mark -
	// #pragma mark ACCESSORS
	// #pragma mark -

	public Box3 boundingBox3(Matrix4 transform) {
		Box3 bounds = null;
		int counter;
		Vector3f vertices[] = new Vector3f[8];
		if (verticesForBoundsInIndentityTransform == null)
			boundingBox3();
		if (verticesForBoundsInIndentityTransform == null)
			return Box3.getInvalidBox();
		for (counter = 0; counter < 8; counter++) {
			if (verticesForBoundsInIndentityTransform[counter] != null)
				vertices[counter] = transform
						.transformPoint(verticesForBoundsInIndentityTransform[counter]);
			else
				vertices[counter] = transform.transformPoint(new Vector3f());

			bounds = MatrixMath.V3UnionBoxAndPoint(bounds, vertices[counter]);
		}

		return bounds;
	}

	public Box3 boundingBox3Copy() {
		try {
			return (Box3) boundingBox3().clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Vector3f[] getCachedOOB() {
		// resolvePart();

		// if (revalCache(CacheFlagsT.CacheFlagBounds) ==
		// CacheFlagsT.CacheFlagBounds)
		boundingBox3();

		return cachedOOB;

	}

	// ========== boundingBox3
	// ======================================================
	//
	// Purpose: Returns the minimum and maximum points of the box which
	// perfectly contains this object. Returns InvalidBox if the part
	// cannot be found.
	//
	// ==============================================================================
	public synchronized Box3 boundingBox3() {
		resolvePart();

		if (revalCache(CacheFlagsT.CacheFlagBounds) == CacheFlagsT.CacheFlagBounds) {
			// if (boundsInIndentityTransform == null)
			// boundsInIndentityTransform =
			// getBoundingBoxFromConnectivityFile();
			if (boundsInIndentityTransform == null)
				boundsInIndentityTransform = getBoundingsFromModel();
			if (boundsInIndentityTransform == null)
				boundsInIndentityTransform = Box3.getInvalidBox();

			Box3 bounds = null;

			if (boundsInIndentityTransform != Box3.getInvalidBox()) {
				setPartDataExist(true);
				// Transform all the points of the bounding box to find
				// the
				// new
				// minimum and maximum.
				if (verticesForBoundsInIndentityTransform == null) {
					Vector3f min = boundsInIndentityTransform.getMin();
					Vector3f max = boundsInIndentityTransform.getMax();

					verticesForBoundsInIndentityTransform = new Vector3f[] {
							new Vector3f(new float[] { min.getX(), min.getY(),
									min.getZ() }),
							new Vector3f(new float[] { min.getX(), min.getY(),
									max.getZ() }),
							new Vector3f(new float[] { min.getX(), max.getY(),
									max.getZ() }),
							new Vector3f(new float[] { min.getX(), max.getY(),
									min.getZ() }),

							new Vector3f(new float[] { max.getX(), min.getY(),
									min.getZ() }),
							new Vector3f(new float[] { max.getX(), min.getY(),
									max.getZ() }),
							new Vector3f(new float[] { max.getX(), max.getY(),
									max.getZ() }),
							new Vector3f(new float[] { max.getX(), max.getY(),
									min.getZ() }), };
				}
				int counter = 0;
				Matrix4 transformation = transformationMatrix();
				Vector3f vertices[] = new Vector3f[8];
				for (counter = 0; counter < 8; counter++) {
					vertices[counter] = transformation
							.transformPoint(verticesForBoundsInIndentityTransform[counter]);
					bounds = MatrixMath.V3UnionBoxAndPoint(bounds,
							vertices[counter]);
				}
				cachedOOB = vertices;
				cacheBounds = bounds;
				Vector3f temp = bounds.getMax().sub(bounds.getMin());
				boundingBoxSize = temp.dot(temp);
			} else {
				setPartDataExist(false);

				Matrix4 transformation = transformationMatrix();

				cacheBounds = new Box3();
				cacheBounds.setMax(transformation.getDefaultTransformPos());
				cacheBounds.setMin(transformation.getDefaultTransformPos());

				if (cachedOOB == null) {
					cachedOOB = new Vector3f[8];
					for (int i = 0; i < 8; i++)
						cachedOOB[i] = transformation.getDefaultTransformPos();
				}
				for (int i = 0; i < 8; i++)
					cachedOOB[i] = transformation.getDefaultTransformPos();
			}
		}

		return cacheBounds;

	}// end boundingBox3

	private Box3 getBoundingsFromModel() {
		LDrawModel modelToDraw = cacheModel;
		Box3 bounds = Box3.getInvalidBox();

		// We need to have an actual model here. Blithely calling
		// boundingBox3 will
		// result in most of our Box3 structure being garbage data!
		if (modelToDraw != null && hidden == false) {
			bounds = modelToDraw.boundingBox3();
		}
		return bounds;
	}

	//
	// private Box3 getBoundingBoxFromConnectivityFile() {
	// Box3 retBox = new Box3();
	//
	// retBox.setMin(new Vector3f(3.40282347E+38f, 3.40282347E+38f,
	// 3.40282347E+38f));
	// retBox.setMax(new Vector3f(-3.40282347E+38f, -3.40282347E+38f,
	// -3.40282347E+38f));
	//
	// ArrayList<CollisionBox> boxes = getCollisionBoxList();
	// if (boxes == null || boxes.size() == 0)
	// return null;
	//
	// for (int j = 0; j < boxes.size(); j++) {
	// CollisionBox collisionBox = boxes.get(j);
	//
	// Vector3f[] boxPos = new Vector3f[8];
	// boxPos[0] = new Vector3f(-collisionBox.getsX(),
	// -collisionBox.getsY(), -collisionBox.getsZ());
	// boxPos[1] = new Vector3f(collisionBox.getsX(),
	// -collisionBox.getsY(), -collisionBox.getsZ());
	// boxPos[2] = new Vector3f(collisionBox.getsX(),
	// -collisionBox.getsY(), collisionBox.getsZ());
	// boxPos[3] = new Vector3f(-collisionBox.getsX(),
	// -collisionBox.getsY(), collisionBox.getsZ());
	// boxPos[4] = new Vector3f(-collisionBox.getsX(),
	// collisionBox.getsY(), -collisionBox.getsZ());
	// boxPos[5] = new Vector3f(collisionBox.getsX(),
	// collisionBox.getsY(), -collisionBox.getsZ());
	// boxPos[6] = new Vector3f(collisionBox.getsX(),
	// collisionBox.getsY(), collisionBox.getsZ());
	// boxPos[7] = new Vector3f(-collisionBox.getsX(),
	// collisionBox.getsY(), collisionBox.getsZ());
	//
	// for (int k = 0; k < 8; k++) {
	// boxPos[k] = boxPos[k].scale(1.03f);
	// boxPos[k] = collisionBox.getTransformMatrix().transformPoint(
	// boxPos[k]);
	//
	// if (retBox.min.x > boxPos[k].x)
	// retBox.min.x = boxPos[k].x;
	// if (retBox.min.y > boxPos[k].y)
	// retBox.min.y = boxPos[k].y;
	// if (retBox.min.z > boxPos[k].z)
	// retBox.min.z = boxPos[k].z;
	//
	// if (retBox.max.x < boxPos[k].x)
	// retBox.max.x = boxPos[k].x;
	// if (retBox.max.y < boxPos[k].y)
	// retBox.max.y = boxPos[k].y;
	// if (retBox.max.z < boxPos[k].z)
	// retBox.max.z = boxPos[k].z;
	// }
	// }
	//
	// return retBox;
	// }

	// ========== displayName
	// =======================================================
	//
	// Purpose: Returns the name of the part as the user typed it. This
	// maintains the user's upper- and lower-case usage.
	//
	// ==============================================================================
	public String displayName() {
		return displayName;

	}// end displayName

	public boolean isPartDataExist() {
		return isPartDataExist;
	}

	public boolean isDraggingPart() {
		return isDragingPart;
	}

	public void isDraggingPart(boolean isDragingPart) {
		this.isDragingPart = isDragingPart;
	}

	public boolean isConnectivityInfoExist() {
		if (isConnectivityInfoExist == true)
			if (connectivityList == null) {
				isConnectivityInfoExist = ConnectivityLibrary.getInstance()
						.hasConnectivity(displayName);
			} else if (connectivityList.isEmpty())
				isConnectivityInfoExist = false;

		return isConnectivityInfoExist;
	}

	public void setPartDataExist(boolean exist) {
		isPartDataExist = exist;
	}

	// ========== position
	// ==========================================================
	//
	// Purpose: Returns the coordinates at which the part is drawn.
	//
	// Notes: This is purely a convenience method. The actual position is
	// encoded in the transformation matrix. If you wish to set the
	// position, you should set either the matrix or the Transformation
	// Components.
	//
	// ==============================================================================
	public Vector3f position() {
		TransformComponents components = transformComponents();
		Vector3f position = components.getTranslate();

		return position;

	}// end position

	/*
	 * To work, this needs to multiply the modelViewGLMatrix by the part
	 * transform.
	 * 
	 * //========== projectedBoundingBoxWithModelView:projection:view:
	 * ================ // // Purpose: Returns the 2D projection (ignore the z)
	 * of the object's bounds. //
	 * //=============================================
	 * ================================= - (Box3)
	 * projectedBoundingBoxWithModelView:(Matrix4)modelView
	 * projection:(Matrix4)projection view:(Box2)viewport; { LDrawModel
	 * *modelToDraw = PartLibrary.sharedPartLibrary().modelForPart:this); Box3
	 * projectedBounds = InvalidBox;
	 * 
	 * projectedBounds = [modelToDraw
	 * projectedBoundingBoxWithModelView:modelViewGLMatrix
	 * projection:projectionGLMatrix view:viewport);
	 * 
	 * return projectedBounds;
	 * 
	 * }//end projectedBoundingBoxWithModelView:projection:view:
	 */

	// ========== referenceName
	// =====================================================
	//
	// Purpose: Returns the name of the part. This is the filename where the
	// part is found. Since Macintosh computers are case-insensitive,
	// I have adopted lower-case as the standard for names.
	//
	// ==============================================================================
	public String referenceName() {
		return referenceName;

	}// end referenceName

	// ========== referencedMPDSubmodel
	// =============================================
	//
	// Purpose: Returns the MPD model to which this part refers, or null if
	// there
	// is no submodel in this part's file which has the name this part
	// specifies.
	//
	// Note: This method is ONLY intended to be used for resolving MPD
	// references. If you want to resolve the general reference, you
	// should call -modelForPart: in the PartLibrary!
	//
	// ==============================================================================
	public LDrawModel referencedMPDSubmodel() {
		LDrawModel model = null;
		LDrawFile enclosingFile = enclosingFile();

		if (enclosingFile != null)
			model = (LDrawModel) enclosingFile.modelWithName(referenceName);

		// No can do if we get a reference back to ourselves. That would be
		// an infinitely-recursing reference, which is bad!
		if (enclosingStep() != null)
			if (model == enclosingStep().enclosingModel())
				model = null;
		return model;
	}// end referencedMPDSubmodel

	// ========== transformComponents
	// ===============================================
	//
	// Purpose: Returns the individual components of the transformation matrix
	// applied to this part.
	//
	// ==============================================================================
	public TransformComponents transformComponents() {
		Matrix4 transformation = transformationMatrix();
		TransformComponents components = TransformComponents
				.getIdentityComponents();

		// This is a pretty darn neat little function. I wish I could say I
		// wrote it.
		// It will extract all the user-friendly components out of this nasty
		// matrix.
		MatrixMath.Matrix4DecomposeTransformation(transformation, components);

		return components;

	}// end transformComponents

	// ========== transformationMatrix
	// ==============================================
	//
	// Purpose: Returns a two-dimensional (row matrix) representation of the
	// part's transformation matrix.
	//
	// +- -+
	// +- -+ +- -+| a d g 0 |
	// |a d g 0 b e h c f i 0 x y z 1| --> |x y z 1|| b e h 0 |
	// +- -+ +- -+| c f i 0 |
	// | x y z 1 |
	// +- -+
	// OpenGL Matrix Format LDraw Matrix
	// (flat column-major of transpose) Format
	//
	// ==============================================================================
	public Matrix4 transformationMatrix() {
		return MatrixMath.Matrix4CreateFromGLMatrix4(glTransformation);

	}// end transformationMatrix

	// #pragma mark -

	// ========== setEnclosingDirective:
	// ============================================
	// ==============================================================================
	public void setEnclosingDirective(LDrawContainer newParent) {
		unresolvePart();
		super.setEnclosingDirective(newParent);
	}

	// ========== setLDrawColor:
	// ====================================================
	//
	// Purpose: Sets the color of this element.
	//
	// ==============================================================================
	public void setLDrawColor(LDrawColor newColor) {
		super.setLDrawColor(newColor);

		unresolvePart();
		invalCache(CacheFlagsT.CacheFlagBounds);

	}// end setLDrawColor:

	// ========== setDisplayName:
	// ===================================================
	//
	// Purpose: Updates the name of the part and attempts to load it into the
	// part library.
	//
	// ==============================================================================
	public void setDisplayName(String newPartName) {
		setDisplayName(newPartName, true, new DispatchGroup());
	}

	// ========== setDisplayName:parse:inGroup:
	// =====================================
	//
	// Purpose: Updates the name of the part. This is the filename where the
	// part is found.
	//
	// If shouldParse istrue, pre-loads the referenced part if
	// possible. Pre-loading is very import in initial model loading,
	// because it enables structual optimizations to be performed prior
	// to OpenGL optimizations. It also results in a more honest load
	// progress bar.
	//
	// Notes: References to LDraw/parts and LDraw/p are simply encoded as the
	// file name. However, references to LDraw/parts/s are encoded as
	// "s\partname.dat". The part library, meanwhile, must properly
	// handle the s\ prefix.
	//
	// ==============================================================================
	/**
	 * @param newPartName
	 * @param shouldParse
	 * @param parentGroup
	 * @uml.property name="displayName"
	 */
	public void setDisplayName(String newPartName, boolean shouldParse,
			DispatchGroup parentGroup) {
		String newReferenceName = newPartName.toLowerCase();
		DispatchGroup parseGroup = null;

		displayName = newPartName;

		referenceName = newReferenceName;

		assert (parentGroup == null || cacheType == PartTypeT.PartTypeUnresolved);

		unresolvePart();

		// Force the part library to parse the model this part will display.
		// This
		// pushes all parsing into the same operation, which improves loading
		// time
		// predictability and allows better potential threading optimization.
		//
		// Ben says: we _have_ to call this, even on MPD and peer models. Since
		// we don't know what kind of thing we are, checking the cache type will
		// always return unresolved. But I don't think I want to force-resolve
		// here - resolving later prevents thrash.
		if (shouldParse == true && newPartName != null
				&& newPartName.length() > 0) {
			parseGroup = new DispatchGroup();
			parseGroup.extendsFromParent(parentGroup);

			if (transformationMatrix().getDet() < 0)
				parseGroup.setReversed();

			referenceName = newPartName + (parseGroup.isCCW() ? "" : "_CW");

			PartLibrary.sharedPartLibrary().loadModelForName(displayName,
					referenceName, parseGroup);
		}

	}// end setDisplayName:

	// ========== setTransformComponents:
	// ===========================================
	//
	// Purpose: Converts the given componets (rotation, scaling, etc.) into an
	// internal transformation matrix represenation.
	//
	// ==============================================================================
	public void setTransformComponents(TransformComponents newComponents) {
		Matrix4 transformation = MatrixMath
				.Matrix4CreateTransformation(newComponents);

		setTransformationMatrix(transformation);

	}// end setTransformComponents:

	// ========== setTransformationMatrix:
	// ==========================================
	//
	// Purpose: Converts the row-major row-vector matrix into a flat column-
	// major column-vector matrix understood by OpenGL.
	//
	//
	// +- -+ +- -++- -+
	// +- -+| a d g 0 | | a b c x || x |
	// |x y z 1|| b e h 0 | | d e f y || y | +- -+
	// +- -+| c f i 0 | --> | g h i z || z | --> |a d g 0 b e h c f i 0 x y z 1|
	// | x y z 1 | | 0 0 0 1 || 1 | +- -+
	// +- -+ +- -++- -+
	// LDraw Matrix Transpose OpenGL Matrix Format
	// Format (flat column-major of transpose)
	// (also Matrix4 format)
	//
	// ==============================================================================
	public void setTransformationMatrix(Matrix4 newMatrix) {
		invalCache(CacheFlagsT.CacheFlagBounds);
		MatrixMath.Matrix4GetGLMatrix4(newMatrix, glTransformation);

		sendMessageToObservers(MessageT.MessageObservedChanged);
	}// end setTransformationMatrix

	// ========== setSelected:
	// ======================================================
	//
	// Purpose: Somebody make this a protocol method.
	//
	// ==============================================================================
	public void setSelected(boolean flag) {
		super.setSelected(flag);

		// would like LDrawContainer to be a protocol. In its absence...
		LDrawDirective enclosingDirective = enclosingDirective();
		if (enclosingDirective != null) {
			if (LDrawContainer.class.isInstance(enclosingDirective))
				((LDrawContainer) enclosingDirective)
						.setSubdirectiveSelected(flag);
		}
	}// end setSelected:

	// #pragma mark -
	// #pragma mark MOVEMENT
	// #pragma mark -

	// ========== displacementForNudge:
	// =============================================
	//
	// Purpose: Returns the amount by which the element wants to move, given a
	// "nudge" in the specified direction. A "nudge" is generated by
	// pressing the arrow keys. We scale this value so as to make
	// nudging go by plate-heights vertically and brick widths
	// horizontally.
	//
	// ==============================================================================
	public Vector3f displacementForNudge(Vector3f nudgeVector) {
		Matrix4 transformationMatrix = Matrix4.getIdentityMatrix4();
		Matrix4 inverseMatrix = Matrix4.getIdentityMatrix4();
		Vector4f worldNudge = new Vector4f(new float[] { 0f, 0f, 0f, 1f });
		Vector4f brickNudge = new Vector4f();

		// convert incoming 3D vector to 4D for our math:
		worldNudge.setX(nudgeVector.getX());
		worldNudge.setY(nudgeVector.getY());
		worldNudge.setZ(nudgeVector.getZ());

		// Figure out which direction we're asking to move the part itthis.
		transformationMatrix = transformationMatrix();
		inverseMatrix = MatrixMath.Matrix4Invert(transformationMatrix);
		float[][] elements = inverseMatrix.getElement();
		elements[3][0] = 0; // zero out the translation part, leaving only
							// rotation etc.
		elements[3][1] = 0;
		elements[3][2] = 0;

		// See if this is a nudge along the brick's "up" direction.
		// If so, the nudge needs to be a different magnitude, to compensate
		// for the fact that Lego bricks are not square!
		brickNudge = MatrixMath.V4MulPointByMatrix(worldNudge, inverseMatrix);
		if (Math.abs(brickNudge.getY()) > Math.abs(brickNudge.getX())
				&& Math.abs(brickNudge.getY()) > Math.abs(brickNudge.getZ())) {
			// The trouble is, we need to do different things for different
			// scales. For instance, in medium mode, we probably want to
			// move 1/2 stud horizontally but 1/3 stud vertically.
			//
			// But in coarse mode, we want to move 1 stud horizontally and
			// vertically. These are different ratios! So I test for known
			// numbers, and only apply modifications if they are recognized.

			if (nudgeVector.getX() % 20 == 0)
				nudgeVector.setX(nudgeVector.getX() * 24.0f / 20.0f);
			else if (nudgeVector.getX() % 10 == 0)
				nudgeVector.setX(nudgeVector.getX() * 8.0f / 10.0f);

			if (nudgeVector.getY() % 20 == 0)
				nudgeVector.setY(nudgeVector.getY() * 24.0f / 20.0f);
			else if (nudgeVector.getY() % 10 == 0)
				nudgeVector.setY(nudgeVector.getY() * 8.0f / 10.0f);

			if (nudgeVector.getZ() % 20 == 0)
				nudgeVector.setZ(nudgeVector.getZ() * 24.0f / 20.0f);
			else if (nudgeVector.getZ() % 10 == 0)
				nudgeVector.setZ(nudgeVector.getZ() * 8.0f / 10.0f);
		}

		// we now have a nudge based on the correct size: plates or bricks.
		return nudgeVector;

	}// end displacementForNudge:

	// ========== componentsSnappedToGrid:minimumAngle:
	// =============================
	//
	// Purpose: Returns a copy of the part's current components, but snapped to
	// the grid. Kinda a weird legacy API.
	//
	// ==============================================================================
	public TransformComponents componentsSnappedToGrid(float gridSpacing,
			float degrees) {
		TransformComponents components = transformComponents();

		return components(components, gridSpacing, degrees);

	}// end componentsSnappedToGrid:minimumAngle:

	// ========== components:snappedToGrid:minimumAngle:
	// ============================
	//
	// Purpose: Aligns the given components to an imaginary grid along lines
	// separated by a distance of gridSpacing. This is done
	// intelligently based on the current orientation of the receiver:
	// if gridSpacing == 20, that is assumed to mean "1 stud," so the
	// y-axis (up) of the part will be aligned along a grid spacing of
	// 24 (1 stud vertically).
	//
	// The part's rotation angles will be adjusted to multiples of the
	// minimum angle specified.
	//
	// Parameters: components - transform to adjust.
	// gridSpacing - the grid line interval along stud widths.
	// degrees - angle granularity. Pass 0 to leave angle
	// unchanged.
	//
	// ==============================================================================
	public TransformComponents components(TransformComponents components,
			float gridSpacing, float degrees) {
		float rotationRadians = (float) Math.toRadians(degrees);

		Matrix4 transformationMatrix = Matrix4.getIdentityMatrix4();
		Vector4f yAxisOfPart = new Vector4f(new float[] { 0, 1, 0, 1 });
		Vector4f worldY = new Vector4f(new float[] { 0, 0, 0, 1 }); // yAxisOfPart
																	// converted
																	// to world
																	// coordinates
		Vector3f worldY3 = new Vector3f(new float[] { 0, 0, 0 });
		float gridSpacingYAxis = 0.0f;
		float gridX = 0.0f;
		float gridY = 0.0f;
		float gridZ = 0.0f;

		// ---------- Adjust position to grid
		// ---------------------------------------

		// Figure out which direction the y-axis is facing in world coordinates:
		transformationMatrix = transformationMatrix();
		float[][] elements = transformationMatrix.getElement();
		elements[3][0] = 0; // zero out the translation part, leaving only
							// rotation etc.
		elements[3][1] = 0;
		elements[3][2] = 0;
		worldY = MatrixMath.V4MulPointByMatrix(yAxisOfPart,
				transformationMatrix());

		worldY3 = MatrixMath.V3FromV4(worldY);
		worldY3 = MatrixMath.V3IsolateGreatestComponent(worldY3);
		worldY3 = MatrixMath.V3Normalize(worldY3);

		// Get the adjusted grid spacing along the y direction. Remember that
		// Lego
		// bricks are not cubical, so the grid along the brick's y-axis should
		// be
		// spaced differently from the grid along its other sides.
		gridSpacingYAxis = gridSpacing;

		if (gridSpacing % 20 == 0)
			gridSpacingYAxis *= 24.0 / 20.0;

		else if (gridSpacing % 10 == 0)
			gridSpacingYAxis *= 8.0 / 10.0;

		// The actual grid spacing, in world coordinates. We will adjust the
		// approrpiate
		// x, y, or z based on which one the part's y-axis is aligned.
		gridX = gridSpacing;
		gridY = gridSpacing;
		gridZ = gridSpacing;

		// Find the direction of the part's Y-axis, and change its grid.
		if (MatrixMath.compareFloat(worldY3.getX(), 0f) != 0)
			gridX = gridSpacingYAxis;

		if (MatrixMath.compareFloat(worldY3.getY(), 0f) != 0)
			gridY = gridSpacingYAxis;

		if (MatrixMath.compareFloat(worldY3.getZ(), 0f) != 0)
			gridZ = gridSpacingYAxis;

		// Snap to the Grid!
		// Figure the closest grid line and bump the part to it.
		// Logically, this is a rounding operation with a granularity of the
		// grid
		// size. So all we need to do is normalize, round, then expand back to
		// the
		// original size.

		components.getTranslate().setX(
				Math.round(components.getTranslate().getX() / gridX) * gridX);
		components.getTranslate().setX(
				Math.round(components.getTranslate().getY() / gridY) * gridY);
		components.getTranslate().setX(
				Math.round(components.getTranslate().getZ() / gridZ) * gridZ);

		// ---------- Snap angles
		// ---------------------------------------------------

		if (rotationRadians != 0) {
			components.getRotate().setX(
					Math.round(components.getRotate().getX() / rotationRadians)
							* rotationRadians);
			components.getRotate().setY(
					Math.round(components.getRotate().getY() / rotationRadians)
							* rotationRadians);
			components.getRotate().setZ(
					Math.round(components.getRotate().getZ() / rotationRadians)
							* rotationRadians);
		}

		// round-off errors here? Potential for trouble.
		return components;

	}// end components:snappedToGrid:minimumAngle:

	// ========== moveBy:
	// ===========================================================
	//
	// Purpose: Moves the receiver in the specified direction.
	//
	// ==============================================================================
	public boolean moveBy(Vector3f moveVector, LDrawGridTypeT type) {
		return moveBy(moveVector, type, true);

	}// end moveBy:

	public boolean moveBy(Vector3f moveVector, LDrawGridTypeT type,
			boolean useSnap) {
		if (useSnap)
			moveVector = LDrawGridTypeT.getSnappedPos(moveVector, type);

		Matrix4 transformationMatrix = transformationMatrix();

		// I NEED to modify the matrix itself here. Some parts have funky,
		// fragile
		// rotation values, and getting the components really badly botches them
		// up.
		transformationMatrix = MatrixMath.Matrix4Translate(
				transformationMatrix, moveVector);

		setTransformationMatrix(transformationMatrix);
		sendMessageToObservers(MessageT.MessageObservedChanged);

		if (MatrixMath.V3EqualPoints(moveVector, new Vector3f(0, 0, 0)))
			return false;
		return true;

	}// end moveBy:

	public void moveTo(Vector3f moveVector, LDrawGridTypeT type) {
		Matrix4 transformationMatrix = transformationMatrix();

		// I NEED to modify the matrix itself here. Some parts have funky,
		// fragile
		// rotation values, and getting the components really badly botches them
		// up.
		float[][] t = transformationMatrix.getElement();

		t[3][0] = Math.round(moveVector.getX() / type.getXZValue())
				* type.getXZValue();
		t[3][1] = Math.round(moveVector.getY() / type.getYValue())
				* type.getYValue();
		t[3][2] = Math.round(moveVector.getZ() / type.getXZValue())
				* type.getXZValue();

		setTransformationMatrix(transformationMatrix);
		sendMessageToObservers(MessageT.MessageObservedChanged);

	}// end moveBy:

	// ========== position:snappedToGrid:
	// ===========================================
	//
	// Purpose: Orients position at discrete points separated by the given grid
	// spacing.
	//
	// Notes: This method may be overridden by subclasses to provide more
	// intelligent grid alignment.
	//
	// This method is provided mainly as a service to drag-and-drop.
	// In the case of LDrawParts, you should generally avoid this
	// method in favor of
	// -[LDrawPart components:snappedToGrid:minimumAngle:].
	//
	// ==============================================================================
	public Vector3f position(Vector3f position, float gridSpacing) {
		TransformComponents components = TransformComponents
				.getIdentityComponents();

		// copy the position into a transform
		components.setTranslate(position);

		// Snap to grid using intelligent LDrawPart logic
		components = components(components, gridSpacing, 0);

		// copy the new position back out of the components
		position = components.getTranslate();

		return position;

	}// end position:snappedToGrid:

	public void rotateByDegrees(float angle, Vector3f rotationVector,
			Vector3f rotationCenter) {
		Matrix4 transform = transformationMatrix();
		Vector3f displacement = rotationCenter;
		Vector3f negativeDisplacement = MatrixMath.V3Negate(rotationCenter);

		// Do the rotation around the specified centerpoint.
		transform = MatrixMath
				.Matrix4Translate(transform, negativeDisplacement); // translate

		transform.rotate((float) Math.toRadians(angle), rotationVector);
		// rotationCenter
		transform = MatrixMath.Matrix4Translate(transform, displacement); // translate
																			// back
																			// to
																			// original
																			// position

		setTransformationMatrix(transform);
		sendMessageToObservers(MessageT.MessageObservedChanged);

	}// end rotateByDegrees:centerPoint:

	// #pragma mark -
	// #pragma mark OBSERVER
	// #pragma mark -

	// ========== observableSaysGoodbyeCruelWorld:
	// ==================================
	//
	// Purpose:
	//
	// ==============================================================================
	public void observableSaysGoodbyeCruelWorld(
			ILDrawObservable doomedObservable) {
		if (cacheType == PartTypeT.PartTypeUnresolved
				|| cacheType == PartTypeT.PartTypeNotFound)
			System.out
					.println("WARNING: LDraw part is receiving a notification that its observer is dying but it thinks it should have no observer.\n");
		if (doomedObservable != cacheModel)
			System.out
					.println("WARNING: LDraw part is receiving a notification from an observer that is not its cached drawable.\n");

		unresolvePart();
	}

	// ========== statusInvalidated:who:
	// ============================================
	//
	// Purpose: This message is sent to us when a directive we are observing is
	// invalidated. We invalidate ourselves. This is what makes our
	// bbox need recalculating when a sub-model changes.
	//
	// ==============================================================================
	public void statusInvalidated(CacheFlagsT flags, ILDrawObservable observable) {
		invalCache(CacheFlagsT.CacheFlagBounds);
	}// end statusInvalidated:who:

	// ========== receiveMessage:who:
	// ===============================================
	//
	// Purpose:
	//
	// ==============================================================================
	public void receiveMessage(MessageT msg, ILDrawObservable observable) {
		switch (msg) {
		case MessageNameChanged:
		case MessageScopeChanged:
		case MessageObservedChanged:
			unresolvePart();
			break;
		}
	}

	// #pragma mark -
	// #pragma mark UTILITIES
	// #pragma mark -

	// ========== containsReferenceTo:
	// ==============================================
	//
	// Purpose: Returns if the part references a model with the given name. This
	// is used by containers to detect circular references.
	//
	// ==============================================================================
	public boolean containsReferenceTo(String name) {
		boolean isMatch = referenceName.equals(name);
		return isMatch;
	}

	// ========== partIsMissing
	// =====================================================
	//
	// Purpose: Identifies whether the part cannot be found in any known places
	// to look for it.
	//
	// ==============================================================================
	public boolean partIsMissing() {
		resolvePart();
		return cacheType == PartTypeT.PartTypeNotFound;
	}

	// #pragma mark -

	// ========== flattenIntoLines:triangles:quadrilaterals:other:currentColor:
	// =====
	//
	// Purpose: Appends the directive into the appropriate container.
	//
	// ==============================================================================
	public void flattenIntoLines(ArrayList<LDrawLine> lines,
			ArrayList<LDrawTriangle> triangles,
			ArrayList<LDrawQuadrilateral> quadriaterals,
			ArrayList<LDrawDirective> everythingElse, LDrawColor parentColor,
			Matrix4 transform, Matrix3 normalTransform, boolean recursive) {
		LDrawModel modelToDraw = null;
		LDrawModel flatCopy = null;
		Matrix4 partTransform = transformationMatrix();
		Matrix4 combinedTransform = null;

		// Nonrecursive flattenings are just trying to collect the primitives.
		// Parts
		// should be completely ignored.
		if (recursive == true) {
			super.flattenIntoLines(lines, triangles, quadriaterals,
					everythingElse, parentColor, transform, normalTransform,
					recursive);

			// Flattening involves applying the part's transform to copies of
			// all
			// referenced vertices. (We are forced to make copies because you
			// can't call
			// glMultMatrix inside a glBegin; the only way to draw all like
			// geometry at
			// once is to have a flat, transformed copy of it.)

			// Do not go through the regular part resolution scheme - it is not
			// thread safe.
			// Look up sub-model first, to avoid taking a lock on the shared
			// library catalog ONLY
			// to discover that we aren't in there.

			modelToDraw = referencedMPDSubmodel();

			if (modelToDraw == null)
				modelToDraw = PartLibrary.sharedPartLibrary()
						.modelForName_threadSafe(referenceName);
			if (modelToDraw == null)
				return;

			try {
				flatCopy = (LDrawModel) modelToDraw.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// concatenate the transform and pass it down
			combinedTransform = MatrixMath.Matrix4Multiply(partTransform,
					transform);

			// Normals are actually transformed by a different matrix.
			normalTransform = MatrixMath
					.Matrix3MakeNormalTransformFromProjMatrix(combinedTransform);

			flatCopy.flattenIntoLines(lines, triangles, quadriaterals,
					everythingElse, getLDrawColor(), combinedTransform,
					normalTransform, recursive);
			flatCopy.clear();
			flatCopy = null;

		}

	}// end flattenIntoLines:triangles:quadrilaterals:other:currentColor:

	// ========== collectPartReport:
	// ================================================
	//
	// Purpose: Collects a report on this part. If this is really an MPD
	// reference, we want to get a report on the submodel and not this
	// actual part.
	//
	// ==============================================================================
	public void collectPartReport(PartReport report) {
		resolvePart();
		if (cacheType == PartTypeT.PartTypeSubmodel
				|| cacheType == PartTypeT.PartTypePeerFile)
			cacheModel.collectPartReport(report);
		else if (cacheType == PartTypeT.PartTypeLibrary)
			report.registerPart(this);

		// There's a bug here: -referencedMPDSubmodel doesn't necessarily tell
		// you if
		// this actually *is* a submodel reference. It may actually resolve to
		// something in the part library. In this case, we would draw the
		// library
		// part, but report the submodel! I'm going to let this ride, because
		// the
		// specification explicitly says the behavior in such a case is
		// undefined.

	}// end collectPartReport:

	// ========== registerUndoActions
	// ===============================================
	//
	// Purpose: Registers the undo actions that are unique to this subclass,
	// not to any superclass.
	//
	// ==============================================================================
	public void registerUndoActions(UndoManager undoManager) {
		super.registerUndoActions(undoManager);
		// todo
		// [[undoManager prepareWithInvocationTarget:this]
		// setTransformComponents:transformComponents]);
		// [[undoManager prepareWithInvocationTarget:this]
		// setDisplayName:displayName]);
		// [[undoManager prepareWithInvocationTarget:this] optimizeOpenGL);
		//
		// [undoManager setActionName:NSLocalizedString(@"UndoAttributesPart",
		// null));

	}// end registerUndoActions:

	// ========== addedMPDModel
	// =====================================================
	//
	// Purpose: This message is sent when a model is added to a MPD file; if we
	// aren't fonud, we unresolve so that we can get a shot at
	// re-resolving to the newly added part.
	//
	// ==============================================================================
	public void addedMPDModel() {
		if (cacheType == PartTypeT.PartTypeNotFound)
			unresolvePart();
	}// end addedMPDModel

	// ========== resolvePart
	// =======================================================
	//
	// Purpose: Find the object this part references and record the way in which
	// it was found.
	//
	// ==============================================================================
	public void resolvePart() {
		if (cacheType == PartTypeT.PartTypeUnresolved) {
			LDrawModel mpdModel = referencedMPDSubmodel();

			if (mpdModel != null) {
				cacheModel = mpdModel;
				cacheDrawable = mpdModel;
				cacheType = PartTypeT.PartTypeSubmodel;
			} else {
				// Try the part library first for speed - sub-paths will thrash
				// the modelmanager.
				cacheModel = PartLibrary.sharedPartLibrary().modelForName(
						referenceName);
				if (cacheModel != null) {
					// Intentional: do not observe library parts - they are
					// immutable so
					// we don't need observations, and messing with the lib
					// parts set is expensive.
					// [cacheModel addObserver:this);

					// WE DO falseT LOOK UP THE DRAWABLE VBO HERE!!! Do that in
					// -optimizeOpenGL
					// instead.
					cacheDrawable = null;
					invalCache(CacheFlagsT.CacheFlagBounds);
					cacheType = PartTypeT.PartTypeLibrary;
				} else {
					cacheModel = ModelManager.sharedModelManager()
							.requestModel(referenceName, enclosingFile());
					if (cacheModel != null) {
						cacheType = PartTypeT.PartTypePeerFile;
						cacheDrawable = cacheModel;
						invalCache(CacheFlagsT.CacheFlagBounds);
						cacheModel.addObserver(this);
					} else {
						cacheType = PartTypeT.PartTypeNotFound;
						cacheDrawable = null;
						cacheModel = null;
						invalCache(CacheFlagsT.CacheFlagBounds);
						// If we are not found, listen to the "sub-model-added"
						// notification; ideally this would be on our enclosing
						// LDrawFile but for
						// now listen to all instances.
						// NotificationCenter.getInstance().addSubscriber(this,
						// NotificationMessageT.LDrawMPDSubModelAdded);
					}
				}
			}

			if (cacheModel != null) {
				verticesForBoundsInIndentityTransform = null;
				boundsInIndentityTransform = null;
				cacheBounds = null;
				connectivityList = null;
				connectivityMatrixItemList = null;
				collisionBoxList = null;

				invalCache(CacheFlagsT.CacheFlagBounds);
				cacheModel.addObserver(this);
			}
		}
	}

	// ========== unresolvePart
	// =====================================================
	//
	// Purpose: This method is called when something potentially breaks the link
	// between a part and the underlying model that represents it.
	// Typical events include: renaming the part (new name, new model),
	// putting the part in a new container (new container, new MPD
	// peers) or deallocing a directive tree in use by the observer
	// (since parts have weak references to their models, this can in
	// theory happen).
	//
	// ==============================================================================
	public void unresolvePart() {
		if (cacheType != PartTypeT.PartTypeUnresolved) {
			if (cacheModel != null
					&& (cacheType == PartTypeT.PartTypeSubmodel || cacheType == PartTypeT.PartTypePeerFile)) {
				// printf("Part %p telling observer/cache %p to forget us.\n",this,cacheModel);
				cacheModel.removeObserver(this);
			}

			if (cacheType == PartTypeT.PartTypeNotFound) {
				// todo
				// NotificationCenter.defaultCenter().removeObserver(this,
				// MessageT.LDrawMPDSubModelAdded, null);
			}

			cacheType = PartTypeT.PartTypeUnresolved;
			cacheDrawable = null;
			cacheModel = null;
		}
	}// end unresolvePart

	// ========== unresolvePartIfPartLibrary
	// ========================================
	//
	// Purpose: Unresolve a part only if it is a library part. There are two
	// cases: actual library parts and parts that were not found (and
	// thus maybe they should be library parts but the library that was
	// loaded was incomplete.
	//
	// This is used by unresolveLibraryParts to reload the library.
	//
	// ==============================================================================
	public void unresolvePartIfPartLibrary() {
		if (cacheType == PartTypeT.PartTypeLibrary
				|| cacheType == PartTypeT.PartTypeNotFound)
			unresolvePart();

	}// end unresolvePartIfPartLibrary

	/**
	 * @return
	 * @uml.property name="referenceName"
	 */
	public String getReferenceName() {
		return referenceName;
	}

	/**
	 * @param referenceName
	 * @uml.property name="referenceName"
	 */
	public void setReferenceName(String referenceName) {
		this.referenceName = referenceName;
	}

	/**
	 * @return
	 * @uml.property name="cacheDrawable"
	 */
	public LDrawDirective getCacheDrawable() {
		return cacheDrawable;
	}

	/**
	 * @param cacheDrawable
	 * @uml.property name="cacheDrawable"
	 */
	public void setCacheDrawable(LDrawDirective cacheDrawable) {
		this.cacheDrawable = cacheDrawable;
	}

	/**
	 * @return
	 * @uml.property name="cacheModel"
	 */
	public LDrawModel getCacheModel() {
		return cacheModel;
	}

	/**
	 * @param cacheModel
	 * @uml.property name="cacheModel"
	 */
	public void setCacheModel(LDrawModel cacheModel) {
		this.cacheModel = cacheModel;
	}

	/**
	 * @return
	 * @uml.property name="cacheType"
	 */
	public PartTypeT getCacheType() {
		return cacheType;
	}

	/**
	 * @param cacheType
	 * @uml.property name="cacheType"
	 */
	public void setCacheType(PartTypeT cacheType) {
		this.cacheType = cacheType;
	}

	/**
	 * @return
	 * @uml.property name="drawLock"
	 */
	public NSLock getDrawLock() {
		return drawLock;
	}

	/**
	 * @param drawLock
	 * @uml.property name="drawLock"
	 */
	public void setDrawLock(NSLock drawLock) {
		this.drawLock = drawLock;
	}

	public static boolean getShrinkSeams() {
		return LDrawGlobalFlag.SHRINK_SEAMS;
	}

	public static float getShrinkAmount() {
		return SHRINK_AMOUNT;
	}

	/**
	 * @return
	 * @uml.property name="displayName"
	 */
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public void receiveNotification(NotificationMessageT notificationType,
			INotificationMessage message) {
		switch (notificationType) {
		case MPDSubModelAdded:
			addedMPDModel();
			break;
		default:
			break;
		}
	}

	public void initWithPartName(String partName, Vector3f origin) {
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("1 16 0 0 0 1 0 0 0 1 0 0 0 1 " + partName);
		initWithLines(lines, new Range(0, 1));
		this.moveBy(origin, LDrawGridTypeT.Fine);
	}

	public void rotateByDegrees(float angle, Vector3f rotationVector,
			Vector3f rotationCenter, LDrawGridTypeT gridUnit) {
		angle = Math.round(angle / gridUnit.getRotationValue())
				* gridUnit.getRotationValue();
		rotateByDegrees(angle, rotationVector, rotationCenter);
	}

	public synchronized ArrayList<Connectivity> getConnectivityList() {
		getConnectivityList(true, true);
		return connectivityList;
	}

	public synchronized ArrayList<MatrixItem> getConnectivityMatrixItemList() {
		getConnectivityList(true, true);
		return connectivityMatrixItemList;
	}

	private synchronized ArrayList<Connectivity> getConnectivityList(
			boolean useConnExtractor, boolean useCache) {
		if (useCache == false) {
			isConnectivityInfoExist = true;
			if (connectivityList != null)
				connectivityList.clear();
			if (connectivityMatrixItemList != null)
				connectivityMatrixItemList = null;
			connectivityList = null;
			connectivityMatrixItemList = null;
		}
		if (useConnExtractor == false)
			if (isConnectivityInfoExist == false)
				return null;

		if (connectivityList == null) {
			if (getCacheType() == PartTypeT.PartTypeUnresolved)
				resolvePart();
			if (getCacheType() == PartTypeT.PartTypeSubmodel) {
				ConnectivityManager connectivityManager = new ConnectivityManager();
				for (LDrawStep step : getCacheModel().steps()) {
					for (LDrawDirective directive : step.subdirectives()) {
						if (directive instanceof LDrawPart) {
							LDrawPart part = (LDrawPart) directive;
							for (Connectivity conn : part.getConnectivityList()) {
								Connectivity conn_copy = null;
								try {
									if (Stud.class.isInstance(conn)) {
										conn_copy = (Stud) ((Stud) conn)
												.clone();
									} else if (Hole.class.isInstance(conn)) {
										conn_copy = (Hole) ((Hole) conn)
												.clone();
									} else {
										conn_copy = (Connectivity) conn.clone();
									}

									if (conn_copy instanceof ICustom2DField) {
										MatrixItem[][] matrix = ((ICustom2DField) conn_copy)
												.getMatrixItem();
										for (int column = 0; column < matrix.length; column++)
											for (int row = 0; row < matrix[column].length; row++)
												matrix[column][row]
														.setParent(conn_copy);
									}
									conn_copy
											.setTransformMatrix(Matrix4.multiply(
													conn.getTransformMatrix(),
													part.transformationMatrix()));
									conn_copy.setParent(this);
									connectivityManager.addConn(conn_copy);
								} catch (CloneNotSupportedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							// connectivityManager.addPart(part);
						}
					}
				}
				connectivityList = connectivityManager.getConnectivityList();
				connectivityMatrixItemList = connectivityManager
						.getConnectivityMatrixItemList();

			} else {
				connectivityList = ConnectivityLibrary.getInstance()
						.getConnectivity(displayName(), useConnExtractor, true);
				if (connectivityList != null) {
					for (Connectivity conn : connectivityList) {
						if (ICustom2DField.class.isInstance(conn)) {
							ICustom2DField custom2DField = (ICustom2DField) conn;
							MatrixItem[][] matrix = custom2DField
									.getMatrixItem();
							if (connectivityMatrixItemList == null)
								connectivityMatrixItemList = new ArrayList<MatrixItem>();
							for (int column = 0; column < matrix.length; column++)
								for (int row = 0; row < matrix[column].length; row++) {
									matrix[column][row].setParent(conn);
									matrix[column][row].setColumnIndex(column);
									matrix[column][row].setRowIndex(row);
									// if (matrix[column][row].getAltitude() ==
									// 23 || matrix[column][row].getAltitude()
									// == 29)
									// continue;
									connectivityMatrixItemList
											.add(matrix[column][row]);
								}
						}
						conn.setParent(this);
					}
				}
			}
		}
		return connectivityList;
	}

	public Matrix4 getTransformMatrixForRotateByDegrees(float angle,
			Vector3f rotationVector, Vector3f center) {
		Matrix4 transform = transformationMatrix();
		Vector3f displacement = center;
		Vector3f negativeDisplacement = MatrixMath.V3Negate(center);

		// Do the rotation around the specified centerpoint.
		transform = MatrixMath
				.Matrix4Translate(transform, negativeDisplacement); // translate

		transform.rotate((float) Math.toRadians(angle), rotationVector);
		// rotationCenter
		transform = MatrixMath.Matrix4Translate(transform, displacement); // translate
		return transform;
	}

	public Matrix4 getTransformMatrixForMoveBy(Vector3f moveVector,
			LDrawGridTypeT gridUnit, boolean useSnap) {
		if (useSnap)
			moveVector = LDrawGridTypeT.getSnappedPos(moveVector, gridUnit);

		Matrix4 transformationMatrix = transformationMatrix();

		// I NEED to modify the matrix itself here. Some parts have funky,
		// fragile
		// rotation values, and getting the components really badly botches them
		// up.
		transformationMatrix = MatrixMath.Matrix4Translate(
				transformationMatrix, moveVector);

		return transformationMatrix;
	}

	public synchronized ArrayList<CollisionBox> getCollisionBoxList() {
		return getCollisionBoxList(true);
	}

	private synchronized ArrayList<CollisionBox> getCollisionBoxList(
			boolean useCache) {
		if (useCache == false) {
			if (collisionBoxList != null)
				collisionBoxList.clear();
			collisionBoxList = null;
		}

		if (collisionBoxList == null) {
			if (getCacheType() == PartTypeT.PartTypeSubmodel) {
				collisionBoxList = new ArrayList<CollisionBox>();
				for (LDrawStep step : getCacheModel().steps()) {
					for (LDrawDirective directive : step.subdirectives()) {
						if (directive instanceof LDrawPart) {
							LDrawPart part = (LDrawPart) directive;
							for (CollisionBox cbox : part.getCollisionBoxList()) {
								try {
									CollisionBox newBox = (CollisionBox) cbox
											.clone();
									newBox.setTransformMatrix(Matrix4.multiply(
											cbox.getTransformMatrix(),
											part.transformationMatrix()));
									collisionBoxList.add(newBox);
									newBox.setParent(this);
								} catch (CloneNotSupportedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
						}
					}
				}
				// if (collisionBoxList.size() > 100)
				// collisionBoxList.clear();
			} else {
				collisionBoxList = ConnectivityLibrary.getInstance()
						.getCollisionBox(displayName(), true);
				if (collisionBoxList != null)
					for (CollisionBox cBox : collisionBoxList) {
						cBox.setParent(this);
						cBox.updateConnectivityOrientationInfo();
					}
			}
		}

		if (collisionBoxList == null)
			collisionBoxList = new ArrayList<CollisionBox>();

		return collisionBoxList;
	}

	public Matrix4 getRotationMatrix() {
		Matrix4 rotationMatrix = transformationMatrix();
		rotationMatrix.element[3][0] = rotationMatrix.element[3][1] = rotationMatrix.element[3][2] = 0;
		return rotationMatrix;
	}

	public Object clone() throws CloneNotSupportedException {
		LDrawPart part = new LDrawPart();
		part.initWithPartName(displayName, position());
		part.setDisplayName(displayName);
		part.setLDrawColor(getLDrawColor());
		part.setTransformationMatrix(transformationMatrix());

		return part;
	}

	public ArrayList<CollisionBox> getCollisionBoxList(Matrix4 transformMatrix,
			Box3 boundingBox) {
		if (getCacheType() != PartTypeT.PartTypeSubmodel) {
			if (LDrawUtilities.isIntersected(boundingBox, boundingBox3(Matrix4
					.multiply(transformationMatrix(), transformMatrix))))
				return getCollisionBoxList();
			else
				return new ArrayList<CollisionBox>();
		}

		ArrayList<CollisionBox> retList = new ArrayList<CollisionBox>();

		Matrix4 newMatrix = Matrix4.multiply(transformationMatrix(),
				transformMatrix);
		for (LDrawPart subPart : LDrawUtilities.extractLDrawPartListModel(
				getCacheModel(), false)) {
			for (CollisionBox cbox : subPart.getCollisionBoxList(newMatrix,
					boundingBox)) {
				try {
					CollisionBox newBox = (CollisionBox) cbox.clone();
					newBox.setTransformMatrix(Matrix4.multiply(
							cbox.getTransformMatrix(),
							subPart.transformationMatrix()));
					retList.add(newBox);
					newBox.setParent(this);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
		return retList;
	}
}
