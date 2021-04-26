package com.safone.orivideo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Insets;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.safone.oriclip.OriClipImageActivity;
import com.safone.oriclip.R;

import java.io.IOException;

public class OriVideoActivity extends AppCompatActivity {

    private OriVideoPlay videoPlay;

    public static void startAct(Activity activity, String path){
        Intent intent = new Intent(activity, OriVideoActivity.class);
        intent.putExtra("path",path);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
//        WindowInsetsController controller = getWindow().getInsetsController();
//        controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
//        getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
//        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        setContentView(R.layout.activity_ori_video);
        videoPlay = findViewById(R.id._oriVideoPlay);
        videoPlay.setBackCallBack(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        String path = getIntent().getStringExtra("path");
        if(!TextUtils.isEmpty(path)){
            videoPlay.setVideoUrl(path);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlay.release();
    }
}