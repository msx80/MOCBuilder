package Builder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import Common.Box2;
import Common.Size2;
import Common.Vector2f;
import LDraw.Support.LDrawPaths;
import LDraw.Support.LDrawUtilities;
import LDraw.Support.type.LDrawGridTypeT;
import Notification.NotificationCenter;
import Notification.NotificationMessageT;

public class BuilderConfigurationManager {
	private static BuilderConfigurationManager _instance = null;

	private String configurationFilename = getDefaultDataDirectoryPath()
			+ "MOCBuilder.ini";

	private static String DefaultDataDirectoryPath = null;

	private String ldrawDirectory;
	private Box2 windowBoundingBox;
	private boolean resetConfiguration;
	private int[] sashDivision;
	private int partBrowserMode;
	private boolean useConnectivity;
	private boolean useCollision;
	private LDrawGridTypeT gridUnit;
	private boolean useDefaultBaseplate;
	private boolean turnOffSound;

	private final String DefaultLDrawDirectory;
	private final Box2 DefaultWindowBoundingBox;
	private final boolean DefaultResetConfiguration;
	private final int[] DefaultSashDivision;
	private final int DefaultPartBrowserMode;
	private final boolean DefaultUseConnectivity;
	private final boolean DefaultUseCollision;
	private final LDrawGridTypeT DefaultGridUnit;
	private final boolean DefaultUseDefaultBasePlate;
	private final boolean DefaultTurnOffSound;

	public static final String getDefaultDataDirectoryPath() {
		if (DefaultDataDirectoryPath == null) {
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				DefaultDataDirectoryPath = (System.getProperty("user.home") + "/MOC Builder/");
			} else
				DefaultDataDirectoryPath = (System.getProperty("user.dir") + "/");
		}
		return DefaultDataDirectoryPath;
	}

	private BuilderConfigurationManager() {
		getDefaultDataDirectoryPath();
		DefaultLDrawDirectory = getDefaultDataDirectoryPath() + "LDraw/";
		DefaultWindowBoundingBox = new Box2();
		DefaultWindowBoundingBox.setOrigin(new Vector2f(0, 0));
		DefaultWindowBoundingBox.setSize(new Size2(800, 600));
		DefaultResetConfiguration = false;
		DefaultSashDivision = new int[] { 100, 400, 100 };
		DefaultPartBrowserMode = 0;
		DefaultUseConnectivity = true;
		DefaultUseCollision = true;
		DefaultGridUnit = LDrawGridTypeT.Medium;
		DefaultUseDefaultBasePlate = true;
		DefaultTurnOffSound = false;

		ldrawDirectory = DefaultLDrawDirectory;
		resetConfiguration = DefaultResetConfiguration;
		windowBoundingBox = new Box2();
		windowBoundingBox.setOrigin(DefaultWindowBoundingBox.getOrigin());
		windowBoundingBox.setSize(DefaultWindowBoundingBox.getSize());
		sashDivision = new int[3];
		sashDivision[0] = DefaultSashDivision[0];
		sashDivision[1] = DefaultSashDivision[1];
		sashDivision[2] = DefaultSashDivision[2];

		partBrowserMode = DefaultPartBrowserMode;
		useConnectivity = DefaultUseConnectivity;
		useCollision = DefaultUseCollision;
		gridUnit = DefaultGridUnit;

		useDefaultBaseplate = DefaultUseDefaultBasePlate;

		turnOffSound = DefaultTurnOffSound;

		loadFromFile();

		if (resetConfiguration == true)
			resetConfigurationFile();

		checkLdrawDirectory(ldrawDirectory);

		NotificationCenter.getInstance().postNotification(
				NotificationMessageT.BrickbuilderConfigurationChanged);
	}

	public synchronized static BuilderConfigurationManager getInstance() {
		if (_instance == null) {
			_instance = new BuilderConfigurationManager();
		}

		return _instance;
	}

	private boolean writeToFile() {
		File directory = new File(getDefaultDataDirectoryPath());
		if (directory.exists() == false)
			directory.mkdir();
		File file = new File(configurationFilename);

		StringBuilder fileContents = new StringBuilder();
		// #LDrawPath(String) default: C:/Program Files (x86)/LDraw/
		fileContents
				.append("#LDrawPath(String) default: C:/Program Files (x86)/LDraw/");
		fileContents.append("\r\n");
		fileContents.append("LDrawDirectory = " + ldrawDirectory);
		fileContents.append("\r\n");
		fileContents.append("\r\n");
		//
		// #Reset to default setting(boolean) default: false
		fileContents
				.append("#Reset to default setting(boolean) default: false");
		fileContents.append("\r\n");
		fileContents.append("Reset = " + resetConfiguration);
		fileContents.append("\r\n");
		fileContents.append("\r\n");
		//
		// #window pos(int, int) default: 0, 0
		fileContents.append("#Window Position(int, int) default: 0, 0");
		fileContents.append("\r\n");
		fileContents
				.append("WindowPosition = " + windowBoundingBox.getOrigin());
		fileContents.append("\r\n");
		fileContents.append("\r\n");
		//
		// #window size(int, int) default: 800, 600
		fileContents.append("#window size(int, int) default: 800, 600");
		fileContents.append("\r\n");
		fileContents.append("WindowSize = " + windowBoundingBox.getSize());
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		// #window sash division(int, int, int) default: 100, 300, 100
		fileContents
				.append("#window sash division(int, int, int) default: 100, 400, 100");
		fileContents.append("\r\n");
		fileContents.append("WindowSashDivision = " + sashDivision[0] + ", "
				+ sashDivision[1] + ", " + sashDivision[2]);
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		// #partBrowser slide size
		fileContents.append("#PartBrowser Mode");
		fileContents.append("\r\n");
		fileContents.append("PartBrowserMode = " + partBrowserMode);
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		// #use connectivity
		fileContents.append("#Use Connectivity");
		fileContents.append("\r\n");
		fileContents.append("UseConnectivity = " + useConnectivity);
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		// #use Collision Detection
		fileContents.append("#Use Collision");
		fileContents.append("\r\n");
		fileContents.append("UseCollision = " + useCollision);
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		// #use Default BasePlate
		fileContents.append("#Use Default Baseplate");
		fileContents.append("\r\n");
		fileContents.append("UseDefaultBaseplate = " + useDefaultBaseplate);
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		// #use Default BasePlate
		fileContents.append("#Turn Off Sound");
		fileContents.append("\r\n");
		fileContents.append("TurnOffSound = " + turnOffSound);
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		// #Grid Unit
		fileContents.append("#Grid Unit");
		fileContents.append("\r\n");
		fileContents.append("GridUnit = " + gridUnit);
		fileContents.append("\r\n");
		fileContents.append("\r\n");

		try {
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(file));
			bos.write(fileContents.toString().getBytes());
			bos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void loadFromFile() {
		String fileContents = null;
		fileContents = LDrawUtilities.stringFromFile(configurationFilename);

		if (fileContents == null) {
			resetConfigurationFile();
		} else {
			String[] lines = fileContents.replaceAll("\r", "").split("\n");
			for (String line : lines) {
				parseLine(line);
			}
		}
	}

	private void parseLine(String line) {
		if (line.startsWith("LDrawDirectory")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1);
				if (tempString.endsWith("/") == false
						&& tempString.endsWith("\\") == false)
					ldrawDirectory = tempString + "/";
				else
					ldrawDirectory = tempString;
				ldrawDirectory = ldrawDirectory.trim();
			} catch (Exception e) {

			}
		} else if (line.startsWith("Reset")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1)
						.trim();
				resetConfiguration = Boolean.parseBoolean(tempString);
			} catch (Exception e) {

			}
		} else if (line.startsWith("WindowPosition")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1);
				String[] tokens = tempString.split(",");
				Vector2f pos = new Vector2f(Float.parseFloat(tokens[0]),
						Float.parseFloat(tokens[1]));
				windowBoundingBox.setOrigin(pos);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (line.startsWith("WindowSize")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1);
				String[] tokens = tempString.split(",");
				Size2 size = new Size2(Float.parseFloat(tokens[0]),
						Float.parseFloat(tokens[1]));
				windowBoundingBox.setSize(size);
			} catch (Exception e) {

			}
		} else if (line.startsWith("WindowSashDivision")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1);
				String[] tokens = tempString.split(",");
				for (int i = 0; i < 3; i++) {
					int value = Integer.parseInt(tokens[i].trim());
					sashDivision[i] = value;
				}
			} catch (Exception e) {

			}
		} else if (line.startsWith("PartBrowserMode")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1);

				int value = Integer.parseInt(tempString.trim());
				partBrowserMode = value;

			} catch (Exception e) {
			}
		} else if (line.startsWith("UseConnectivity")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1)
						.trim();
				useConnectivity = Boolean.parseBoolean(tempString);
			} catch (Exception e) {

			}
		} else if (line.startsWith("UseCollision")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1)
						.trim();
				useCollision = Boolean.parseBoolean(tempString);
			} catch (Exception e) {

			}
		} else if (line.startsWith("UseDefaultBaseplate")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1)
						.trim();
				useDefaultBaseplate = Boolean.parseBoolean(tempString);
			} catch (Exception e) {

			}
		} else if (line.startsWith("TurnOffSound")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1)
						.trim();
				turnOffSound = Boolean.parseBoolean(tempString);
			} catch (Exception e) {

			}
		} else if (line.startsWith("GridUnit")) {
			try {
				String tempString = line.substring(line.indexOf("=") + 1)
						.trim();
				gridUnit = LDrawGridTypeT.valueOf(tempString);
			} catch (Exception e) {
			}
		}
	}

	public void resetConfigurationFile() {
		ldrawDirectory = DefaultLDrawDirectory;
		windowBoundingBox = DefaultWindowBoundingBox;
		resetConfiguration = DefaultResetConfiguration;

		sashDivision = new int[3];
		sashDivision[0] = DefaultSashDivision[0];
		sashDivision[1] = DefaultSashDivision[1];
		sashDivision[2] = DefaultSashDivision[2];

		partBrowserMode = DefaultPartBrowserMode;

		useDefaultBaseplate = DefaultUseDefaultBasePlate;
		
		turnOffSound = DefaultTurnOffSound;

		writeToFile();
	}

	public void checkLdrawDirectory(String path) {
		if (path == null) {
			System.exit(0);
		}
		if (path.endsWith("/") == false && path.endsWith("\\") == false)
			path += File.separator;

		File ldrawPath = new File(path + LDrawPaths.LDCONFIG_FILE_NAME);

		if (ldrawPath.exists() == false) {
			Display display = Display.getDefault();
			Shell shell = display.getActiveShell();
			if (shell == null) {
				shell = new Shell(display);
			}
			DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
			dialog.setMessage("Locate LDraw directory");
			dialog.setFilterPath(getDefaultDataDirectoryPath() + "ldraw");
			dialog.setText("Open Directory");
			checkLdrawDirectory(dialog.open());
			if (!shell.isDisposed()) {
				shell.dispose();
			}
		} else {
			setLDrawDirectory(path);
		}
	}

	public String getLDrawDirectory() {
		return ldrawDirectory;
	}

	public void setLDrawDirectory(String ldrawDirectory) {
		LDrawPaths.getInstance().setPreferredLDrawPath(ldrawDirectory);
		this.ldrawDirectory = ldrawDirectory;
	}

	public Vector2f getWindowPosition() {
		return windowBoundingBox.getOrigin();
	}

	public void setWindowPosition(int x, int y) {
		this.windowBoundingBox.setOrigin(new Vector2f(x, y));
	}

	public Size2 getWindowSize() {
		return windowBoundingBox.getSize();
	}

	public void setWindowSize(int width, int height) {
		this.windowBoundingBox.setSize(new Size2(width, height));
	}

	public int[] getSashDivision() {
		return this.sashDivision;
	}

	public void setSashDivision(int[] division) {
		for (int i = 0; i < 3; i++)
			sashDivision[i] = division[i];
	}

	public void updateFile() {
		writeToFile();
	}

	public void setPartBrowserMode(int mode) {
		partBrowserMode = mode;
	}

	public int getPartBrowserMode() {
		return partBrowserMode;
	}

	public boolean isUseConnectivity() {
		return useConnectivity;
	}

	public void setUseConnectivity(boolean useConnectivity) {
		this.useConnectivity = useConnectivity;
	}

	public boolean isUseCollision() {
		return useCollision;
	}

	public void setUseCollision(boolean useCollision) {
		this.useCollision = useCollision;
	}

	public LDrawGridTypeT getGridUnit() {
		return gridUnit;
	}

	public void setGridUnit(LDrawGridTypeT gridUnit) {
		this.gridUnit = gridUnit;
	}

	public boolean isUseDefaultBaseplate() {
		return useDefaultBaseplate;
	}

	public void setUseDefaultBaseplate(boolean enable) {
		useDefaultBaseplate = enable;
	}

	public boolean isTurnOffSound() {
		return turnOffSound;
	}

	public void setTurnOffSound(boolean turnOffSound) {
		this.turnOffSound = turnOffSound;
	}	
	
}
