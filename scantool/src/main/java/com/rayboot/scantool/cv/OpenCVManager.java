package com.rayboot.scantool.cv;

import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.rayboot.scantool.view.CropImageView;

import org.opencv.core.Mat;

import java.util.HashMap;


/**
 * openCV调用入口类
 * Created by wudi on 2015/4/21.
 */
public class OpenCVManager {

	private static OpenCVManager mInstance;

	static {
    	System.loadLibrary("opencv_java");
    	System.loadLibrary("scan_tool");
    }

    private static final String TAG = "OpenCVManager";
    private static final String THREAD_NAME = "Image_Handle";
    private static final int MSG_IMAGE_SCAN = 1;
    private static final int MSG_IMAGE_CROP = 2;
	private static final int MSG_MAT_SCAN = 3;

	private boolean isInit = false;
	private int[][] mPointArray;

	private HandlerThread mImageHandle;
	private ImageHandler mHandler;
	private ImageHandleListener mImageHandleListener;

	private MatHandleListener mMatHandleListener;

	private String mCropImgPath;
	private int[][] mCropPointArray;
	private int[] mResultImageSide;
	private boolean mCancel;
	private long mCurTime;


	class ImageHandler extends Handler {

		public ImageHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if (mCancel) {
				return;
			}
			switch (msg.what) {
			case MSG_IMAGE_SCAN:
				// 图片扫描
				String path = (String) msg.obj;
				mPointArray = OpenCVNative.nScan(path);
				mImageHandleListener.onScanFinish();
				break;
			case MSG_IMAGE_CROP:
				// 图片裁剪
				String resultPath = (String) msg.obj;
				OpenCVNative.nCrop(mCropImgPath, mCropPointArray, mResultImageSide, resultPath);
				mImageHandleListener.onCropFinish();
				break;
			case MSG_MAT_SCAN:
				// Mat扫描
				long time = System.currentTimeMillis();
				mCurTime = time;
				Mat mat = (Mat) msg.obj;
				int[][] result = OpenCVNative.nScanFromMat(mat.nativeObj);
				mat.release();
				if (mMatHandleListener != null && time == mCurTime) {
					mMatHandleListener.onScanMatFinish(result);
				}
				break;

			default:
				break;
			}

		}
	}

    private OpenCVManager () {
        mImageHandle = new HandlerThread(THREAD_NAME);
        mImageHandle.start();
        mHandler = new ImageHandler(mImageHandle.getLooper());
    }

	public static OpenCVManager getInstance() {
		if (mInstance == null) {
			mInstance = new OpenCVManager();
		}
		return mInstance;
	}

	public void init(BaseLoaderCallback loaderCallback) {
        if(!isInit){
			loaderCallback.onManagerConnected();
            isInit = true;
        }
    }

	/**
	 * 根据图片路径找边
	 * @param path
	 */
    public void findBrim(final String path) {
    	Log.d("OpenCVManager", "path = " + path);
    	mHandler.post(new Runnable() {

			@Override
			public void run() {
				mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_SCAN, path));
			}
		});
    }

	/**
	 * 从扫描结果中获取左上点
	 * @return
	 */
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

	/**
	 * 从扫描结果中获取上右点
	 * @return
	 */
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

	/**
	 * 从扫描结果中获取右下点
	 * @return
	 */
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

	/**
	 * 从扫描结果中获取下左点
	 * @return
	 */
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

	/**
	 * 根据指定的分辨率截图
	 * @param path
	 * @param pointMap
	 * @param resultImageSide
	 * @param resultPath
	 */
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
		mHandler.removeMessages(MSG_IMAGE_SCAN);
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_CROP, resultPath));
			}
		});
	}

	/**
	 * 自适应分辨率截图
	 * @param path
	 * @param pointMap
	 * @param resultPath
	 */
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
		if (mResultImageSide == null || mResultImageSide.length < 2) {
			mResultImageSide = new int[2];
		}
		mResultImageSide[0] = autoWH.x;
		mResultImageSide[1] = autoWH.y;
		Log.d(TAG, "cropImage resultImageSide: Length = " + mResultImageSide[0] + ", width = " + mResultImageSide[1]);
		mHandler.removeMessages(MSG_IMAGE_CROP);
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				mHandler.sendMessage(Message.obtain(mHandler, MSG_IMAGE_CROP, resultPath));
			}
		});
	}

	private Point getAutoWH() {
		double w = Math.min(distancePointToPoint(mCropPointArray[0][0], mCropPointArray[0][1], mCropPointArray[1][0], mCropPointArray[1][1]),
				distancePointToPoint(mCropPointArray[2][0], mCropPointArray[2][1], mCropPointArray[3][0], mCropPointArray[3][1]));

		double h = Math.max(distancePointToLine(mCropPointArray[0][0], mCropPointArray[0][1],
				mCropPointArray[2][0], mCropPointArray[2][1], mCropPointArray[3][0], mCropPointArray[3][1]), distancePointToLine(mCropPointArray[1][0], mCropPointArray[1][1],
				mCropPointArray[2][0], mCropPointArray[2][1], mCropPointArray[3][0], mCropPointArray[3][1]));
		return new Point((int) w, (int) h);
	}

	/**
	 * 计算点到线的距离
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static double distancePointToLine(float x0, float y0, float x1, float y1, float x2, float y2) {
		double distance = Math.abs((y2 - y1) * x0 + (x1 - x2) * y0 + x2 * y1 - x1 * y2) / Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x1 - x2, 2));

		return distance;
	}

	/**
	 * 计算点到点的距离
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static double distancePointToPoint(float x1, float y1, float x2, float y2) {
		return Math.sqrt((x1 - x2)*(x1-x2) + (y1 - y2)*(y1-y2));
	}

	public void setImageHandleListener(ImageHandleListener listener) {
		mImageHandleListener = listener;
	}

	public void setMatHandleListener(MatHandleListener matHandleListener) {
		this.mMatHandleListener = matHandleListener;
	}

	/**
	 * 通过mat找边
	 * @param mat
	 */
	public void scanFromMat(Mat mat) {
		if (mat == null || mat.rows() <= 0 || mat.cols() <= 0) {
			return;
		}
		Mat tmpMat = new Mat();
		mat.copyTo(tmpMat);
		mHandler.removeMessages(MSG_MAT_SCAN);
		Message msg = Message.obtain(mHandler, MSG_MAT_SCAN, tmpMat);
		mHandler.sendMessage(msg);
	}

	public void reset() {
		mCropImgPath = null;
		mCropPointArray = null;
		mPointArray = null;
		mResultImageSide = null;
		isInit = false;
	}

	public void release() {
		reset();
		mCancel = true;
		mHandler.removeMessages(MSG_IMAGE_SCAN);
		mHandler.removeMessages(MSG_IMAGE_CROP);
		mHandler.removeMessages(MSG_MAT_SCAN);
		mHandler = null;
		mImageHandle.quit();
		mImageHandle = null;
		mInstance = null;
	}

}
