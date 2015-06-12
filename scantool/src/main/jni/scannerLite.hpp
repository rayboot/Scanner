/* 
 * File: scannerLite.cpp
 * Author: daisygao
 * An OpenCV program implementing the recognition feature of the app "CamScanner". 
 * It extracts the main document object from an image and adjust it to A4 size. 
 */
#include <opencv2/opencv.hpp>
#include <algorithm>
#include <string>
#include <vector>
#include <android/log.h>
#include <math.h>
using namespace cv;
using namespace std;

#define  LOG_TAG    "OCV:scanner_lite"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

/**
 * Get edges of an image
 * @param gray - grayscale input image
 * @param canny - output edge image
 */
void getCanny(Mat gray, Mat &canny) {
  Mat thres;
  double high_thres = threshold(gray, thres, 0, 255, CV_THRESH_BINARY | CV_THRESH_OTSU), low_thres = high_thres * 0.5;
  cv::Canny(gray, canny, low_thres, high_thres);
}

struct Line {
  Point _p1;
  Point _p2;
  Point _center;

  Line(Point p1, Point p2) {
	_p1 = p1;
	_p2 = p2;
	_center = Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
  }
};

bool cmp_y(const Line &p1, const Line &p2) {
  return p1._center.y < p2._center.y;
}

bool cmp_x(const Line &p1, const Line &p2) {
  return p1._center.x < p2._center.x;
}

/**
 * Compute intersect point of two lines l1 and l2
 * @param l1
 * @param l2
 * @return Intersect Point
 */
Point2f computeIntersect(Line l1, Line l2) {
  int x1 = l1._p1.x, x2 = l1._p2.x, y1 = l1._p1.y, y2 = l1._p2.y;
  int x3 = l2._p1.x, x4 = l2._p2.x, y3 = l2._p1.y, y4 = l2._p2.y;
  if (float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)) {
	Point2f pt;
	pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
	pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
	return pt;
  }
  return Point2f(-1, -1);
}

/**
 * 方法一
 */
void scan(const char* file, bool debug = true) {
	LOGD("path: %s \n",file);
  /* get input image */
  Mat img = imread(file);
  // resize input image to img_proc to reduce computation
  Mat img_proc;
  int w = img.size().width, h = img.size().height, min_w = 200;
  double scale = min(10.0, w * 1.0 / min_w);
  int w_proc = w * 1.0 / scale, h_proc = h * 1.0 / scale;
  if (w_proc == 0)
  {
	  LOGD("w_proc is 0");
  }

  if (h_proc == 0)
  {
    LOGD("h_proc is 0");
  }

  if (scale == 0)
  {
    LOGD("scale is 0");
  }

  if (w == 0)
  {
    LOGD("w is 0");
  }

  if (h == 0)
  {
    LOGD("h is 0");
  }

  resize(img, img_proc, Size(w_proc, h_proc));
  Mat img_dis = img_proc.clone();

  /* get four outline edges of the document */
  // get edges of the image
  Mat gray, canny;
  cvtColor(img_proc, gray, CV_BGR2GRAY);
  getCanny(gray, canny);

  // extract lines from the edge image
  vector<Vec4i> lines;
  vector<Line> horizontals, verticals;
  HoughLinesP(canny, lines, 1, CV_PI / 180, w_proc / 3, w_proc / 3, 20);
  for (size_t i = 0; i < lines.size(); i++) {
	Vec4i v = lines[i];
	double delta_x = v[0] - v[2], delta_y = v[1] - v[3];
	Line l(Point(v[0], v[1]), Point(v[2], v[3]));
	// get horizontal lines and vertical lines respectively
	if (fabs(delta_x) > fabs(delta_y)) {
	  horizontals.push_back(l);
	} else {
	  verticals.push_back(l);
	}
	// for visualization only
	if (debug)
	  line(img_proc, Point(v[0], v[1]), Point(v[2], v[3]), Scalar(0, 0, 255), 1, CV_AA);
  }

  // edge cases when not enough lines are detected
  if (horizontals.size() < 2) {
	if (horizontals.size() == 0 || horizontals[0]._center.y > h_proc / 2) {
	  horizontals.push_back(Line(Point(0, 0), Point(w_proc - 1, 0)));
	}
	if (horizontals.size() == 0 || horizontals[0]._center.y <= h_proc / 2) {
	  horizontals.push_back(Line(Point(0, h_proc - 1), Point(w_proc - 1, h_proc - 1)));
	}
  }
  if (verticals.size() < 2) {
	if (verticals.size() == 0 || verticals[0]._center.x > w_proc / 2) {
	  verticals.push_back(Line(Point(0, 0), Point(0, h_proc - 1)));
	}
	if (verticals.size() == 0 || verticals[0]._center.x <= w_proc / 2) {
	  verticals.push_back(Line(Point(w_proc - 1, 0), Point(w_proc - 1, h_proc - 1)));
	}
  }
  // sort lines according to their center point
  sort(horizontals.begin(), horizontals.end(), cmp_y);
  sort(verticals.begin(), verticals.end(), cmp_x);
  // for visualization only
  if (debug) {
	line(img_proc, horizontals[0]._p1, horizontals[0]._p2, Scalar(0, 255, 0), 2, CV_AA);
	line(img_proc, horizontals[horizontals.size() - 1]._p1, horizontals[horizontals.size() - 1]._p2, Scalar(0, 255, 0), 2, CV_AA);
	line(img_proc, verticals[0]._p1, verticals[0]._p2, Scalar(255, 0, 0), 2, CV_AA);
	line(img_proc, verticals[verticals.size() - 1]._p1, verticals[verticals.size() - 1]._p2, Scalar(255, 0, 0), 2, CV_AA);
  }

  /* perspective transformation */

  // define the destination image size: A4 - 200 PPI
  int w_a4 = 1654, h_a4 = 2339;
  //int w_a4 = 595, h_a4 = 842;
  Mat dst = Mat::zeros(h_a4, w_a4, CV_8UC3);

  // corners of destination image with the sequence [tl, tr, bl, br]
  vector<Point2f> dst_pts, img_pts;
  dst_pts.push_back(Point(0, 0));
  dst_pts.push_back(Point(w_a4 - 1, 0));
  dst_pts.push_back(Point(0, h_a4 - 1));
  dst_pts.push_back(Point(w_a4 - 1, h_a4 - 1));

  // corners of source image with the sequence [tl, tr, bl, br]
  img_pts.push_back(computeIntersect(horizontals[0], verticals[0]));
  img_pts.push_back(computeIntersect(horizontals[0], verticals[verticals.size() - 1]));
  img_pts.push_back(computeIntersect(horizontals[horizontals.size() - 1], verticals[0]));
  img_pts.push_back(computeIntersect(horizontals[horizontals.size() - 1], verticals[verticals.size() - 1]));

  // convert to original image scale
  for (size_t i = 0; i < img_pts.size(); i++) {
	// for visualization only
	if (debug) {
	  circle(img_proc, img_pts[i], 10, Scalar(255, 255, 0), 3);
	}
	img_pts[i].x *= scale;
	img_pts[i].y *= scale;
  }

  // get transformation matrix
  Mat transmtx = getPerspectiveTransform(img_pts, dst_pts);

  // apply perspective transformation
  warpPerspective(img, dst, transmtx, dst.size());

  // save dst img
  imwrite("/sdcard/doc2.jpg", dst);

  // for visualization only
}

/**
 * 方法二
 */
void find_squares(const char* file)
{
	IplImage* pImgSrc = NULL;    //源图像
	IplImage* pImg8u = NULL;     //灰度图
	IplImage* pImgCanny = NULL;  //边缘检测后的图
	IplImage* pImgDst = NULL;    //在图像上画上检测到的直线后的图像
	CvSeq* lines = NULL;
	CvMemStorage* storage = NULL;

	/*边缘检测*/
	pImgSrc = cvLoadImage (file, 1);
	pImg8u = cvCreateImage (cvGetSize(pImgSrc), IPL_DEPTH_8U, 1);
	pImgCanny = cvCreateImage (cvGetSize(pImgSrc), IPL_DEPTH_8U, 1);
	pImgDst = cvCreateImage (cvGetSize(pImgSrc), IPL_DEPTH_8U, 1);
	cvCvtColor (pImgSrc, pImg8u, CV_BGR2GRAY);
	cvCanny (pImg8u, pImgCanny, 20, 200, 3);

	/*检测直线*/
	storage = cvCreateMemStorage (0);
	lines = cvHoughLines2 (pImgCanny, storage, CV_HOUGH_PROBABILISTIC, 1, CV_PI/180, 80, 200, 10);
	pImgDst = cvCreateImage (cvGetSize(pImgSrc), IPL_DEPTH_8U, 3);
	cvCvtColor (pImg8u, pImgDst, CV_GRAY2BGR);

	/*在pImgDst上画出检测到的直线*/
	for (int i = 0; i < lines->total; i++)
	{
		CvPoint* line = (CvPoint*)cvGetSeqElem (lines, i);
		cvLine (pImgDst, line[0], line[1], CV_RGB(255,0,0), 3, 8);
	}
	Mat mat = Mat(pImgDst);
	imwrite(file, mat);

	cvReleaseImage (&pImgSrc);
	cvReleaseImage (&pImg8u);
	cvReleaseImage (&pImgCanny);
	cvReleaseImage (&pImgDst);
	cvReleaseMemStorage (&storage);
}

double angle(cv::Point pt1, cv::Point pt2, cv::Point pt0 ) {
    double dx1 = pt1.x - pt0.x;
    double dy1 = pt1.y - pt0.y;
    double dx2 = pt2.x - pt0.x;
    double dy2 = pt2.y - pt0.y;
    return (dx1*dx2 + dy1*dy2)/sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
}

/**
 * 方法三
 */
void find_squares2(const char* file, vector<vector<Point> >& squares)
{
	LOGD("path: %s \n",file);
	Mat image = imread(file);
    // blur will enhance edge detection
    Mat blurred(image);
    medianBlur(image, blurred, 9);

    Mat gray0(blurred.size(), CV_8U), gray;
    vector<vector<Point> > contours;

    // find squares in every color plane of the image
    for (int c = 0; c < 3; c++)
    {
        int ch[] = {c, 0};
        mixChannels(&blurred, 1, &gray0, 1, ch, 1);

        // try several threshold levels
        const int threshold_level = 2;
        for (int l = 0; l < threshold_level; l++)
        {
            // Use Canny instead of zero threshold level!
            // Canny helps to catch squares with gradient shading
            if (l == 0)
            {
                Canny(gray0, gray, 10, 20, 3); //

                // Dilate helps to remove potential holes between edge segments
                dilate(gray, gray, Mat(), Point(-1,-1));
            }
            else
            {
                    gray = gray0 >= (l+1) * 255 / threshold_level;
            }

            // Find contours and store them in a list
            findContours(gray, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

            // Test contours
            vector<Point> approx;
            for (size_t i = 0; i < contours.size(); i++)
            {
                    // approximate contour with accuracy proportional
                    // to the contour perimeter
                    approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);

                    // Note: absolute value of an area is used because
                    // area may be positive or negative - in accordance with the
                    // contour orientation
                    if (approx.size() == 4 &&
                            fabs(contourArea(Mat(approx))) > 1000 &&
                            isContourConvex(Mat(approx)))
                    {
                            double maxCosine = 0;

                            for (int j = 2; j < 5; j++)
                            {
                                    double cosine = fabs(angle(approx[j%4], approx[j-2], approx[j-1]));
                                    maxCosine = MAX(maxCosine, cosine);
                            }

                            if (maxCosine < 0.3)
                                    squares.push_back(approx);
                    }
            }
        }
    }
    if(squares.size() == 0)
    {
    	vector<Point> approx;
    	approx.push_back(Point(0,0));
    	approx.push_back(Point(image.size().width,0));
    	approx.push_back(Point(image.size().width,image.size().height));
    	approx.push_back(Point(0,image.size().height));
    	squares.push_back(approx);
    }
    image.release();
    blurred.release();
    LOGD("squares size: %i \n", squares.size());
}

int max_x(vector <Point> points)
{ //求向量最大值
	int maxdata = points[0].x;
	int len=points.size(),i;  //a.size() 求得向量当前存储数量
	for(i=1;i<len;i++)
	{
	 if (points[i].x > maxdata)
		 maxdata = points[i].x;
	}
	return maxdata;
}

int max_y(vector <Point> points)
{ //求向量最大值
	int maxdata = points[0].y;
	int len = points.size(),i;  //a.size() 求得向量当前存储数量
	for(i = 1 ; i < len ; i++)
	{
	 if (points[i].y > maxdata)
		 maxdata = points[i].y;
	}
	return maxdata;
}

float min_x(vector <Point> points)
{ //求向量最小值
	float mindata=points[0].x;
	int len=points.size(),i;
	for(i=1;i<len;i++)
	{
		if (points[i].x < mindata)
			mindata=points[i].x;
	}
	return mindata;
}

float min_y(vector <Point> points)
{ //求向量最小值
	float mindata=points[0].y;
	int len=points.size(),i;
	for(i=1;i<len;i++)
	{
		if (points[i].y < mindata)
			mindata=points[i].y;
	}
	return mindata;
}

/**
 * 给已知的四个点确定位置
 */
void findPoint(CvPoint2D32f scrQuad[], CvPoint2D32f result[])
{

	int i,len = 4;
	float max = 0;
	float min = sqrt(pow(scrQuad[0].x, 2) + pow(scrQuad[0].y, 2));
	int max_len_index = 0, min_len_index = 0;
	for (i = 0 ; i<len ; i++) {
		float distance = sqrt(pow(scrQuad[i].x, 2) + pow(scrQuad[i].y, 2));
		LOGD("find max and min len = %i index = %i max = %f min = %f distance = %f", len, i, max, min, distance);
		if (max < distance) {
			LOGD("find max index = %i max = %f distance = %f", i, max,distance);
			max = distance;
			max_len_index = i;
		}
		if (min > distance) {
			LOGD("find min index = %i min = %f distance = %f", i, min,distance);
			min = distance;
			min_len_index = i;
		}
	}

	int x1 = scrQuad[min_len_index].x;
	int y1 = scrQuad[min_len_index].y;
	int x2 = scrQuad[max_len_index].x;
	int y2 = scrQuad[max_len_index].y;
	float y;

	for(i=0;i<4;i++)
	{
		if(i == min_len_index)
		{
			result[0] = scrQuad[min_len_index];
		}
		else if (i == max_len_index)
		{
			result[2] = scrQuad[max_len_index];
		}
		else
		{
			int scrx = scrQuad[i].x;
			int scry = scrQuad[i].y;
			y = (y1-y2)/(float)(x1-x2)*scrx+(x1*y2-x2*y1)/(float)(x1-x2);
			LOGE("find y = %f", y);
			if(y > scry)
			{
				result[1] = scrQuad[i];
			}
			else
			{
				result[3] = scrQuad[i];
			}
		}
	}

	for (i=0 ; i<4 ; i++) {
			LOGD("find scr point x = %f y = %f", scrQuad[i].x,scrQuad[i].y);
	}

	for (i=0 ; i<4 ; i++) {
		LOGD("find point x = %f y = %f", result[i].x,result[i].y);
	}
}

void findPointFromVector(vector<Point> scrVector, vector<Point>& result)
{
	CvPoint2D32f scrQuad[scrVector.size()], handledScrQuad[scrVector.size()];

	for (int i = 0 ; i < scrVector.size(); i++)
	{
		scrQuad[i].x = scrVector[i].x;
		scrQuad[i].y = scrVector[i].y;
	}
	findPoint(scrQuad, handledScrQuad);
	int len = sizeof(handledScrQuad) / sizeof(handledScrQuad[0]);
	for (int i = 0 ; i < len; i++)
	{
		result.push_back(handledScrQuad[i]);
	}
}

/**
 * 切图方法
 */
void crop(const char* file, vector<Point> points, int jniside[], const char* resultFile){
	LOGD("crop start");
	// 读取图片
	Mat image = imread(file);
	int row_starte = min_y(points);
	int row_end = max_y(points);
	int col_starte = min_x(points);
	int col_end = max_x(points);

	LOGD("crop op start newimage width = %i height = %i", row_starte-row_end, col_starte - col_end);
	// 裁剪图片
	int row,col,r_len = row_end - row_starte,c_len = col_end - col_starte;
	if(r_len > c_len)
	{
		row = max(jniside[0], jniside[1]);
		col = min(jniside[0], jniside[1]);
	}
	else
	{
		row = min(jniside[0], jniside[1]);
		col = max(jniside[0], jniside[1]);
	}

	LOGD("crop op start newcropped_image row = %i col = %i", row, col);
	Mat cropped_image = Mat(row, col, image.type());

	LOGD("fuzhi op start");
	// 透视变换
	IplImage *src,*dst,temp,temp2;
	temp = IplImage(image);
	src = &temp;
	temp2 = IplImage(cropped_image);
	dst = &temp2;
//	dst->origin = src->origin;
	cvZero(dst);

	CvMat* wap_matrix = cvCreateMat(3, 3, CV_32FC1);
	CvPoint2D32f scrQuad[4], handledScrQuad[4], dstQuad[4];
	scrQuad[0].x = points[0].x;
	scrQuad[0].y = points[0].y;
	scrQuad[1].x = points[1].x;
	scrQuad[1].y = points[1].y;
	scrQuad[2].x = points[2].x;
	scrQuad[2].y = points[2].y;
	scrQuad[3].x = points[3].x;
	scrQuad[3].y = points[3].y;
	dstQuad[0].x = 0;
	dstQuad[0].y = 0;
	dstQuad[1].x = cropped_image.size().width;
	dstQuad[1].y = 0;
	dstQuad[2].x = cropped_image.size().width;
	dstQuad[2].y = cropped_image.size().height;
	dstQuad[3].x = 0;
	dstQuad[3].y = cropped_image.size().height;
	findPoint(scrQuad, handledScrQuad);

	LOGD("cvGetPerspectiveTransform op start");
	cvGetPerspectiveTransform(
			handledScrQuad,
			dstQuad,
			wap_matrix
	);

	LOGD("cvWarpPerspective op start");
	cvWarpPerspective(src, dst, wap_matrix);

	LOGD("imwrite op start");
	Mat tmp_mat = Mat(dst);
	imwrite(resultFile, tmp_mat);
//	dst.release();
//	blurred.release();
	LOGD("Release op tmp_mat.release();");
//	image.release();
	tmp_mat.release();
	LOGD("Release op cropped_image.release();");
	cropped_image.release();
	LOGD("Release op cvReleaseMat(&wap_matrix);");
	cvReleaseMat(&wap_matrix);
//	LOGD("Release op cvReleaseImage(&dst);");
//	cvReleaseImage(&dst);
//	cvReleaseImage(&src);
	LOGD("crop end");
}
