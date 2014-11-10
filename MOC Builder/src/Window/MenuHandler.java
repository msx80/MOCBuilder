package Window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import Bricklink.SetImporterFromBricklink;
import Bricklink.ChildDialog.MappingDialog;
import Bricklink.ChildDialog.TextlInputDialog;
import Bricklink.ChildDialog.UploadBricklinkWantedListDialog;
import Builder.BrickSelectionManager;
import Builder.BuilderConfigurationManager;
import Builder.CopyNPasteManager;
import Command.LDrawPart;
import Command.PartTypeT;
import Common.Vector3f;
import ConnectivityEditor.Window.ConnectivityEditorUI;
import Exports.UpdateManagerDlg;
import Grouping.GroupingManager;
import LDraw.Files.LDrawMPDModel;
import LDraw.Support.type.LDrawGridTypeT;
import Notification.ILDrawSubscriber;
import Notification.INotificationMessage;
import Notification.NotificationCenter;
import Notification.NotificationMessageT;
import OtherTools.OverlapCheckDlg;
import OtherTools.PartReplaceDlg;
import UndoRedo.LDrawUndoRedoManager;

public class MenuHandler implements ILDrawSubscriber {

	MOCBuilder brickBuilder = null;
	Shell shell = null;

	private MenuItem file_new;
	private MenuItem file_open;
	private MenuItem file_save;
	private MenuItem file_saveAs;
	private MenuItem file_import_file;
	private MenuItem file_import_wantedList;
	private MenuItem file_import_setinventory;
	private MenuItem file_import_basePlate;
	private MenuItem file_export_file;
	private MenuItem file_export_wantedList;
	private MenuItem file_export_basePlate;
	private MenuItem file_modelInfo;
	private MenuItem file_exit;

	private MenuItem edit_undo;
	private MenuItem edit_redo;
	private MenuItem edit_cut;
	private MenuItem edit_copy;
	private MenuItem edit_paste;
	private MenuItem edit_Hide;
	private MenuItem edit_HideAll;
	private MenuItem edit_ShowAll;
	private MenuItem edit_replace;
	private MenuItem edit_defaultbaseplate;
	private MenuItem edit_setGrid;
	private MenuItem edit_setGrid_coarse;
	private MenuItem edit_setGrid_medium;
	private MenuItem edit_setGrid_fine;
	private MenuItem edit_group;
	private MenuItem edit_group_addNewGroup;
	private MenuItem edit_group_removeEmptyGroups;
	private MenuItem edit_group_putAllIntoASingleGroup;
	private MenuItem edit_submodel;
	private MenuItem edit_submodel_addNewSubmodel;
	private MenuItem edit_submodel_makeSelectedPartsIntoASubmodel;
	private MenuItem edit_submodel_extractParts;

	private MenuItem view_partBrowser;
	private MenuItem view_partBrowser_preview;
	private MenuItem view_groupEditor;
	private MenuItem view_minifigureBuilder;

	private MenuItem tools_overlapCheck;
	private MenuItem tools_connectivityEditor;
	private MenuItem tools_publishBI;
	private MenuItem tools_brickwizard;

	private MenuItem option_shortcut;
	private MenuItem option_soundEffect;
	private MenuItem option_account;
	private MenuItem option_showPartInfo;

	private MenuItem help_about;
	private MenuItem help_faq;
	private MenuItem help_update;
	private MenuItem help_home;

	public MenuHandler() {
	}

	public MenuHandler(MOCBuilder builder, Shell shell) {
		this();
		this.brickBuilder = builder;
		this.shell = shell;
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.BrickbuilderConfigurationChanged);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.LDrawPartSelected);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.UndoRedoManagerUpdated);
		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.CopyNPasteManagerUpdated);
	}

	public void generateMenu() {
		Menu menubar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menubar);

		MenuItem cascadeMenu = new MenuItem(menubar, SWT.CASCADE);
		cascadeMenu.setText("&File");

		Menu menu_file = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_file);

		file_new = new MenuItem(menu_file, SWT.NONE);
		file_new.addSelectionListener(new FileNewListener());
		file_new.setText("&New");

		file_open = new MenuItem(menu_file, SWT.NONE);
		file_open.addSelectionListener(new FileOpenListener());
		file_open.setText("&Open...");

		file_save = new MenuItem(menu_file, SWT.NONE);
		file_save.addSelectionListener(new FileSaveListener());
		file_save.setText("&Save");

		file_saveAs = new MenuItem(menu_file, SWT.NONE);
		file_saveAs.addSelectionListener(new FileSaveAsListener());
		file_saveAs.setText("Save &As...");

		MenuItem separator = new MenuItem(menu_file, SWT.SEPARATOR);

		cascadeMenu = new MenuItem(menu_file, SWT.CASCADE);
		cascadeMenu.setText("I&mport");

		Menu menu_file_import = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_file_import);

		file_import_file = new MenuItem(menu_file_import, SWT.NONE);
		file_import_file.addSelectionListener(new FileImportModelListener());
		file_import_file.setText("From &File...");

		file_import_wantedList = new MenuItem(menu_file_import, SWT.NONE);
		file_import_wantedList
				.addSelectionListener(new FileImportModelListener());
		file_import_wantedList.setText("From &Wanted List...");
		file_import_wantedList.setEnabled(false);

		file_import_setinventory = new MenuItem(menu_file_import, SWT.NONE);
		file_import_setinventory
				.addSelectionListener(new FileImportCatalogListener());
		file_import_setinventory.setText("From Set &Inventory...");
		// file_import_setinventory.setEnabled(false);

		file_import_basePlate = new MenuItem(menu_file_import, SWT.NONE);
		file_import_basePlate
				.addSelectionListener(new FileImportModelListener());
		file_import_basePlate.setText("From &Base Plate...");
		file_import_basePlate.setEnabled(false);

		cascadeMenu = new MenuItem(menu_file, SWT.CASCADE);
		cascadeMenu.setText("&Export");

		Menu menu_file_export = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_file_export);

		file_export_file = new MenuItem(menu_file_export, SWT.NONE);
		file_export_file.addSelectionListener(new FileImportModelListener());
		file_export_file.setText("To &File...");
		file_export_file.setEnabled(false);

		file_export_wantedList = new MenuItem(menu_file_export, SWT.NONE);
		file_export_wantedList
				.addSelectionListener(new FileExportModelToWantedListListener());
		file_export_wantedList.setText("To &Wanted List...");

		file_export_basePlate = new MenuItem(menu_file_export, SWT.NONE);
		file_export_basePlate
				.addSelectionListener(new FileImportModelListener());
		file_export_basePlate.setText("To &Base Plate...");
		file_export_basePlate.setEnabled(false);

		separator = new MenuItem(menu_file, SWT.SEPARATOR);

		file_modelInfo = new MenuItem(menu_file, SWT.NONE);
		file_modelInfo.addSelectionListener(new FileModelInfoListener());
		file_modelInfo.setText("Model &Info...");

		separator = new MenuItem(menu_file, SWT.SEPARATOR);

		file_exit = new MenuItem(menu_file, SWT.NONE);
		file_exit.addSelectionListener(new FileExitListener());
		file_exit.setText("E&xit");

		cascadeMenu = new MenuItem(menubar, SWT.CASCADE);
		cascadeMenu.setText("&Edit");

		Menu menu_edit = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_edit);

		edit_undo = new MenuItem(menu_edit, SWT.NONE);
		edit_undo.addSelectionListener(new EditUndoListener());
		edit_undo.setText("&Undo");
		edit_undo.setEnabled(false);

		edit_redo = new MenuItem(menu_edit, SWT.NONE);
		edit_redo.addSelectionListener(new EditRedoListener());
		edit_redo.setText("&Redo");
		edit_undo.setEnabled(false);

		separator = new MenuItem(menu_edit, SWT.SEPARATOR);

		edit_cut = new MenuItem(menu_edit, SWT.NONE);
		edit_cut.addSelectionListener(new EditCutListener());
		edit_cut.setText("Cu&t");
		edit_cut.setEnabled(false);

		edit_copy = new MenuItem(menu_edit, SWT.NONE);
		edit_copy.addSelectionListener(new EditCopyListener());
		edit_copy.setText("&Copy");
		edit_copy.setEnabled(false);

		edit_paste = new MenuItem(menu_edit, SWT.NONE);
		edit_paste.addSelectionListener(new EditPasteListener());
		edit_paste.setText("&Paste");
		edit_paste.setEnabled(false);

		separator = new MenuItem(menu_edit, SWT.SEPARATOR);
		edit_Hide = new MenuItem(menu_edit, SWT.NONE);
		edit_Hide.addSelectionListener(new EditHideListener());
		edit_Hide.setText("&Hide");
		edit_Hide.setEnabled(false);

		edit_HideAll = new MenuItem(menu_edit, SWT.NONE);
		edit_HideAll.addSelectionListener(new EditHideAllListener());
		edit_HideAll.setText("Hide&All");
		edit_HideAll.setEnabled(true);

		edit_ShowAll = new MenuItem(menu_edit, SWT.NONE);
		edit_ShowAll.addSelectionListener(new EditShowAllListener());
		edit_ShowAll.setText("&ShowAll");
		edit_ShowAll.setEnabled(true);

		separator = new MenuItem(menu_edit, SWT.SEPARATOR);

		edit_replace = new MenuItem(menu_edit, SWT.NONE);
		edit_replace.addSelectionListener(new EditReplaceListener());
		edit_replace.setText("&Find/Replace...");

		separator = new MenuItem(menu_edit, SWT.SEPARATOR);

		edit_group = new MenuItem(menu_edit, SWT.CASCADE);
		edit_group.setText("&Group");
		Menu menu_edit_group = new Menu(edit_group);
		edit_group.setMenu(menu_edit_group);

		edit_group_addNewGroup = new MenuItem(menu_edit_group, SWT.NONE);
		edit_group_addNewGroup.addSelectionListener(new EditNewGroupListener());
		edit_group_addNewGroup.setText("Add A &New Group");
		edit_group_addNewGroup.setEnabled(true);

		edit_group_removeEmptyGroups = new MenuItem(menu_edit_group, SWT.NONE);
		edit_group_removeEmptyGroups
				.addSelectionListener(new EditRemoveEmptyGroupsListener());
		edit_group_removeEmptyGroups.setText("Remove Empty Groups");
		edit_group_removeEmptyGroups.setEnabled(true);

		edit_group_putAllIntoASingleGroup = new MenuItem(menu_edit_group,
				SWT.NONE);
		edit_group_putAllIntoASingleGroup
				.addSelectionListener(new EditSingleGroupListener());
		edit_group_putAllIntoASingleGroup
				.setText("Put All Parts into A Single Group");
		edit_group_putAllIntoASingleGroup.setEnabled(false);

		separator = new MenuItem(menu_edit, SWT.SEPARATOR);
		edit_submodel = new MenuItem(menu_edit, SWT.CASCADE);
		edit_submodel.setText("Sub&model");
		Menu menu_edit_submodel = new Menu(edit_submodel);
		edit_submodel.setMenu(menu_edit_submodel);

		edit_submodel_addNewSubmodel = new MenuItem(menu_edit_submodel,
				SWT.NONE);
		edit_submodel_addNewSubmodel
				.addSelectionListener(new EditNewSubmodelListener());
		edit_submodel_addNewSubmodel.setText("Add A New Submodel");
		edit_submodel_addNewSubmodel.setEnabled(true);

		edit_submodel_makeSelectedPartsIntoASubmodel = new MenuItem(
				menu_edit_submodel, SWT.NONE);
		edit_submodel_makeSelectedPartsIntoASubmodel
				.addSelectionListener(new EditSelectedPartsIntoASubModelListener());
		edit_submodel_makeSelectedPartsIntoASubmodel
				.setText("Make Selected Parts Into A Submodel");
		edit_submodel_makeSelectedPartsIntoASubmodel.setEnabled(false);

		edit_submodel_extractParts = new MenuItem(menu_edit_submodel, SWT.NONE);
		edit_submodel_extractParts
				.addSelectionListener(new EditExtractPartsListener());
		edit_submodel_extractParts.setText("Extract Parts");
		edit_submodel_extractParts.setEnabled(false);

		separator = new MenuItem(menu_edit, SWT.SEPARATOR);

		edit_defaultbaseplate = new MenuItem(menu_edit, SWT.CHECK);
		edit_defaultbaseplate.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				BuilderConfigurationManager.getInstance()
						.setUseDefaultBaseplate(
								((MenuItem) arg0.getSource()).getSelection());
				NotificationCenter.getInstance().postNotification(
						NotificationMessageT.BrickbuilderConfigurationChanged);
				NotificationCenter.getInstance().postNotification(
						NotificationMessageT.NeedReDraw);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		edit_defaultbaseplate.setText("Enable &BasePlate");
		edit_defaultbaseplate.setSelection(BuilderConfigurationManager
				.getInstance().isUseDefaultBaseplate());

		edit_setGrid = new MenuItem(menu_edit, SWT.CASCADE);
		edit_setGrid.setText("Set &Grid");

		Menu menu_edit_gridunit = new Menu(edit_setGrid);
		edit_setGrid.setMenu(menu_edit_gridunit);

		edit_setGrid_coarse = new MenuItem(menu_edit_gridunit, SWT.RADIO);
		edit_setGrid_coarse.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				BuilderConfigurationManager.getInstance().setGridUnit(
						LDrawGridTypeT.Coarse);
				NotificationCenter.getInstance().postNotification(
						NotificationMessageT.BrickbuilderConfigurationChanged);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		edit_setGrid_coarse.setText("&Coarse");
		if (BuilderConfigurationManager.getInstance().getGridUnit() == LDrawGridTypeT.Coarse)
			edit_setGrid_coarse.setSelection(true);

		edit_setGrid_medium = new MenuItem(menu_edit_gridunit, SWT.RADIO);
		edit_setGrid_medium.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				BuilderConfigurationManager.getInstance().setGridUnit(
						LDrawGridTypeT.Medium);
				NotificationCenter.getInstance().postNotification(
						NotificationMessageT.BrickbuilderConfigurationChanged);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		edit_setGrid_medium.setText("&Medium");
		if (BuilderConfigurationManager.getInstance().getGridUnit() == LDrawGridTypeT.Medium)
			edit_setGrid_medium.setSelection(true);

		edit_setGrid_fine = new MenuItem(menu_edit_gridunit, SWT.RADIO);
		edit_setGrid_fine.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				BuilderConfigurationManager.getInstance().setGridUnit(
						LDrawGridTypeT.Fine);
				NotificationCenter.getInstance().postNotification(
						NotificationMessageT.BrickbuilderConfigurationChanged);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		edit_setGrid_fine.setText("&Fine");
		if (BuilderConfigurationManager.getInstance().getGridUnit() == LDrawGridTypeT.Fine)
			edit_setGrid_fine.setSelection(true);

		cascadeMenu = new MenuItem(menubar, SWT.CASCADE);
		cascadeMenu.setText("&View");

		Menu menu_view = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_view);

		view_partBrowser = new MenuItem(menu_view, SWT.CASCADE);
		view_partBrowser.setText("Show Part &Browser");
		view_partBrowser.setEnabled(false);

		Menu menu_view_partBrowser = new Menu(view_partBrowser);
		view_partBrowser.setMenu(menu_view_partBrowser);

		view_partBrowser_preview = new MenuItem(menu_view_partBrowser,
				SWT.CHECK);
		view_partBrowser_preview.setText("Show &Preview");
		view_partBrowser_preview.setEnabled(false);

		view_groupEditor = new MenuItem(menu_view, SWT.CHECK);
		view_groupEditor.setText("Show &Group Editor");
		view_groupEditor.setEnabled(false);

		view_minifigureBuilder = new MenuItem(menu_view, SWT.CHECK);
		view_minifigureBuilder.setText("Show &Minifigure Builder");
		view_minifigureBuilder.setEnabled(false);

		cascadeMenu = new MenuItem(menubar, SWT.CASCADE);
		cascadeMenu.setText("&Tools");

		Menu menu_tool = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_tool);

		tools_overlapCheck = new MenuItem(menu_tool, SWT.NONE);
		tools_overlapCheck.setText("&Overlap Checker");
		tools_overlapCheck
				.addSelectionListener(new ToolsOverlapCheckListener());
		tools_overlapCheck.setEnabled(true);

		tools_connectivityEditor = new MenuItem(menu_tool, SWT.NONE);
		tools_connectivityEditor.setText("&Connectivity Editor");
		tools_connectivityEditor
				.addSelectionListener(new ToolsConnectivityEditorListener());
		tools_connectivityEditor.setEnabled(true);

		tools_publishBI = new MenuItem(menu_tool, SWT.NONE);
		tools_publishBI.setText("&Publish To Building Instruction");
		tools_publishBI
				.addSelectionListener(new ToolsConnectivityEditorListener());
		tools_publishBI.setEnabled(false);

		tools_brickwizard = new MenuItem(menu_tool, SWT.NONE);
		tools_brickwizard.setText("Brick &Wizard");
		tools_brickwizard
				.addSelectionListener(new ToolsConnectivityEditorListener());
		tools_brickwizard.setEnabled(false);

		cascadeMenu = new MenuItem(menubar, SWT.CASCADE);
		cascadeMenu.setText("&Options");

		Menu menu_option = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_option);

		option_shortcut = new MenuItem(menu_option, SWT.NONE);
		option_shortcut.setText("&Keyboard Shortcuts...");
		option_shortcut.addSelectionListener(new OptionShortcutListener());
		option_shortcut.setEnabled(true);

		option_soundEffect = new MenuItem(menu_option, SWT.CHECK);
		option_soundEffect.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				BuilderConfigurationManager.getInstance().setTurnOffSound(
						!((MenuItem) arg0.getSource()).getSelection());
				NotificationCenter.getInstance().postNotification(
						NotificationMessageT.BrickbuilderConfigurationChanged);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		option_soundEffect.setText("Turn On &SoundEffect");
		option_soundEffect.setSelection(!BuilderConfigurationManager
				.getInstance().isTurnOffSound());

		option_account = new MenuItem(menu_option, SWT.NONE);
		option_account.setText("&Account...");
		option_account.setEnabled(false);

		option_showPartInfo = new MenuItem(menu_option, SWT.CHECK);
		option_showPartInfo.setText("Show Part &Info.");
		option_showPartInfo.setEnabled(false);

		// PreferencesView view = new PreferencesView();

		cascadeMenu = new MenuItem(menubar, SWT.CASCADE);
		cascadeMenu.setText("&Help");

		Menu menu_help = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_help);

		MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION
				| SWT.YES);
		messageBox.setText("About MOC Builder");
		messageBox.setMessage("MOC Builder\r\n\r\n"
				+ "Version: Public Release 1.02a\r\n"
				+ "Build id: 20141110-1400_kr");

		if (!SWT.getPlatform().equals("cocoa")) {
			help_about = new MenuItem(menu_help, SWT.NONE);
			help_about.addSelectionListener(new HelpAboutListener());
			help_about.setText("&About MOC Builder...");
			help_about.setData(messageBox);
		} else {
			CocoaUIEnhancer enhancer = new CocoaUIEnhancer(MOCBuilder.APP_NAME);
			enhancer.hookApplicationMenu(shell.getDisplay(), new Listener() {

				@Override
				public void handleEvent(Event arg0) {
					shell.dispose();
				}
			}, messageBox, null);
		}

		help_faq = new MenuItem(menu_help, SWT.NONE);
		help_faq.addSelectionListener(new HelpFaqListener());
		help_faq.setText("Frequently Asked Questions (FAQ)");

		help_home = new MenuItem(menu_help, SWT.NONE);
		help_home.addSelectionListener(new HelpReportListener());
		help_home.setText("Suggestion or Bug &report");

		help_update = new MenuItem(menu_help, SWT.NONE);
		help_update.addSelectionListener(new HelpUpdateListener());
		help_update.setText("&Update...");
		help_update.setEnabled(false);
	}

	class FileNewListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			brickBuilder.newLDrawFile();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileOpenListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			if (brickBuilder.checkChanged(shell)) {
				String path = "";
				FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
				fileDialog.setFilterExtensions(new String[] {
						"*.ldr;*.mpd;*.dat", "*.*" });
				path = fileDialog.open();
				if (path != null) {
					brickBuilder.openFile(path);
				}
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileSaveListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			if (brickBuilder.getWorkingLDrawFile().path() == null) {
				brickBuilder.saveAs(shell, null);
			} else {
				brickBuilder.saveFile();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileSaveAsListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			brickBuilder.saveAs(shell, null);
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileImportModelListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			String path = "";
			FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
			fileDialog.setFilterExtensions(new String[] { "*.ldr;*.mpd;*.dat",
					"*.*" });
			try {
				path = fileDialog.open();
				if (path != null) {
					brickBuilder.importFile(path, new Vector3f());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileExportModelToWantedListListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			try {
				new UploadBricklinkWantedListDialog(shell, SWT.DIALOG_TRIM)
						.open();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileModelInfoListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			try {
				ModelInfoDlg.getInstance(shell, SWT.DIALOG_TRIM);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileExitListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			Display display = shell.getDisplay();
			shell.dispose();
			display.dispose();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditReplaceListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			try {
				new PartReplaceDlg(shell, SWT.DIALOG_TRIM | SWT.ON_TOP).open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditUndoListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			LDrawUndoRedoManager.getInstance().undo();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditRedoListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			LDrawUndoRedoManager.getInstance().redo();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditCutListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			CopyNPasteManager.getInstance().setCutList(
					BrickSelectionManager.getInstance().getSelectedPartList());

		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditCopyListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			CopyNPasteManager.getInstance().setCopyList(
					BrickSelectionManager.getInstance().getSelectedPartList());
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditPasteListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			CopyNPasteManager.getInstance().paste(new Vector3f());
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditHideListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			for (LDrawPart part : BrickSelectionManager.getInstance()
					.getSelectedPartList())
				part.setHidden(true);
			BrickSelectionManager.getInstance().clearSelection();

			NotificationCenter.getInstance().postNotification(
					NotificationMessageT.NeedReDraw);
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditHideAllListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			for (LDrawPart part : MOCBuilder.getInstance().getAllPartInFile())
				part.setHidden(true);
			NotificationCenter.getInstance().postNotification(
					NotificationMessageT.NeedReDraw);
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditShowAllListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			for (LDrawPart part : MOCBuilder.getInstance().getAllPartInFile())
				part.setHidden(false);
			NotificationCenter.getInstance().postNotification(
					NotificationMessageT.NeedReDraw);
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditNewGroupListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			MOCBuilder.getInstance().addStepToWorkingFile();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditRemoveEmptyGroupsListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			MOCBuilder.getInstance().removeEmptyStep();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditSingleGroupListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			GroupingManager.getInstance().mergeAll();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditNewSubmodelListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			LDrawMPDModel model = MOCBuilder.getInstance().makeASubmodel();
			if (model != null)
				MOCBuilder.getInstance().changeActiveModel(model);

		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditSelectedPartsIntoASubModelListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			MOCBuilder.getInstance().makeASubmodelFromSelection();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class EditExtractPartsListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			if (BrickSelectionManager.getInstance().isEmpty())
				return;
			LDrawPart part = BrickSelectionManager.getInstance().getFirstPart();
			if (BrickSelectionManager.getInstance().isTheOnlySelectedPart(part) == false)
				return;
			if (part.getCacheType() != PartTypeT.PartTypeSubmodel)
				return;

			MOCBuilder.getInstance().extractPartsFromASubmodel(part);
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class ToolsOverlapCheckListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			try {
				OverlapCheckDlg dlg = new OverlapCheckDlg(shell,
						SWT.DIALOG_TRIM);
				dlg.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class ToolsConnectivityEditorListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			if (BrickSelectionManager.getInstance().getNumOfSelectedParts() == 1) {
				LDrawPart part = BrickSelectionManager.getInstance()
						.getSelectedPartList().get(0);
				try {
					ConnectivityEditorUI.getInstance(part.getDisplayName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				ConnectivityEditorUI.getInstance(null);
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class FileImportCatalogListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			try {
				TextlInputDialog urlDialog = new TextlInputDialog(shell,
						SWT.DIALOG_TRIM);
				String setNo = (String) (urlDialog.open());
				if (setNo != null) {
					boolean isSuccess = SetImporterFromBricklink.getInstance()
							.getPartNoListFrom(setNo);
					if (isSuccess == false) {
						MessageBox msg = new MessageBox(shell);
						msg.setMessage("Fail to import! Please check the "
								+ setNo + " is valid.");
						msg.open();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class BricklinkMappingListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			try {
				MappingDialog dialog = new MappingDialog(shell, SWT.DIALOG_TRIM);
				dialog.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class HelpAboutListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			MessageBox messageBox = (MessageBox) ((MenuItem) event.getSource())
					.getData();
			messageBox.open();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class HelpFaqListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			Program.launch("https://github.com/MOCBuilderOrg/MOCBuilder/wiki/FAQs");
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class HelpReportListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			Program.launch("https://github.com/MOCBuilderOrg/MOCBuilder/issues");
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	class HelpUpdateListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			new UpdateManagerDlg(shell, SWT.DIALOG_TRIM).open();
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}

	@Override
	public void receiveNotification(NotificationMessageT messageType,
			INotificationMessage msg) {
		if (messageType == NotificationMessageT.BrickbuilderConfigurationChanged) {
			BuilderConfigurationManager configuration = BuilderConfigurationManager
					.getInstance();
			edit_defaultbaseplate.setSelection(configuration
					.isUseDefaultBaseplate());

			option_soundEffect.setSelection(!configuration.isTurnOffSound());
			switch (configuration.getGridUnit()) {
			case Coarse:
				edit_setGrid_coarse.setSelection(true);
				edit_setGrid_medium.setSelection(false);
				edit_setGrid_fine.setSelection(false);
				break;
			case Medium:
				edit_setGrid_coarse.setSelection(false);
				edit_setGrid_medium.setSelection(true);
				edit_setGrid_fine.setSelection(false);
				break;
			case Fine:
				edit_setGrid_coarse.setSelection(false);
				edit_setGrid_medium.setSelection(false);
				edit_setGrid_fine.setSelection(true);
				break;
			default:
				break;
			}

		} else if (messageType == NotificationMessageT.LDrawPartSelected) {
			shell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					// tools_connectivityEditor.setEnabled(!BrickSelectionManager
					// .getInstance().isEmpty());
					edit_cut.setEnabled(!BrickSelectionManager.getInstance()
							.isEmpty());
					edit_copy.setEnabled(!BrickSelectionManager.getInstance()
							.isEmpty());
					edit_Hide.setEnabled(!BrickSelectionManager.getInstance()
							.isEmpty());

					edit_group_putAllIntoASingleGroup
							.setEnabled(!BrickSelectionManager.getInstance()
									.isEmpty());
					edit_submodel_extractParts
							.setEnabled(!BrickSelectionManager.getInstance()
									.isEmpty());
					edit_submodel_makeSelectedPartsIntoASubmodel
							.setEnabled(!BrickSelectionManager.getInstance()
									.isEmpty());
				}
			});
		} else if (messageType == NotificationMessageT.UndoRedoManagerUpdated) {
			shell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					edit_undo.setEnabled(!LDrawUndoRedoManager.getInstance()
							.isEmptyUndoStack());
					edit_redo.setEnabled(!LDrawUndoRedoManager.getInstance()
							.isEmptyRedoStack());
				}
			});

		} else if (messageType == NotificationMessageT.CopyNPasteManagerUpdated) {
			shell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					edit_paste.setEnabled(!CopyNPasteManager.getInstance()

					.isEmptyClipboard());
				}
			});
		}
	}

	class OptionShortcutListener implements SelectionListener {
		public void widgetSelected(SelectionEvent event) {
			try {
				ShortcutDlg dlg = new ShortcutDlg(shell, SWT.DIALOG_TRIM);

				dlg.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void widgetDefaultSelected(SelectionEvent event) {
		}
	}
}
