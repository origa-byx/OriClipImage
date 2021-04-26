package com.safone.oriimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Region;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntRange;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

/**
 * @by: origami
 * @date: {2021/4/20}
 * @info:
 *       {@link #setImageBitmap(Bitmap)} -> 设置待剪切图片
 *       {@link #getClipBitmap()} or {@link #getClipPath(String, boolean)} -> 获取剪切结果
 **/
public class OriClipImageView extends View {


    private float mScale = 1f;//放缩结果参数
    private final float[] mTranslation = new float[]{0f,0f};//偏移参数

    private float centerTW = 0;//初始横向偏移 -> 横向居中
    private final Point centerPoint = new Point(0,0);//中心点, 决定裁剪结果

    private final float[] mTRange = new float[]{0f,0f};//最大偏移

    private Bitmap mBitmap;//原始位图
    private final Paint mPaint;
    private final Path mPath;

    private boolean simPoint = true;//是否全程单点触控, 判断触发点击事件

    private float mR = 0;//半径
    private float minScale = 1f;//最小放缩, 根据位图大小确定

    private final int mustPx = 5;//偏移放缩保留像素,预防紧贴情况下强转int可能产生的裁剪时数组越界

    private final SparseArray<Point> pointSparseArray = new SparseArray<>();
    private int markX = 0;//双指放缩参数

    public OriClipImageView(Context context) {
        this(context,null);
    }

    public OriClipImageView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public OriClipImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5);
        mPath = new Path();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionMasked = event.getActionMasked();
        switch (actionMasked){
            case MotionEvent.ACTION_POINTER_DOWN:
                simPoint = false;
            case MotionEvent.ACTION_DOWN:
                int index = event.getActionIndex();
                pointSparseArray.put(event.getPointerId(index), new Point(
                        (int) event.getX(index),
                        (int) event.getY(index) ));
                return true;
            case MotionEvent.ACTION_MOVE:
                int size = event.getPointerCount();
                if(size == 1) {//单指移动
                    int key_1 = event.getPointerId(0);
                    Point point = pointSparseArray.get(key_1);
                    float moveX = event.getX(0) - point.x;
                    float moveY = event.getY(0) - point.y;
                    if(Math.abs(moveX) > 3 || Math.abs(moveY) > 3){
                        simPoint = false;
                        mTranslation[0] += moveX / mScale;
                        mTranslation[1] += moveY / mScale;
                        point.x = (int) event.getX(0);
                        point.y = (int) event.getY(0);
                        pointSparseArray.put(key_1,point);
//                        Log.e("ORI","mTranslation -> " + mTranslation[0] + "  |  " + mTranslation[1]);
                        invalidate();
                        return true;
                    }
                }else if(size == 2) {//双指缩放
                    int key_0 = event.getPointerId(0);
                    int key_1 = event.getPointerId(1);
                    Point point_0 = pointSparseArray.get(key_0);
                    Point point_1 = pointSparseArray.get(key_1);
                    Point point_now_0 = new Point((int) event.getX(0), (int) event.getY(0));
                    Point point_now_1 = new Point((int) event.getX(1), (int) event.getY(1));
                    float distance = getDistance(point_now_0, point_now_1) - getDistance(point_0, point_1);
                    if(Math.abs(distance) > 5){
                        if(distance > 0){//放大
                            markX++;
                        }else {//缩小
                            if(mScale == minScale){
                                pointSparseArray.put(key_0, point_now_0);
                                pointSparseArray.put(key_1, point_now_1);
                                return true;
                            }
                            markX--;
                        }
                        mScale =(float) Math.tanh((double) markX / 90) + 1;
                        pointSparseArray.put(key_0, point_now_0);
                        pointSparseArray.put(key_1, point_now_1);
                        if(mScale < minScale){
                            mScale = minScale; markX++;
                        }
                        invalidate();
                        return true;
                    }
                }else if(size >= 3) {//TODO 三指旋转 (斜率的变化 反应到 旋转角度上) --> 只关心前三根手指
                    int key_0 = event.getPointerId(0);
                    int key_1 = event.getPointerId(1);
                    int key_2 = event.getPointerId(2);
                    pointSparseArray.put(key_0,new Point((int) event.getX(0),(int) event.getY(0)));
                    pointSparseArray.put(key_1,new Point((int) event.getX(1),(int) event.getY(1)));
                    pointSparseArray.put(key_2,new Point((int) event.getX(2),(int) event.getY(2)));
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if(simPoint) { performClick(); }
                pointSparseArray.clear();
                simPoint = true;
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                pointSparseArray.remove(event.getPointerId(event.getActionIndex()));
                return true;
        }
        return true;
//        return super.onTouchEvent(event);
    }

    /**
     * 强制不能{@link ViewGroup.LayoutParams#WRAP_CONTENT}
     *      替换成 MATCH_PARENT
     **/
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int modeW = MeasureSpec.getMode(widthMeasureSpec);
        int modeH = MeasureSpec.getMode(heightMeasureSpec);
        if(modeW != MeasureSpec.EXACTLY){ widthMeasureSpec
                = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.EXACTLY); }
        if(modeH != MeasureSpec.EXACTLY){ heightMeasureSpec
                = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec),MeasureSpec.EXACTLY); }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mR =(float) Math.min(getMeasuredWidth(), getMeasuredHeight()) / 5 * 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mBitmap != null) {
            canvas.save();
            if(Math.abs(mTranslation[0] * mScale) > (mTRange[0] * mScale - mR - mustPx)) {
                if(mTranslation[0] < 0) {
                    mTranslation[0] = - ( mTRange[0] - (mR + mustPx) / mScale );
                }else {
                    mTranslation[0] = mTRange[0] - (mR + mustPx) / mScale;
                }
            }
            if(Math.abs(mTranslation[1] * mScale) > (mTRange[1] * mScale - mR - mustPx)) {
                if(mTranslation[1] < 0) {
                    mTranslation[1] = - ( mTRange[1] - (mR + mustPx) / mScale );
                }else {
                    mTranslation[1] = mTRange[1] - (mR + mustPx) / mScale;
                }
            }
            canvas.scale(mScale, mScale,
                    (float) getWidth() / 2 + (mTranslation[0] + centerTW) * mScale,
                    (float) getHeight() / 2 + mTranslation[1] * mScale);
            canvas.translate((mTranslation[0] + centerTW) * mScale, mTranslation[1] * mScale);
            canvas.drawBitmap(mBitmap, 0,0, null);
            canvas.restore();
        }
        canvas.save();
        mPath.reset();
        mPath.addCircle((float) getWidth() / 2,(float) getHeight() / 2,
                mR, Path.Direction.CCW);
        canvas.clipPath(mPath, Region.Op.DIFFERENCE);
        canvas.drawColor(Color.argb(150,0,0,0));
        canvas.drawCircle((float) getWidth() / 2,(float) getHeight() / 2,
                mR, mPaint);
        canvas.restore();
    }

    /**
     * 设置待剪切图片
     */
    public void setImagePath(String path){
        setImageBitmap(BitmapFactory.decodeFile(path));
    }

    public void setImageBitmap(Bitmap bm) {
        post(new Runnable() {
            @Override
            public void run() {
                Matrix matrix = new Matrix();
                float[] scale = new float[]{1f, 1f};
                if(bm.getWidth() < getW()){ scale[0] = (float) getW() / (float) bm.getWidth(); }
                if(bm.getHeight() < getH()){ scale[1] = (float) getH() / (float) bm.getHeight(); }
                float s = Math.max(scale[0],scale[1]);
                matrix.postScale(s,s);
                mBitmap = Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight(),matrix,true);
                centerTW = (float) (getW() - mBitmap.getWidth()) / 2;
                mTRange[0] = (float) mBitmap.getWidth() / 2;
                mTRange[1] = (float) mBitmap.getHeight() / 2;
                minScale = Math.max(
                        2 * (mR + mustPx) / (float) mBitmap.getWidth(),
                        2 * (mR + mustPx) / (float) mBitmap.getHeight()
                );
                centerPoint.x = mBitmap.getWidth() / 2;
                centerPoint.y = getH() / 2;
                postInvalidate();
            }
        });
    }

    /**
     * 裁剪并保存图片
     * @param path 例如: "test/image/head" or  "test"
     * @param isRandom: 是否随机命名 (否的话会一直覆盖原来的图片)
     * @return null : 保存失败  notNull : 成功
     */
    public String getClipPath(String path,boolean isRandom){
        return saveBitmap(getClipBitmap(),path,isRandom);
    }

    /**
     * 裁剪图片
     * @return
     */
    public Bitmap getClipBitmap(){
        return getClipBitmap(Bitmap.Config.RGB_565);
    }

    public Bitmap getClipBitmap(Bitmap.Config config){
        int rect_w = (int) (2 * mR + 0.5f);
        Bitmap clipBitmap = Bitmap.createBitmap(rect_w, rect_w, config);
        Matrix matrix = new Matrix();
        matrix.postScale(mScale,mScale);
        Bitmap scaleBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
        int[] pxs = new int[scaleBitmap.getWidth() * scaleBitmap.getHeight()];
        scaleBitmap.getPixels(pxs,0,scaleBitmap.getWidth(),0,0,scaleBitmap.getWidth(),scaleBitmap.getHeight());
        int[] offsets = new int[2];
        offsets[0] = Math.max((int) ((centerPoint.x - mTranslation[0]) * mScale - mR + 0.5f), 1);
        offsets[1] = Math.max((int) ((centerPoint.y - mTranslation[1]) * mScale - mR + 0.5f), 1);
        clipBitmap.setPixels(pxs,offsets[1] * scaleBitmap.getWidth() + offsets[0],scaleBitmap.getWidth(),0,0,rect_w,rect_w);
        return clipBitmap;
    }

    /**
     * 保存位图
     * @param bitmap
     * @param path 例如: "test/image/head"
     * @param isRandom 是否随机命名图片
     * @return null ：保存失败
     */
    public String saveBitmap(Bitmap bitmap,String path,boolean isRandom) {
        String savePath;
        File filePic;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory().getPath() + File.separator + path + File.separator;
        } else {
            Log.e("ORI", "saveBitmap : sdcard not mounted");
            return null;
        }
        if(isRandom){
            savePath += (getRandomString(14) + ".jpg");
        }else {
            savePath += "_headImage.jpg";
        }
        try {
            filePic = new File(savePath);
            if (!filePic.exists()) {
                if(filePic.getParentFile() != null) {
                    filePic.getParentFile().mkdirs();
                }
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e("ORI", "saveBitmap : " + e.getMessage());
            return null;
        }
        return savePath;
    }


    public String getRandomString(@IntRange(from = 8) int length){
        String ran = "abcdefghijkmlnopqrstuvwxyz123456789";
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        Calendar date = Calendar.getInstance();
        builder.append(date.get(Calendar.YEAR))
                .append(date.get(Calendar.MONTH) < 10 ? "0" + date.get(Calendar.MONTH) : date.get(Calendar.MONTH))
                .append(date.get(Calendar.DATE) < 10 ? "0" + date.get(Calendar.DATE) : date.get(Calendar.DATE));
        for (int i = 0; i < (length - 8); i++) {
            builder.append(ran.charAt(random.nextInt(ran.length())));
        }
        return builder.toString();
    }

    private int getW(){
        return getWidth() == 0 ? getMeasuredWidth() : getWidth();
    }

    private int getH(){
        return getHeight() == 0 ? getMeasuredHeight() : getHeight();
    }

    private float getDistance(Point p1, Point p2){
        return (float) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}
