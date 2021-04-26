package com.safone.orivideo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.TimedText;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.safone.oriclip.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @by: origami
 * @date: {2021/4/22}
 * @info:
 **/
public class OriVideoPlay extends RelativeLayout{

    private Context mContext;

    // View
    private SurfaceView mSurfaceView;
    private ImageView _video_back, _video_play;
    private OriVideoSeekBar _video_seek_bar;
    private TextView _video_txt_now, _video_txt_end, _video_title;

    private LinearLayout _video_ui_bot;
    private LinearLayout _video_ui_top;
    private RelativeLayout _video_ui_right;

    private Timer timer;
    private ValueAnimator mAnimator,nAnimator;

    //---
    private MediaPlayer mMediaPlayer;
    AudioManager mAudioManager;

    private boolean onPreparedFlag = true, isBarShow = true;

    private int longDu = 1;
    private long systemMis = System.currentTimeMillis();

    private int _top_H = 0,_bot_H = 0;
    private int _right_W = dp2px(200);

    private Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == 0){
                _video_txt_now.setText(int2Time(msg.arg1));
            }else if(msg.what == 1){
                hideBar();
            }
        }
    };


    public OriVideoPlay(Context context) {
        this(context, null);
    }

    public OriVideoPlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OriVideoPlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        View layout = LayoutInflater.from(context).inflate(R.layout.activity_ori_video_player, this, true);
        mSurfaceView = layout.findViewById(R.id._surfaceView_video);
        _video_back = layout.findViewById(R.id._video_back);
        _video_play = layout.findViewById(R.id._video_play);
        _video_seek_bar = layout.findViewById(R.id._video_seek_bar);
        _video_txt_now = layout.findViewById(R.id._video_txt_now);
        _video_txt_end = layout.findViewById(R.id._video_txt_end);
        _video_title = layout.findViewById(R.id._video_title);
        _video_ui_bot = layout.findViewById(R.id._video_ui_bot);
        _video_ui_top = layout.findViewById(R.id._video_ui_top);
        _video_ui_right = layout.findViewById(R.id._video_ui_right);

        mMediaPlayer =  new MediaPlayer();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        init();

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("ORI","click_ok  -> " + isBarShow);
                if(isBarShow){
                    hideBar();
                }else {
                    showBar();
                }
            }
        });

        _video_seek_bar.setEventListener(new OriVideoSeekBar.EventListener() {
            @Override
            public void seekTo(float v) {
                mMediaPlayer.seekTo((int) (mMediaPlayer.getDuration() * v));
            }
        });
        _video_play.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onClickPlay(false);
            }
        });
        post(new Runnable() {
            @Override
            public void run() {
                _top_H = _video_ui_top.getHeight();
                _bot_H = _video_ui_bot.getHeight();
            }
        });

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int dp2px(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    private void onClickPlay(boolean mustPlay){
        if(onPreparedFlag){return;}
        if(mMediaPlayer.isPlaying()){
            if(mustPlay){return;}
            _video_play.setImageResource(R.mipmap.play);
            mMediaPlayer.pause();
        }else {
            _video_play.setImageResource(R.mipmap.pause);
            startPlay();
        }
    }

    private void init() {
        if(mMediaPlayer == null){ mMediaPlayer = new MediaPlayer(); }
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                onPreparedFlag = false;
                mp.setVolume(1f,1f);
                mp.setLooping(false);
                longDu = mp.getDuration();
                _video_txt_end.setText(int2Time(longDu));
                onClickPlay(true);
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.e("ORI","当前缓冲: " + percent);
                _video_seek_bar.setBufferValue( (float) percent / 100f);
            }
        });
        mMediaPlayer.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
            @Override
            public void onTimedText(MediaPlayer mp, TimedText text) {
                Log.e("ORI","onTimedText: " + text.getText());
                _video_txt_now.setText(text.getText());
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                _video_play.setImageResource(R.mipmap.play);
            }
        });
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Log.e("ORI","SurfaceHolder-> surfaceCreated");
                mMediaPlayer.setDisplay(holder);
                if(!onPreparedFlag) { onClickPlay(true); }
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.e("ORI","format-> " + format + " width-> " + width + " height-> " + height);
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                Log.e("ORI","SurfaceHolder-> surfaceDestroyed");
                mMediaPlayer.pause();
            }
        });
    }

    /**
     * 事件拦截
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(onPreparedFlag){ return true; }//如果正在准备中，直接拦截所有子VIEW事件
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 事件分发
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 监控音量上下键
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setBackCallBack(OnClickListener listener){
        _video_back.setOnClickListener(listener);
    }

    public void setVideoUrl(String url){
        try {
            if(mMediaPlayer != null){ mMediaPlayer.reset(); }
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ORI"," error: " + e.getMessage());
        }
    }

    private void startPlay(){
        if(!onPreparedFlag) {
            mMediaPlayer.start();
            systemMis = System.currentTimeMillis();
            if(timer == null) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                            int position = mMediaPlayer.getCurrentPosition();
                            Message msg = new Message();
                            msg.what = 0;
                            msg.arg1 = position;
                            handler.sendMessage(msg);
                            _video_seek_bar.setPlayValue((float) position / longDu);
                        }
                        if(isBarShow){
                            if (System.currentTimeMillis() - systemMis > 3000) {
                                handler.sendEmptyMessage(1);
                                systemMis = System.currentTimeMillis();
                            }
                        }else {
                            systemMis = System.currentTimeMillis();
                        }
                    }
                }, 1000, 1000);
            }
        }
    }

    /**
     * 释放资源
     */
    public void release(){
        if(mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        handler.removeCallbacksAndMessages(0);
        handler = null;
    }

    /**
     * 隐藏UI
     */
    private void hideBar(){
        if(mAnimator == null){
            mAnimator = ObjectAnimator.ofFloat(0,1);
            mAnimator.setDuration(800);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    _video_ui_top.setTranslationY(_top_H * value * -1);
                    _video_ui_bot.setTranslationY(_bot_H * value);
                }
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    _video_ui_top.setVisibility(INVISIBLE);
                    _video_ui_bot.setVisibility(INVISIBLE);
                }
            });
        }
       if(nAnimator != null && nAnimator.isRunning()){
            nAnimator.cancel();
            _video_ui_top.setVisibility(INVISIBLE);
            _video_ui_bot.setVisibility(INVISIBLE);
        }else if(!mAnimator.isRunning()){
            mAnimator.start();
        }
        isBarShow = false;
    }

    /**
     * 显示UI
     */
    private void showBar(){
        if(nAnimator == null){
            nAnimator = ObjectAnimator.ofFloat(1,0);
            nAnimator.setDuration(800);
            nAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    _video_ui_top.setTranslationY(_top_H * value * -1);
                    _video_ui_bot.setTranslationY(_bot_H * value);
                }
            });
//                nAnimator.addListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        isBarShow = true;
//                    }
//                });
        }
        if(mAnimator != null && mAnimator.isRunning()){
            mAnimator.cancel();
        }
        if(!nAnimator.isRunning()) {
            _video_ui_top.setVisibility(VISIBLE);
            _video_ui_bot.setVisibility(VISIBLE);
            nAnimator.start();
        }
        isBarShow = true;
    }


    private String int2Time(int ms){
        int s_total = ms / 1000;
        int hh = s_total / ( 60 * 60 );
        int mm = (s_total - hh * (60 * 60)) / 60;
        int ss = s_total - hh * (60 * 60) - mm * 60;
        return (hh > 9 ? "" + hh : "0" + hh) + ":" +
                (mm > 9 ? "" + mm : "0" + mm) + ":" +
                (ss > 9 ? "" + ss : "0" + ss);
    }

}
