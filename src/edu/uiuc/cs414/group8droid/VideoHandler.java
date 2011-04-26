package edu.uiuc.cs414.group8droid;

import java.util.Queue;

import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class VideoHandler implements Runnable {
	BitmapFactory drawBuffer;
	Queue<FrameQueue> q;
	ImageView mVideoDisplay;
	
	public VideoHandler(ImageView mVideoDisplay, Queue q) {
		this.mVideoDisplay = mVideoDisplay;
		this.q = q;
	}
	
	@Override
	public void run() {
		FrameQueue packet;
		if ((packet = q.poll()) != null) {
			mVideoDisplay.post(new Runnable() {
				public void run() {
					mVideoDisplay.setImageBitmap(/*WHATEVER HERE */);
				}
			});
		}
	}

}
