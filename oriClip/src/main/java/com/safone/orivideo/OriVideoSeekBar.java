package com.safone.orivideo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.safone.oriclip.R;

/**
 * @by: origami
 * @date: {2021/4/22}
 * @info:
 **/
public class OriVideoSeekBar extends View {

    private final Paint mPaint;

    private final int[] mColor = new int[]{Color.WHITE, Color.GRAY, Color.BLUE};

    private final Point downPoint = new Point(0,0);

    private int seekW = dp2px(2);
    private int defW;

    private float bufferValue = 0f;
    private float playValue = 0f;

    private boolean moveFlag = false;

    private EventListener mEventListener;
    public interface EventListener{
        void seekTo(float v);
    }

    public OriVideoSeekBar(Context context) {
        this(context,null);
    }

    public OriVideoSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public OriVideoSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.OriVideoSeekBar);
        mColor[0] = attr.getColor(R.styleable.OriVideoSeekBar__color_init,mColor[0]);
        mColor[1] = attr.getColor(R.styleable.OriVideoSeekBar__color_buffer,mColor[1]);
        mColor[2] = attr.getColor(R.styleable.OriVideoSeekBar__color_play,mColor[2]);
        seekW = attr.getDimensionPixelOffset(R.styleable.OriVideoSeekBar__line_hight,seekW);
        defW = seekW;
        attr.recycle();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStyle(Paint.Style.FILL);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if(bufferValue != 1) {
            mPaint.setColor(mColor[0]);
            canvas.drawRect(getPaddingLeft(), getCenterH() - seekW,
                    getWidth() - getPaddingRight(), getCenterH() + seekW, mPaint);
        }
        if(bufferValue != 0){
            mPaint.setColor(mColor[1]);
            canvas.drawRect(getPaddingLeft(), getCenterH() - seekW,
                    (getWidth() - getPaddingRight()) * bufferValue, getCenterH() + seekW, mPaint);
        }
        mPaint.setColor(mColor[2]);
        canvas.drawRect(getPaddingLeft(), getCenterH() - seekW,
                (getWidth() - getPaddingRight()) * playValue, getCenterH() + seekW, mPaint);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                downPoint.x = (int) event.getX();
                downPoint.y = (int) event.getY();
                moveFlag = false;
                if(seekW != defW * 2) {
                    seekW = defW * 2;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - downPoint.x) > 3 || moveFlag) {
                    moveFlag = true;
                    playValue = Math.max((event.getX() - getPaddingLeft()) / getW(), 0);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                seekW = defW;
                if(mEventListener != null) {
                    if (moveFlag) {
                        mEventListener.seekTo(playValue);
                    }else {
                        float playValue_op = Math.max((event.getX() - getPaddingLeft()) / getW(), 0);
                        if(Math.abs(playValue_op - playValue) > 0.05){
                            playValue = playValue_op;
                            mEventListener.seekTo(playValue_op);
                        }
                    }
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setBufferValue(float bufferValue) {
        if(bufferValue != this.bufferValue) {
            this.bufferValue = bufferValue;
            postInvalidate();
        }
    }

    public void setPlayValue(float playValue) {
        if(playValue != this.playValue) {
            this.playValue = playValue;
            postInvalidate();
        }
    }

    private int getW(){
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getCenterH(){
        return (getHeight() - getPaddingTop() - getPaddingBottom()) / 2;
    }

    private int dp2px(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    public void setEventListener(EventListener mEventListener) {
        this.mEventListener = mEventListener;
    }
}
