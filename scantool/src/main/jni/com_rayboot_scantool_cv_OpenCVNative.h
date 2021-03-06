/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_rayboot_scantool_cv_OpenCVNative */

#ifndef _Included_com_rayboot_scantool_cv_OpenCVNative
#define _Included_com_rayboot_scantool_cv_OpenCVNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_rayboot_scantool_cv_OpenCVNative
 * Method:    nScan
 * Signature: (Ljava/lang/String;)[[I
 */
JNIEXPORT jobjectArray JNICALL Java_com_rayboot_scantool_cv_OpenCVNative_nScan
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_rayboot_scantool_cv_OpenCVNative
 * Method:    nCrop
 * Signature: (Ljava/lang/String;[[I[ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_rayboot_scantool_cv_OpenCVNative_nCrop__Ljava_lang_String_2_3_3I_3ILjava_lang_String_2
  (JNIEnv *, jclass, jstring, jobjectArray, jintArray, jstring);

/*
 * Class:     com_rayboot_scantool_cv_OpenCVNative
 * Method:    nScanFromMat
 * Signature: (J)[[I
 */
JNIEXPORT jobjectArray JNICALL Java_com_rayboot_scantool_cv_OpenCVNative_nScanFromMat
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_rayboot_scantool_cv_OpenCVNative
 * Method:    nCrop
 * Signature: (Lorg/opencv/core/Mat;Lorg/opencv/core/Rect;Lorg/opencv/core/Mat;)V
 */
JNIEXPORT void JNICALL Java_com_rayboot_scantool_cv_OpenCVNative_nCrop__Lorg_opencv_core_Mat_2Lorg_opencv_core_Rect_2Lorg_opencv_core_Mat_2
  (JNIEnv *, jclass, jobject, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif
