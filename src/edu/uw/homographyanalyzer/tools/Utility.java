package edu.uw.homographyanalyzer.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

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
