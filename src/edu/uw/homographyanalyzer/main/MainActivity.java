package edu.uw.homographyanalyzer.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.camera.BaseImageTaker;
import edu.uw.homographyanalyzer.camera.ExternalApplication;
import edu.uw.homographyanalyzer.global.GlobalLogger;
import edu.uw.homographyanalyzer.global.LoggerInterface;
import edu.uw.homographyanalyzer.reusable.ComputerVision;
import edu.uw.homographyanalyzer.reusable.ComputerVisionCallback;
import edu.uw.homographyanalyzer.tools.Utility;

/*
 * Sample Activity meant to demonstrate how to use the implemented
 *  wrapper for related OpenCV library and many other building blocks. 
 *  
 * UI Description:
 * This is essentially an activity where we can take 2 pictures,
 * reference and target. 
 * 
 * We then can do a homography transformation between the two images
 * with a ransac_treshold value and other possible variables that we specify.
 * The application would call a file explorer intent to open up the 
 * folder where the images and results were placed. 
 * The application also generates a textual file that 
 * represent the number of keypoints, treshold, inliers, etc. 
 * 
 *   The UI looks as follow:
 *   When the application first runs, there is an option to create a
 *   new workspace. Workspace is a folder where images and the textual
 *   data are placed. The application would be able to parse the textual
 *   info and show the info on the UI.
 *   
 *   Another key design is that the folder would be able to be opened
 *   in the computer so we can investigate it easily. Also the data layout
 *   would be the same as our computer program, so we can easily investigate the result
 *   using our laptop. 
 *   
 *   It's also possible to create the workspace manually using a computer
 *   and put the input image manually then have the phone to parse it.
 *   (eg. we make an input folder and place the reference and other images there
 *   with the appropriate naming convention, then have the phoen to compute
 *   homography for each of the pictures and place it on the output folder.)
 *   
 *   Textual information:
 *   
 *   Folder layout:
 *   /mnt/sdcard/ApplianceReader/[WORKSPACE_NAME]
 *   	input/
 *   	output/
 *   	info.txt
 *   
 *   input -> REFERENCE.png, [NUMBER].png 
 *   where REFERENCE.png is the reference image and [NUMBER].png is 
 *   the target image
 *   
 *   output -> contains the result of each of the transformed image
 *   _[number of inliers]_[NUMBER]_[RESULT/ORIG_W_KP/MATCHED].png
 *   
 *   where: RESULT is the final homography image
 *   		ORIG_W_KP is the original image with keypoint drawn on it
 *   		MATCHED is the original and target put side by side with inliers 
 *   				feature points connect the 2 images.
 */

// Workspace Activity
public class MainActivity extends Activity implements LoggerInterface,
		ComputerVisionCallback, OnClickListener {

	// Workspace home folder
	private static final String mHomePath = "/mnt/sdcard/ApplianceReader/";
	// Logging tag
	private static final String TAG = "HomographyAnalyzer";
	// Reference image file name
	private static final String REFERENCE_IMAGE_FILE_NAME = "reference.bmp";
	// Input folder name (folder where the target images are stored)
	private static final String TARGET_IMAGE_FOLDER_NAME = "input";
	// Output folder name (folder where the generated images are stored)
	private static final String OUTPUT_IMAGE_FOLDER_NAME = "output";

	// Current workspace name
	// Full workspace path would be mHomePath + mWorkspaceName
	private String mWorkspaceName;

	// Computer Vision library wrapper
	private ComputerVision mCV;

	// RANSAC threshold value for homography transformation
	private int mRansacTreshold = 3;

	// The kind of feature detector to be used
	private final static int FEATURE_DETECTOR = FeatureDetector.ORB;
	private final static int DESCRIPTOR_EXTRACTOR = DescriptorExtractor.ORB;

	// Intent request code
	private final static int ACTION_TAKE_REFERENCE_IMAGE = 0;
	private final static int ACTION_TAKE_TARGET_IMAGE = 1;

	// UI widgets
	private final static int NUM_OF_BUTTONS = 4;
	private Button[] mButtons = new Button[NUM_OF_BUTTONS];
	private Button btnCompute, btnTakeReference, btnTakeTarget,
			btnBrowseWorkspace;

	// UI Thread handler
	private Handler mHandler;

	// This class' context
	private Context mContext;

	// Progress dialog to indicate that computation is running
	private ProgressDialog mProgressDialog;

	private EditText txtWorkspaceName, txtRansacTreshold, txtLogging;

	public void initWidgets() {
		mButtons[0] = btnCompute = (Button) findViewById(R.id.btnCompute);
		mButtons[1] = btnTakeReference = (Button) findViewById(R.id.btnTakeReference);
		mButtons[2] = btnTakeTarget = (Button) findViewById(R.id.btnTakeTarget);
		mButtons[3] = btnBrowseWorkspace = (Button) findViewById(R.id.btnBrowseWorkspace);
		txtWorkspaceName = (EditText) findViewById(R.id.txtWorkspaceName);
		txtRansacTreshold = (EditText) findViewById(R.id.txtRansacTreshold);
		txtLogging = (EditText) findViewById(R.id.txtLogging);

		for (int i = 0; i < NUM_OF_BUTTONS; i++) {
			mButtons[i].setOnClickListener(this);
			mButtons[i].setEnabled(false);
		}

	}

	// UI Button click handler
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		// logd("Button clicked");

		if (txtWorkspaceName.getText().toString().equals("")) {
			loge("Enter workspace name first!");
			return;
		} else {
			mWorkspaceName = txtWorkspaceName.getText().toString();
			if (!checkWorkspacePath()) {
				logd("Skipping things because no appropriate workspace folder!");
			}
		}

		// Take Reference Image
		if (v.getId() == btnTakeReference.getId()) {
			takePicture(getReferenceImagePath(), ACTION_TAKE_REFERENCE_IMAGE);
		}

		// Take Target Image
		else if (v.getId() == btnTakeTarget.getId()) {
			takePicture(getTargetImagePath(), ACTION_TAKE_TARGET_IMAGE);
		}

		// Browse the workspace
		else if (v.getId() == btnBrowseWorkspace.getId()) {
			browsePath(getWorkspacePath());
		}

		// Compute
		else if (v.getId() == btnCompute.getId()) {
			compute();
		}

	}

	/*
	 * Function that handles btnCompute() click event
	 */
	public void compute() {
		mProgressDialog = ProgressDialog.show(this, "Computing", "Please wait",
				true, false);
		Runnable computation = new Runnable() {

			@Override
			public void run() {
				try {
					// Mat of reference image
					Mat reference_img = getReferenceMat();

					// Compute keypoints of the reference image
					MatOfKeyPoint ref_kp = mCV.findKeyPoints(FEATURE_DETECTOR,
							reference_img);

					// Compute the descriptor
					Mat reference_descriptor = Utility.computeDescriptors(
							reference_img, ref_kp, DESCRIPTOR_EXTRACTOR);

					// Get each of the target images
					String[] target_files = getTargetImagePaths();

					logd("Number of ref img keypoints=" + ref_kp.size().height
							* ref_kp.size().width);

					// For each target image
					for (int i = 0; i < target_files.length; i++) {
						// Get the target image matrix
						Mat target_img = getMatFromFile(target_files[i]);

						// Compute the target image keypoints
						MatOfKeyPoint target_kp = mCV.findKeyPoints(
								FEATURE_DETECTOR, target_img);
						logd("Number of keypoints: " + target_kp.size().height
								* target_kp.size().width);

						// Compute the target's descriptor
						Mat target_descriptor = Utility.computeDescriptors(
								target_img, target_kp, DESCRIPTOR_EXTRACTOR);

						// Target mat with keypoints drawn on it
						Mat target_with_keypoints = new Mat();

						// Mat of reference to target correspondences drawn on
						// it
						Mat correspondence_image = new Mat();

						// Grab the matching keypoints of the target image
						MatOfDMatch descriptors = Utility
								.getMatchingCorrespondences(
										target_descriptor,reference_descriptor);

						// Inlier keypoints
						// MatOfKeyPoint target_kp_filtered =
						// Utility.getMatchingKeypointsFromDescriptors(matches,
						// target_kp);
						// logd("Number of inlier keypoints: " +
						// target_kp_filtered.size().height *
						// target_kp_filtered.size().width);

						Utility.drawMatches(reference_img, ref_kp, target_img,
								target_kp, descriptors,
								correspondence_image);

						Utility.drawKeypoints_RGBA(target_img,
								target_with_keypoints, target_kp);

						MatOfPoint2f pts[] = Utility.getCorrespondences(
								descriptors, ref_kp, target_kp);

						// Compute the homography matrix
						Mat H = Calib3d.findHomography(pts[1], pts[0],
								Calib3d.RANSAC, mRansacTreshold);
						

						Mat target_warped_img = new Mat();
						Imgproc.warpPerspective(
								target_img,
								target_warped_img,
								H,
								new Size(reference_img.width(), reference_img
										.height()));

						Bitmap target_warped_bmp = Bitmap.createBitmap(
								reference_img.width(),
								reference_img.height(),
								Config.ARGB_8888);
						
						// Bitmap with keypoints
						Bitmap target_with_keypoints_bmp = Bitmap.createBitmap(
								target_with_keypoints.width(),
								target_with_keypoints.height(),
								Config.ARGB_8888);

						Bitmap correspondence_image_bmp = Bitmap
								.createBitmap(correspondence_image.width(),
										correspondence_image.height(),
										Config.ARGB_8888);

						Utils.matToBitmap(target_with_keypoints,
								target_with_keypoints_bmp);

						Utils.matToBitmap(correspondence_image,
								correspondence_image_bmp);
						
						Utils.matToBitmap(target_warped_img,target_warped_bmp);

						// The path to store the result
						String target_keypoints_path = getOutputImagePath_keypoint(target_files[i]);
						String target_correspondence_path = getOutputImagePath_correspondence(target_files[i]);
						String target_warped_path = getOutputImagePath_warped(target_files[i]);
						
						Utility.saveBitmapToFile(target_with_keypoints_bmp,
								target_keypoints_path);
						Utility.saveBitmapToFile(correspondence_image_bmp,
								target_correspondence_path);
						Utility.saveBitmapToFile(target_warped_bmp,
								target_warped_path);
						
						logd("File created: " + target_keypoints_path);
					}

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				finally {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mProgressDialog.dismiss();
						}
					});
				}
			}
		};

		// Separate Thread to do the heavy computation
		Thread computeThread = new Thread(computation);
		computeThread.start();

	}

	/*
	 * Get reference Mat
	 */
	public Mat getReferenceMat() throws FileNotFoundException, IOException {
		return getMatFromFile(getReferenceImagePath());
	}

	/*
	 * Given a file path of a bitmap file returns a Mat representation of it
	 */
	public Mat getMatFromFile(String path) throws FileNotFoundException,
			IOException {
		Bitmap bmp = getBitmapFromFile(path);
		Mat mat = new Mat();
		Utils.bitmapToMat(bmp, mat);

		return mat;
	}

	/*
	 * Get the bitmap of reference image
	 */
	public Bitmap getReferenceBitmap() throws FileNotFoundException,
			IOException {
		return getBitmapFromFile(getReferenceImagePath());
	}

	/*
	 * Return the absolute path for each of the target images
	 */
	public String[] getTargetImagePaths() {
		File target_image_path = new File(getTargetFolderPath());
		File[] images = target_image_path.listFiles();
		String[] paths = new String[images.length];

		for (int i = 0; i < images.length; i++) {
			paths[i] = images[i].getAbsolutePath();
		}

		return paths;
	}

	/*
	 * Given an input image, returns the path of where the input image with
	 * keypoints drawn on it would be stored
	 */
	public String getOutputImagePath_keypoint(String target_files) {
		File input = new File(target_files);
		String input_name = input.getName();
		int i;
		for (i = input_name.length() - 1; i >= 0; i--) {
			if (input_name.charAt(i) == '.') {
				break;
			}
		}

		if (i == 0) {
			throw new RuntimeException("Path doesn't have file extension: " + i);
		}

		File outputPath = new File(getOutputFolderPath(), input_name.substring(
				0, i) + "_keypoints.bmp");

		return outputPath.getAbsolutePath();

	}

	/*
	 * Given an input image, returns the path of where the correspondence image
	 * (an image where features between reference and the image is drawn on it)
	 * would be stored
	 */
	public String getOutputImagePath_correspondence(String target_files) {
		File input = new File(target_files);
		String input_name = input.getName();
		int i;
		for (i = input_name.length() - 1; i >= 0; i--) {
			if (input_name.charAt(i) == '.') {
				break;
			}
		}

		if (i == 0) {
			throw new RuntimeException("Path doesn't have file extension: " + i);
		}

		File outputPath = new File(getOutputFolderPath(), input_name.substring(
				0, i) + "_correspondence.bmp");

		return outputPath.getAbsolutePath();

	}
	
	/*
	 * Given an input image, returns the path of where the warped image
	 * would be stored
	 */
	public String getOutputImagePath_warped(String target_files) {
		File input = new File(target_files);
		String input_name = input.getName();
		int i;
		for (i = input_name.length() - 1; i >= 0; i--) {
			if (input_name.charAt(i) == '.') {
				break;
			}
		}

		if (i == 0) {
			throw new RuntimeException("Path doesn't have file extension: " + i);
		}

		File outputPath = new File(getOutputFolderPath(), input_name.substring(
				0, i) + "_warped.bmp");

		return outputPath.getAbsolutePath();

	}


	/*
	 * Given an absolute image path returns its bitmap
	 */
	public Bitmap getBitmapFromFile(String path) throws FileNotFoundException,
			IOException {
		return Media.getBitmap(getContentResolver(),
				Uri.fromFile(new File(path)));
	}

	/*
	 * Take a picture (eg. using external camera application)
	 * 
	 * path = the absolute path of where the file should be stored
	 */
	public void takePicture(String path, int action) {
		Intent cameraTaking = new Intent(this, ExternalApplication.class);
		Bundle b = new Bundle();
		b.putString(BaseImageTaker.IMAGE_PATH, path);
		cameraTaking.putExtras(b);
		// Open up the camera application to take picture
		startActivityForResult(cameraTaking, action);
	}

	public void browsePath(String folder_path) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(new File(
				getWorkspacePath())));
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		// logd("folder_path = " + folder_path);
		// intent.setData(Uri.fromFile(new File(folder_path)));
		// intent.setType("file/*");
		// intent.setDataAndType(Uri.fromFile(new File(folder_path)), "file/*");
		startActivity(intent);
	}

	/*
	 * Returns true if workspace path exist Will create one if no such folder
	 * exists
	 */
	public boolean checkWorkspacePath() {
		// String path
		String path = mHomePath;
		// File points to path
		File path_file;

		// Needs to have / at the end
		if (mHomePath.charAt(mHomePath.length() - 1) != '/') {
			path += "/";
		}
		path += mWorkspaceName;

		path_file = new File(path);

		if (!path_file.exists()) {
			// Try to create the path if it doesn't exist
			if (!path_file.mkdirs()) {
				loge("Error on creating the workspace folder!");
				return false;
			}
		}
		return true;
	}

	/*
	 * Get the complete workspace path
	 */
	public String getWorkspacePath() {
		String path = mHomePath + mWorkspaceName;
		File path_file = new File(path);

		if (!path_file.exists()) {
			if (!path_file.mkdirs()) {
				loge("Couldn't create workspace path!");
				throw new RuntimeException("Couldn't create workspace path!");
			}
		}

		return path_file.getAbsolutePath();
	}

	/*
	 * Read out the UI textbox and return the parsed ransac threshold value.
	 * 
	 * Returns an error if error happens.
	 */
	public double getRansacTreshold() {
		return Double.parseDouble(txtRansacTreshold.getText().toString());
	}

	/*
	 * 
	 * Get the complete path of the target image folder
	 */
	public String getTargetFolderPath() {
		File input_path = new File(getWorkspacePath() + "/"
				+ TARGET_IMAGE_FOLDER_NAME);

		// Create the target folder if needed
		if (!input_path.exists()) {
			if (!input_path.mkdirs()) {
				loge("Couldn't create a folder for target images!");
				throw new RuntimeException(
						"Couldn't create a folder for target images!");
			}
		}

		return input_path.getAbsolutePath();

	}

	/*
	 * Return the full path of the reference image
	 */
	public String getReferenceImagePath() {
		String reference_folder_path = getWorkspacePath();
		File ref_file = new File(reference_folder_path,
				REFERENCE_IMAGE_FILE_NAME);

		return ref_file.getAbsolutePath();
	}

	public String getOutputFolderPath() {
		String workspace_path = getWorkspacePath();

		if (workspace_path.charAt(workspace_path.length() - 1) != '/') {
			workspace_path += "/";
		}

		File workspace_folder = new File(workspace_path
				+ OUTPUT_IMAGE_FOLDER_NAME);
		workspace_folder.mkdirs();

		return workspace_folder.getAbsolutePath();
	}

	/*
	 * Return the full path of the target image
	 * 
	 * Since we can have multiple target image, this function handles the naming
	 * convention. (eg. create the new file with appropriate name for the next
	 * target image)
	 * 
	 * We're using this naming convention [NUMBER].bmp So if there are images
	 * 1.bmp through 3.bmp already on the input folder, this function would
	 * return a path for 4.bmp
	 */
	public String getTargetImagePath() {
		String target_folder_path = getTargetFolderPath();
		File target_folder_file = new File(target_folder_path);
		File[] target_images_files = target_folder_file.listFiles();

		int largest = 0;
		for (File target_image : target_images_files) {
			String name = target_image.getName();
			// logd("name= " + name);
			// Remove file type
			String name_wo_type = "";
			for (int i = 0; i < name.length(); i++) {
				if (name.charAt(i) == '.')
					break;
				name_wo_type += name.charAt(i);
			}

			// Note that this assumes the target files are numbered
			int file_name_integer = Integer.parseInt(name_wo_type);
			largest = Math.max(largest, file_name_integer);
		}
		// logd("getTargetImagePath(): largest= " + largest);

		File output_file = new File(target_folder_path, "" + ++largest + ".bmp");

		return output_file.getAbsolutePath();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homography);

		/*
		 * These needs to be done first before any call to logd because logd is
		 * using handler
		 */
		// Initializes UI Thread handler
		mHandler = new Handler();
		// Activity's context
		mContext = getApplicationContext();

		// Init UI
		initWidgets();

		// This needs to be done first because many other objets
		// dependent on this global logger
		new GlobalLogger(this);

		// Initialize OpenCV engine
		mCV = new ComputerVision(this, this, this);
		mCV.initializeService();

		// Prevent user from doing anything before the CV engine is initialized
		mProgressDialog = ProgressDialog.show(this, "OpenCV Engine",
				"Initializing", true, false);

	}

	/*
	 * Invoked when OpenCV engine has finished its initialization
	 * 
	 * @see edu.uw.homographyanalyzer.reusable.ComputerVisionCallback#
	 * onInitServiceFinished()
	 */
	@Override
	public void onInitServiceFinished() {
		// TODO Auto-generated method stub
		// logd("onInitServiceFinished()");
		for (int i = 0; i < NUM_OF_BUTTONS; i++) {
			mButtons[i].setEnabled(true);
		}
		mProgressDialog.dismiss();
	}

	/*
	 * Invoked if OpenCV engine failed to initialize
	 * 
	 * @see
	 * edu.uw.homographyanalyzer.reusable.ComputerVisionCallback#onInitServiceFailed
	 * ()
	 */
	@Override
	public void onInitServiceFailed() {
		// TODO Auto-generated method stub
		loge("onInitServiceFailed()");
	}

	// Called when a started intent returns
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTION_TAKE_REFERENCE_IMAGE:
			if (resultCode == RESULT_OK) {
				String imagePath;
				imagePath = data.getExtras().getString(
						BaseImageTaker.IMAGE_PATH);
				logd("Reference image taken: " + imagePath);
			} else {

			}

			break;
		case ACTION_TAKE_TARGET_IMAGE:
			if (resultCode == RESULT_OK) {
				String imagePath;
				imagePath = data.getExtras().getString(
						BaseImageTaker.IMAGE_PATH);
				logd("Target image taken: " + imagePath);
			} else {

			}

			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_homography, menu);
		return true;
	}

	@Override
	public void logd(final String msg) {
		Log.d(TAG, msg);
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void loge(String msg) {
		Log.e(TAG, msg);
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void cvLogd(String msg) {
		// TODO Auto-generated method stub
		logd("cvLogd()");
	}

	@Override
	public void cvLogd(String tag, String msg) {
		txtLogging.append(msg);
		logd(msg);
	}

	@Override
	public void cvLoge(String msg) {
		txtLogging.append(msg);
		loge(msg);
	}

	@Override
	public void cvLoge(String tag, String msg) {
		loge(msg);
	}

}
