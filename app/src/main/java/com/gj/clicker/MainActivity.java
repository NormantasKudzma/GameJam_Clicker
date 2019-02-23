package com.gj.clicker;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ovl.engine.EngineConfig;
import com.ovl.engine.OverloadEngine;
import com.ovl.engine.android.OverloadEngineAndroid;
import com.ovl.utils.Vector2;

public class MainActivity extends Activity {
	private ClickerGame game = new ClickerGame();
	OverloadEngineAndroid engine;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		getWindow().getDecorView().getRootView().setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent e) {
				final float hw = OverloadEngine.getInstance().frameWidth * 0.5f;
				final float hh = -OverloadEngine.getInstance().frameHeight * 0.5f;
				
				int pointerIndex = e.getActionIndex();
				int pointerId = e.getPointerId(pointerIndex);
				
				switch (e.getActionMasked()){
					case MotionEvent.ACTION_POINTER_DOWN:
					case MotionEvent.ACTION_DOWN:{

						Vector2 pos = new Vector2(e.getX(pointerIndex), e.getY(pointerIndex));
						pos.div(hw, hh).sub(1.0f, -1.0f);
						game.postClick(pos);
						break;
					}
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_POINTER_UP:{
						getWindow().getDecorView().getRootView().performClick();
						break;
					}
				}
				return true;
			}
			
		});
		
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo info = am.getDeviceConfigurationInfo();
		
		if (info.reqGlEsVersion >= 0x20000){
			if (engine == null){
				EngineConfig cfg = new EngineConfig();
				cfg.game = game;
				cfg.isDebug = true;
				engine = new OverloadEngineAndroid(cfg);
			}
			setContentView(engine.getSurfaceView(this));
		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	    super.onWindowFocusChanged(hasFocus);
	    if (hasFocus) {
	    	getWindow().getDecorView().getRootView().setSystemUiVisibility(
	    		View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
	        );
	    }
	}
}
