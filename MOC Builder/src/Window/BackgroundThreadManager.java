package Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BackgroundThreadManager implements Runnable {
	private static BackgroundThreadManager _instance = null;

	private BackgroundThreadManager() {
		runnableList = new ArrayList<Runnable>();
		numOfRunnable = new Integer(0);
		terminatedRunableCnt = new Integer(0);
		threadPool = new ArrayList<Thread>();
		for (int i = 0; i < 5; i++) {
			Thread t = new Thread(this);
			t.start();
			threadPool.add(t);
		}
	}

	public synchronized static BackgroundThreadManager getInstance() {
		if (_instance == null)
			_instance = new BackgroundThreadManager();
		return _instance;
	}

	private List<Runnable> runnableList;
	private boolean isTerminated = false;
	private ArrayList<Thread> threadPool = null;


	public void add(Runnable runnable) {
		synchronized (runnableList) {
			runnableList.add(runnable);
			numOfRunnable += 1;
		}
	}

	public void terminate() {
		this.isTerminated = true;
		for (Thread t : threadPool)
			try {
				t.interrupt();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public int sizeOfThread() {
		return runnableList.size();
	}
	
	public boolean isAllFinish(){
		if(numOfRunnable==0 && terminatedRunableCnt==0)return true;
		return false;
	}

	private static Integer numOfRunnable;
	private static Integer terminatedRunableCnt;

	@Override
	public void run() {
		Runnable runnable = null;
		while (isTerminated == false) {
			synchronized (runnableList) {
				if (runnableList.size() > 0)
					runnable = runnableList.remove(0);
				else {
					synchronized (this) {
						if (numOfRunnable.equals(terminatedRunableCnt)
								&& terminatedRunableCnt.equals(0)==false) {
							numOfRunnable = terminatedRunableCnt = 0;
							ProgressBarManager.getInstance().setProgress(-1);
						}else if (numOfRunnable.equals(terminatedRunableCnt)
								&& terminatedRunableCnt.equals(0)) 
							ProgressBarManager.getInstance().setProgress(-1);
					}
				}
			}
			if (runnable != null) {		
				try{
				runnable.run();
				}catch(Exception e){
					e.printStackTrace();
				}				
						
				if (isTerminated)
					break;

				synchronized (this) {
					terminatedRunableCnt += 1;
				}
				ProgressBarManager.getInstance()
						.setProgress(
								(int) ((terminatedRunableCnt + 1.0f)
										/ numOfRunnable * 100));

				runnable = null;				
			} else {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
