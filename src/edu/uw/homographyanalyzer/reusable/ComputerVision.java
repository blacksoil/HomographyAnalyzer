package edu.uw.homographyanalyzer.reusable;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;

import android.app.Activity;
import android.content.Context;

/*
 * Helper class that wraps the OpenCV algorithm 
 * required for our purposes.
 * 
 * Note that some functions might take a while to return.
 * Threading might be needed to avoid ANR.
 */
public class ComputerVision {
	private Context mContext;
	private Activity mActivity;
	private ComputerVisionCallback mCallback;

	private final static boolean DEBUG = true;
	private final static String TAG = "ComputerVision.java";

	public ComputerVision(Context ctx, Activity activity,
			ComputerVisionCallback callback) {
		mContext = ctx;
		mCallback = callback;
		mActivity = activity;
	}

	// Used to hook with the OpenCV service
	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(
			mActivity) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
			}
				if(DEBUG) Logd("Service hook-up finished successfully!");
				mCallback.onInitServiceFinished();
				break;
			default: {
				Loge("Service hook-up failed!");
				mCallback.onInitServiceFailed();
			}
				break;
			}
		}
	};

	/*
	 * Asynchronous OpenCV service loader.
	 * 
	 * The first thing to do before using any other functions Try to connect
	 * with the OpenCV service.
	 * 
	 * mCallback.onInitServiceFinished() would be invoked once the
	 * initialization is done
	 */
	public void initializeService() {
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2,
				mContext, mOpenCVCallBack)) {
			Loge("Couldn't load OpenCV Engine!");
		}
		if(DEBUG) Logd("OpenCV Engine loaded");
	}
	
	
	/*
	 * Find the keypoints of a matrix
	 * featureDetector can be obtained from the FeatureDetector class
	 * (eg. FeatureDetector.FAST)
	 */
	public MatOfKeyPoint findKeyPoints(int featureDetector, Mat img){
		//DescriptorExtractor dExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
		
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		//MatOfKeyPoint descriptors = new MatOfKeyPoint();
		
		//Mat descriptors = new Mat();
		FeatureDetector fd = FeatureDetector.create(featureDetector);
		//DescriptorExtractor de = DescriptorExtractor.create(DescriptorExtractor.ORB);
		
		fd.detect(img, keypoints);
		//de.compute(img, keypoints, descriptors);
		
		return keypoints;
	}
	
	/*
	 * Given a MatOfKeyPoint return Point[] which is only the
	 * x and y coordinates of the keypoints.  
	 * 
	 */
	public Point[] convertMatOfKeyPointToPointArray(MatOfKeyPoint source){
		KeyPoint[] keyPointArray = source.toArray();
		Point[] result = new Point[keyPointArray.length];
		for(int i = 0 ; i < keyPointArray.length ; i++){
			result[i] = keyPointArray[i].pt;
		}
		
		return result;
	}
	
	/*
	 * Given the reference points and the other keypoints
	 * Returns the homography matrix to transform the other to be 
	 * of the same perspective as the reference.
	 * RANSAC method is used.
	 */
	public Mat findHomography(MatOfKeyPoint referenceKeyPoints, MatOfKeyPoint otherKeyPoint,
								int ransac_treshold){
		// Intermediate data structures expected by the findHomography function
		// provided by the library
		Point[] referencePoints, otherPoints;
		MatOfPoint2f matReference, matOther;
		Mat result;
		
		referencePoints = convertMatOfKeyPointToPointArray(referenceKeyPoints);
		otherPoints = convertMatOfKeyPointToPointArray(otherKeyPoint);
		matReference = new MatOfPoint2f(referencePoints);
		matOther = new MatOfPoint2f(otherPoints);
		
		result = Calib3d.findHomography(matReference, matOther, Calib3d.RANSAC, ransac_treshold);
		
		return result;
	}
	
	// Logging function that propagates to the callback
	public void Logd(String msg) {
		mCallback.cvLogd(TAG, msg);
	}

	public void Loge(String msg) {
		mCallback.cvLoge(TAG, msg);
	}
}
