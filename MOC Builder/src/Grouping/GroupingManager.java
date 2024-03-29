package Grouping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import org.jsoup.safety.Cleaner;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import Builder.BrickGroupForTransform;
import Command.LDrawColorT;
import Command.LDrawPart;
import Common.Vector3f;
import Connectivity.Direction6T;
import Connectivity.GlobalConnectivityManager;
import LDraw.Files.LDrawStep;
import LDraw.Files.LDrawStepRotationT;
import LDraw.Support.ColorLibrary;
import LDraw.Support.LDrawDirective;
import LDraw.Support.MatrixMath;
import LDraw.Support.type.LDrawGridTypeT;
import Window.MOCBuilder;

public class GroupingManager {
	private static GroupingManager _instance = null;

	private ArrayList<LDrawPart> cachedInstruction = null;

	private GroupingManager() {
	}

	public synchronized static GroupingManager getInstance() {
		if (_instance == null)
			_instance = new GroupingManager();
		return _instance;
	}

	public void doGrouping() {
		for (LDrawStep step : MOCBuilder.getInstance().getWorkingLDrawFile()
				.activeModel().steps())
			doGrouppingByBoundingBox(step);
		MOCBuilder.getInstance().removeEmptyStep();

		preprocess(true);
		ArrayList<LDrawPart> sequenceList = getInstruction();

		for (LDrawStep step : MOCBuilder.getInstance().getWorkingLDrawFile()
				.activeModel().steps())
			doGroupingByColor(step);

		MOCBuilder.getInstance().removeEmptyStep();

		mergingSteps();

		adjustSequenceOfSteps(sequenceList);

		MOCBuilder.getInstance().removeEmptyStep();
		preprocess(true);
	}

	private void doGrouppingByBoundingBox(LDrawStep step) {
		ArrayList<LDrawPart> allParts = new ArrayList<LDrawPart>();
		if (step == null)
			return;

		if (step.stepRotationType() != LDrawStepRotationT.LDrawStepRotationNone)
			return;

		for (LDrawDirective directive : step.subdirectives())
			if (directive instanceof LDrawPart)
				allParts.add((LDrawPart) directive);

		if (allParts.size() == 0)
			return;

		GlobalConnectivityManager connectivityManager = GlobalConnectivityManager
				.getInstance();

		ArrayList<LDrawPart> newGroup = new ArrayList<LDrawPart>();
		ArrayList<LDrawPart> waitingGroup = new ArrayList<LDrawPart>();
		while (allParts.size() != 0) {
			newGroup.clear();
			waitingGroup.clear();
			waitingGroup.add(allParts.get(0));
			while (waitingGroup.size() != 0) {
				LDrawPart part = waitingGroup.remove(0);
				newGroup.add(part);
				for (LDrawPart adjacentPart : connectivityManager
						.getAdjacentPartList(part.boundingBox3())) {
					if (newGroup.contains(adjacentPart)
							|| waitingGroup.contains(adjacentPart)
							|| allParts.contains(adjacentPart) == false)
						continue;
					waitingGroup.add(adjacentPart);
				}
			}
			allParts.removeAll(newGroup);
			makeAGroup(newGroup);
		}
	}

	public void doGrouping(LDrawStep step) {
		ArrayList<LDrawPart> sequenceList = getInstruction();
		doGroupingByColor(step);
		MOCBuilder.getInstance().removeEmptyStep();

		mergingSteps();

		adjustSequenceOfSteps(sequenceList);

		MOCBuilder.getInstance().removeEmptyStep();
		preprocess(true);
	}

	private void mergingSteps() {
		preprocess(true);

		// merge two steps if there exist hole to stud connection and stud to
		// hole connection.
		for (LDrawStep step : steps) {
			for (LDrawStep otherStep : steps) {
				if (step == otherStep)
					continue;
				boolean needMerge = false;
				Boolean hasStudToHoleConnection = null;
				for (LDrawPart part : partListPerStepMap.get(step)) {
					for (ConnectionPoint point : connectionPointPerPartMap
							.get(part)) {
						LDrawPart cPart = point.getTo().getConnectivity()
								.getParent();
						if (point.isStudToHoleConnection() == null)
							continue;
						if (cPart.enclosingStep() != otherStep)
							continue;
						if (hasStudToHoleConnection == null)
							hasStudToHoleConnection = point
									.isStudToHoleConnection();
						else if (hasStudToHoleConnection != point
								.isStudToHoleConnection()) {
							needMerge = true;
							break;
						}

					}
					if (needMerge)
						break;
				}

				if (needMerge == true) {
					System.out.println(step.browsingDescription() + "->"
							+ otherStep.browsingDescription());
					for (LDrawPart part : partListPerStepMap.get(step))
						MOCBuilder.getInstance()
								.ChangeDirectivesParentStepAction(part,
										part.enclosingStep(), otherStep);
					preprocess(true);
					break;
				}
			}
		}
	}

	private void adjustSequenceOfSteps(ArrayList<LDrawPart> sequenceList) {
		ArrayList<LDrawStep> stepList = new ArrayList<LDrawStep>();
		for (LDrawPart part : sequenceList) {
			if (stepList.contains(part.enclosingStep()) == false)
				stepList.add(part.enclosingStep());
		}
		for (int i = 0; i < stepList.size(); i++)
			MOCBuilder.getInstance().changeStepIndex(stepList.get(i), i);
	}

	/*
	 * 색 기준으로 쪼개 보자.
	 */
	private void doGroupingByColor(LDrawStep step) {
		ArrayList<LDrawPart> allParts = null;
		if (step == null)
			return;

		if (step.stepRotationType() != LDrawStepRotationT.LDrawStepRotationNone)
			return;
		allParts = partListPerStepMap.get(step);

		if (allParts == null || allParts.size() == 0)
			return;

		// 1. search major colors
		final HashMap<LDrawColorT, Float> colorVolumeMap = new HashMap<LDrawColorT, Float>();
		for (LDrawPart part : allParts) {
			LDrawColorT partColor = part.getLDrawColor().getColorCode();
			Vector3f size = part.boundingBox3().getMax()
					.sub(part.boundingBox3().getMin());
			float volume = (float) Math.sqrt(size.x * size.y * size.z);
			if (colorVolumeMap.containsKey(partColor) == false)
				colorVolumeMap.put(partColor, volume);
			else
				colorVolumeMap.put(partColor, colorVolumeMap.get(partColor)
						+ volume);
		}

		ArrayList<LDrawPart> remainingParts = new ArrayList<LDrawPart>(allParts);
		Collections.sort(remainingParts,
				Collections.reverseOrder(new Comparator<LDrawPart>() {
					@Override
					public int compare(LDrawPart o1, LDrawPart o2) {
						return colorVolumeMap.get(
								o1.getLDrawColor().getColorCode()).compareTo(
								colorVolumeMap.get(o2.getLDrawColor()
										.getColorCode()));
					}
				}));

		// 2. separate bricks having similar colors to a new group.
		ArrayList<LDrawPart> newGroup = new ArrayList<LDrawPart>();
		ArrayList<LDrawPart> waitingList = new ArrayList<LDrawPart>();
		while (remainingParts.size() != 0) {
			newGroup.clear();
			waitingList.add(remainingParts.get(0));
			while (waitingList.size() != 0) {
				LDrawPart part = waitingList.remove(0);
				newGroup.add(part);

				for (LDrawPart subPart : GlobalConnectivityManager
						.getInstance().getDirectlyConnectedParts(part)) {
					if (remainingParts.contains(subPart) == false
							|| newGroup.contains(subPart)
							|| waitingList.contains(subPart))
						continue;

					if (subPart.getLDrawColor().getColorCode()
							.getDistance(part.getLDrawColor().getColorCode()) > 20000)
						continue;
					waitingList.add(subPart);
				}
			}
			// 2.1 add bricks connected to only the new group to the new group.
			if (newGroup.size() > 1) {
				ArrayList<LDrawPart> connList = new ArrayList<LDrawPart>();

				while (true) {
					connList.clear();
					connList.addAll(getConnectedPartListOnlyToTheGroup(
							newGroup, allParts));
					int t = 0;
					for (LDrawPart subPart : connList)
						if (newGroup.contains(subPart) == false) {
							newGroup.add(subPart);
							t++;
						}
					if (t == 0)
						break;

				}
				makeAGroup(newGroup);
			}
			remainingParts.removeAll(newGroup);
		}

		// 3. separates connected bricks into new group.
		doGroupingByConnection(step);
	}

	/*
	 * 가장 아래 높이부터 가장 높은 높이까지 계단 오르듯이 한단계씩 오르면서 스텝을 슬라이싱해서 보여줘보자.
	 */
	private void doGroupingFromBottomtoTop(LDrawStep step) {
		ArrayList<LDrawPart> allParts = new ArrayList<LDrawPart>();
		if (step == null)
			return;

		if (step.stepRotationType() != LDrawStepRotationT.LDrawStepRotationNone)
			return;

		for (LDrawDirective directive : step.subdirectives())
			if (directive instanceof LDrawPart)
				allParts.add((LDrawPart) directive);

		if (allParts.size() == 0)
			return;

		ArrayList<LDrawPart> remainingParts = new ArrayList<LDrawPart>(allParts);
		ArrayList<LDrawPart> newGroup = new ArrayList<LDrawPart>();
		float height = Float.MIN_VALUE;
		for (LDrawPart part : remainingParts)
			if (height < part.boundingBox3().getMax().y)
				height = part.boundingBox3().getMax().y;
		while (remainingParts.size() != 0) {
			newGroup.clear();
			for (LDrawPart part : remainingParts) {
				if (part.boundingBox3().getMin().y >= height)
					newGroup.add(part);
			}
			ArrayList<LDrawPart> tempList = new ArrayList<LDrawPart>();
			for (LDrawPart part : newGroup) {
				for (LDrawPart subPart : getConnectedPartListExceptCustom2d(
						part, remainingParts))
					if (tempList.contains(subPart) == false)
						tempList.add(subPart);
			}
			newGroup.addAll(tempList);
			makeAGroup(newGroup);
			remainingParts.removeAll(newGroup);
			height -= LDrawGridTypeT.Fine.getYValue();
		}
	}

	private void doGroupingByConnection(LDrawStep step) {
		ArrayList<LDrawPart> allParts = new ArrayList<LDrawPart>();
		if (step == null)
			return;

		if (step.stepRotationType() != LDrawStepRotationT.LDrawStepRotationNone)
			return;

		allParts = partListPerStepMap.get(step);
		if (allParts == null || allParts.size() == 0)
			return;

		ArrayList<LDrawPart> remainingParts = new ArrayList<LDrawPart>(allParts);
		ArrayList<LDrawPart> newGroup = new ArrayList<LDrawPart>();
		while (remainingParts.size() != 0) {
			newGroup.clear();
			LDrawPart part = remainingParts.get(0);
			newGroup.add(part);

			for (LDrawPart subPart : getConnectedPartList(part, remainingParts)) {
				if (remainingParts.contains(subPart) == false
						|| newGroup.contains(subPart))
					continue;
				newGroup.add(subPart);
			}

			// 3.1 add bricks connected to only the new group to the new group.
			if (newGroup.size() > 1) {
				ArrayList<LDrawPart> connList = new ArrayList<LDrawPart>();
				while (true) {
					connList.clear();
					connList.addAll(getConnectedPartListOnlyToTheGroup(
							newGroup, allParts));
					int t = 0;
					for (LDrawPart subPart : connList)
						if (newGroup.contains(subPart) == false) {
							newGroup.add(subPart);
							t++;
						}
					if (t == 0)
						break;
				}
				makeAGroup(newGroup);
			}

			remainingParts.removeAll(newGroup);
		}
	}

	private void makeAGroup(ArrayList<LDrawPart> parts) {
		LDrawStep oldStep, newStep;
		MOCBuilder builder = MOCBuilder.getInstance();
		newStep = builder.addStepToWorkingFile();
		for (LDrawPart part : parts) {
			oldStep = part.enclosingStep();
			builder.ChangeDirectivesParentStepAction(part, oldStep, newStep);
		}
	}

	private ArrayList<LDrawPart> getConnectedPartList(LDrawPart part,
			ArrayList<LDrawPart> partList) {
		ArrayList<LDrawPart> retList = new ArrayList<LDrawPart>();
		ArrayList<LDrawPart> waitingList = new ArrayList<LDrawPart>();
		ArrayList<LDrawPart> remainingParts = new ArrayList<LDrawPart>(partList);

		waitingList.add(part);
		remainingParts.remove(part);
		ArrayList<LDrawPart> connectedParts = new ArrayList<LDrawPart>();
		while (waitingList.size() != 0) {
			connectedParts.clear();
			part = waitingList.remove(0);
			retList.add(part);

			for (LDrawPart subPart : GlobalConnectivityManager.getInstance()
					.getDirectlyConnectedParts(part)) {
				if (remainingParts.contains(subPart) == false)
					continue;
				remainingParts.remove(subPart);
				if (waitingList.contains(subPart) == false)
					waitingList.add(subPart);
			}
		}
		return retList;
	}

	private ArrayList<LDrawPart> getConnectedPartListExceptCustom2d(
			LDrawPart part, ArrayList<LDrawPart> partList) {
		ArrayList<LDrawPart> retList = new ArrayList<LDrawPart>();
		ArrayList<LDrawPart> waitingList = new ArrayList<LDrawPart>();
		waitingList.add(part);
		while (waitingList.size() != 0) {
			part = waitingList.remove(0);
			retList.add(part);
			for (LDrawPart subPart : GlobalConnectivityManager.getInstance()
					.getDirectlyConnectedPartsExceptCustom2d(part)) {
				if (partList.contains(subPart) == false
						|| retList.contains(subPart)
						|| waitingList.contains(subPart))
					continue;
				waitingList.add(subPart);
			}
		}

		return retList;
	}

	private ArrayList<LDrawPart> getConnectedPartListOnlyToTheGroup(
			ArrayList<LDrawPart> newGroup, ArrayList<LDrawPart> partList) {
		ArrayList<LDrawPart> retList = new ArrayList<LDrawPart>();
		for (LDrawPart part : newGroup) {
			for (LDrawPart subPart : GlobalConnectivityManager.getInstance()
					.getDirectlyConnectedParts(part)) {
				if (partList.contains(subPart) == false
						|| retList.contains(subPart))
					continue;

				boolean isOnlyConnectedToNewGroup = true;
				for (LDrawPart subSubPart : GlobalConnectivityManager
						.getInstance().getDirectlyConnectedParts(subPart)) {
					if (newGroup.contains(subSubPart) == false)
						isOnlyConnectedToNewGroup = false;
				}
				if (isOnlyConnectedToNewGroup)
					retList.add(subPart);
			}
		}
		return retList;
	}

	private ArrayList<LDrawStep> steps;
	private HashMap<LDrawStep, ArrayList<LDrawPart>> partListPerStepMap;
	private HashMap<LDrawPart, ArrayList<ConnectionPoint>> connectionPointPerPartMap;

	public ArrayList<LDrawPart> getInstruction() {
		preprocess();
		ArrayList<LDrawPart> retList = new ArrayList<LDrawPart>();
		for (LDrawStep step : MOCBuilder.getInstance().getWorkingLDrawFile()
				.activeModel().steps())
			if (partListPerStepMap.get(step) != null)
				if (partListPerStepMap.get(step).size() > 0)
					retList.addAll(getSequenceInStep(step));
		return retList;
	}

	private ArrayList<LDrawPart> getSequenceInStep(LDrawStep step) {
		ArrayList<LDrawPart> retList = new ArrayList<LDrawPart>();
		ArrayList<LDrawPart> bottomParts = getBottomPart(step);
		ArrayList<LDrawPart> waitingList = new ArrayList<LDrawPart>();
		waitingList.addAll(bottomParts);
		retList.addAll(bottomParts);
		while (waitingList.size() != 0) {
			LDrawPart part = waitingList.remove(0);
			for (ConnectionPoint cPoint : connectionPointPerPartMap.get(part)) {
				LDrawPart cPart = cPoint.getTo().getConnectivity().getParent();
				if (cPart.enclosingStep() == step
						&& retList.contains(cPart) == false
						&& waitingList.contains(cPart) == false) {
					waitingList.add(cPart);
					retList.add(cPart);
				}
			}
		}

		for (LDrawPart part : partListPerStepMap.get(step))
			if (retList.contains(part) == false)
				retList.add(part);

		return retList;
	}

	private void preprocess() {
		preprocess(false);
	}

	private void preprocess(boolean update) {

		if (steps == null || update) {
			ArrayList<LDrawPart> partList = null;
			steps = MOCBuilder.getInstance().getWorkingLDrawFile()
					.activeModel().steps();
			partListPerStepMap = new HashMap<LDrawStep, ArrayList<LDrawPart>>();
			connectionPointPerPartMap = new HashMap<LDrawPart, ArrayList<ConnectionPoint>>();

			for (LDrawStep step : steps) {
				partList = new ArrayList<LDrawPart>();
				for (LDrawDirective directive : step.subdirectives())
					if (directive instanceof LDrawPart)
						partList.add((LDrawPart) directive);
				partListPerStepMap.put(step, partList);

				ArrayList<ConnectionPoint> connectionPoint = null;
				for (LDrawPart part : partList) {
					connectionPoint = new ArrayList<ConnectionPoint>();
					for (ConnectionPoint point : GlobalConnectivityManager
							.getInstance().getConnectionPoints(part)) {
						if (connectionPoint.contains(point))
							continue;
						connectionPoint.add(point);
					}
					connectionPointPerPartMap.put(part, connectionPoint);
				}
			}
		}
	}

	private ArrayList<LDrawPart> getBottomPart(LDrawStep step) {
		preprocess();

		ArrayList<LDrawPart> partList = null;
		if (step == null) {
			partList = MOCBuilder.getInstance().getAllPartInActiveModel();
		} else
			partList = partListPerStepMap.get(step);
		ArrayList<LDrawPart> retPartList = new ArrayList<LDrawPart>();

		LDrawPart retPart = null;
		for (LDrawPart part : partList) {
			if (isBottomPart(part, partList)) {
				if (retPart == null) {
					retPart = part;
				} else if (Math.round(part.boundingBox3().getMax()
						.sub(retPart.boundingBox3().getMax()).y) > 0) {
					retPart = part;
					retPartList.clear();
				} else if (Math.round(part.boundingBox3().getMax()
						.sub(retPart.boundingBox3().getMax()).y) == 0)
					retPartList.add(part);
			}
		}

		if (retPart == null) {
			for (LDrawPart part : partList) {
				if (retPart == null) {
					retPart = part;
				} else if (Math.round(part.boundingBox3().getMax()
						.sub(retPart.boundingBox3().getMax()).y) > 0) {
					retPart = part;
					retPartList.clear();
				} else if (Math.round(part.boundingBox3().getMax()
						.sub(retPart.boundingBox3().getMax()).y) == 0)
					retPartList.add(part);
			}
		}

		retPartList.add(retPart);
		return retPartList;
	}

	private boolean isBottomPart(LDrawPart part, ArrayList<LDrawPart> partList) {
		preprocess();
		boolean isBottomBrick = false;

		for (ConnectionPoint connectionPoint : connectionPointPerPartMap
				.get(part)) {
			if (connectionPoint.isStudToHoleConnection() != null) {
				if (connectionPoint.isStudToHoleConnection() == true
						&& connectionPoint.getFrom().getDirectionVector()
								.equals(new Vector3f(0, 1, 0)))
					isBottomBrick = true;
				else {
					if (partList.contains(connectionPoint.getTo()
							.getConnectivity().getParent())) {
						isBottomBrick = false;
						break;
					}
				}
			}
		}
		return isBottomBrick;
	}

	public void clear() {
		cachedInstruction = null;
		preprocess(true);
	}

	public void mergeAll() {
		preprocess(true);
		LDrawStep firstStep = null;
		for (LDrawStep step : MOCBuilder.getInstance().getWorkingLDrawFile()
				.activeModel().steps()) {

			if (firstStep == null) {
				firstStep = step;
				continue;
			}

			for (LDrawPart part : partListPerStepMap.get(step))
				MOCBuilder.getInstance().ChangeDirectivesParentStepAction(part,
						part.enclosingStep(), firstStep);
		}
		MOCBuilder.getInstance().removeEmptyStep();
	}
}
