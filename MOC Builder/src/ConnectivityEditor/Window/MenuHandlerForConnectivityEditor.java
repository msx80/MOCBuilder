package ConnectivityEditor.Window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import Builder.BuilderConfigurationManager;
import LDraw.Support.LDrawPaths;
import Window.CocoaUIEnhancer;
import Window.PreferencesView;

public class MenuHandlerForConnectivityEditor extends SelectionAdapter {

	ConnectivityEditor connEditor = null;
	Shell shell = null;

	public MenuHandlerForConnectivityEditor() {
	}

	public MenuHandlerForConnectivityEditor(ConnectivityEditor builder,
			Shell shell) {
		this();
		this.connEditor = builder;
		this.shell = shell;
	}

	@Override
	public void widgetSelected(SelectionEvent event) {
		String menuText = ((MenuItem) event.getSource()).getText();

		if (menuText.equals("Exit"))
			shell.dispose();
		else if (menuText.equals("New")) {
			connEditor.newLDrawFile();
		} else if (menuText.equals("Open...")) {
			if (connEditor.checkChanged(shell)) {

				connEditor.openFile(null);

			}
		} else if (menuText.equals("Save")) {
			connEditor.saveAs(shell, null);
		} else if (menuText.equals("Save As...")) {
			connEditor.saveAs(shell, null);
		} else if (menuText.equals("About BrickBuilder...")) {
			// pop up an about page.
			MessageBox messageBox = (MessageBox) ((MenuItem) event.getSource())
					.getData();
			messageBox.open();
		} else if (menuText.equals("Suggestion or Bug report")) {
			Program.launch("https://drive.google.com/#folders/0B5hp4f0ytGSfdkxzN2ZXT0VNYXM");
		} else if (menuText.equals("Undo")) {
			// LDrawUndoRedoManager.getInstance().undo();
		} else if (menuText.equals("Redo")) {
			// LDrawUndoRedoManager.getInstance().redo();
		} else if (menuText.equals("Preferences")) {
			PreferencesView preferencesView = (PreferencesView) ((MenuItem) event
					.getSource()).getData();
			preferencesView.showDialog(shell.getDisplay());
		}
	}

	public void generateMenu() {
		Menu menubar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menubar);

		MenuItem cascadeMenu = new MenuItem(menubar, SWT.CASCADE);
		cascadeMenu.setText("File");

		Menu menu_file = new Menu(cascadeMenu);
		cascadeMenu.setMenu(menu_file);

		MenuItem subMenu = new MenuItem(menu_file, SWT.NONE);
		subMenu.addSelectionListener(this);
		subMenu.setText("Open...");

		subMenu = new MenuItem(menu_file, SWT.NONE);
		subMenu.addSelectionListener(this);
		subMenu.setText("Save As...");

		subMenu = new MenuItem(menu_file, SWT.SEPARATOR);
		subMenu.setText("");

		subMenu = new MenuItem(menu_file, SWT.NONE);
		subMenu.addSelectionListener(this);
		subMenu.setText("Exit");
	}
}
