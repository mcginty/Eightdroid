package edu.uiuc.cs414.group8droid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class OutStreamHandler extends Thread {
	private Socket outSock;
	private DataOutputStream out = null;
	private ObjectInputStream in = null;
	private boolean isConnected = false;
	private Queue<DataPacket> sendQueue;
	final static int MAX_LATENCY_MS = 500;
	SkeletonActivity parent;
    public AudioRecordThread audioRecordThread;
	
    final static int droidserverPort = 6668;
    
    public OutStreamHandler(SkeletonActivity parent){
    	this.parent = parent;
    	sendQueue = new LinkedBlockingQueue<DataPacket>(10);
    }

	public void run() {
		System.out.println("Client OutNet thread started...");
		
		try {
			Log.d("outnet", "outStream thread started");
			outSock = new Socket(parent.serverIP, droidserverPort);
			//System.out.println("Network Successfully connected to client " + clientSock.getInetAddress().toString());
			isConnected = true;	
			out = new DataOutputStream(outSock.getOutputStream());
			Log.d("outnet", "reverse connection with server established");
			
	        audioRecordThread = new AudioRecordThread(parent);
	        (new Thread(audioRecordThread)).start();
			
			while (true) {
				//System.out.println("Attempting write of bytes.");
				if (!sendQueue.isEmpty()) {
					DataPacket pkt = sendQueue.poll();
					//System.out.println("Latency: "+ (((new Date()).getTime() - parent.initialTimestamp) - pkt.getTimestamp()));
					//if (((new Date()).getTime() - parent.initialTimestamp) - pkt.getTimestamp() < MAX_LATENCY_MS) {
					if(true){
						int size = pkt.getSerializedSize();
						//System.out.println("Sending a packet of size " + size + " and type " + pkt.getType().toString() + " to client.");
						out.writeInt(size);
						out.write(pkt.toByteArray());
						Log.d("outnet", "Outnet packet sent of size " + size);
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	public void queuePacket(DataPacket pkt) {
		if (sendQueue.size() == 10) {
			for (int i=0; i<10; i++) sendQueue.remove();
		}
		//System.out.println("pkt with timestamp " + pkt.getTimestamp() + " queued. qsize: " + sendQueue.size());
		sendQueue.add(pkt);
	}
	
	public boolean isConnected() {
		return isConnected;
	}
}
