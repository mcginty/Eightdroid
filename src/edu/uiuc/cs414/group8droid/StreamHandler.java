package edu.uiuc.cs414.group8droid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

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

    
    SkeletonActivity parent;
    byte[] dataBuffer;
    
    final static String TAG = "Eightdroid";
    final static int MAX_LATENCY_MS = 500;
    
    final static int streamPort = 6666;
//    final static String serverIP = "192.17.255.225";

    long initTimestamp;
    
    public VideoHandler videoHandler;
    public AudioHandler audioHandler;
    public AudioRecordThread audioRecordThread;
    
    public StreamHandler(SkeletonActivity parent){
    	this.parent = parent;
    	initTimestamp = 0;
    }
	
    public void run() {
		Log.d("Stream", "StreamHandler running...");
		while (true) {
        	try {				
        		sock = new Socket(parent.serverIP, streamPort);
				input = new ObjectInputStream(sock.getInputStream());
				
				// Spawn audio and video worker threads
		        videoHandler = new VideoHandler(parent);
		        (new Thread(videoHandler)).start();
		        audioHandler = new AudioHandler(parent);
		        (new Thread(audioHandler)).start();
				Log.d("Stream", "Successfully connected to server.");

				readStream();
			} catch (IOException ee) {
				Log.e("network", "failed to connect to server");
				break;
			}
		}
    }	
	public void readStream() {
		// Read header
		int size;
		while (!done) {
			//Log.d("Stream", "Attempting read of bytes...");
			try {
				size = input.readInt();
				//Log.d("Eightdroid", "Received packet of size " + size);
				byte[] bytes = new byte[size];
				input.readFully(bytes);
				DataPacket pkt = DataPacket.parseFrom(bytes);
				if (initTimestamp == 0)
					initTimestamp = (new Date()).getTime();
				//Log.d("Stream", "Latency: " + ((pkt.getTimestamp() - ((new Date()).getTime() - initTimestamp))));
				if (pkt.getType() == DataPacket.PacketType.VIDEO) {
					videoHandler.queueFrame(pkt); // modified from parent
				}
				else if (pkt.getType() == DataPacket.PacketType.AUDIO) {
					//Log.d("Stream", "Queued an audio packet!");
					audioHandler.queueFrame(pkt); //modified from parent
				}
			} catch (IOException e) {
				Log.e("Stream", "IOException in receiving the packet. Message: " + e.getStackTrace());
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
