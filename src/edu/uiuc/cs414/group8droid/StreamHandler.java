package edu.uiuc.cs414.group8droid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class StreamHandler implements Runnable {
    /**
     * Sockets required to handle our own protocol stuff. We're *not* using
     * any kind of HTTP or RTSP streaming for the time being.
     */
    Socket sock;
    DataInputStream input;
    DataOutputStream output;
    LinkedBlockingQueue<FrameQueue> videoQueue;
    LinkedBlockingQueue<FrameQueue> audioQueue;
    LinkedBlockingQueue<FrameQueue> controlQueue;
    
    byte[] dataBuffer;
    final static int streamPort = 3827;
    final static String serverIP = "192.17.248.215";
    final static byte videoFlag = 0x01;
    final static byte audioFlag = 0x02;
    final static byte controlFlag = 0x03;
    static final private String TAG = "Eightdroid";
    
    public StreamHandler(LinkedBlockingQueue<FrameQueue> vidQueue,
    					 LinkedBlockingQueue<FrameQueue> audioQueue,
    					 LinkedBlockingQueue<FrameQueue> controlQueue){
    	this.videoQueue = vidQueue;
    	this.audioQueue = audioQueue;
    	this.controlQueue = controlQueue;
    }
	public void run() {
		Log.d(TAG, "entered streamhandler");
        try {
			sock = new Socket(serverIP, streamPort);
			input = new DataInputStream(sock.getInputStream());
			output = new DataOutputStream(sock.getOutputStream());
			//readStream();
		} catch (UnknownHostException e) {
			Log.e(TAG, "unknown host");
		} catch (IOException e) {
			Log.e(TAG, "IO fail on socket connection");
		}
	}
	
	public void readStream() {
		Log.d(TAG, "Entered read stream");
		while(true){
			// Read header
			FrameQueue curFrame = new FrameQueue();
			try {
				curFrame.controltime = input.readInt();
				curFrame.timestamp = input.readInt();
				curFrame.servertime = input.readDouble();
				curFrame.size = input.readInt();
				curFrame.checksum = input.readShort();
				curFrame.flags = input.readByte();
			} catch (IOException e) {
				Log.e(TAG, "Failed to read header from server");
			}
			Log.d(TAG, "Read timestamp"+curFrame.timestamp);
			Log.d(TAG, "Read flag"+curFrame.flags);
			// Read data
			int totalBytesRead = 0;
			int curBytesRead = 0;
			dataBuffer = new byte[curFrame.size];
			do {
				try {
					curBytesRead = input.read(dataBuffer, totalBytesRead, curFrame.size - totalBytesRead);
				} catch (IOException e) {
					Log.e(TAG, "Failed to read data from server");
				}
				totalBytesRead += curBytesRead;
			}
			while (totalBytesRead < curFrame.size);
			Log.d(TAG, "Read "+totalBytesRead+" Bytes out of size "+curFrame.size);
			
			// Enqueue frame in corresponding queue
			//if(curFrame.flags == videoFlag)
				//videoQueue.add(curFrame);
			//else if(curFrame.flags == audioFlag)
				//audioQueue.add(curFrame);
			//else if(curFrame.flags == controlFlag)
				//controlQueue.add(curFrame);
		}
	}
	
	public void writeStream() {
		
		// Dequeue frame
		FrameQueue newFrame = new FrameQueue();
		//TODO newFrame = dataQueue.remove();
		
		// Write header
		try {
			output.writeInt(newFrame.controltime);
			output.writeInt(newFrame.timestamp);
			output.writeDouble(newFrame.servertime);
			output.writeInt(newFrame.size);
			output.writeShort(newFrame.checksum);
			output.writeByte(newFrame.flags);
		} catch (IOException e) {
			Log.e("Eightdroid", "Failed to write header to server");
		}

		// Write data
		dataBuffer = new byte[newFrame.size];
			
		try {
			output.write(dataBuffer, 0, newFrame.size);
		} catch (IOException e) {
			Log.e("Eightdroid", "Failed to write data to sserver");
		}

	}
	
	public void close() {
		try {
			output.close();
			input.close();
			sock.close();
		} 
		catch (IOException e) {
			System.out.println(e);
		}
	}
}
