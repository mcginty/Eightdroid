package edu.uiuc.cs414.group8droid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket.ControlType;

public class ControlHandler implements Runnable {
    /**
     * Sockets required to handle our own protocol stuff. We're *not* using
     * any kind of HTTP or RTSP streaming for the time being.
     */
	boolean done = false;
	boolean isConnected = false;
    Socket sock;
    DataInputStream input;
    DataOutputStream output;
    SkeletonActivity parent;
    private Queue<ControlPacket> sendQueue;
    
    final static int controlPort = 6667;
    final static String serverIP = "192.17.252.150";
    
    public ControlHandler(SkeletonActivity parent){
    	this.parent = parent;
    	sendQueue = new LinkedBlockingQueue<ControlPacket>(10);
    }
	public void run() {
		Log.d("Control", "ControlHandler running...");
        while (true) {
        	try {
				sock = new Socket(serverIP, controlPort);
				input = new DataInputStream(sock.getInputStream());
				output = new DataOutputStream(sock.getOutputStream());
				
				isConnected = true;
				
				Log.d("Control", "Successfully connected to server.");
				break;

			} catch (UnknownHostException e) {
				Log.e("Control", "unknown host");
			} catch (IOException e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					Log.e("Control", "Thread sleep failed for some reason.");
				}
				//Log.e(TAG, "IO fail on socket connection");
			}
        }
		while (true) {
			//System.out.println("Attempting write of bytes.");
			if (!sendQueue.isEmpty()) {
				try {
					ControlPacket pkt = sendQueue.poll();
					int size = pkt.getSerializedSize();
					output.writeInt(size);
					output.write(pkt.toByteArray());
					Log.d("Control", "Control pkt sent of size: "+size);
				} catch (IOException e) {
					Log.e("Control", "Failed to send control packet");
				}
			}
		}
	}

	public void queuePacket(ControlPacket pkt) {
		if (sendQueue.size() == 10) {
			for (int i=0; i<10; i++) sendQueue.remove();
		}
		sendQueue.add(pkt);
	}

	public void sendControlPkt(ControlPacket pkt) {
		Log.d("Control", "Entered sendControlPkt..");
		try {
			if(isConnected()){
				int size = pkt.getSerializedSize();
				output.writeInt(size);
				output.write(pkt.toByteArray());
				Log.d("Control", "Control pkt sent of size: "+size);				
			}
			else
				Log.d("Control", "Control not yet connected");

		} catch (IOException e) {
			Log.e("Control", "Could not send packet");
		}
	}
	
	public boolean isConnected() {
		return isConnected;
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
