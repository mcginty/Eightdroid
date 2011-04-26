package edu.uiuc.cs414.group8droid;

import java.util.Queue;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioHandler implements Runnable {
	AudioTrack audioOut;
	Queue<FrameQueue> q;
	final static int SAMPLE_RATE = 44100;
	final static int BUFFER_SIZE = 1024;
	
	public AudioHandler(Queue q) {
		this.q = q;
		audioOut = new AudioTrack(
				AudioManager.STREAM_VOICE_CALL, 
				SAMPLE_RATE, 
				AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT, 
				BUFFER_SIZE, 
				AudioTrack.MODE_STREAM);
	}
	@Override
	public void run() {
		
	}

}
