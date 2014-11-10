package Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import Builder.BrickSelectionManager;
import Command.LDrawPart;
import Command.PartTypeT;
import Grouping.GroupingManager;
import LDraw.Files.LDrawMPDModel;
import LDraw.Files.LDrawStep;
import LDraw.Support.LDrawDirective;
import LDraw.Support.PartCache;
import Notification.ILDrawSubscriber;
import Notification.INotificationMessage;
import Notification.LDrawDirectiveAdded;
import Notification.LDrawDirectiveModified;
import Notification.LDrawDirectiveRemoved;
import Notification.NotificationCenter;
import Notification.NotificationMessageT;
import Resource.ResourceManager;
import UndoRedo.ChangeDirectivesIndexAction;
import UndoRedo.ChangeDirectivesParentStepAction;
import UndoRedo.ChangeStepIndexAction;
import UndoRedo.DirectiveAction;
import UndoRedo.LDrawUndoRedoManager;

public class GroupEditorView implements ILDrawSubscriber, Runnable, Listener {
	private Tree groupTreeComponent = null;
	private TreeEditor editor;
	private MOCBuilder mocBuilder = null;
	private boolean isDraging;
	private HashMap<NotificationMessageT, Boolean> flags;
	private HashMap<NotificationMessageT, ArrayList<INotificationMessage>> messageListMap;
	private boolean isTerminate = false;
	private Lock mutexForMessageListMap;
	private Image isExistImage;
	private Image connectivityImage;
	private Image noConnectivityImage;
	private Image folderImage;

	public GroupEditorView(MOCBuilder builder) {
		mutexForMessageListMap = new ReentrantLock(true);
		this.mocBuilder = builder;

		flags = new HashMap<NotificationMessageT, Boolean>();
		messageListMap = new HashMap<NotificationMessageT, ArrayList<INotificationMessage>>();

		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawDirectiveDidChange);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawPartAdded);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawPartRemoved);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawStepRemoved);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawStepAdded);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawPartSelected);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawPartTransformed);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawFileActiveModelDidChanged);

		flags.put(NotificationMessageT.LDrawDirectiveDidChange, false);
		flags.put(NotificationMessageT.LDrawPartAdded, false);
		flags.put(NotificationMessageT.LDrawPartRemoved, false);
		flags.put(NotificationMessageT.LDrawPartSelected, false);
		flags.put(NotificationMessageT.LDrawFileActiveModelDidChanged, false);
		flags.put(NotificationMessageT.LDrawStepAdded, false);
		flags.put(NotificationMessageT.LDrawStepRemoved, false);
		flags.put(NotificationMessageT.LDrawPartTransformed, false);

		messageListMap.put(NotificationMessageT.LDrawDirectiveDidChange,
				new ArrayList<INotificationMessage>());
		messageListMap.put(NotificationMessageT.LDrawPartAdded,
				new ArrayList<INotificationMessage>());
		messageListMap.put(NotificationMessageT.LDrawPartRemoved,
				new ArrayList<INotificationMessage>());
		messageListMap.put(NotificationMessageT.LDrawPartSelected,
				new ArrayList<INotificationMessage>());
		messageListMap.put(NotificationMessageT.LDrawFileActiveModelDidChanged,
				new ArrayList<INotificationMessage>());
		messageListMap.put(NotificationMessageT.LDrawStepRemoved,
				new ArrayList<INotificationMessage>());
		messageListMap.put(NotificationMessageT.LDrawStepAdded,
				new ArrayList<INotificationMessage>());
		messageListMap.put(NotificationMessageT.LDrawPartTransformed,
				new ArrayList<INotificationMessage>());

		initTree();
		startUpdateViewThread();
	}

	public void terminate() {
		this.isTerminate = true;
	}

	private void startUpdateViewThread() {
		new Thread(this).start();
	}

	protected ArrayList<INotificationMessage> getMessageList(
			NotificationMessageT messageType) {
		ArrayList<INotificationMessage> copy = new ArrayList<INotificationMessage>();
		mutexForMessageListMap.lock();
		ArrayList<INotificationMessage> original = messageListMap
				.get(messageType);
		copy.addAll(original);
		original.clear();
		mutexForMessageListMap.unlock();

		return copy;
	}

	protected void updateStep(final LDrawStep step) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (mocBuilder != null) {
					int index = 0;
					TreeItem treeItem_Step = null;
					groupTreeComponent.setVisible(false);
					groupTreeComponent.setRedraw(false);
					String stepName;
					for (LDrawDirective directive : mocBuilder
							.getWorkingLDrawFile().activeModel()
							.subdirectives()) {
						if (LDrawStep.class.isInstance(directive)) {
							if (index < groupTreeComponent.getItemCount()) {
								treeItem_Step = groupTreeComponent
										.getItem(index);
								if (treeItem_Step.getData() != directive)
									treeItem_Step.setData(directive);
							} else {
								treeItem_Step = new TreeItem(
										groupTreeComponent, SWT.NONE);
								stepName = ((LDrawStep) directive)
										.getStepName();
								if (stepName == null || "".equals(stepName)) {
									treeItem_Step
											.setText(((LDrawStep) directive)
													.browsingDescription());
								} else {
									treeItem_Step.setText(stepName);
								}
								treeItem_Step.setData(directive);
							}
							if (mocBuilder.getCurrentStep() == directive) {
								setBold(treeItem_Step);
							}
							index++;
						}
					}
					for (; index < groupTreeComponent.getItemCount(); index++) {
						groupTreeComponent.getItem(index).dispose();
					}

					treeItem_Step = null;
					for (TreeItem item : groupTreeComponent.getItems())
						if (item.getData() == step) {
							treeItem_Step = item;
							break;
						}
					if (treeItem_Step == null) {
						groupTreeComponent.setRedraw(true);
						groupTreeComponent.setVisible(true);
						return;
					}
					if (mocBuilder.getCurrentStep() == step)
						setBold(treeItem_Step);

					treeItem_Step.removeAll();
					ArrayList<LDrawDirective> directives = step.subdirectives();
					TreeItem treeItem = null;
					for (LDrawDirective directive : directives) {
						if (LDrawPart.class.isInstance(directive) == false)
							continue;
						treeItem = new TreeItem(treeItem_Step, SWT.NONE);
						String description = PartCache.getInstance()
								.getPartName(
										((LDrawPart) directive).displayName());
						if (description == null)
							description = ((LDrawPart) directive).displayName();
						else
							description = ((LDrawPart) directive).displayName()
									+ " : " + description;
						treeItem.setText(description
								+ " "
								+ ((LDrawPart) directive).getLDrawColor()
										.getColorCode());
						treeItem.setData(directive);
						LDrawPart part = (LDrawPart) directive;
						if (part.isPartDataExist() == false)
							treeItem.setImage(isExistImage);
						else if (part.isConnectivityInfoExist()) {
							treeItem.setImage(connectivityImage);
						} else {
							treeItem.setImage(noConnectivityImage);
						}
					}
					treeItem_Step.setExpanded(true);
					groupTreeComponent.setRedraw(true);
					groupTreeComponent.setVisible(true);
				}
			}
		});
	}

	private void initTree() {
		if (mocBuilder != null) {
			new Thread(new Runnable() {
				public void run() {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (!groupTreeComponent.isDisposed()) {
								groupTreeComponent.setVisible(false);
								groupTreeComponent.removeAll();
								int index = 0;
								for (LDrawDirective directive : mocBuilder
										.getWorkingLDrawFile().activeModel()
										.subdirectives()) {
									if (LDrawStep.class.isInstance(directive)) {
										index++;
										drawStep(index, (LDrawStep) directive);
									}
								}
								setSelection();
								groupTreeComponent.setVisible(true);
							}
						}
					});
				}
			}).start();
		}
	}

	private void drawStep(int index, LDrawStep step) {
		TreeItem treeItem_Step = new TreeItem(groupTreeComponent, SWT.NONE);
		String stepName = step.getStepName();
		if (stepName == null || "".equals(stepName)) {
			treeItem_Step.setText("Step " + index);
		} else {
			treeItem_Step.setText(stepName);
		}
		treeItem_Step.setData(step);
		treeItem_Step.setImage(folderImage);

		if (mocBuilder.getCurrentStep() == step) {
			setBold(treeItem_Step);
		}

		TreeItem treeItem;
		groupTreeComponent.setRedraw(false);
		for (LDrawDirective directive : step.subdirectives()) {
			if (LDrawPart.class.isInstance(directive)) {
				LDrawPart part = (LDrawPart) directive;
				treeItem = new TreeItem(treeItem_Step, SWT.NONE);

				String description = PartCache.getInstance().getPartName(
						((LDrawPart) directive).displayName());
				if (description == null)
					description = ((LDrawPart) directive).displayName();
				else
					description = ((LDrawPart) directive).displayName() + " : "
							+ description;

				treeItem.setText(description
						+ " "
						+ ((LDrawPart) directive).getLDrawColor()
								.getColorCode());
				treeItem.setData(part);
				if (part.isPartDataExist() == false)
					treeItem.setImage(isExistImage);
				else if (part.isConnectivityInfoExist())
					treeItem.setImage(connectivityImage);
				else {
					treeItem.setImage(noConnectivityImage);
				}
			}
		}
		treeItem_Step.setExpanded(true);
		groupTreeComponent.setRedraw(true);
	}

	private void setBold(TreeItem selectedItem) {
		Display display = groupTreeComponent.getDisplay();
		if (groupTreeComponent.getFont() == null)
			return;
		FontData datas[] = groupTreeComponent.getFont().getFontData();
		for (FontData data : datas) {
			data.setStyle(SWT.NORMAL);
		}
		Font normalFont = new Font(display, datas);
		for (FontData data : datas) {
			data.setStyle(SWT.BOLD);
		}
		Font boldFont = new Font(display, datas);
		for (TreeItem item : groupTreeComponent.getItems()) {
			if (item.equals(selectedItem)) {
				item.setFont(boldFont);
			} else {
				item.setFont(normalFont);
			}
		}
	}

	@Override
	public void receiveNotification(NotificationMessageT messageType,
			INotificationMessage msg) {
		flags.put(messageType, true);
		if (msg != null) {
			mutexForMessageListMap.lock();
			messageListMap.get(messageType).add(msg);
			mutexForMessageListMap.unlock();
		}
	}

	public void generateView(final Composite parent) {
		Display display = parent.getDisplay();

		GridLayout layout = new GridLayout();
		layout.marginTop = -5;
		layout.marginLeft = -5;
		layout.marginRight = -5;
		layout.marginBottom = -5;

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Groups");

		groupTreeComponent = new Tree(composite, SWT.MULTI | SWT.BORDER);
		groupTreeComponent.setLayout(new GridLayout());
		groupTreeComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		groupTreeComponent.setMenu(createPopupMenu(parent.getShell()));

		isExistImage = ResourceManager.getInstance().getImage(display,
				"/Resource/Image/not_exist.png");
		connectivityImage = ResourceManager.getInstance().getImage(display,
				"/Resource/Image/chain.png");
		noConnectivityImage = ResourceManager.getInstance().getImage(display,
				"/Resource/Image/chain_exclamation.png");
		folderImage = ResourceManager.getInstance().getImage(display,
				"/Resource/Image/folder_brick.png");
		groupTreeComponent.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {

			}

			@Override
			public void mouseDown(MouseEvent e) {
				TreeItem item = groupTreeComponent.getItem(new Point(e.x, e.y));
				if (item == null) {
					BrickSelectionManager.getInstance().clearSelection();
					groupTreeComponent.setSelection(new TreeItem[0]);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				TreeItem[] selectedItems = groupTreeComponent.getSelection();
				if (selectedItems.length == 1) {
					Object object = selectedItems[0].getData();
					if (object instanceof LDrawStep) {
						BrickSelectionManager.getInstance().clearSelection();
						LDrawStep step = (LDrawStep) object;
						for (LDrawDirective subDirective : step.subdirectives()) {
							if (subDirective instanceof LDrawPart) {
								LDrawPart part = (LDrawPart) subDirective;
								BrickSelectionManager.getInstance()
										.addPartToSelection(part);
							}
						}
					}
					GlobalFocusManager.getInstance().forceFocusToMainView();
				}
			}
		});

		groupTreeComponent.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] selectedItems = groupTreeComponent.getSelection();
				if (selectedItems.length == 1) {
					Object object = selectedItems[0].getData();
					if (object instanceof LDrawStep) {
						setBold(selectedItems[0]);
						mocBuilder.setCurrentStep((LDrawStep) object);
						return;
					}
				}
				BrickSelectionManager.getInstance().clearSelection();
				LDrawDirective directive = null;
				for (TreeItem selectedItem : selectedItems) {
					directive = (LDrawDirective) selectedItem.getData();
					if (directive instanceof LDrawStep) {
						LDrawStep step = (LDrawStep) directive;
						for (LDrawDirective subDirective : step.subdirectives()) {
							if (subDirective instanceof LDrawPart) {
								LDrawPart part = (LDrawPart) subDirective;
								BrickSelectionManager.getInstance()
										.addPartToSelection(part);
							}
						}
					} else if (directive instanceof LDrawPart) {
						LDrawPart part = (LDrawPart) directive;
						if (BrickSelectionManager.getInstance()
								.containsInSelection(part)) {
							continue;
						}
						BrickSelectionManager.getInstance().addPartToSelection(
								part);
						// GlobalConnectivityManager.getInstance()
						// .updateMatrix(part);
					}
				}
				if (selectedItems.length == 1) {
					LDrawPart part = (LDrawPart) directive;
					mocBuilder.getBrickMovementGuideRenderer().setLDrawPart(
							part);
					MOCBuilder.getInstance().getCamera()
							.moveTo(part.position());
				}
				// GlobalConnectivityManager_0622.getInstance().updateMatrixAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		groupTreeComponent.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {

				switch (arg0.keyCode) {
				case SWT.DEL:
					deleteSelectedItems();
					break;
				case SWT.F5:
					initTree();
					break;
				case SWT.F2:
					if (groupTreeComponent.getSelectionCount() == 1)
						renameStep();
					break;
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}
		});

		groupTreeComponent.addKeyListener(new BuilderEventHandler(mocBuilder));
		setDragAndDrop();

		editor = new TreeEditor(groupTreeComponent);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
	}

	private Menu createPopupMenu(Decorations parent) {
		Menu menu = new Menu(parent, SWT.POP_UP);
		MenuItem addStepItem = new MenuItem(menu, SWT.PUSH);
		addStepItem.setText("New Group");
		addStepItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selectedItems = groupTreeComponent.getSelection();
				DirectiveAction action = new DirectiveAction();
				LDrawStep step;
				if (selectedItems.length == 1) {
					Object object = selectedItems[0].getData();
					if (object instanceof LDrawStep) {
						LDrawMPDModel model = mocBuilder.getWorkingLDrawFile()
								.activeModel();
						step = mocBuilder.addStepToWorkingFileAt(model
								.indexOfDirective((LDrawStep) object));
					} else {
						step = mocBuilder.addStepToWorkingFile();
					}
				} else {
					step = mocBuilder.addStepToWorkingFile();
				}
				action.addDirective(step);
				LDrawUndoRedoManager.getInstance().pushUndoAction(action);

				GlobalFocusManager.getInstance().forceFocusToMainView();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		final MenuItem renameItem = new MenuItem(menu, SWT.PUSH);
		renameItem.setText("Rename Group");
		renameItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				renameStep();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		new MenuItem(menu, SWT.SEPARATOR);

		final MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
		deleteItem.setText("Delete");
		deleteItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSelectedItems();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		new MenuItem(menu, SWT.SEPARATOR);

		MenuItem hideStep = new MenuItem(menu, SWT.PUSH);
		hideStep.setText("Hide");
		hideStep.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleHide();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		MenuItem showStep = new MenuItem(menu, SWT.PUSH);
		showStep.setText("Show");
		showStep.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleShow();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		MenuItem hideAllStep = new MenuItem(menu, SWT.PUSH);
		hideAllStep.setText("Hide All");
		hideAllStep.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleHideAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		MenuItem showAllStep = new MenuItem(menu, SWT.PUSH);
		showAllStep.setText("Show All");
		showAllStep.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleShowAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		new MenuItem(menu, SWT.SEPARATOR);

		final MenuItem makeGroupItem = new MenuItem(menu, SWT.PUSH);
		makeGroupItem.setText("Group Selected Parts");
		makeGroupItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				mocBuilder.makeNewStepFromSeletion();
				GlobalFocusManager.getInstance().forceFocusToMainView();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		final MenuItem seprateStep = new MenuItem(menu, SWT.PUSH);
		seprateStep.setText("Separate Into Subgroups based on Connection");
		seprateStep.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleSeparateStep();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		MenuItem mergeStep = new MenuItem(menu, SWT.PUSH);
		mergeStep.setText("Put All Into a Single Group");
		mergeStep.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleMergeStep();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		new MenuItem(menu, SWT.SEPARATOR);
		final MenuItem makeModel = new MenuItem(menu, SWT.PUSH);
		makeModel.setText("Make Selected Parts Into a Submodel");
		makeModel.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleMakeASubmodel();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		makeModel.setEnabled(false);

		final MenuItem extractModel = new MenuItem(menu, SWT.PUSH);
		extractModel.setText("Extract Parts From a Submodel");
		extractModel.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleExtractPartsFromASubmodel();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menu.addMenuListener(new MenuListener() {

			@Override
			public void menuShown(MenuEvent e) {
				int count = groupTreeComponent.getSelectionCount();
				if (count > 1) {
					seprateStep.setEnabled(true);
					deleteItem.setEnabled(true);
					makeModel.setEnabled(true);
					extractModel.setEnabled(false);
					LDrawDirective directive;
					for (TreeItem item : groupTreeComponent.getSelection()) {
						directive = (LDrawDirective) item.getData();
						if (directive instanceof LDrawStep) {
							makeGroupItem.setEnabled(false);
							return;
						}
					}
					makeGroupItem.setEnabled(true);
					renameItem.setEnabled(false);
				} else if (count == 1) {					
					makeModel.setEnabled(true);
					extractModel.setEnabled(false);
					if (groupTreeComponent.getSelection()[0].getData() instanceof LDrawPart) {
						if (((LDrawPart) groupTreeComponent.getSelection()[0]
								.getData()).getCacheType() == PartTypeT.PartTypeSubmodel)
							extractModel.setEnabled(true);
					}

					seprateStep.setEnabled(true);
					deleteItem.setEnabled(true);
					makeGroupItem.setEnabled(false);
					if (groupTreeComponent.getSelection()[0].getData() instanceof LDrawStep)
						renameItem.setEnabled(true);
					else
						renameItem.setEnabled(false);
				} else {
					makeModel.setEnabled(false);
					extractModel.setEnabled(false);
					seprateStep.setEnabled(false);
					deleteItem.setEnabled(false);
					makeGroupItem.setEnabled(false);
					renameItem.setEnabled(false);
				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});

		return menu;
	}

	protected void handleMakeASubmodel() {
		for (final TreeItem item : groupTreeComponent.getSelection()) {
			if (item.getData() instanceof LDrawStep) {
				LDrawStep step = (LDrawStep) item.getData();
				for (LDrawDirective directive : step.subdirectives()) {
					if (directive instanceof LDrawPart) {
						BrickSelectionManager.getInstance().addPartToSelection(
								(LDrawPart) directive);
					}
				}
			} else if (item.getData() instanceof LDrawPart) {
				LDrawPart part = (LDrawPart) item.getData();
				BrickSelectionManager.getInstance().addPartToSelection(part);
			}
		}
		mocBuilder.makeASubmodelFromSelection();
	}

	protected void handleMergeStep() {
		GroupingManager.getInstance().mergeAll();

	}

	protected void handleSeparateStep() {
		for (final TreeItem item : groupTreeComponent.getSelection()) {
			if (item.getData() instanceof LDrawStep) {
				LDrawStep step = (LDrawStep) item.getData();
				GroupingManager.getInstance().doGrouping(step);
			}
		}

	}

	protected void handleExtractPartsFromASubmodel() {
		if (groupTreeComponent.getSelection().length != 1)
			return;
		TreeItem item = groupTreeComponent.getSelection()[0];
		LDrawPart part = (LDrawPart) item.getData();

		mocBuilder.extractPartsFromASubmodel(part);
	}

	protected void handleShowAll() {
		for (final TreeItem item : groupTreeComponent.getItems()) {
			if (item.getData() instanceof LDrawStep) {
				LDrawStep step = (LDrawStep) item.getData();
				for (LDrawDirective directive : step.subdirectives())
					if (directive instanceof LDrawPart)
						((LDrawPart) directive).setHidden(false);
			}
		}

		NotificationCenter.getInstance().postNotification(
				NotificationMessageT.NeedReDraw);
	}

	protected void handleHideAll() {
		for (final TreeItem item : groupTreeComponent.getItems()) {
			if (item.getData() instanceof LDrawStep) {
				LDrawStep step = (LDrawStep) item.getData();
				for (LDrawDirective directive : step.subdirectives())
					if (directive instanceof LDrawPart)
						((LDrawPart) directive).setHidden(true);
			}
		}
		NotificationCenter.getInstance().postNotification(
				NotificationMessageT.NeedReDraw);
	}

	protected void handleHide() {
		for (final TreeItem item : groupTreeComponent.getSelection()) {
			if (item.getData() instanceof LDrawStep) {
				LDrawStep step = (LDrawStep) item.getData();
				for (LDrawDirective directive : step.subdirectives())
					if (directive instanceof LDrawPart)
						((LDrawPart) directive).setHidden(true);
			}else if (item.getData() instanceof LDrawPart) {
				((LDrawPart) item.getData()).setHidden(true);
			}
		}
		NotificationCenter.getInstance().postNotification(
				NotificationMessageT.NeedReDraw);
	}

	protected void handleShow() {
		for (final TreeItem item : groupTreeComponent.getSelection()) {
			if (item.getData() instanceof LDrawStep) {
				LDrawStep step = (LDrawStep) item.getData();
				for (LDrawDirective directive : step.subdirectives())
					if (directive instanceof LDrawPart)
						((LDrawPart) directive).setHidden(false);
			}else if (item.getData() instanceof LDrawPart) {
				((LDrawPart) item.getData()).setHidden(true);
			}
		}
		NotificationCenter.getInstance().postNotification(
				NotificationMessageT.NeedReDraw);
	}

	void setDragAndDrop() {
		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		int operations = DND.DROP_MOVE;
		DragSource source = new DragSource(groupTreeComponent, operations);
		source.setTransfer(types);
		source.addDragListener(new DragSourceListener() {

			@Override
			public void dragStart(DragSourceEvent event) {
				if (groupTreeComponent.getSelectionCount() == 0) {
					event.doit = false;
				} else {
					isDraging = true;
					event.image = null;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = "DRAG";
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				if (isDraging) {
					isDraging = false;
					initTree();
				}
			}
		});

		DropTarget target = new DropTarget(groupTreeComponent, operations);
		target.setTransfer(types);
		target.addDropListener(new DropTargetListener() {

			@Override
			public void dropAccept(DropTargetEvent event) {
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (event.data == null
						|| groupTreeComponent.getSelectionCount() == 0) {
					event.detail = DND.DROP_NONE;
					isDraging = false;
					return;
				}

				TreeItem[] selectedItems = groupTreeComponent.getSelection();
				Object data;
				LDrawDirective targetDirective = null;
				LDrawStep targetStep = null;
				boolean targetIsStep = false;

				if (event.item != null) {
					if (event.item.equals(selectedItems[0])) {
						event.detail = DND.DROP_NONE;
						isDraging = false;
						return;
					}
					data = event.item.getData();
					if (data instanceof LDrawStep) {
						targetStep = (LDrawStep) data;
						targetIsStep = true;
					} else if (data instanceof LDrawDirective) {
						targetDirective = (LDrawDirective) data;
						targetStep = targetDirective.enclosingStep();
					} else
						return;
				} else
					return;

				mocBuilder.setCurrentStep(targetStep);

				data = selectedItems[0].getData();
				LDrawDirective srcDirective = null;
				LDrawStep srcStep = null;

				boolean srcIsStep = false;
				boolean srcDirectiveHasSameStep = true;
				if (data instanceof LDrawStep) {
					srcStep = (LDrawStep) data;
					srcIsStep = true;
				}

				if (srcIsStep) {
					LDrawMPDModel model = mocBuilder.getWorkingLDrawFile()
							.activeModel();
					int newIndex = model.indexOfDirective(targetStep);
					int oldIndex = model.indexOfDirective(srcStep);
					mocBuilder.changeStepIndex(srcStep, newIndex);
					mocBuilder.setCurrentStep(srcStep);
					LDrawUndoRedoManager.getInstance().pushUndoAction(
							new ChangeStepIndexAction(mocBuilder, srcStep,
									oldIndex, newIndex));
					GlobalFocusManager.getInstance().forceFocusToMainView();

				} else {
					LDrawStep tempStep = null;
					for (int i = 0; i < selectedItems.length; i++) {
						srcDirective = (LDrawDirective) (selectedItems[i]
								.getData());
						tempStep = srcDirective.enclosingStep();
						if (srcStep != null && srcStep != tempStep) {
							srcDirectiveHasSameStep = false;
							break;
						}
						srcStep = tempStep;
					}

					if (srcDirectiveHasSameStep && srcStep == targetStep) {
						ChangeDirectivesIndexAction action = new ChangeDirectivesIndexAction();
						int newIndex;
						int oldIndex;
						for (int i = 0; i < selectedItems.length; i++) {
							srcDirective = (LDrawDirective) selectedItems[i]
									.getData();
							if (targetIsStep)
								newIndex = i;
							else
								newIndex = targetStep
										.indexOfDirective(targetDirective)
										+ i
										+ 1;
							oldIndex = srcStep.indexOfDirective(srcDirective);
							action.add(mocBuilder, srcStep, srcDirective,
									oldIndex, newIndex);
							mocBuilder.changeDirectiveIndex(srcStep,
									srcDirective, newIndex);
						}
						LDrawUndoRedoManager.getInstance().pushUndoAction(
								action);
						GlobalFocusManager.getInstance().forceFocusToMainView();
						BrickSelectionManager.getInstance().clearSelection(
								false);
					} else {
						ChangeDirectivesParentStepAction action = new ChangeDirectivesParentStepAction();
						int oldIndex;
						for (int i = 0; i < selectedItems.length; i++) {
							srcDirective = (LDrawDirective) selectedItems[i]
									.getData();
							srcStep = (LDrawStep) (srcDirective
									.enclosingDirective());
							oldIndex = srcStep.indexOfDirective(srcDirective);
							action.add(mocBuilder, targetStep, srcDirective,
									oldIndex);
							mocBuilder.ChangeDirectivesParentStepAction(
									srcDirective, srcStep, targetStep);
						}
						LDrawUndoRedoManager.getInstance().pushUndoAction(
								action);
						GlobalFocusManager.getInstance().forceFocusToMainView();
						BrickSelectionManager.getInstance().clearSelection(
								false);
					}
				}
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				if (isDraging) {
					event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
					if (event.item != null) {
						TreeItem item = (TreeItem) event.item;
						Object data = item.getData();
						if (groupTreeComponent.getSelection().length == 0)
							return;
						Object selectedData = groupTreeComponent.getSelection()[0]
								.getData();
						if (data.equals(selectedData)
								|| (selectedData instanceof LDrawStep && data instanceof LDrawPart)) {
							event.feedback = DND.FEEDBACK_NONE;
						} else {
							event.feedback |= DND.FEEDBACK_SELECT;
						}
					}
				} else {
					event.detail = DND.DROP_NONE;
				}
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {

			}

			@Override
			public void dragLeave(DropTargetEvent event) {

			}

			@Override
			public void dragEnter(DropTargetEvent event) {

			}
		});
	}

	private void deleteSelectedItems() {
		TreeItem[] selectedItems = groupTreeComponent.getSelection();
		DirectiveAction action = new DirectiveAction();
		LDrawDirective directive;
		for (TreeItem item : selectedItems) {
			directive = (LDrawDirective) item.getData();
			action.removeDirective(directive);
			if (directive instanceof LDrawStep) {
				mocBuilder.removeDirectiveFromWorkingFile(directive, true);
			}
		}
		LDrawUndoRedoManager.getInstance().pushUndoAction(action);
		mocBuilder.removeSelectedDirective();
		GlobalFocusManager.getInstance().forceFocusToMainView();
	}

	private void setSelection() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (!groupTreeComponent.isDisposed()) {
					ArrayList<TreeItem> list = setSelection(groupTreeComponent
							.getItems());
					final TreeItem[] items = new TreeItem[list.size()];
					list.toArray(items);
					groupTreeComponent.setSelection(items);
				}
			}
		});
	}

	private ArrayList<TreeItem> setSelection(TreeItem[] parentItems) {
		ArrayList<TreeItem> selectedItems = new ArrayList<TreeItem>();
		ArrayList<LDrawPart> selected = BrickSelectionManager.getInstance()
				.getSelectedPartList();
		LDrawDirective directive;
		for (TreeItem item : parentItems) {
			directive = (LDrawDirective) item.getData();
			if (directive == null)
				continue;
			else if (directive instanceof LDrawStep) {
				selectedItems.addAll(setSelection(item.getItems()));
			} else if (directive instanceof LDrawDirective
					&& selected.contains(directive)) {
				selectedItems.add(item);
			}
		}

		return selectedItems;
	}

	private void renameStep() {
		for (final TreeItem item : groupTreeComponent.getSelection()) {
			if (item.getData() instanceof LDrawStep) {
				final Text newEditor = new Text(groupTreeComponent, SWT.NONE);
				newEditor.setText(item.getText());
				newEditor.addListener(SWT.FocusOut, this);
				newEditor.addListener(SWT.KeyUp, this);
				newEditor.selectAll();
				newEditor.setFocus();
				editor.setEditor(newEditor, item);
			}
		}
	}

	@Override
	public void run() {
		long lastRedrawTreeTime = System.currentTimeMillis();
		HashMap<LDrawStep, Boolean> alreadyUpdated = new HashMap<LDrawStep, Boolean>();
		boolean allUpdated = false;
		while (isTerminate == false) {
			alreadyUpdated.clear();
			allUpdated = false;
			if (flags.get(NotificationMessageT.LDrawPartSelected)) {
				setSelection();
				flags.put(NotificationMessageT.LDrawPartSelected, false);
			}
			if (flags.get(NotificationMessageT.LDrawFileActiveModelDidChanged)) {
				if (allUpdated == false) {
					initTree();
					allUpdated = true;
				}
				flags.put(NotificationMessageT.LDrawFileActiveModelDidChanged,
						false);
			}
			if (System.currentTimeMillis() - lastRedrawTreeTime > 100) {
				if (flags.get(NotificationMessageT.LDrawPartAdded)) {
					if (allUpdated == false)
						for (INotificationMessage msg : getMessageList(NotificationMessageT.LDrawPartAdded)) {
							LDrawStep step = (LDrawStep) ((LDrawDirectiveAdded) msg)
									.getParent();
							if (alreadyUpdated.get(step) == null) {
								updateStep(step);
								alreadyUpdated.put(step, true);
							}
						}
					messageListMap.get(NotificationMessageT.LDrawPartAdded)
							.clear();
					flags.put(NotificationMessageT.LDrawPartAdded, false);
				}
				if (flags.get(NotificationMessageT.LDrawPartRemoved)) {
					if (allUpdated == false)
						for (INotificationMessage msg : getMessageList(NotificationMessageT.LDrawPartRemoved)) {
							LDrawStep step = (LDrawStep) ((LDrawDirectiveRemoved) msg)
									.getParent();
							if (alreadyUpdated.get(step) == null) {
								updateStep(step);
								alreadyUpdated.put(step, true);
							}
						}
					messageListMap.get(NotificationMessageT.LDrawPartRemoved)
							.clear();
					flags.put(NotificationMessageT.LDrawPartRemoved, false);
				}

				if (flags.get(NotificationMessageT.LDrawDirectiveDidChange)) {
					if (allUpdated == false)
						for (INotificationMessage msg : getMessageList(NotificationMessageT.LDrawDirectiveDidChange)) {
							LDrawStep step = (LDrawStep) ((LDrawDirectiveModified) msg)
									.getParent();
							if (alreadyUpdated.get(step) == null) {
								updateStep(step);
								alreadyUpdated.put(step, true);
							}
						}
					messageListMap.get(
							NotificationMessageT.LDrawDirectiveDidChange)
							.clear();
					flags.put(NotificationMessageT.LDrawPartRemoved, false);
				}
				if (flags.get(NotificationMessageT.LDrawStepAdded)) {
					if (allUpdated == false) {
						initTree();
						allUpdated = true;
					}
					messageListMap.get(NotificationMessageT.LDrawStepAdded)
							.clear();
					flags.put(NotificationMessageT.LDrawStepAdded, false);
				}
				if (flags.get(NotificationMessageT.LDrawStepRemoved)) {
					if (allUpdated == false) {
						initTree();
						allUpdated = true;
					}
					messageListMap.get(NotificationMessageT.LDrawStepRemoved)
							.clear();
					flags.put(NotificationMessageT.LDrawStepRemoved, false);
				}
				if (flags.get(NotificationMessageT.LDrawPartTransformed)) {
					if (allUpdated == false)
						for (INotificationMessage msg : getMessageList(NotificationMessageT.LDrawPartTransformed)) {
							LDrawStep step = (LDrawStep) ((LDrawDirectiveModified) msg)
									.getParent();
							if (alreadyUpdated.get(step) != null) {
								updateStep(step);
								alreadyUpdated.put(step, true);
							}
						}
					messageListMap.get(
							NotificationMessageT.LDrawPartTransformed).clear();
					flags.put(NotificationMessageT.LDrawPartTransformed, false);
				}
				lastRedrawTreeTime = System.currentTimeMillis();
			}

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public void handleEvent(Event event) {
		Text text = (Text) editor.getEditor();
		if (event.keyCode == 0 || event.keyCode == SWT.CR) {
			String name = text.getText();
			TreeItem item = editor.getItem();
			LDrawStep step = (LDrawStep) item.getData();
			step.setStepName(name);
			if ("".equals(name)) {
				name = "Step "
						+ (step.enclosingDirective().indexOfDirective(step) + 1);
			}
			item.setText(name);
			text.dispose();
		} else if (event.keyCode == SWT.ESC) {
			text.dispose();
		}
	}
}
