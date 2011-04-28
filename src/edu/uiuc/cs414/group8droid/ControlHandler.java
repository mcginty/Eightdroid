package edu.uiuc.cs414.group8droid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import android.util.Log;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class ControlHandler implements Runnable {
    /**
     * Sockets required to handle our own protocol stuff. We're *not* using
     * any kind of HTTP or RTSP streaming for the time being.
     */
	boolean done = false;
    Socket sock;
    ObjectInputStream input;
    ObjectOutputStream output;
    SkeletonActivity parent;
    byte[] dataBuffer;
    
    final static String TAG = "Eightdroid";
    final static int MAX_LATENCY_MS = 500;
    
    final static int controlPort = 6667;
    final static String serverIP = "192.17.252.150";
    long initTimestamp;
    
    public VideoHandler videoHandler;
    public AudioHandler audioHandler;
    
    public ControlHandler(SkeletonActivity parent){
    	this.parent = parent;
    	initTimestamp = 0;
    }
	public void run() {
		Log.d("Control", "ControlHandler running...");
        while (true) {
        	try {
				sock = new Socket(serverIP, controlPort);
				//input = new ObjectInputStream(sock.getInputStream());
				output = new ObjectOutputStream(sock.getOutputStream());
				
				// Spawn audio and video worker threads
		        //videoHandler = new VideoHandler(parent);
		        //(new Thread(videoHandler)).start();

		        //audioHandler = new AudioHandler(parent);
		        //(new Thread(audioHandler)).start();
				
				Log.d(TAG, "Successfully connected to server.");
				writeStream();
			} catch (UnknownHostException e) {
				Log.e(TAG, "unknown host");
			} catch (IOException e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					Log.e(TAG, "Thread sleep failed for some reason.");
				}
				//Log.e(TAG, "IO fail on socket connection");
			}
        }
	}
	
	public void writeStream() {
		// Read header
		int size;
		while (!done) {
			Log.d("Control", "Attempting to write...");
			try {
				size = input.readInt();
				Log.d("Control", "Received packet of size " + size);
				byte[] bytes = new byte[size];
				input.readFully(bytes);
				DataPacket pkt = DataPacket.parseFrom(bytes);
				if (initTimestamp == 0)
					initTimestamp = (new Date()).getTime();
				Log.d("Eightdroid", "Latency: " + ((pkt.getTimestamp() - ((new Date()).getTime() - initTimestamp))));
				if (pkt.getType() == DataPacket.PacketType.VIDEO) {
					videoHandler.queueFrame(pkt); // modified from parent
				}
				else if (pkt.getType() == DataPacket.PacketType.AUDIO) {
					Log.d("Eightdroid", "Queued an audio packet!");
					audioHandler.queueFrame(pkt); //modified from parent
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
