package edu.uiuc.cs414.group8droid;

import android.hardware.Camera;

public class VideoRecordThread extends Thread {
	SkeletonActivity parent;
	Camera mCamera;
	int cams;
	
	public VideoRecordThread(SkeletonActivity parent) {
		this.parent = parent;
	}
	
	public void run() {
		mCamera = Camera.open();
		//mCamera.setPreviewDisplay(R.id.previewSurface);
	}
}
