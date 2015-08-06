/*
 * Copyright 2015 Cesar Diez Sanchez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rayboot.scantool.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;


/**
 * @author cesards
 */
public class CropImageView extends ImageView {
	private static String TAG = "CropImageView";
	private int mSrcBitmapWidth;
	private int mSrcBitmapHeight;

	public enum PointLocation {
		/**
		 * 左边上的点
		 */
		LT,

		/**
		 * 上边上的点
		 */
		TR,

		/**
		 * 右边上的点
		 */
		RB,

		/**
		 * 下边上的点
		 */
		BL;
	}
	
	private HashMap<PointLocation, Point> mPointMap;
	
	private float mRatio = -1;

	private Paint mPaint;

	private int mViewWidth;

	private int mViewHight;

	private HashMap<PointLocation, Rect> mAreaMap;

	private PointLocation mUsingKey;

	private float mVerticalOffset;
	private float mHorizontalOffset;

	private float mLastonTouchMoveEventX = -1;

	private float mLastonTouchMoveEventY = -1;

	public CropImageView(Context context) {
		super(context);
		mPaint = new Paint();
		
		mPaint.setStyle(Style.STROKE);//设置非填充
		mPaint.setStrokeWidth(6);//笔宽2像素
		mPaint.setColor(Color.RED);//设置为红笔
		mPaint.setAntiAlias(true);//锯齿不显示
		
		mAreaMap = new HashMap<PointLocation, Rect>();
	}
	
	public void setCropPiontMap(HashMap<PointLocation, Point> map) {
		mPointMap = map;
	}

	public void setRatio(float ratio) {
		this.mRatio = ratio;
	}

	public void setOffset(float horizontalOffset, float verticalOffset) {
		this.mHorizontalOffset = horizontalOffset;
		this.mVerticalOffset = verticalOffset;
	}

	public void setSrcImageWAndH(int width, int height) {
		mSrcBitmapWidth = width;
		mSrcBitmapHeight = height;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		Drawable d = getDrawable();
		mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
		mViewHight = MeasureSpec.getSize(heightMeasureSpec);

		calculatePoints();
		calculateOffsetAndRatio(mViewWidth, mViewHight);
	}

	private void calculateOffsetAndRatio(int width, int height) {
		float ratio;
		float horizontalOffset = 0f;
		float verticalOffset = 0f;
		// 计算缩放比
		if (mSrcBitmapWidth > width || mSrcBitmapHeight > height) {
			float viewAspectRatio = width / ((float) height);
			float bitmapAspectRatio = mSrcBitmapWidth / ((float) mSrcBitmapHeight);
			if (viewAspectRatio > bitmapAspectRatio) {
				// 卡高度
				ratio = (float) height / (float) mSrcBitmapHeight;
				float resultWidth = ((float) height) * bitmapAspectRatio;
				horizontalOffset = ((float) width - resultWidth) / 2.0F;
			} else {
				// 卡宽度
				ratio = (float) width / (float) mSrcBitmapWidth;
				float resultHeight = ((float) width) / bitmapAspectRatio;
				verticalOffset = ((float) height - resultHeight) / 2.0F;
			}
		} else {
			ratio = 1.0f;
			horizontalOffset = (width - mSrcBitmapWidth) / 2.0f;
			verticalOffset = (height - mSrcBitmapHeight) / 2.0f;
		}

		setRatio(ratio);
		setOffset(horizontalOffset, verticalOffset);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
//		canvas.drawLine(0, 0, 1000, 1000, mPaint);
		float startX = getPointXFromMap(PointLocation.LT) + mHorizontalOffset;
		float startY = getPointYFromMap(PointLocation.LT) + mVerticalOffset;
		float endX = getPointXFromMap(PointLocation.TR) + mHorizontalOffset;
		float endY = getPointYFromMap(PointLocation.TR) + mVerticalOffset;
		drawLine(canvas, startX, startY, endX, endY);
		Log.d(TAG, "1 startX : " + startX + " startY : " + startY + " endX : " + endX + " endY : " + endY);

		startX = getPointXFromMap(PointLocation.TR) + mHorizontalOffset;
		startY = getPointYFromMap(PointLocation.TR) + mVerticalOffset;
		endX = getPointXFromMap(PointLocation.RB) + mHorizontalOffset;
		endY = getPointYFromMap(PointLocation.RB) + mVerticalOffset;
		drawLine(canvas, startX, startY, endX, endY);
		Log.d(TAG, "2 startX : " + startX + " startY : " + startY + " endX : " + endX + " endY : " + endY);

		startX = getPointXFromMap(PointLocation.RB) + mHorizontalOffset;
		startY = getPointYFromMap(PointLocation.RB) + mVerticalOffset;
		endX = getPointXFromMap(PointLocation.BL) + mHorizontalOffset;
		endY = getPointYFromMap(PointLocation.BL) + mVerticalOffset;
		drawLine(canvas, startX, startY, endX, endY);
		Log.d(TAG, "3 startX : " + startX + " startY : " + startY + " endX : " + endX + " endY : " + endY);

		startX = getPointXFromMap(PointLocation.BL) + mHorizontalOffset;
		startY = getPointYFromMap(PointLocation.BL) + mVerticalOffset;
		endX = getPointXFromMap(PointLocation.LT) + mHorizontalOffset;
		endY = getPointYFromMap(PointLocation.LT) + mVerticalOffset;
		drawLine(canvas, startX, startY, endX, endY);
		Log.d(TAG, "4 startX : " + startX + " startY : " + startY + " endX : " + endX + " endY : " + endY);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mUsingKey = null;
			Set<Entry<PointLocation, Rect>> rectSet = mAreaMap.entrySet();
			Iterator<Entry<PointLocation, Rect>> it=rectSet.iterator();
			while(it.hasNext()) {
				Entry entry=(Entry)it.next();
				Rect rect = (Rect) entry.getValue();
				if (rect.contains((int) event.getX(), (int) event.getY())) {
					mUsingKey = (PointLocation) entry.getKey();
					break;
				}
			}
			mLastonTouchMoveEventX = event.getX();
			mLastonTouchMoveEventY = event.getY();
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mUsingKey = null;
			mLastonTouchMoveEventX = -1;
			mLastonTouchMoveEventY = -1;
			break;
			
		case MotionEvent.ACTION_MOVE:
			if (mLastonTouchMoveEventX != -1 && mLastonTouchMoveEventY != -1) {
				double moveLen = Math.sqrt(Math.pow(event.getX() - mLastonTouchMoveEventX, 2) + Math.pow(event.getY() - mLastonTouchMoveEventY, 2));
				if (moveLen > 3) {
					updataPoint(event.getX(), event.getY());
					calculateRect();
					invalidate();
				}
			}
			break;

		default:
			break;
		}
		
		
		return true;
	}
	
	public void release () {
		mAreaMap.clear();
		mLastonTouchMoveEventX = -1;
		mLastonTouchMoveEventY = -1;
		mVerticalOffset = 0;
		mHorizontalOffset = 0;
		if (mPointMap != null) {
			mPointMap.clear();
		}
		mUsingKey = null;
		mRatio = 0;
	}
	
	private void updataPoint(float x, float y) {
		Point point = mPointMap.get(mUsingKey);
		if (point == null) {
			return;
		}
		point.x = (int) (point.x + (x - mLastonTouchMoveEventX) / mRatio + 0.5);
		point.y = (int) (point.y + (y - mLastonTouchMoveEventY) / mRatio + 0.5);
		mLastonTouchMoveEventX = x;
		mLastonTouchMoveEventY = y;
		Log.d(TAG, "onTouchEvent change " + point);
	}

	private void drawLine(Canvas canvas, float startX, float startY, float stopX, float stopY) {
		if (canvas == null) {
			return;
		}
		canvas.drawLine(startX, startY, stopX, stopY, mPaint);
	}

	private float getPointXFromMap(PointLocation pl) {
		if (mPointMap == null || mPointMap.isEmpty() || mRatio == 0) {
			return 0;
		}
		Point point = mPointMap.get(pl);
		return point.x * mRatio;
	}
	
	private float getPointYFromMap(PointLocation pl) {
		if (mPointMap == null || mPointMap.isEmpty() || mRatio == 0) {
			return 0;
		}
		Point piont = mPointMap.get(pl);
		
		return piont.y * mRatio;
	}
	
	private float getXFromPoint(Point point) {
		return point.x * mRatio +  + mHorizontalOffset;
	}
	
	private float getYFromPoint(Point point) {
		return point.y * mRatio + mVerticalOffset;
	}
	
	private void calculatePoints() {
		if (mPointMap == null || mPointMap.isEmpty()) {
			return;
		}
		
		calculateRect();
	}
	
	private void calculateRect() {
		Point ltPoint = mPointMap.get(PointLocation.LT);
		Point trPoint = mPointMap.get(PointLocation.TR);
		Point rbPoint = mPointMap.get(PointLocation.RB);
		Point blPoint = mPointMap.get(PointLocation.BL);
		
		mAreaMap.put(PointLocation.LT, getRectFromPoint(ltPoint));
		
		mAreaMap.put(PointLocation.TR, getRectFromPoint(trPoint));
		
		mAreaMap.put(PointLocation.RB, getRectFromPoint(rbPoint));
		
		mAreaMap.put(PointLocation.BL, getRectFromPoint(blPoint));
		
	}

	private Rect getRectFromPoint(Point point) {
		float minLen = dip2px(getContext(), 40f);
		int left = (int) (getXFromPoint(point) - minLen + 0.5);
		if (left < 0) {
			left = 0;
		}
		int right = (int) (getXFromPoint(point) + minLen + 0.5);
		if (right > mViewWidth) {
			right = mViewWidth;
		}
		int top = (int) (getYFromPoint(point) - minLen + 0.5);
		if (top < 0) {
			top = 0;
		}
		int bottom = (int) (getYFromPoint(point) + minLen + 0.5);
		if (bottom > mViewHight) {
			bottom = mViewHight;
		}
		
		Rect rect = new Rect(left, top, right, bottom);
		return rect;
	}
	
	private static float dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return dipValue * scale;
	}

}
