package edu.uiuc.cs414.group8droid;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class AudioHandler implements Runnable {
	AudioTrack audioOut;
	Queue<DataPacket> q;
	SkeletonActivity parent;	
	final static String TAG = "Eightdroid";
	final static int SAMPLE_RATE = 8000;
	final static int BUFFER_SIZE = 557*2;
	
	public AudioHandler(SkeletonActivity parent) {
		this.parent = parent;
	}
	@Override
	public void run() {
		Log.d("Audio", "AudioHandler Thread running.");
		audioOut = new AudioTrack(
				AudioManager.STREAM_MUSIC, 
				SAMPLE_RATE, 
				AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT, 
				BUFFER_SIZE,
				AudioTrack.MODE_STREAM);
		audioOut.play();
		DataPacket packet;
		q = new LinkedBlockingQueue<DataPacket>();
		while (true) {
			while (q.size() > 10){ q.remove(); Log.d("Arecord","Audio packet dropped");}
			if ( !q.isEmpty() ) {
				packet = q.poll();
				//Log.d("Audio", "Grabbed some AUDIO packeterrr with timestamp " + packet.getTimestamp());
				byte[] audio = packet.getData().toByteArray();
				if(parent.initPhoneStamp == 0)
					parent.initPhoneStamp = (new Date()).getTime();
				audioOut.write(audio, 0, audio.length);
			}
		}
	}
	public void queueFrame(DataPacket pkt) {
		q.add(pkt);
	}

}
