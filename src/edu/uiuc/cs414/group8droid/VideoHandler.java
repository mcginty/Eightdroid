package edu.uiuc.cs414.group8droid;

import java.util.Queue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class VideoHandler implements Runnable {
	Queue<FrameQueue> q;
	ImageView mVideoDisplay;
	boolean active;
	
	public VideoHandler(ImageView mVideoDisplay, Queue q) {
		this.mVideoDisplay = mVideoDisplay;
		this.q = q;
	}

	@Override
	public void run() {
		FrameQueue packet;
		active = true;
		while (this.active) {
			if ((packet = q.poll()) != null) {
				final Bitmap imageBuffer = BitmapFactory.decodeByteArray(packet.dataBuffer, 0, packet.dataBuffer.length);
				mVideoDisplay.post(new Runnable() {
					public void run() {
						mVideoDisplay.setImageBitmap(imageBuffer);
					}
				});
			}
		}
	}
}
