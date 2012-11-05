package edu.uw.homographyanalyzer.reusable;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

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
	 * Given an input image, draws the keypoints on it and produce an output mat
	 * 
	 * This function wraps Features2d.drawKeypoints that takes only RGB image so
	 * that it'd take RGBA (which is what most of our mat would be of)
	 */
	public void drawKeypoints_RGBA(Mat src, Mat dst,
			MatOfKeyPoint keypoints) {
		Mat src_rgb = new Mat();
		Mat dst_rgb = new Mat();
		Imgproc.cvtColor(src, src_rgb, Imgproc.COLOR_RGBA2RGB);
		Features2d.drawKeypoints(src_rgb, keypoints, dst_rgb);
		Imgproc.cvtColor(dst_rgb, dst, Imgproc.COLOR_RGB2RGBA);

		// Imgproc.cvtColor(src_rgb, dst, Imgproc.COLOR_RGB2RGBA);

	}

	/*
	 * Given the keypoints, compute the feature descriptors
	 */
	public Mat computeDescriptors(Mat img, MatOfKeyPoint kp,
			int descriptorExtractor_type) {
		Mat desc = new Mat();
		// Feature extractor
		DescriptorExtractor de = DescriptorExtractor
				.create(descriptorExtractor_type);

		de.compute(img, kp, desc);

		return desc;
	}

	/*
	 * Given two descriptors, compute the matches
	 */
	public MatOfDMatch getMatchingCorrespondences(Mat queryDescriptors,
			Mat trainDescriptors) {
		// Holds the result
		MatOfDMatch matches = new MatOfDMatch();
		// Flann-based descriptor
		DescriptorMatcher dm = DescriptorMatcher
				.create(DescriptorMatcher.BRUTEFORCE_SL2);
		// Compute matches
		dm.match(queryDescriptors, trainDescriptors, matches);

		return matches;
	}

	/*
	 * Given a feature descriptor, a MatOfDmatch, which describes the reference
	 * and target image and also MatOfKeyPoint for the reference and the target
	 * image, this method returns MatOfPoints2f for the reference and target
	 * image to be used for homography computation
	 * 
	 * Return: [0] = reference
	 *         [1] = target
	 */
	public MatOfPoint2f[] getCorrespondences(MatOfDMatch descriptors,
			MatOfKeyPoint ref_kp, MatOfKeyPoint tgt_kp) {

		// The source of computation
		DMatch[] descriptors_array = descriptors.toArray();
		KeyPoint[] ref_kp_array = ref_kp.toArray();
		KeyPoint[] tgt_kp_array = tgt_kp.toArray();

		// The result
		Point[] ref_pts_array = new Point[descriptors_array.length];
		Point[] tgt_pts_array = new Point[descriptors_array.length];

		for (int i = 0; i < descriptors_array.length; i++) {
			ref_pts_array[i] = ref_kp_array[descriptors_array[i].trainIdx].pt;
			tgt_pts_array[i] = tgt_kp_array[descriptors_array[i].queryIdx].pt;
		}
		
		MatOfPoint2f ref_pts = new MatOfPoint2f(ref_pts_array);
		MatOfPoint2f tgt_pts = new MatOfPoint2f(tgt_pts_array);
		
		MatOfPoint2f[] results = new MatOfPoint2f[2];
		results[0] = ref_pts;
		results[1] = tgt_pts;
		return results;
	}
	
	/*
	 * Just like getCorrespondences except this returns only inliers
	 * 
	 * Return: [0] = reference
	 *         [1] = target
	 */
	public MatOfKeyPoint[] getInlierCorrespondences(MatOfDMatch descriptors,
			MatOfKeyPoint ref_kp, MatOfKeyPoint tgt_kp) {

		// The source of computation
		DMatch[] descriptors_array = descriptors.toArray();
		KeyPoint[] ref_kp_array = ref_kp.toArray();
		KeyPoint[] tgt_kp_array = tgt_kp.toArray();
		
		KeyPoint[] rslt_array_ref = new KeyPoint[descriptors_array.length];
		KeyPoint[] rslt_array_tgt = new KeyPoint[descriptors_array.length];
		


		for (int i = 0; i < descriptors_array.length; i++) {
			rslt_array_ref[i] = ref_kp_array[descriptors_array[i].trainIdx];
			rslt_array_tgt[i] = tgt_kp_array[descriptors_array[i].queryIdx];
		}
		
		MatOfKeyPoint rslt_ref = new MatOfKeyPoint(rslt_array_ref);
		MatOfKeyPoint rslt_tgt = new MatOfKeyPoint(rslt_array_tgt);
		MatOfKeyPoint[] rsult = new MatOfKeyPoint[2];
		
		rsult[0] = rslt_ref;
		rsult[1] = rslt_tgt;
		
		return rsult;
	}

	public void drawMatches(Mat img1, MatOfKeyPoint keypoints1,
			Mat img2, MatOfKeyPoint keypoints2, MatOfDMatch matches1to2,
			Mat outImg) {
		Mat img1_rgb = new Mat();
		Mat img2_rgb = new Mat();
		
		Imgproc.cvtColor(img1, img1_rgb, Imgproc.COLOR_RGBA2RGB);
		Imgproc.cvtColor(img2, img2_rgb, Imgproc.COLOR_RGBA2RGB);

		Features2d.drawMatches(img1_rgb, keypoints1, img2_rgb, keypoints2,
				matches1to2, outImg);
	}
	
	// This functions returns sub-images from the source image.
	// Sub-image is defined by ROI as follow:
	// The return Mat[i] would be ROI of the source_image defined by ROI.get(i)
	// Each Point in the ROI.get(i) is the polygon coordinates describing
	// the area of interest.
	//
	// Note that however many polygons are there would be rounded 
	public Mat[] cropImage(Mat source_image, List<List<Point>> ROI){
		Mat[] results = new Mat[ROI.size()];
		
		for(int i = 0 ; i < ROI.size() ; i++){
			List<Point> coordinates = ROI.get(i);
			/*
			 * a  _____  b
			 *   |     |
			 * c |_____| d
			 */
			
			boolean firstTime = true;
			int xMin = 0, xMax = 0, yMin = 0, yMax = 0;
			
			// Approximate the polygon as a rectangle
			for(Point p : coordinates){
				if(firstTime){
					firstTime = false;
					xMin = xMax = (int) p.x;
					yMin = yMax = (int) p.y;
				}
				else{
					xMin = Math.min(xMin, (int) p.x);
					yMin = Math.min(yMin, (int) p.y);
					xMax = Math.max(xMax, (int) p.x);
					yMax = Math.max(yMax, (int) p.y);
				}
			}
			
			results[i] = new Mat(source_image, new Rect(xMin, yMin, xMax - xMin, yMax - yMin));
		}
		
		return results;
	}
	
	// Logging function that propagates to the callback
	public void Logd(String msg) {
		mCallback.cvLogd(TAG, msg);
	}

	public void Loge(String msg) {
		mCallback.cvLoge(TAG, msg);
	}
}
