package edu.uiuc.cs414.group8droid;

//acquire samples and dump 
//May 11 2010 Bob G 

import android.app.Activity; 
import android.graphics.Color; 
import android.media.AudioFormat; 
import android.media.AudioRecord; 
import android.os.Bundle; 
import android.view.MotionEvent; 
import android.widget.TextView; 

public class AudioRecordThread extends Thread { 
	SkeletonActivity parent;
	public AudioRecord audioRecord; 
	public int mSamplesRead; //how many samples read 
	public int buffersizebytes; 
	public int buflen; 
	public int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO; 
	public int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
	public static short[] buffer; //+-32767 
	public static final int SAMPPERSEC = 8000; //samp per sec 8000, 11025, 22050 44100 or 48000 

	public AudioRecordThread(SkeletonActivity parent) {
		this.parent = parent;
	}
	/** Called when the activity is first created. */ 
	@Override 
	public void run(){ 
		//setContentView(R.layout.main); 
		buffersizebytes = AudioRecord.getMinBufferSize(SAMPPERSEC,channelConfiguration,audioEncoding); //4096 on ion 
		buffer = new short[buffersizebytes]; 
		buflen=buffersizebytes/2; 
		audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,SAMPPERSEC, 
				channelConfiguration,audioEncoding,buffersizebytes); //constructor 

		try { 
			audioRecord.startRecording(); 
			mSamplesRead = audioRecord.read(buffer, 0, buffersizebytes); 
			audioRecord.stop(); 
		} catch (Throwable t) { 
		//Log.e("AudioRecord", "Recording Failed"); 
		} 
	} 
}//thread