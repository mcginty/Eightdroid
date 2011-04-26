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

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import edu.uiuc.cs414.group8droid.R;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single viListew: an EditText that
 * displays and edits some internal text.
 */
public class SkeletonActivity
		extends Activity 
		implements OnErrorListener, OnBufferingUpdateListener, OnCompletionListener, 
					MediaPlayer.OnPreparedListener, SurfaceHolder.Callback {
    
    static final private int BACK_ID = Menu.FIRST;
    static final private String TAG = "Eightdroid";
    SurfaceHolder holder;
    MediaPlayer mp;
    SurfaceView mPreview;
    
    LinkedBlockingQueue<FrameQueue> dataQueue;
    
    public SkeletonActivity() {
    }

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.skeleton_activity);
        // Our MediaPlayer will stream the video to this VideoView
        mPreview = ((SurfaceView) findViewById(R.id.streamingVideo));
        holder = mPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

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
        menu.add(0, BACK_ID, 0, R.string.back).setShortcut('0', 'b');

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
        case BACK_ID:
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
            finish();
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
		mp = MediaPlayer.create(this, R.raw.sample_mpeg4);
        mp.setOnErrorListener(this);
        mp.setOnBufferingUpdateListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setDisplay(holder);
        Log.d(TAG, "Video Height: " + mp.getVideoHeight() + ", Width: " + mp.getVideoWidth());
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		holder.setFixedSize(mp.getVideoWidth(), mp.getVideoHeight());
        mp.start();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "onError--->   what:" + what + "    extra:" + extra);
        if (mp != null) {
            mp.stop();
            mp.release();
        }
		return false;
	}
}
