package edu.uiuc.cs414.group8droid;

public class FrameQueue {
	int controltime;
	int timestamp;
	double servertime; // NEEDED for latency calculation
	int size;
	short checksum;
	byte flags;
	byte[] dataBuffer;
	
	public FrameQueue() {
	}
}
