package edu.uiuc.cs414.group8droid;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

public class VideoHandler implements Runnable {
	Queue<DataPacket> q;
	boolean active;
	SkeletonActivity parent;
	final static String TAG = "Eightdroid";
	
	public VideoHandler(SkeletonActivity parent) {
		this.parent = parent;
	}

	@Override
	public void run() {
		Log.d(TAG, "VideoHandler Thread running.");
		DataPacket packet;
		q = new LinkedBlockingQueue<DataPacket>();
		while (true) {
			if ( !q.isEmpty() ) {
				packet = q.poll();
				Log.d(TAG, "Grabbed packet with timestamp " + packet.getTimestamp());
				byte[] jpeg = packet.getData().toByteArray();
				final Bitmap imageBuffer = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
				parent.mVideoDisplay.post(new Runnable() {
					public void run() {
						parent.mVideoDisplay.setImageBitmap(imageBuffer);
					}
				});
			}
		}
	}
	public void queueFrame(DataPacket pkt) {
		q.add(pkt);
	}
}
