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
    LinkedBlockingQueue<FrameQueue> dataQueue;
    byte[] dataBuffer;
    final static int streamPort = 3827;
    final static String serverIP = "127.0.0.1";
    
    public StreamHandler(LinkedBlockingQueue<FrameQueue> queue){
    	this.dataQueue = queue;
    }
	public void run() {
        try {
			sock = new Socket(serverIP, streamPort);
			input = new DataInputStream(sock.getInputStream());
			output = new DataOutputStream(sock.getOutputStream());
			readStream();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void readStream() {
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
			Log.e("Eightdroid", "Failed to read header from server");
			e.printStackTrace();
		}

		// Read data
		int totalBytesRead = 0;
		int curBytesRead = 0;
		dataBuffer = new byte[curFrame.size];
		do {
			try {
				curBytesRead = input.read(dataBuffer, totalBytesRead, curFrame.size - totalBytesRead);
			} catch (IOException e) {
				Log.e("Eightdroid", "Failed to read data from server");
				e.printStackTrace();
			}
			totalBytesRead += curBytesRead;
		}
		while (totalBytesRead < curFrame.size);
		
		// Enqueue frame
		dataQueue.add(curFrame);
	}
	
	public void writeStream() {
		
		// Dequeue frame
		FrameQueue newFrame = new FrameQueue();
		newFrame = dataQueue.remove();
		
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
			e.printStackTrace();
		}

		// Write data
		dataBuffer = new byte[newFrame.size];
			
		try {
			output.write(dataBuffer, 0, newFrame.size);
		} catch (IOException e) {
			Log.e("Eightdroid", "Failed to write data to sserver");
			e.printStackTrace();
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
