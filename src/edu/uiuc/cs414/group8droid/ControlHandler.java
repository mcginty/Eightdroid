package edu.uiuc.cs414.group8droid;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import android.util.Log;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class ControlHandler implements Runnable {
    /**
     * Sockets required to handle our own protocol stuff. We're *not* using
     * any kind of HTTP or RTSP streaming for the time being.
     */
	boolean done = false;
	boolean isConnected = false;
    Socket sock;
    ObjectInputStream input;
    DataOutputStream output;
    SkeletonActivity parent;
    
    final static int controlPort = 6667;
    final static String serverIP = "192.17.252.150";
    
    public ControlHandler(SkeletonActivity parent){
    	this.parent = parent;
    }
	public void run() {
		Log.d("Control", "ControlHandler running...");
        while (true) {
        	try {
				sock = new Socket(serverIP, controlPort);
				//input = new ObjectInputStream(sock.getInputStream());
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
	}
	
	public void sendRemotePkt(ControlPacket pkt) {
		Log.d("Control", "Entered sendRemotePkt..");
		try {
			if(isConnected()){
				int size = pkt.getSerializedSize();
				output.writeInt(size);
				output.write(pkt.toByteArray());
				Log.d("Control", "Remote pkt sent of size: "+size);				
			}
			else
				Log.d("Control", "Control not yet connected");

		} catch (IOException e) {
			Log.e("Control", "Could not send remote packet");
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
