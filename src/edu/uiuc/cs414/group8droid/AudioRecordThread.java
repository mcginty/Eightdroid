package edu.uiuc.cs414.group8droid;

import java.util.Date;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.google.protobuf.ByteString;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket.PacketType;

public class AudioRecordThread extends Thread { 
	SkeletonActivity parent;
	public AudioRecord audioRecord; 
	
	public int mSamplesRead; //how many samples read 
	public int buffersizebytes; 
	public int buflen; 
	public int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO; 
	public int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
	public static byte[] buffer; //+-32767 
	public static final int SAMPPERSEC = 8000; //samp per sec 8000, 11025, 22050 44100 or 48000 

	public AudioRecordThread(SkeletonActivity parent) {
		this.parent = parent;
	}
	/** Called when the activity is first created. */ 
	@Override 
	public void run(){ 
		//setContentView(R.layout.main); 
		Log.d("Arecord","About to start recording");
		buffersizebytes = AudioRecord.getMinBufferSize(SAMPPERSEC,channelConfiguration,audioEncoding); //4096 on ion
		Log.d("Arecord", "buffer is bytes: " + buffersizebytes);
		
		//buffersizebytes = 1024;
		buffer = new byte[buffersizebytes]; 
		buflen=buffersizebytes/2; 
		audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,SAMPPERSEC, 
				channelConfiguration,audioEncoding,buffersizebytes); //constructor 
		Log.d("Arecord","Audio Record Allocated");
		try { 
			audioRecord.startRecording(); 
			Log.d("Arecord","AudioRecord.StartRecording() passed");
			while(true)
			{
				long tempInitTimestamp = (new Date()).getTime();
				mSamplesRead = audioRecord.read(buffer, 0, buffersizebytes); 
				ByteString buf = ByteString.copyFrom(buffer);
				DataPacket proto = DataPacket.newBuilder()
					.setTimestamp(tempInitTimestamp /*- parent.initialTimestamp*/)
					.setServertime(parent.delta/* - parent.initialTimestamp*/)
					.setType(PacketType.AUDIO)
					.setData(buf).build();
				parent.outnet.queuePacket(proto);
			}
		} catch (Throwable t) { 
			Log.e("AudioRecord", "Recording Failed"); 
		} 
		/*
		AudioTrack audioOut;
		audioOut = new AudioTrack(
				AudioManager.STREAM_MUSIC, 
				SAMPPERSEC, 
				AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				AudioFormat.ENCODING_PCM_16BIT, 
				SAMPPERSEC,
				AudioTrack.MODE_STREAM);
		audioOut.play();
		Log.d("Arecord","About to start playing");
		audioOut.write(buffer, 0, buffersizebytes);
		 */
	} 
}//thread