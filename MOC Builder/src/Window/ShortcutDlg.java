package Window;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import Builder.ShortcutKeyManager;
import Builder.ShortcutKeyManager.ShortcutKeyT;

public class ShortcutDlg extends Dialog {

	protected Object result;
	protected Shell shell;
	private Table table;

	private HashMap<ShortcutKeyT, String> newKeyMap;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public ShortcutDlg(Shell parent, int style) {
		super(parent, style);
		setText("Shortcut Key Setting");
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(800, 600);
		shell.setText(getText());

		table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
		table.setBounds(10, 10, 774, 512);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		Button btnNewButton = new Button(shell, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ShortcutKeyManager.getInstance().reset();
				newKeyMap.clear();
				createTable();
			}
		});
		btnNewButton.setBounds(10, 528, 75, 34);
		btnNewButton.setText("Reset");

		Button btnCancel = new Button(shell, SWT.NONE);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.dispose();
			}
		});
		btnCancel.setText("Cancel/Close");
		btnCancel.setBounds(489, 528, 95, 34);

		Button btnApply = new Button(shell, SWT.NONE);
		btnApply.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				boolean hasProblem = false;
				for (Entry<ShortcutKeyT, String> entry : newKeyMap.entrySet()) {
					if (entry.getValue().equals("Duplicated!")) {
						hasProblem = true;
						break;
					}
				}
				if (hasProblem) {
					// pop up error dialog
					MessageBox msgBox = new MessageBox(shell, SWT.ICON_WARNING);
					msgBox.setMessage("Key mappings are duplicated. Please check it.");
					msgBox.open();
					return;
				}
				LinkedHashMap<ShortcutKeyT, String> keyMap = ShortcutKeyManager
						.getInstance().getKeyMap();
				for (Entry<ShortcutKeyT, String> entry : newKeyMap.entrySet()) {
					if (entry.getValue().equals("") == false) {
						keyMap.put(entry.getKey(), entry.getValue());
					}
				}
				createTable();
				ShortcutKeyManager.getInstance().writeKeyMapToFile();
			}
		});
		btnApply.setText("Apply");
		btnApply.setBounds(408, 528, 75, 34);

		newKeyMap = new HashMap<ShortcutKeyT, String>();
		createTable();
		addSelectionListener();

		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
				}
			}
		});
	}

	private void createTable() {

		table.setVisible(false);
		table.removeAll();
		String[] titles = { "Index", "Description", "Current Key",
				"New Key                " };
		for (int loopIndex = 0; loopIndex < titles.length; loopIndex++) {
			TableColumn column = new TableColumn(table, SWT.NULL);
			column.setText(titles[loopIndex]);
		}

		int index = 0;
		for (Entry<ShortcutKeyT, String> entry : ShortcutKeyManager
				.getInstance().getKeyMap().entrySet()) {
			final TableItem item = new TableItem(table, SWT.NULL);
			item.setText(0, "" + index++);
			if (entry.getKey().getDescription() == null)
				item.setText(1, "");
			else
				item.setText(1, entry.getKey().getDescription());
			item.setText(2, entry.getValue());
			if (newKeyMap.containsKey(entry.getKey()))
				item.setText(3, newKeyMap.get(entry.getKey()));
			else
				item.setText(3, "");
		}
		for (int loopIndex = 0; loopIndex < titles.length; loopIndex++) {
			table.getColumn(loopIndex).pack();
		}
		table.setVisible(true);
	}

	private void addSelectionListener() {
		table.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Point pt = new Point(event.x, event.y);
				final TableItem item = table.getItem(pt);
				if (item != null) {
					for (int col = 0; col < table.getColumnCount(); col++) {
						Rectangle rect = item.getBounds(col);
						if (rect.contains(pt)) {
							final int column = col;
							if (column == 3) {
								TableEditor editor = new TableEditor(table);
								final Text text = new Text(table, SWT.NONE);
								text.setEditable(false);
								editor.grabHorizontal = true;
								editor.setEditor(text, item, column);
								text.forceFocus();
								text.addFocusListener(new FocusListener() {

									@Override
									public void focusLost(FocusEvent arg0) {
										newKeyMap.put(ShortcutKeyT.byValue(item
												.getText(1)), text.getText());
										item.setText(3, text.getText());
										text.dispose();
									}

									@Override
									public void focusGained(FocusEvent arg0) {
										// TODO Auto-generated method stub

									}
								});
								text.addKeyListener(new KeyListener() {
									@Override
									public void keyReleased(KeyEvent arg0) {
										if (isConflict(item.getText(1),
												text.getText()))
											text.setText("Duplicated!");
										else
											table.forceFocus();
									}

									private boolean isConflict(String desc,
											String keyStr) {
										LinkedHashMap<ShortcutKeyT, String> keyMap = ShortcutKeyManager
												.getInstance().getKeyMap();
										for (Entry<ShortcutKeyT, String> entry : keyMap
												.entrySet()) {
											if (desc.equals(entry.getKey()
													.getDescription()))
												continue;

											if (newKeyMap.containsKey(entry
													.getKey())
													&& newKeyMap.get(
															entry.getKey())
															.equals("") == false
													&& newKeyMap
															.get(entry.getKey())
															.equals("Duplicated!") == false) {
												if (newKeyMap
														.get(entry.getKey())
														.toLowerCase()
														.equals(keyStr
																.toLowerCase()))
													return true;

											} else if (entry
													.getValue()
													.toLowerCase()
													.equals(keyStr
															.toLowerCase())) {
												return true;
											}
										}
										return false;
									}

									@Override
									public void keyPressed(KeyEvent arg0) {
										text.setText(KeyCodeStringUtil
												.getKeyCodeString(arg0));
									}
								});
							}
						}
					}
				}
			}
		});
	}

}
