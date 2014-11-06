package Window;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import Builder.BuilderConfigurationManager;
import Common.Size2;
import Common.Vector2f;

public class ProgressDlg extends Dialog {

	public static void main(String args[]) {
		new ProgressDlg(new Shell(), SWT.DIALOG_TRIM).open();
	}

	protected Object result;
	protected Shell shell;
	private ProgressBar progressbar;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public ProgressDlg(Shell parent, int style) {
		super(parent, SWT.NO_TRIM);
		setText("Progress Dialog");
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
		Vector2f pos = BuilderConfigurationManager.getInstance()
				.getWindowPosition();
		Size2 size = BuilderConfigurationManager.getInstance().getWindowSize();
		shell.setLocation((int) (pos.getX() + size.getWidth() / 2 - 100),
				(int) (pos.getY() + size.getHeight() / 2));
		new Label(shell, SWT.NONE);

		Display display = getParent().getDisplay();

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				ProgressBarManager.getInstance().remove(progressbar);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (shell.isDisposed() == false) {
					shell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {

							if (progressbar.isDisposed() == false) {
								if (BackgroundThreadManager.getInstance()
										.isAllFinish()
										&& shell.isDisposed() == false)
									shell.dispose();
							}
						}
					});
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				shell.dispose();
			}
		}).start();
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
		shell = new Shell(getParent(), SWT.DIALOG_TRIM);
		shell.setSize(450, 60);
		shell.setText(getText());
		shell.setLayout(new GridLayout(10, false));
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		shell.setLayoutData(gridData);

		Label label_Progress = new Label(shell, SWT.NONE);
		label_Progress.setText("Run...");
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		label_Progress.setLayoutData(gridData);

		progressbar = new ProgressBar(shell, SWT.SMOOTH);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 8;
		gridData.grabExcessHorizontalSpace = true;
		progressbar.setLayoutData(gridData);

		ProgressBarManager.getInstance().add(progressbar);

		// gridData = new GridData();
		// gridData.heightHint = 25;
		// gridData.widthHint = 25;
		// gridData.horizontalAlignment = GridData.FILL;
		// Button btnStop = new Button(shell, SWT.BUTTON2);
		// btnStop.addSelectionListener(new SelectionAdapter() {
		// @Override
		// public void widgetSelected(SelectionEvent arg0) {
		//
		// }
		// });
		// btnStop.setLayoutData(gridData);
		// btnStop.setImage(ResourceManager.getInstance().getImage(
		// shell.getDisplay(), "/Resource/Image/stop.png"));
	}
}
