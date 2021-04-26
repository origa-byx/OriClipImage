package com.safone.oriclip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class OriClipImageActivity extends AppCompatActivity {

    private OriClipImageView oriClip;

    public static void startAct(Activity activity, String path){
        Intent intent = new Intent(activity, OriClipImageActivity.class);
        intent.putExtra("path",path);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ori_clip_image);
        oriClip = findViewById(R.id.oriClip);
        String path = getIntent().getStringExtra("path");
        oriClip.setImageBitmap(BitmapFactory.decodeFile(path));
        oriClip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("ORI"," onclick");
            }
        });
        findViewById(R.id.ori_cj).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap clipBitmap = oriClip.getClipBitmap();
                String test1 = oriClip.getClipPath("test1", true);
                Log.e("ORI","path: " + test1);
            }
        });
    }

}