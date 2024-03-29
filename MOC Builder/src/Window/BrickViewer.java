package Window;

import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolTip;

import com.jogamp.opengl.swt.GLCanvas;

import Builder.ConnectivityRendererForBrickViewer;
import Builder.MainCamera;
import Command.LDrawColor;
import Command.LDrawPart;
import Common.Size2;
import Common.Vector3f;
import LDraw.Files.LDrawFile;
import LDraw.Files.LDrawModel;
import LDraw.Support.LDrawDirective;
import LDraw.Support.LDrawGLCamera;
import LDraw.Support.LDrawGLCameraScroller;
import LDraw.Support.LDrawGLRenderer;
import LDraw.Support.MatrixMath;
import UndoRedo.DirectiveAction;
import UndoRedo.LDrawUndoRedoManager;

public class BrickViewer extends GLCanvas implements GLEventListener, MouseListener,
		MouseTrackListener, MouseMoveListener {
	protected LDrawGLRenderer glRenderer;
	protected ConnectivityRendererForBrickViewer metaInfoRenderer;
	protected MainCamera camera;
	ToolTip tooltip;
	LDrawFile ldrawFile;
	String fileName;

	LDrawGLCameraScroller scroller;
	boolean isDraging;

	public BrickViewer(Composite parent, ToolTip tooltip) {
		super(parent,SWT.NO_BACKGROUND,null,null);
		setSize(parent.getSize());		
		
		this.tooltip = tooltip;

		initRenderer();
		initEventListener(parent);
		newLDrawFile();
	}

	private void initEventListener(final Composite parent) {
		addDragDetectListener(new DragDetectListener() {
			
			@Override
			public void dragDetected(DragDetectEvent e) {
				if (tooltip.isVisible()) {
					tooltip.setVisible(false);
				}
				DNDTransfer.getInstance().setData(fileName);
				parent.notifyListeners(SWT.DragDetect, new Event());
			}
		});
		
		addMouseListener(this);
		addMouseMoveListener(this);
		addMouseTrackListener(this);
		addGLEventListener(this);
		
	}

	private void initRenderer() {
		camera = new MainCamera();
		glRenderer = new LDrawGLRenderer();
		glRenderer.initWithBoundsCamera(null, camera);
		scroller = new LDrawGLCameraScroller();
		glRenderer.setDelegate(null, scroller);
	}


	private void newLDrawFile() {
		ldrawFile = LDrawFile.newEditableFile();
		ldrawFile.activeModel().addStep();
		ldrawFileChanged();
	}

	public LDrawFile getWorkingLDrawFile() {
		return ldrawFile;
	}

	private void ldrawFileChanged() {
		if (glRenderer != null)
			glRenderer.setLDrawDirective(ldrawFile);
	}

	public void setDirectiveToWorkingFile(final String partName,
			final LDrawColor newColor) {
		if (partName != "") {
			fileName = partName;
			LDrawPart part = new LDrawPart();
			part.initWithPartName(partName, new Vector3f(0, 0, 0));
			part.setLDrawColor(newColor);
			setDirectiveToWorkingFile(part);
		} else {
			fileName = null;
			setDirectiveToWorkingFile(null);
		}
	}

	public void setDirectiveToWorkingFile(LDrawDirective directive) {
		newLDrawFile();
		if (directive != null) {
			if (fileName == null) {
				fileName = directive.activeModel().fileName();
			}
			ldrawFile.activeModel().steps().get(0).addDirective(directive);
			Vector3f bound = MatrixMath.V3Sub(
					directive.boundingBox3().getMax(), directive.boundingBox3()
							.getMin());

			float distance = (float) Math.sqrt(bound.x * bound.x + bound.y
					* bound.y + bound.z * bound.z);
			camera.setDefault();
			camera.setDistanceBetweenObjectToCamera(distance * 2);
		}
		display();

	}

	public LDrawDirective getDirective() {

		ArrayList<LDrawDirective> directives = ldrawFile.activeModel().steps()
				.get(0).subdirectives();
		if (directives.size() > 0) {
			return directives.get(0);
		}
		return null;
	}

	public LDrawGLCamera getCamera() {
		return camera;
	}

	public LDrawGLRenderer getGLRenderer() {
		return glRenderer;
	}

	@Override
	public void display(GLAutoDrawable glautodrawable) {
		GL2 gl2 = (GL2) glautodrawable.getGL(); // get the OpenGL graphics

		gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl2.glLoadIdentity(); // Reset The Modelview Matrix

		glRenderer.draw(gl2);
		camera.tickle();

		metaInfoRenderer.draw(gl2);
	}

	@Override
	public void dispose(GLAutoDrawable glautodrawable) {
	}

	@Override
	public void init(GLAutoDrawable glautodrawable) {
		GL2 gl2 = (GL2) glautodrawable.getGL(); // get the OpenGL graphics

		metaInfoRenderer = new ConnectivityRendererForBrickViewer(this);

		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		// gl2.glBindVertexArray(0);

		glRenderer.prepareOpenGL(gl2);

	}

	@Override
	public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width,
			int height) {

		GL2 gl = (GL2) glautodrawable.getGL(); // get the OpenGL graphics
		// context

		if (height == 0)
			height = 1; // prevent divide by zero

		// Set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);

		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GL_PROJECTION); // choose projection matrix
		gl.glLoadIdentity(); // reset projection matrix

		// Enable the model-view transform
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity(); // reset

		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);

		camera.setScreenSize(width, height);
		scroller.setDocumentSize(new Size2(width, height));

		display(glautodrawable);
	}

	public LDrawPart visiblePart() {
		if (getWorkingLDrawFile() == null)
			return null;

		for (LDrawDirective directive : ldrawFile.activeModel().visibleStep()
				.subdirectives()) {
			if (LDrawPart.class.isInstance(directive))
				return (LDrawPart) directive;
		}

		return null;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (isDraging) {
			camera.rotate(e.x, e.y);
			display();
		}
	}

	@Override
	public void mouseEnter(MouseEvent e) {
	}

	@Override
	public void mouseExit(MouseEvent e) {
		if (tooltip.isVisible()) {
			tooltip.setVisible(false);
		}
	}

	@Override
	public void mouseHover(MouseEvent e) {
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		if (fileName != null) {
			LDrawPart part = new LDrawPart();
			part.initWithPartName(fileName, new Vector3f(0, 0, 0));
			MOCBuilder.getInstance().addDirectiveToWorkingFile(part);
			DirectiveAction action = new DirectiveAction();
			action.addDirective(part);
			LDrawUndoRedoManager.getInstance().pushUndoAction(action);
			GlobalFocusManager.getInstance().forceFocusToMainView();
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		this.setFocus();
		if (e.button == 1) {
			ArrayList<LDrawDirective> directives = ldrawFile.activeModel()
					.steps().get(0).subdirectives();
			if (directives.size() > 0) {
				LDrawModel model = ((LDrawPart) directives.get(0))
						.getCacheModel();
				tooltip.setText(model.modelDescription());
				tooltip.setMessage(fileName);
				tooltip.setVisible(true);
			}
		} else if (e.button == 3) {
			isDraging = true;
			camera.startRotate(e.x, e.y);
			if (tooltip.isVisible()) {
				tooltip.setVisible(false);
			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		isDraging = false;
	}
}
