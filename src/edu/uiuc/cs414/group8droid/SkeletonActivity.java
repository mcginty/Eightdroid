/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uiuc.cs414.group8droid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket.ControlCode;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket.ControlType;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket.PacketType;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single viListew: an EditText that
 * displays and edits some internal text.
 */
public class SkeletonActivity
		extends Activity
		implements SurfaceHolder.Callback{
	
    Camera mCamera;

    static final private int EXIT_ID = Menu.FIRST;
	private static final String TAG = "Eightdroid";
    public SurfaceView mPreviewSurface;
    public ImageView mVideoDisplay;
    public StreamHandler stream;
    public ControlHandler control;
    public Timer pingTimer;
    public EditText ipEdit;
    public EditText portEdit;
    public EditText serverEdit;
    public String serverIP;
    final static String defaultNameserverPort = "3825";
    final static String defaultNameserverIP = "192.17.255.225";
    final static String defaultServerName = "alice";
    
    // Gesture data
    float startx = 0, starty = 0, endx = 0, endy = 0;
    
    
    //public VideoHandler videoHandler;
    //public AudioHandler audioHandler;
    
    Queue<DataPacket> audioQueue;
    Queue<DataPacket> videoQueue;
    boolean active;

    //public SkeletonActivity() {
    //}
    

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d("UI", "gui thread created");


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Eightdroid");
        wl.acquire();
        
        Log.d("UI", "Edit Texts set");
        
        // Inflate our UI from its XML layout description.
        //setContentView(R.layout.video_view);
        setContentView(R.layout.home_screen);


        ipEdit = (EditText)findViewById(R.id.ipaddr);
        portEdit = (EditText)findViewById(R.id.port);
        serverEdit = (EditText)findViewById(R.id.server);
        
        Log.d("UI", "EditTexts hooked");
        
        //ipEdit.setText(nameserverIP);
        ipEdit.setHint("Nameserver");
        portEdit.setHint("Port");
        serverEdit.setHint("Server Name");
        //portEdit.setText("3825");
        
        Log.d("UI", "in UI setup");
        
       // AudioRecordThread audioRecordThread;
       // Log.d("Arecord","About to start the recorder");
       // SkeletonActivity parent = null;
	   // audioRecordThread = new AudioRecordThread(parent);
       // (new Thread(audioRecordThread)).start();
    }

    public void initStream(View v)
    {   
        try {
			serverIP = nameserverConnect(serverEdit.getText().toString(),"query",ipEdit.getText().toString(),portEdit.getText().toString());
		} catch (IOException e) {
			Log.d("nameserver", "nameserver threw IOException");
			e.printStackTrace();
		}
		
		if(serverIP == ""){
			Log.e("nameserver", "nameserver did not provide you with a valid IP");
			return;			
		}

        
        setContentView(R.layout.video_view);
        mVideoDisplay = ((ImageView) findViewById(R.id.streamingVideo));

        mVideoDisplay.setOnTouchListener(mTouchListener);
        
        Log.d("UI", "Set Touch Listener");
        audioQueue = new LinkedBlockingQueue<DataPacket>(10);
        videoQueue = new LinkedBlockingQueue<DataPacket>(10);
        
        stream = new StreamHandler(this);
        (new Thread(stream)).start();
        
        control = new ControlHandler(this);
        (new Thread(control)).start();
        
        //mPreviewSurface = ((SurfaceView) findViewById(R.id.previewSurface));
        //mPreviewSurface.getHolder().addCallback(this);
        //mPreviewSurface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new PingTask(), 0, 10000);   
    }
    
	private String nameserverConnect(String sname, String stype, String nsIP, String nsPort) throws IOException {
		String TrimmedIP;
	    Socket nssock;
	    DataInputStream nsinput;
	    DataOutputStream nsoutput;

		while(true){ //Try again if this was the server's first connection
			Log.d("nameserver", "Connecting to nameserver");
			nssock = new Socket(nsIP, Integer.parseInt(nsPort));
			
			nsoutput = new DataOutputStream(nssock.getOutputStream());
			Log.d("nameserver", "nameserver out stream opened");
			
			nsinput = new DataInputStream(nssock.getInputStream());
			Log.d("nameserver", "nameserver in stream opened");
			
			byte[] name = new byte[16];
			byte[] tname = sname.getBytes("US-ASCII");
			byte[] type = new byte[16];
			byte[] ttype = stype.getBytes("US-ASCII");
			byte[] ip = new byte[48];
			
			java.lang.System.arraycopy(tname, 0, name, 0, tname.length);
			java.lang.System.arraycopy(ttype, 0, type, 0, ttype.length);
			
			Log.d("nameserver", "Nameserver communication started");				

			nsoutput.write(name);
			nsoutput.write(type);
			nsinput.readFully(ip);
			
			String ServerIP = new String(ip,0,0,48);
			ServerIP.trim();
			TrimmedIP = ServerIP.replace("\0", "");
			
			Log.d("nameserver", "Nameserver communication completed");
			
			Log.d("nameserver", "Server IP: " + TrimmedIP);
			nssock.close();
			
			if(ServerIP.codePointAt(0) == 'x')
				continue;
			else
				break;
			}
		if(TrimmedIP.codePointAt(0) == 'z')
			return "";
		
		return TrimmedIP;
	}
    
    public void defaultConfig(View v)
    {
    	serverEdit.setText(defaultServerName);
    	ipEdit.setText(defaultNameserverIP);
    	portEdit.setText(defaultNameserverPort);
    }
    
    public void clearConfig(View v)
    {
    	serverEdit.setText("");
    	ipEdit.setText("");
    	portEdit.setText("");
    }

	private class PingTask extends TimerTask {

		@Override
		public void run() {
			ControlPacket pingCtrl = ControlPacket.newBuilder()
			.setType(ControlType.PING)
			.build();
			control.sendPing(pingCtrl);
		}
	}
    
    private OnTouchListener mTouchListener = new OnTouchListener() {
    	@ Override
	public boolean onTouch(View v, MotionEvent event) {
		
		//Log.d("UI", "inside onTouch");
		ControlCode curCode = null;
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		{
			startx = event.getX();
			Log.d("UI", "Startx: "+startx);
			starty = event.getY();
			Log.d("UI", "Starty: "+starty);
			break;
		}
		case MotionEvent.ACTION_UP:
		{
			endx = event.getX()-startx;
			Log.d("UI", "Endx: "+endx);
			endy = event.getY()-starty;
			Log.d("UI", "Endy: "+endy);
			if (endx < 0){
				if (endy > 0) {
					if (Math.abs(endy) > Math.abs(endx))
						curCode = ControlCode.DOWN;
					else
						curCode = ControlCode.LEFT;
				}
				else if (endy < 0) {
					if (Math.abs(endy) > Math.abs(endx))
						curCode = ControlCode.UP;
					else
						curCode = ControlCode.LEFT;
				}
			}
			else if (endx > 0) {
				if (endy > 0) {
					if (Math.abs(endy) > Math.abs(endx))
						curCode = ControlCode.DOWN;
					else
						curCode = ControlCode.RIGHT;
				}
				else if (endy < 0){
					if (Math.abs(endy) > Math.abs(endx))
						curCode = ControlCode.UP;
					else
						curCode = ControlCode.RIGHT;
				}				
			}
		Log.d("UI", "got gesture"+curCode);	
		ControlPacket remoteCtrl = ControlPacket.newBuilder()
								   .setType(ControlType.REMOTE)
								   .setControl(curCode)
								   .build();
		control.queuePacket(remoteCtrl);
		break;
		}

		}
		
		return true;
	}
    };
    
    /**
     * Called when the activity is about to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Called when your activity's options menu needs to be created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // We are going to create two menus. Note that we assign them
        // unique integer IDs, labels from our string resources, and
        // given them shortcuts.
        menu.add(0, EXIT_ID, 0, R.string.exit).setShortcut('0', 'b');

        return true;
    }

    /**
     * Called right before your activity's option menu is displayed.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    /**
     * Called when a menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case EXIT_ID:
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A call-back for when the user presses the back button.
     */
    OnClickListener mBackListener = new OnClickListener() {
        public void onClick(View v) {
            //finish();
        }
    };

    /**
     * A call-back for when the user presses the clear button.
     */
    OnClickListener mClearListener = new OnClickListener() {
        public void onClick(View v) {
        }
    };

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "Opening camera for recording...");
        mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(mPreviewSurface.getHolder());
		} catch (IOException e) {
			Log.e(TAG, "Exception when trying to set preview display: " + e.toString());
		}
		
		// Merge here plox
		//RecordPreviewCallback rpc = new RecordPreviewCallback(this);
		Parameters params = mCamera.getParameters();
		params.setPreviewFrameRate(5);
		params.setPreviewFormat(ImageFormat.JPEG);
		mCamera.setParameters(params);
		mCamera.setPreviewCallback(rpc);
		mCamera.setDisplayOrientation(90);
		mCamera.startPreview();
		
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		mCamera.unlock();
		mCamera.release();
	}

}
