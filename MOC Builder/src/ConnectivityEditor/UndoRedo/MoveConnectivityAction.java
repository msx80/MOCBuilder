package ConnectivityEditor.UndoRedo;

import java.util.ArrayList;
import java.util.HashMap;

import Builder.BrickSelectionManager;
import Command.LDrawPart;
import Common.Matrix4;
import Connectivity.Connectivity;
import Connectivity.GlobalConnectivityManager;
import UndoRedo.IAction;

public class MoveConnectivityAction implements IAction {
	private ArrayList<Connectivity> connList;
	private HashMap<Connectivity, Matrix4> originalTransformMap;
	private HashMap<Connectivity, Matrix4> newTransformMap;

	public MoveConnectivityAction() {
		connList = new ArrayList<Connectivity>();
		originalTransformMap = new HashMap<Connectivity, Matrix4>();
		newTransformMap = new HashMap<Connectivity, Matrix4>();
	}

	public void addMoveConnectivity(Connectivity conn, Matrix4 originalTransform,
			Matrix4 newTransform) {
		connList.add(conn);
		originalTransformMap.put(conn, originalTransform);
		newTransformMap.put(conn, newTransform);
	}

	@Override
	public void undoAction() {
//		System.out.println("undo");
		for (Connectivity conn : connList){
			conn.setTransformMatrix(originalTransformMap.get(conn));
			conn.updateConnectivityOrientationInfo();
		}

		GlobalConnectivityManager.getInstance().updateMatrixAll();
		BrickSelectionManager.getInstance()
				.updateScreenProjectionVerticesMapAll();
	}

	@Override
	public void redoAction() {
//		System.out.println("redo");
		for (Connectivity conn : connList){
			conn.setTransformMatrix(newTransformMap.get(conn));
			conn.updateConnectivityOrientationInfo();
		}
	}
}
