package edu.uiuc.cs414.group8droid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;

import android.util.Log;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class StreamHandler implements Runnable {
    /**
     * Sockets required to handle our own protocol stuff. We're *not* using
     * any kind of HTTP or RTSP streaming for the time being.
     */
	boolean done = false;
    Socket sock;
    ObjectInputStream input;
    ObjectOutputStream output;
    Queue<DataPacket> videoQueue;
    Queue<DataPacket> audioQueue;
    byte[] dataBuffer;
    final static String TAG = "Eightdroid";
    final static int streamPort = 6666;
    final static String serverIP = "192.17.255.27";
    
    public StreamHandler(Queue<DataPacket> videoQueue, Queue<DataPacket> audioQueue){
    	this.videoQueue = videoQueue;
    	this.audioQueue = audioQueue;
    }
	public void run() {
		Log.d("Eightdroid", "StreamHandler running...");
        while (true) {
        	try {
				sock = new Socket(serverIP, streamPort);
				input = new ObjectInputStream(sock.getInputStream());
				//output = new ObjectOutputStream(sock.getOutputStream());
				Log.d(TAG, "Successfully connected to server.");
				readStream();
			} catch (UnknownHostException e) {
				Log.e(TAG, "unknown host");
			} catch (IOException e) {
				Log.e(TAG, "IO fail on socket connection");
			}
        }
	}
	
	public void readStream() {
		// Read header
		int size;
		while (!done) {
			Log.d(TAG, "Attempting read of bytes...");
			try {
				size = input.readInt();
				Log.d("Eightdroid", "Received packet of size " + size);
				byte[] bytes = new byte[size];
				input.readFully(bytes);
				DataPacket pkt = DataPacket.parseFrom(bytes);
				if (pkt.getType() == DataPacket.PacketType.VIDEO) {
					Log.d("Eightdroid", "Queued a video packet!");
					videoQueue.add(pkt);
				}
				else if (pkt.getType() == DataPacket.PacketType.AUDIO) {
					Log.d("Eightdroid", "Queued an audio packet!");
					audioQueue.add(pkt);
				}
			} catch (IOException e) {
				Log.e("Eightdroid", "IOException in receiving the packet. Message: " + e.getStackTrace());
				close();
				return;
			}
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
