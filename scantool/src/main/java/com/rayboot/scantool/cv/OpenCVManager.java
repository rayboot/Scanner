package com.rayboot.scantool.cv;

import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.rayboot.scantool.view.CropImageView;

import java.util.HashMap;


/**
 * Created by Administrator on 2015/4/21.
 */
public class OpenCVManager {
    
    static {
    	System.loadLibrary("opencv_java");
    	System.loadLibrary("scan_tool");
    }

    private static final String TAG = "OpenCVManager";
    private static final String THREAD_NAME = "Image_Handle";
    private static final int MSG_IMAGE_SCAN = 1;
    private static final int MSG_IMAGE_CROP = 2;
    
    private org.opencv.android.BaseLoaderCallback mLoaderCallback;
	private boolean isInit = false;
	private int[][] mPointArray;
	
	private HandlerThread mImageHandle;
	private ImageHandler mHandler;
	private ImageHandleListener mImageHandleListener;
	
	private String mCropImgPath;
	private int[][] mCropPointArray;
	private int[] mResultImageSide;
	
	
	
	class ImageHandler extends Handler {
		private OpenCVManager mOpenCVManager;

		public ImageHandler(OpenCVManager opencv, Looper looper) {
			super(looper);
			mOpenCVManager = opencv;
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_IMAGE_SCAN:
				String path = (String) msg.obj;
				mPointArray = mOpenCVManager.nScan(path);
				mImageHandleListener.onScanFinish();
				break;
			case MSG_IMAGE_CROP:
				String resultPath = (String) msg.obj;
				nCrop(mCropImgPath, mCropPointArray, mResultImageSide, resultPath);
				mImageHandleListener.onCropFinish();
				break;

			default:
				break;
			}
			
		}
	}

    public OpenCVManager (org.opencv.android.BaseLoaderCallback loaderCallback) {
        mLoaderCallback = loaderCallback;
        mImageHandle = new HandlerThread(THREAD_NAME);
        mImageHandle.start();
        mHandler = new ImageHandler(this, mImageHandle.getLooper());
    }

    public void init() {
        if(!isInit && org.opencv.android.OpenCVLoader.initDebug()){
            mLoaderCallback.onManagerConnected(org.opencv.android.LoaderCallbackInterface.SUCCESS);
            isInit = true;
        }
    }

    public void findBrim(final String path) {
    	Log.d("OpenCVManager", "path = " + path);
    	mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_SCAN, path));
			}
		});
//    	mPointArray = nScan(path);
    }
    
	public Point getLTPoint() {
    	if (mPointArray == null) {
    		return null;
    	}
    	int[] array = mPointArray[0];
    	if (array == null) {
    		return null;
    	}
    	return new Point(array[0], array[1]);
	}
    
    public Point getTRPoint() {
    	if (mPointArray == null) {
    		return null;
    	}
    	int[] array = mPointArray[1];
    	if (array == null) {
    		return null;
    	}
    	return new Point(array[0], array[1]);
	}
    
    public Point getRBPiont() {
    	if (mPointArray == null) {
    		return null;
    	}
    	int[] array = mPointArray[2];
    	if (array == null) {
    		return null;
    	}
    	return new Point(array[0], array[1]);
	}
    
    public Point getBLPoint() {
    	if (mPointArray == null) {
    		return null;
    	}
    	int[] array = mPointArray[3];
    	if (array == null) {
    		return null;
    	}
    	return new Point(array[0], array[1]);
	}

	public void cropImage(String path, HashMap<CropImageView.PointLocation,Point> pointMap, int[] resultImageSide, final String resultPath){
		if (pointMap == null || pointMap.size() < 4) {
			return;
		}
		mCropImgPath = path;
		mCropPointArray = new int[4][2];
		mCropPointArray[0][0] = pointMap.get(CropImageView.PointLocation.LT).x;
		mCropPointArray[0][1] = pointMap.get(CropImageView.PointLocation.LT).y;
		mCropPointArray[1][0] = pointMap.get(CropImageView.PointLocation.TR).x;
		mCropPointArray[1][1] = pointMap.get(CropImageView.PointLocation.TR).y;
		mCropPointArray[2][0] = pointMap.get(CropImageView.PointLocation.RB).x;
		mCropPointArray[2][1] = pointMap.get(CropImageView.PointLocation.RB).y;
		mCropPointArray[3][0] = pointMap.get(CropImageView.PointLocation.BL).x;
		mCropPointArray[3][1] = pointMap.get(CropImageView.PointLocation.BL).y;
		mResultImageSide = resultImageSide;
		Log.d(TAG, "cropImage resultImageSide: Length = " + resultImageSide[0] + ", width = " + resultImageSide[1]);
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_CROP, resultPath));
			}
		});
	}

	public void autoCropImage(String path, HashMap<CropImageView.PointLocation,Point> pointMap, final String resultPath){
		if (pointMap == null || pointMap.size() < 4) {
			return;
		}
		mCropImgPath = path;
		mCropPointArray = new int[4][2];
		mCropPointArray[0][0] = pointMap.get(CropImageView.PointLocation.LT).x;
		mCropPointArray[0][1] = pointMap.get(CropImageView.PointLocation.LT).y;
		mCropPointArray[1][0] = pointMap.get(CropImageView.PointLocation.TR).x;
		mCropPointArray[1][1] = pointMap.get(CropImageView.PointLocation.TR).y;
		mCropPointArray[2][0] = pointMap.get(CropImageView.PointLocation.RB).x;
		mCropPointArray[2][1] = pointMap.get(CropImageView.PointLocation.RB).y;
		mCropPointArray[3][0] = pointMap.get(CropImageView.PointLocation.BL).x;
		mCropPointArray[3][1] = pointMap.get(CropImageView.PointLocation.BL).y;
		Point autoWH = getAutoWH();
		mResultImageSide[0] = autoWH.x;
		mResultImageSide[1] = autoWH.y;
		Log.d(TAG, "cropImage resultImageSide: Length = " + mResultImageSide[0] + ", width = " + mResultImageSide[1]);
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_CROP, resultPath));
			}
		});
	}

	public Point getAutoWH() {
		int  l = getLine(mCropPointArray[0][0], mCropPointArray[0][1], mCropPointArray[3][0], mCropPointArray[3][1]);
		int  t = getLine(mCropPointArray[0][0], mCropPointArray[0][1], mCropPointArray[1][0], mCropPointArray[1][1]);
		int  r = getLine(mCropPointArray[1][0], mCropPointArray[1][1], mCropPointArray[2][0], mCropPointArray[2][1]);
		int  b = getLine(mCropPointArray[2][0], mCropPointArray[2][1], mCropPointArray[3][0], mCropPointArray[3][1]);
		return new Point(Math.max(l, r), Math.max(t, b));
	}

	public int getLine(int x, int y, int tx, int ty) {
		return (int) Math.sqrt((x - tx)*(x-tx) + (y - ty)*(y-ty));
	}

	public void setImageHandleListener(ImageHandleListener listener) {
		mImageHandleListener = listener;
	}
    
    private static native int[][] nScan(String path);
    private static native void nCrop(String path, int[][] pointArray, int[] resultImageSide, String resultPath);

}
