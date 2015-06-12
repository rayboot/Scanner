#include <com_rayboot_scantool_cv_OpenCVManager.h>
#include <scannerLite.hpp>

JNIEXPORT jobjectArray JNICALL Java_com_rayboot_scantool_cv_OpenCVManager_nScan(JNIEnv *env, jclass obj, jstring prompt)
{
	vector<vector<Point> > squares;
	find_squares2(env->GetStringUTFChars(prompt, 0), squares);

	int size = 4;

	jobjectArray result;

	jclass intArrCls = env->FindClass("[I");

	result = env->NewObjectArray(size, intArrCls, NULL);

    if (squares.size() < 1)
	{
		return NULL;
	}
	vector<Point> v = squares[0];
    if (v.size() < size)
    {
        return NULL;
    }
	vector<Point> resultVector;
	findPointFromVector(v, resultVector);
	for (int i = 0 ; i < size ; i ++)
	{
		Point point = resultVector[i];
		jint tmp[2]; /* make sure it is large enough! */
		tmp[0] = point.x;
		tmp[1] = point.y;

		jintArray iarr = env->NewIntArray(2);

		env->SetIntArrayRegion(iarr, 0, 2, tmp);

		env->SetObjectArrayElement(result, i, iarr);

		env->DeleteLocalRef(iarr);
	}
	return result;
}

JNIEXPORT void JNICALL Java_com_rayboot_scantool_cv_OpenCVManager_nCrop(JNIEnv *env, jclass obj, jstring prompt, jobjectArray array, jintArray sides, jstring result)
{
	LOGD("jni nCrop start");
	if(prompt == NULL || array == NULL)
	{
		return;
	}
	int i,j;

	int row = env->GetArrayLength(array);

	jarray myarray = (jarray) (env->GetObjectArrayElement(array, 0));

	int col =env->GetArrayLength(myarray);

	vector<Point> jniData(row);

	LOGD("jni fuzhi op start. row = %i, col = %i", row, col);
	for (i = 0; i < row; i++){
	  LOGD("jni fuzhi op doing. i = %i", i);
	  myarray = (jarray) (env->GetObjectArrayElement(array, i));

	  jint *coldata = env->GetIntArrayElements((jintArray)myarray, 0 );

	  if (col >= 2)
	  {
		  LOGD("jni fuzhi op doing. coldata[0] = %i, coldata[1] = %i", coldata[0], coldata[1]);
		  jniData[i] = Point(coldata[0], coldata[1]);
	  }
	  env->ReleaseIntArrayElements((jintArray)myarray, coldata, 0);

	}

	int len = env->GetArrayLength(sides);
	int jniside[len];
	for(i=0;i<len;i++)
	{
		jniside[i] = env->GetIntArrayElements(sides, 0)[i];
	}
	LOGD("jni crop op start");
	crop(env->GetStringUTFChars(prompt, 0), jniData, jniside, env->GetStringUTFChars(result, 0));
	LOGD("jni nCrop end");
}
