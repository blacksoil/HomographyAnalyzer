package edu.uw.homographyanalyzer.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import edu.uw.homographyanalyzer.global.GlobalLogger;

public class Utility {
	/*
	 * Save a bitmap to a file
	 */
	public static void saveBitmapToFile(Bitmap bmp, String path) {
		try {
			FileOutputStream out = new FileOutputStream(new File(path));
			bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
		} catch (FileNotFoundException e) {
			GlobalLogger.getInstance().loge("Couldn't create file: " + path);
			e.printStackTrace();
		}
	}

	/*
	 * Save a Mat to a file
	 */
	public static void saveMatToFile(Mat mat, String path) {
		Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mat, bmp);
	}

	/*
	 * Given an input image, draws the keypoints on it and produce an output mat
	 * 
	 * This function wraps Features2d.drawKeypoints that takes only RGB image so
	 * that it'd take RGBA (which is what most of our mat would be of)
	 */
	public static void drawKeypoints_RGBA(Mat src, Mat dst,
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
	public static Mat computeDescriptors(Mat img, MatOfKeyPoint kp,
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
	public static MatOfDMatch getMatchingCorrespondences(Mat queryDescriptors,
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
	public static MatOfPoint2f[] getCorrespondences(MatOfDMatch descriptors,
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

	public static void drawMatches(Mat img1, MatOfKeyPoint keypoints1,
			Mat img2, MatOfKeyPoint keypoints2, MatOfDMatch matches1to2,
			Mat outImg) {
		Mat img1_rgb = new Mat();
		Mat img2_rgb = new Mat();

		Imgproc.cvtColor(img1, img1_rgb, Imgproc.COLOR_RGBA2RGB);
		Imgproc.cvtColor(img2, img2_rgb, Imgproc.COLOR_RGBA2RGB);

		Features2d.drawMatches(img1_rgb, keypoints1, img2_rgb, keypoints2,
				matches1to2, outImg);
	}

	/*
	 * Given a feature descriptor and the whole set of target image's keypoints,
	 * return matching keypoints.
	 */
	/*
	 * public static MatOfKeyPoint getMatchingKeypointsFromDescriptors(
	 * MatOfDMatch descriptor_matches, MatOfKeyPoint tgt_kp){
	 * 
	 * // Convert descriptor to array DMatch[] desc_matches =
	 * descriptor_matches.toArray();
	 * 
	 * KeyPoint[] tgt_kp_array = tgt_kp.toArray(); KeyPoint[] result_array = new
	 * KeyPoint[desc_matches.length];
	 * 
	 * //TODO: Incorportate distance between features for(int i = 0 ; i <
	 * desc_matches.length ; i++){ DMatch match = desc_matches[i];
	 * match.distance result_array[i] = tgt_kp_array[match.queryIdx]; }
	 * 
	 * MatOfKeyPoint result = new MatOfKeyPoint(result_array);
	 * 
	 * return result; }
	 */
}
