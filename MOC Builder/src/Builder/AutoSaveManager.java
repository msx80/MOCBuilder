package Builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import Window.MOCBuilder;

public class AutoSaveManager {
	private final String DefaultFilepath = BuilderConfigurationManager.getDefaultDataDirectoryPath()+"autoSave.ldr";
	private static AutoSaveManager _instance = null;
	private boolean isTerminate = false;
	private MOCBuilder builder = null;

	private AutoSaveManager() {
	}

	public boolean isExistAutoSaveFile() {
		File file = new File(DefaultFilepath);
		return file.exists();
	}

	public void loadAutoSavedFile() {
		if (builder == null)
			builder = MOCBuilder.getInstance();
		if (isExistAutoSaveFile()) {
			builder.openFile(DefaultFilepath);
			builder.getWorkingLDrawFile().setPath(null);
		}
	}

	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				long lastRunTime = System.currentTimeMillis();
				while (isTerminate == false) {
					if (System.currentTimeMillis() - lastRunTime > 10000) {// 10
																			// sec.
						autoSave();
						lastRunTime = System.currentTimeMillis();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				terminate();
			}
		}).start();

	}

	public synchronized static AutoSaveManager getInstance() {
		if (_instance == null)
			_instance = new AutoSaveManager();
		return _instance;
	}

	public void terminate() {
		isTerminate = true;
		File file = new File(DefaultFilepath);
		file.delete();
	}

	private void autoSave() {
		if (builder == null)
			builder = MOCBuilder.getInstance();
		File file = new File(DefaultFilepath);
		try {
			file.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			isTerminate = true;
			return;
		}
		FileOutputStream fos;
		try {
			String str = builder.getWorkingLDrawFile().write();
			fos = new FileOutputStream(file);
			fos.write(str.getBytes());
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
