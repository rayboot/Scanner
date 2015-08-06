package com.rayboot.scantool.cv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Created by wswd on 2015/7/22.
 */
public class OpenCVNative {
    public static native int[][] nScan(String path);
    public static native void nCrop(String path, int[][] pointArray, int[] resultImageSide, String resultPath);
    public static native int[][] nScanFromMat(long srcMat);
    public static native void nCrop(Mat srcMat, Rect rect, Mat resultMat);
}
