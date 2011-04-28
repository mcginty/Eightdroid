package edu.uiuc.cs414.group8droid;

import android.util.Log;

public class ControlHandler implements Runnable {

	
	public ControlHandler() {
	
	}

	@Override
	public void run() {
		while(true) 
		{
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			Log.e("Eightdroid", "Sleep failed");
		}
		
		}
		
	}
	
	
}
