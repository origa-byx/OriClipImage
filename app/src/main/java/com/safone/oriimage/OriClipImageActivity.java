package com.safone.oriimage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/**
 * demo
 */
public class OriClipImageActivity extends AppCompatActivity {

    private OriClipImageView oriClip;

    public static void startActForRe(Activity activity, String path, int requestCode){
        Intent intent = new Intent(activity, OriClipImageActivity.class);
        intent.putExtra("path",path);
        activity.startActivityForResult(intent,requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ori_clip_image);
        oriClip = findViewById(R.id.oriClip);
        String path = getIntent().getStringExtra("path");
        oriClip.setImagePath(path);
        oriClip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("ORI"," onclick");
            }
        });
        findViewById(R.id.ori_cj).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String test1 = oriClip.getClipPath("test1", true);
                setResult(Activity.RESULT_OK,new Intent().putExtra("path",test1));
                Log.e("ORI","path: " + test1);
                finish();
            }
        });
    }

}