LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include $(LOCAL_PATH)/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := com_rayboot_scantool_cv_OpenCVManager.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := scan_tool

include $(BUILD_SHARED_LIBRARY)