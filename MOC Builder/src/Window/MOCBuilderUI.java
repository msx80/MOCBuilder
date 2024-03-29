package Window;

import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import BrickControlGuide.BrickMovementGuideRenderer;
import Builder.AutoSaveManager;
import Builder.BrickSelectionManager;
import Builder.BuilderConfigurationManager;
import Builder.ConnectivityRendererForBrickViewer;
import Builder.DragSelectionInfoRenderer;
import Builder.MainCamera;
import Builder.MetaInfoRenderer;
import Command.LDrawPart;
import Common.Size2;
import Common.Vector3f;
import Connectivity.ConnectivityTestResultT;
import Connectivity.GlobalConnectivityManager;
import Connectivity.GlobalConnectivityRenderer;
import LDraw.Support.LDrawGLRenderer;
import LDraw.Support.MatrixMath;
import Notification.ILDrawSubscriber;
import Notification.INotificationMessage;
import Notification.NotificationCenter;
import Notification.NotificationMessageT;
import Resource.ResourceManager;
import Resource.SoundEffectManager;
import Resource.SoundEffectT;
import UndoRedo.DirectiveAction;
import UndoRedo.LDrawUndoRedoManager;

import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

public class MOCBuilderUI implements GLEventListener, ILDrawSubscriber {

	FPSAnimator animator;
	LDrawGLRenderer glRenderer;
	MetaInfoRenderer metaInfoRenderer;
	BrickMovementGuideRenderer brickMovementGuideRenderer;
	GlobalConnectivityRenderer connectivityRenderer;
	PartBrowserUI browserUI;
	GroupEditorView groupEditorView;
	// FileInfoUI fileInfoUI;
	GLCanvas glcanvas;
	MainCamera camera;
	MOCBuilder mocBuilder = null;
	DragSelectionInfoRenderer brickSelectionInfoRenderer;
	GlobalBoundingBoxRenderer boundingBoxRenderer;

	Shell shell;
	SashForm sashForm;
	Composite mainView;

	ToolBarHandler toolbar;

	public MOCBuilderUI(MOCBuilder builder) {
		this.mocBuilder = builder;
	}

	public void open(Display display) {
		shell = new Shell(display);
		mocBuilder.setShell(shell);
		BuilderConfigurationManager configurationManager = BuilderConfigurationManager
				.getInstance();
		shell.setText(MOCBuilder.APP_NAME);
		shell.setSize((int) configurationManager.getWindowSize().getWidth(),
				(int) configurationManager.getWindowSize().getHeight());
		shell.setLocation(new Point((int) configurationManager
				.getWindowPosition().getX(), (int) configurationManager
				.getWindowPosition().getY()));
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent event) {
				if (!mocBuilder.checkChanged(shell)) {
					event.doit = false;
				}
				super.shellClosed(event);
			}
		});
		shell.setImage(ResourceManager.getInstance().getImage(display,
				"/Resource/Image/bl_new_icon.png"));
		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (shell.getMaximized() || shell.getMinimized())
					return;

				BuilderConfigurationManager.getInstance().setWindowPosition(
						shell.getLocation().x, shell.getLocation().y);
				BuilderConfigurationManager.getInstance().setWindowSize(
						shell.getSize().x, shell.getSize().y);

				groupEditorView.terminate();
				BackgroundThreadManager.getInstance().terminate();
				animator.stop();
				AutoSaveManager.getInstance().terminate();
			}
		});
		camera = mocBuilder.getCamera();

		// generate window component
		generateComposite();

		// fileInfoUI = new FileInfoUI(brickBuilder);
		// fileInfoUI.open();

		shell.open();

		GlobalFocusManager.getInstance().forceFocusToMainView();
		if (AutoSaveManager.getInstance().isExistAutoSaveFile()) {
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION
					| SWT.YES | SWT.NO);
			messageBox.setText("AutoSave");
			messageBox
					.setMessage("Temperary saved file is existing. \r\n Do you want to load it?");
			if (messageBox.open() == SWT.YES) {
				AutoSaveManager.getInstance().loadAutoSavedFile();
			}
		}
		AutoSaveManager.getInstance().start();

		// brickBuilder.openFile("j:/connectivity Test.ldr");

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		BuilderConfigurationManager.getInstance().updateFile();
		browserUI.close();
		groupEditorView.terminate();
		AutoSaveManager.getInstance().terminate();
		shell.dispose();
		display.dispose();
	}

	private void generateComposite() {
		GridLayout gridlayout = new GridLayout();
		gridlayout.numColumns = 1;
		gridlayout.marginTop = 0;
		gridlayout.marginBottom = 0;
		gridlayout.marginHeight = 0;
		shell.setLayout(gridlayout);
		// Menu bar
		new MenuHandler(mocBuilder, shell).generateMenu();

		// Toolbar
		toolbar = new ToolBarHandler(mocBuilder, shell);
		toolbar.generateToolbar();

		sashForm = new SashForm(shell, SWT.HORIZONTAL);
		sashForm.setLocation(10, 40);
		sashForm.setSashWidth(3);
		sashForm.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.FILL_VERTICAL));

		browserUI = new PartBrowserUI(sashForm, SWT.NONE);

		mainView = new Composite(sashForm, SWT.BORDER);
		mainView.setLayout(new FillLayout());
		mainView.setBounds(10, 40, 728, 519);

		GLProfile glprofile = GLProfile.getDefault();
		GLCapabilities glcapabilities = new GLCapabilities(glprofile);
		glcanvas = new GLCanvas(mainView, SWT.NO_BACKGROUND, glcapabilities,
				null);
		initEventListener();
		animator = new FPSAnimator(glcanvas, 30);
		GlobalFocusManager.getInstance(mainView);

		// right panel
		Composite rightPanel = new Composite(sashForm, SWT.BORDER);
		rightPanel.setLayout(new GridLayout());
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SashForm sashForm2 = new SashForm(rightPanel, SWT.NONE);
		sashForm2.setOrientation(SWT.VERTICAL);
		sashForm2.setLayout(new GridLayout());
		sashForm2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// ModelListView
		ModelListView modelListView = new ModelListView(sashForm2, SWT.NONE);

		// GroupEditor
		groupEditorView = new GroupEditorView(mocBuilder);
		groupEditorView.generateView(sashForm2);

		sashForm2.setWeights(new int[] { 1, 4 });

		sashForm.setWeights(BuilderConfigurationManager.getInstance()
				.getSashDivision());

		sashForm.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				BuilderConfigurationManager.getInstance().setSashDivision(
						((SashForm) arg0.getSource()).getWeights());
			}
		});

		setDragTarget(mainView);

		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		StatusBar statusBar = new StatusBar(shell, SWT.BORDER, mocBuilder);
		statusBar.setLayoutData(gridData);

	}

	public void setDragTarget(Control control) {
		final TextTransfer textTransfer = TextTransfer.getInstance();
		final FileTransfer fileTransfer = FileTransfer.getInstance();
		DropTarget target = new DropTarget(control, DND.DROP_COPY
				| DND.DROP_MOVE | DND.DROP_DEFAULT);
		target.setTransfer(new Transfer[] { fileTransfer, textTransfer });
		target.addDropListener(new DropTargetListener() {
			LDrawPart part;

			@Override
			public void dropAccept(DropTargetEvent event) {
			}

			@Override
			public void drop(DropTargetEvent event) {
				mocBuilder.getMetaInfoRenderer().setShowBaseplate(
						BuilderConfigurationManager.getInstance()
								.isUseDefaultBaseplate());
				if (part == null) {
					Object object = event.data;
					if (object != null) {
						if (fileTransfer.isSupportedType(event.currentDataType)) {
							String[] filePaths = (String[]) object;
							for (String path : filePaths) {
								if (path.toLowerCase().endsWith(".ldr")
										|| path.toLowerCase().endsWith(".mpd")
										|| path.toLowerCase().endsWith(".dat")) {

									Point coordinates = mainView
											.toControl(new Point(event.x,
													event.y));
									Vector3f pos = camera.screenToWorldXZ(
											coordinates.x, coordinates.y, 0);

									mocBuilder.importFile(path, pos);
									return;
								}
								if (path.toLowerCase().endsWith(".lxf")
										|| path.toLowerCase()
												.endsWith(".lxfml")) {
									// Point coordinates = mainView
									// .toControl(new Point(event.x,
									// event.y));
									// Vector3f pos = camera.screenToWorldXZ(
									// coordinates.x, coordinates.y, 0);

									mocBuilder.openFile(path);
									return;
								}
							}
						}
					}
				} else {
					if (BuilderConfigurationManager.getInstance()
							.isUseConnectivity()
							&& GlobalConnectivityManager.getInstance()
									.isConnectable(part).getResultType() == ConnectivityTestResultT.False) {
						// nothing
						if (BuilderConfigurationManager.getInstance()
								.isTurnOffSound() == false)
							SoundEffectManager.getInstance().playSoundEffect(
									SoundEffectT.ConnectingFail);
					} else {
						mocBuilder.addDirectiveToWorkingFile(part);
						BrickSelectionManager.getInstance()
								.clearSelection(true);
						BrickSelectionManager.getInstance().addPartToSelection(
								part);

						DirectiveAction action = new DirectiveAction();
						action.addDirective(part);
						LDrawUndoRedoManager.getInstance().pushUndoAction(
								action);
						part = null;
						if (BuilderConfigurationManager.getInstance()
								.isTurnOffSound() == false)
							SoundEffectManager.getInstance().playSoundEffect(
									SoundEffectT.ConnectingSuccess);

					}
				}
				mainView.setFocus();
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				if (part != null) {
					Point coordinates = mainView.toControl(new Point(event.x,
							event.y));
					GlobalMousePosition.getInstance().setPos(coordinates.x,
							coordinates.y);

					Vector3f hitPos = new Vector3f(mocBuilder.getHittedPos(
							coordinates.x, coordinates.y, true));
					if (MatrixMath.compareFloat(hitPos.y, 0) == 0)
						hitPos = hitPos.add(getPartOffset(part, hitPos));

					mocBuilder.moveDirectiveTo(part, hitPos);
				}
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					if ((event.operations & DND.DROP_COPY) != 0) {
						event.detail = DND.DROP_COPY;
					} else {
						event.detail = DND.DROP_NONE;
					}
				}

				if (fileTransfer.isSupportedType(event.currentDataType)) {
					if (event.detail != DND.DROP_COPY) {
						event.detail = DND.DROP_NONE;
					}
				}
			}

			@Override
			public void dragLeave(DropTargetEvent event) {
				if (part != null) {
					removeBrick(part);
				}
				event.operations = DND.DROP_NONE;
				event.detail = DND.DROP_NONE;
			}

			private void removeBrick(LDrawPart part) {
				mocBuilder.removeDirectiveFromWorkingFile(part, true);
				brickMovementGuideRenderer.setLDrawPart(null);
			}

			private void addBrick(String partName, DropTargetEvent event) {
				Point coordinates = mainView.toControl(new Point(event.x,
						event.y));
				Vector3f pos = camera.screenToWorldXZ(coordinates.x,
						coordinates.y, 0);
				part = new LDrawPart();
				part.initWithPartName(partName, pos);
				part.setLDrawColor(DNDTransfer.getInstance().getColor());
				// part.getConnectivityList();
				mocBuilder.addDirectiveToWorkingFileForDragAndDrop(part);

				BrickSelectionManager.getInstance().clearSelection();
			}

			@Override
			public void dragEnter(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					if ((event.operations & DND.DROP_COPY) != 0) {
						event.detail = DND.DROP_COPY;
					} else {
						event.detail = DND.DROP_NONE;
					}
				}
				for (TransferData data : event.dataTypes) {
					if (fileTransfer.isSupportedType(data)) {
						event.currentDataType = data;
						if (event.detail != DND.DROP_COPY) {
							event.detail = DND.DROP_NONE;
						}
						return;
					}
				}
				if (textTransfer.isSupportedType(event.currentDataType)) {
					Object object = DNDTransfer.getInstance().getData();
					if (object != null) {
						String partName = object.toString();
						if (browserUI.contains(partName)) {
							addBrick(partName, event);
							BrickSelectionManager.getInstance()
									.addPartToSelection(part);
							if (part.getConnectivityMatrixItemList() == null)
								mocBuilder.getMetaInfoRenderer()
										.setShowBaseplate(false);
							return;
						} else {
							if (mocBuilder.getWorkingLDrawFile() != null) {
								if (mocBuilder.getWorkingLDrawFile()
										.modelNames().contains(partName)) {
									addBrick(partName, event);
									BrickSelectionManager.getInstance()
											.addPartToSelection(part);
									if (part.getConnectivityMatrixItemList() == null)
										mocBuilder.getMetaInfoRenderer()
												.setShowBaseplate(false);
									return;
								}
							}
						}
					}
					event.detail = DND.DROP_NONE;
				}
			}
		});
	}

	@Override
	public void dispose(GLAutoDrawable glautodrawable) {
		animator.stop();
	}

	@Override
	public void init(GLAutoDrawable glautodrawable) {
		GL2 gl2 = (GL2) glautodrawable.getGL(); // get the OpenGL graphics
		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		// gl2.glBindVertexArray(0);

		// init gl related component
		glRenderer = mocBuilder.getGLRenderer();
		metaInfoRenderer = mocBuilder.getMetaInfoRenderer();
		brickMovementGuideRenderer = mocBuilder.getBrickMovementGuideRenderer();
		connectivityRenderer = mocBuilder.getConnectivityRenderer();
		brickSelectionInfoRenderer = mocBuilder.getBrickSelectionInfoRenderer();
		boundingBoxRenderer = GlobalBoundingBoxRenderer.getInstance(camera);

		mocBuilder.initGLReleatedComponent(new Size2(800f, 600f), gl2);
		// animator.start();
	}

	ConnectivityRendererForBrickViewer testRenderer = null;

	@Override
	public void display(GLAutoDrawable glautodrawable) {
		GL2 gl2 = (GL2) glautodrawable.getGL(); // get the OpenGL graphics

		gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl2.glLoadIdentity(); // Reset The Modelview Matrix
		camera.tickle();

		try {
			metaInfoRenderer.draw(gl2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// long t = System.nanoTime();
		try {
			glRenderer.draw(gl2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println((System.nanoTime()-t));
		try {
			brickMovementGuideRenderer.draw(gl2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			connectivityRenderer.draw(gl2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			brickSelectionInfoRenderer.draw(gl2);
			boundingBoxRenderer.draw(gl2);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width,
			int height) {
		GL2 gl = (GL2) glautodrawable.getGL(); // get the OpenGL graphics
												// context

		if (height == 0)
			height = 1; // prevent divide by zero
		camera.setScreenSize(width, height);
		BrickSelectionManager.getInstance()
				.updateScreenProjectionVerticesMapAll();
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
		brickSelectionInfoRenderer.setCanvasSize(width, height);
	}

	private void initEventListener() {
		BuilderEventHandler eventHandler = new BuilderEventHandler(mocBuilder);
		glcanvas.addMouseListener(eventHandler);
		glcanvas.addMouseMoveListener(eventHandler);
		glcanvas.addMouseWheelListener(eventHandler);
		glcanvas.addMouseTrackListener(eventHandler);
		// init keyboard event handler for test
		glcanvas.addKeyListener(eventHandler);
		glcanvas.addKeyListener(toolbar);
		glcanvas.addGLEventListener(this);

		mainView.addKeyListener(eventHandler);
		mainView.addMouseWheelListener(eventHandler);

		EventHandlerForCursor cursorEventHandler = new EventHandlerForCursor(
				mainView);
		glcanvas.addKeyListener(cursorEventHandler);
		glcanvas.addMouseMoveListener(cursorEventHandler);
		mainView.addKeyListener(cursorEventHandler);
		mainView.addMouseMoveListener(cursorEventHandler);

		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.NeedReDraw);
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (shell.isDisposed() == false) {
					if (needRedraw) {
						needRedraw = false;
						glcanvas.display();
					} else
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}

			}

		}).start();
	}

	private boolean needRedraw = false;

	@Override
	public void receiveNotification(NotificationMessageT messageType,
			INotificationMessage msg) {
		needRedraw = true;
	}

	private Vector3f getPartOffset(LDrawPart part, Vector3f hitWorldPos) {
		Vector3f retValue = null;

		Vector3f veye = new Vector3f(0, 0,
				camera.getDistanceBetweenObjectToCamera());
		veye = camera.getModelViewMatrix().multiply(veye);
		veye = camera.getLookAtPos().add(veye);

		float y_hitted = part.boundingBox3(part.getRotationMatrix()).getMax().y;
		y_hitted *= -1.0f;

		double tanThetaX = veye.y / (veye.x - hitWorldPos.x);
		double tanThetaZ = veye.y / (veye.z - hitWorldPos.z);

		double offsetX = y_hitted / tanThetaX;
		double offsetZ = y_hitted / tanThetaZ;

		retValue = new Vector3f((float) offsetX, y_hitted, (float) offsetZ);

		return retValue;
	}
}
