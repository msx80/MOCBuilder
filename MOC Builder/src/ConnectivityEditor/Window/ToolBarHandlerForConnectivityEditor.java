package ConnectivityEditor.Window;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import Command.LDrawPart;
import Common.Matrix4;
import Common.Vector3f;
import Connectivity.CollisionBox;
import Connectivity.Connectivity;
import Connectivity.IConnectivity;
import ConnectivityEditor.ConnectivityControlGuide.ConnectivityMovementGuideRenderer;
import ConnectivityEditor.UndoRedo.AddNDeleteConnAction;
import ConnectivityEditor.UndoRedo.ConnectivityEditorUndoRedoManager;
import ConnectivityEditor.UndoRedo.MoveConnectivityAction;
import Resource.ResourceManager;

public class ToolBarHandlerForConnectivityEditor extends SelectionAdapter {
	private static final String DefaultResourcePath = "/Resource/Image/toolbar/";
	ConnectivityEditor connEditor = null;
	private ConnectivityMovementGuideRenderer connectivityMovementGuideRenderer;
	private ConnectivitySelectionManager selectionManager;

	Shell shell = null;

	ToolItem item_openFile;
	ToolItem item_save;
	ToolItem item_remove;

	ToolItem item_rotateXClockwise;
	ToolItem item_rotateXCClockwise;

	ToolItem item_rotateYClockwise;
	ToolItem item_rotateYCClockwise;

	ToolItem item_rotateZClockwise;
	ToolItem item_rotateZCClockwise;

	ToolBar toolBar;

	public ToolBarHandlerForConnectivityEditor() {

	}

	public ToolBarHandlerForConnectivityEditor(ConnectivityEditor connEditor,
			Shell shell) {
		this();
		this.connEditor = connEditor;
		this.shell = shell;

		this.connectivityMovementGuideRenderer = connEditor
				.getConnMovementGuideRenderer();
		this.selectionManager = ConnectivitySelectionManager.getInstance();
	}

	public ToolBar getToolBar() {
		return toolBar;
	}

	@Override
	public void widgetSelected(SelectionEvent event) {
		ToolItem eventSrc = (ToolItem) event.getSource();

		if (eventSrc == item_openFile) {
			if (connEditor.checkChanged(shell)) {
				connEditor.openFile(null);
			}
		} else if (eventSrc == item_save) {
			connEditor.saveAs(shell, null);
		} else if (eventSrc == item_remove) {
			handleDeleteConnectivity();
		} else if (eventSrc == item_rotateXClockwise) {
			handleRotateSelectedConnectivity(new Vector3f(45, 0, 0));

		} else if (eventSrc == item_rotateXCClockwise) {
			Vector3f degree = new Vector3f(-45, 0, 0);
			handleRotateSelectedConnectivity(degree);
		} else if (eventSrc == item_rotateYClockwise) {
			Vector3f degree = new Vector3f(0, 45, 0);
			handleRotateSelectedConnectivity(degree);
		} else if (eventSrc == item_rotateYCClockwise) {
			Vector3f degree = new Vector3f(0, -45, 0);
			handleRotateSelectedConnectivity(degree);
		} else if (eventSrc == item_rotateZClockwise) {
			Vector3f degree = new Vector3f(0, 0, -45);
			handleRotateSelectedConnectivity(degree);
		} else if (eventSrc == item_rotateZCClockwise) {
			Vector3f degree = new Vector3f(0, 0, 45);
			handleRotateSelectedConnectivity(degree);
		}
	}

	private void handleDeleteConnectivity() {
		AddNDeleteConnAction action = new AddNDeleteConnAction();
		LDrawPart part = ConnectivityEditor.getInstance().getWorkingPart();
		if (part == null)
			return;
		for (Connectivity conn : selectionManager.getSelectedConnectivityList()) {
			action.removeConnectivity(conn);
			if (conn instanceof CollisionBox)
				part.getCollisionBoxList().remove(conn.getConnectivity());
			else
				part.getConnectivityList().remove(conn.getConnectivity());
		}
		handleBrickControlGuideDisplay(null);

		ConnectivityEditorUndoRedoManager.getInstance().pushUndoAction(action);

	}

	private void handleRotateSelectedConnectivity(Vector3f rotationVector) {

		if (connectivityMovementGuideRenderer.getConn() != null) {
			connectivityMovementGuideRenderer.getConn().rotateBy(
					(float) Math.toRadians(rotationVector.length()),
					rotationVector.scale(1 / rotationVector.length()));
			selectionManager
					.moveSelectedConnectivityBy(connectivityMovementGuideRenderer
							.getConn().getConnectivity());
			handleConnectivityMove();
		} else if (selectionManager.isEmpty() == false) {
			selectionManager
					.getSelectedConnectivityList()
					.get(0)
					.rotateBy((float) Math.toRadians(rotationVector.length()),
							rotationVector.scale(1 / rotationVector.length()));
			selectionManager.moveSelectedConnectivityBy(selectionManager
					.getSelectedConnectivityList().get(0));
			handleConnectivityMove();
		}
	}

	public void generateToolbar() {
		Display display = shell.getDisplay();
		toolBar = new ToolBar(shell, SWT.FLAT | SWT.RIGHT);
		toolBar.setBounds(10, 10, 728, 15);

		item_openFile = new ToolItem(toolBar, SWT.None);
		item_openFile.setImage(ResourceManager.getInstance().getImage(display,
				DefaultResourcePath + "active_open.png"));
		item_openFile.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_open.png"));
		item_openFile.addSelectionListener(this);
		item_openFile.setToolTipText("OPEN");

		item_save = new ToolItem(toolBar, SWT.None);
		item_save.setImage(ResourceManager.getInstance().getImage(display,
				DefaultResourcePath + "active_save.png"));
		item_save.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_save.png"));
		item_save.addSelectionListener(this);
		item_save.setToolTipText("SAVE");

		ToolItem separator = new ToolItem(toolBar, SWT.SEPARATOR);

		item_remove = new ToolItem(toolBar, SWT.None);
		item_remove.setImage(ResourceManager.getInstance().getImage(display,
				DefaultResourcePath + "active_delete.png"));
		item_remove.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_delete.png"));
		item_remove.addSelectionListener(this);
		item_remove.setToolTipText("Delete Selected Connectivity");

		separator = new ToolItem(toolBar, SWT.SEPARATOR);

		item_rotateXCClockwise = new ToolItem(toolBar, SWT.None);
		item_rotateXCClockwise.setImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "active_rotatex-ccw.png"));
		item_rotateXCClockwise.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_rotatex-ccw.png"));
		item_rotateXCClockwise.addSelectionListener(this);
		item_rotateXCClockwise.setToolTipText("X CCW");

		item_rotateXClockwise = new ToolItem(toolBar, SWT.None);
		item_rotateXClockwise.setImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "active_rotatex.png"));
		item_rotateXClockwise.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_rotatex.png"));
		item_rotateXClockwise.addSelectionListener(this);
		item_rotateXClockwise.setToolTipText("X CW");

		item_rotateYCClockwise = new ToolItem(toolBar, SWT.None);
		item_rotateYCClockwise.setImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "active_rotatey-ccw.png"));
		item_rotateYCClockwise.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_rotatey-ccw.png"));
		item_rotateYCClockwise.addSelectionListener(this);
		item_rotateYCClockwise.setToolTipText("Y CCW");

		item_rotateYClockwise = new ToolItem(toolBar, SWT.None);
		item_rotateYClockwise.setImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "active_rotatey.png"));
		item_rotateYClockwise.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_rotatey.png"));
		item_rotateYClockwise.addSelectionListener(this);
		item_rotateYClockwise.setToolTipText("Y CW");

		item_rotateZCClockwise = new ToolItem(toolBar, SWT.None);
		item_rotateZCClockwise.setImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "active_rotatez-ccw.png"));
		item_rotateZCClockwise.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_rotatez-ccw.png"));
		item_rotateZCClockwise.addSelectionListener(this);
		item_rotateZCClockwise.setToolTipText("Z CCW");

		item_rotateZClockwise = new ToolItem(toolBar, SWT.None);
		item_rotateZClockwise.setImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "active_rotatez.png"));
		item_rotateZClockwise.setDisabledImage(ResourceManager.getInstance().getImage(
				display, DefaultResourcePath + "inactive_rotatez.png"));
		item_rotateZClockwise.addSelectionListener(this);
		item_rotateZClockwise.setToolTipText("Z CW");

		separator = new ToolItem(toolBar, SWT.SEPARATOR);

		toolBar.pack();
	}

	public void handleConnectivityMove() {
		ConnectivitySelectionManager selectionManager = ConnectivitySelectionManager
				.getInstance();

		MoveConnectivityAction action = new MoveConnectivityAction();
		Matrix4 originalMatrix;
		ArrayList<Connectivity> connList = selectionManager
				.getSelectedConnectivityList();

		for (Connectivity conn : connList) {
			originalMatrix = selectionManager
					.getInitialMoveTransformMatrix(conn);
			action.addMoveConnectivity(conn, originalMatrix,
					conn.getTransformMatrix());
		}
		ConnectivityEditorUndoRedoManager.getInstance().pushUndoAction(action);
	}

	private void handleBrickControlGuideDisplay(IConnectivity conn) {
		connectivityMovementGuideRenderer.setConn(conn);
	}
}
