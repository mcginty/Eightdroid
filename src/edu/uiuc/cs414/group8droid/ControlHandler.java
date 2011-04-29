package edu.uiuc.cs414.group8droid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket.ControlType;

public class ControlHandler implements Runnable {
    /**
     * Sockets required to handle our own protocol stuff. We're *not* using
     * any kind of HTTP or RTSP streaming for the time being.
     */
	
	// Flags
	boolean done = false;
	boolean isConnected = false;
	
	// Network vars
    Socket sock;
    DataInputStream input;
    DataOutputStream output;
    SkeletonActivity parent;
    private Queue<ControlPacket> sendQueue;
    
    // Latency vars
    private long startLatency;
    private long endLatency;
    private long latency;
    
    final static int controlPort = 6667;
  //  final static String serverIP = "192.17.252.150";
    
    public ControlHandler(SkeletonActivity parent){
    	this.parent = parent;
    	sendQueue = new LinkedBlockingQueue<ControlPacket>(10);
    }
	public void run() {
		Log.d("Control", "ControlHandler running...");
        while (true) {
        	try {
				sock = new Socket(parent.serverIP, controlPort);
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

	public void setLatencyTime(long t1, long t2){
		startLatency = t1;
		endLatency = t2;
	}
	
	public void sendLatency() {
		Log.d("Control", "Start Latency: "+startLatency);
		Log.d("Control", "End Latency: "+endLatency);
		Log.d("Control", "delta: "+parent.delta);
		latency = endLatency - (startLatency - parent.delta);
		Log.d("Control", "Sending latency to server...");
		
		ControlPacket latencyCtrl = ControlPacket.newBuilder()
		   .setType(ControlType.LATENCY)
		   .setLatency(latency)
		   .build();
		int size = latencyCtrl.getSerializedSize();
		
		try {
			output.writeInt(size);
			output.write(latencyCtrl.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("Control", "Failed to send latency");
		}

		Log.d("Control", "Latency sent of size: "+size+" with value:"+latency);
	
	}
	
	public void queuePacket(ControlPacket pkt) {
		if (sendQueue.size() == 10) {
			for (int i=0; i<10; i++) sendQueue.remove();
		}
		sendQueue.add(pkt);
	}

	public void sendPing(ControlPacket pkt) {
		Log.d("Control", "Entered sendControlPkt..");
		try {
			if(isConnected()){
				// Send initial ping request
				int size = pkt.getSerializedSize();
				long ping_start = (new Date()).getTime();
				output.writeInt(size);
				output.write(pkt.toByteArray());
				Log.d("Control", "Ping sent of size:"+size+" at time:"+ping_start);
				
				// Receive response from ping request
				size = input.readInt();					
				byte[] bytes = new byte[size];
				input.readFully(bytes);
				long ping_end = (new Date()).getTime();
				Log.d("Control", "Ping received of size: "+size+" at time:"+ping_end);
				ControlPacket serverPkt = ControlPacket.parseFrom(bytes);
				long servertime = serverPkt.getServertime();
				
				// Do delta calculation
				long rtt = ping_end-ping_start;
				parent.delta = servertime-(ping_start+rtt/2);
				Log.d("Control", "Delta calculated as: "+parent.delta);
				
				
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
