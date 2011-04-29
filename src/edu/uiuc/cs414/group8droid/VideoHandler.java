package edu.uiuc.cs414.group8droid;

import java.util.Date;
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
		Log.d("Video", "VideoHandler Thread running.");
		DataPacket packet;
		q = new LinkedBlockingQueue<DataPacket>();
		while (true) {
			if ( !q.isEmpty() ) {
				packet = q.poll();
				parent.endPhoneStamp = (new Date()).getTime();
				long playTimeStamp = parent.endPhoneStamp - parent.initPhoneStamp;
				Log.d("Video", "playTimeStamp: "+playTimeStamp);
				long captureTimeStamp = packet.getTimestamp();
				Log.d("Video", "captureTimeStamp: "+captureTimeStamp);
				//if(playTimeStamp > captureTimeStamp) {
				//	
				//}
				//if(playTimeStamp < captureTimeStamp) {
				//try {
				//	Thread.sleep(captureTimeStamp - playTimeStamp);
				//} catch (InterruptedException e) {
				//	Log.e("Video", "Interrupted video thread during sleep");
				//}	
				//Log.d("Video", "Grabbed packet with timestamp " + packet.getTimestamp());
				byte[] jpeg = packet.getData().toByteArray();
				final Bitmap imageBuffer = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
				parent.mVideoDisplay.post(new Runnable() {
					public void run() {
						parent.mVideoDisplay.setImageBitmap(imageBuffer);
					}
				});
				long endLatency = (new Date()).getTime();
				long startLatency = packet.getServertime();
				parent.control.setLatencyTime(startLatency, endLatency);
				//}
			}
		}
	}
	public void queueFrame(DataPacket pkt) {
		q.add(pkt);
	}
}
