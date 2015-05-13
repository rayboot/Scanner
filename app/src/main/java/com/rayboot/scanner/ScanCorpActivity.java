package com.rayboot.scanner;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.rayboot.scantool.constant.OpenCVConstant;
import com.rayboot.scantool.cv.ImageHandleListener;
import com.rayboot.scantool.cv.OpenCVManager;
import com.rayboot.scantool.view.CropImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import java.io.File;
import java.util.HashMap;


public class ScanCorpActivity extends ActionBarActivity implements ImageHandleListener {

    private static final String TAG = "ScanHandleActivity";
    private static final int MSG_SCAN_FINISH = 0;
    private static final int MSG_CROP_FINISH = 1;

    private OpenCVManager mOpenCVManager;
    private String mContentImagePath;
    private int[] mResultImageSide;

    private Bitmap mScrBitmap;

    private HashMap<CropImageView.PointLocation, Point> mPointMap;
    private LinearLayout mContentLayout;

    private ProgressDialog mProgressDialog;
    String resultImagePath;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SCAN_FINISH:
                    mScrBitmap = null;
                    mScrBitmap = BitmapFactory.decodeFile(mContentImagePath);
                    if (mScrBitmap != null) {
                        CropImageView view = new CropImageView(ScanCorpActivity.this);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        view.setLayoutParams(params);
                        view.setImageBitmap(mScrBitmap);
                        view.setCropPiontMap(getPiontMap());
                        mContentLayout.addView(view);
                    }

                    dismissProgressDialog();
                    break;

                case MSG_CROP_FINISH:
                    dismissProgressDialog();
                    break;

                default:
                    break;
            }
        };
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    File file = new File(mContentImagePath);

                    if (file.exists()) {
                        //若该文件存在
                        mOpenCVManager.findBrim(mContentImagePath);

                        showProgressDialog(getString(R.string.scan_dialog_title), getString(R.string.scan_dialog_msg));
                    }
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_scan_corp, null);
        setContentView(mContentLayout);
        Intent i = getIntent();
        mContentImagePath = i.getStringExtra(OpenCVConstant.KEY_SCAN_SRC_IMG);
        mResultImageSide = i.getIntArrayExtra(OpenCVConstant.KEY_HANDLED_IMAGE_SIDE);
        resultImagePath = i.getStringExtra(OpenCVConstant.KEY_SCAN_RESULT_IMG);

        Button mCropBtn = (Button) findViewById(R.id.crop_btn);
        mCropBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File file = new File(resultImagePath);
                if (file.exists()) {
                    file.delete();
                }
                mOpenCVManager.autoCropImage(mContentImagePath, mPointMap, resultImagePath);
                showProgressDialog(getString(R.string.crop_dialog_title), getString(R.string.crop_dialog_msg));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mOpenCVManager = new OpenCVManager(mLoaderCallback);
        mOpenCVManager.init();
        mOpenCVManager.setImageHandleListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScrBitmap.recycle();
    }

    private void showProgressDialog(CharSequence title, CharSequence message) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private HashMap<CropImageView.PointLocation, Point> getPiontMap() {
        if (mPointMap != null) {
            return mPointMap;
        }
        mPointMap = new HashMap<CropImageView.PointLocation, Point>();
        Point ltPoint = mOpenCVManager.getLTPoint();
        mPointMap.put(CropImageView.PointLocation.LT, ltPoint);

        Point blPoint = mOpenCVManager.getBLPoint();
        mPointMap.put(CropImageView.PointLocation.BL, blPoint);

        Point rbPoint = mOpenCVManager.getRBPiont();
        mPointMap.put(CropImageView.PointLocation.RB, rbPoint);

        Point trPoint = mOpenCVManager.getTRPoint();
        mPointMap.put(CropImageView.PointLocation.TR, trPoint);
        return mPointMap;
    }

    @Override
    public void onScanFinish() {
        mHandler.sendEmptyMessage(MSG_SCAN_FINISH);
    }

    @Override
    public void onCropFinish() {
        mHandler.sendEmptyMessage(MSG_CROP_FINISH);
    }

}
