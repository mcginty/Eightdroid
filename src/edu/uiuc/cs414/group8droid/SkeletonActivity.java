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

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single viListew: an EditText that
 * displays and edits some internal text.
 */
public class SkeletonActivity
		extends Activity {
    
    static final private int EXIT_ID = Menu.FIRST;
    static final private String TAG = "Eightdroid";

    public ImageView mVideoDisplay;
    public StreamHandler stream;
    
    // Gesture data
    public mGesture curGesture;
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

        
        // Inflate our UI from its XML layout description.
        //setContentView(R.layout.video_view);
        setContentView(R.layout.home_screen);
        Log.d("UI", "in UI setup");
        
        AudioRecordThread audioRecordThread;
        Log.d("Arecord","About to start the recorder");
        SkeletonActivity parent = null;
		audioRecordThread = new AudioRecordThread(parent);
        (new Thread(audioRecordThread)).start();
        
    }

    public void initStream(View v)
    {
        Log.d("UI","inside initStream");
        
        setContentView(R.layout.video_view);
        mVideoDisplay = ((ImageView) findViewById(R.id.streamingVideo));

        mVideoDisplay.setOnTouchListener(mTouchListener);
        
        Log.d("UI", "Set Touch Listener");
        audioQueue = new LinkedBlockingQueue<DataPacket>(10);
        videoQueue = new LinkedBlockingQueue<DataPacket>(10);
        
        stream = new StreamHandler(this);
        (new Thread(stream)).start();

    }
    
    private enum mGesture { UP, DOWN, LEFT, RIGHT, NULL };
	
    private OnTouchListener mTouchListener = new OnTouchListener() {
    	@ Override
	public boolean onTouch(View v, MotionEvent event) {
		
		//Log.d("UI", "inside onTouch");
		
		
		
		//ControlPacket curGesture = mGesture.NULL;
		//ControlPacket control;
		//control.
		
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
						curGesture = mGesture.DOWN;
					else
						curGesture = mGesture.LEFT;
				}
				else if (endy < 0) {
					if (Math.abs(endy) > Math.abs(endx))
						curGesture = mGesture.UP;
					else
						curGesture = mGesture.LEFT;
				}
			}
			else if (endx > 0) {
				if (endy > 0) {
					if (Math.abs(endy) > Math.abs(endx))
						curGesture = mGesture.DOWN;
					else
						curGesture = mGesture.RIGHT;
				}
				else if (endy < 0){
					if (Math.abs(endy) > Math.abs(endx))
						curGesture = mGesture.UP;
					else
						curGesture = mGesture.RIGHT;
				}				
			}
		Log.d("UI", "got gesture"+curGesture);	
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

}
