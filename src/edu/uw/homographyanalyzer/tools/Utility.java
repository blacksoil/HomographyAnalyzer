package edu.uw.homographyanalyzer.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import edu.uw.homographyanalyzer.global.GlobalLogger;

public class Utility {
	/*
	 * Save a bitmap to a file
	 */
	public static void saveBitmapToFile(Bitmap bmp, String path){
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
	public static void saveMatToFile(Mat mat, String path){
		Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mat, bmp);
	}
	
	/*
	 * Given an input image, draws the keypoints on it
	 * and produce an output mat
	 * 
	 * This function wraps Features2d.drawKeypoints that takes
	 * only RGB image so that it'd take RGBA (which is what most
	 * of our mat would be of)
	 */
	public static void drawKeypoints_RGBA(Mat src, Mat dst, MatOfKeyPoint keypoints){
		Mat src_rgb = new Mat();
		Mat dst_rgb = new Mat();
		Imgproc.cvtColor(src, src_rgb, Imgproc.COLOR_RGBA2RGB);
		Features2d.drawKeypoints(src_rgb, keypoints, dst_rgb);
		Imgproc.cvtColor(dst_rgb, dst, Imgproc.COLOR_RGB2RGBA);
		
		//Imgproc.cvtColor(src_rgb, dst, Imgproc.COLOR_RGB2RGBA);
		
	}
	
}
