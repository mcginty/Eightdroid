package edu.uiuc.cs414.group8droid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

import android.util.Log;

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
    final static int streamPort = 6666;
    final static String serverIP = "192.17.255.27";
    
    public StreamHandler(Queue<DataPacket> videoQueue, Queue<DataPacket> audioQueue){
    	this.videoQueue = videoQueue;
    	this.audioQueue = audioQueue;
    }
	public void run() {
		Log.d("Eightdroid", "StreamHandler running...");
        try {
			sock = new Socket(serverIP, streamPort);
			input = new ObjectInputStream(sock.getInputStream());
			output = new ObjectOutputStream(sock.getOutputStream());
			readStream();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void readStream() {
		// Read header
		byte size;
		while (!done) {
			try {
				size = input.readByte();
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
				e.printStackTrace();
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
