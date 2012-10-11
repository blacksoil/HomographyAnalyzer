package edu.uw.homographyanalyzer.main;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
	
	// Current workspace name
	// Full workspace path would be mHomePath + mWorkspaceName
	private String mWorkspaceName;
	
	// Ransac treshold value for homography transformation
	private int mRansacTreshold;
	
	// Logging tag
	private static final String TAG = "HomographyAnalyzer";
	
	// Intent request code
	private final static int ACTION_TAKE_REFERENCE_IMAGE = 0;
	private final static int ACTION_TAKE_TARGET_IMAGE = 1;
	
	// UI widgets
	private final static int NUM_OF_BUTTONS = 4;
	private Button[] mButtons = new Button[NUM_OF_BUTTONS];
	private Button btnCompute, btnTakeReference, btnTakeTarget, btnBrowseWorkspace;
	
	private EditText txtWorkspaceName, txtRansacTreshold, txtLogging;
	
	public void initWidgets(){
		mButtons[0] = btnCompute = (Button) findViewById(R.id.btnCompute);
		mButtons[1] = btnTakeReference = (Button) findViewById(R.id.btnTakeReference);
		mButtons[2] = btnTakeTarget = (Button) findViewById(R.id.btnTakeTarget);
		mButtons[3] = btnBrowseWorkspace = (Button) findViewById(R.id.btnBrowseWorkspace);
		txtWorkspaceName = (EditText) findViewById(R.id.txtWorkspaceName);
		txtRansacTreshold = (EditText) findViewById(R.id.txtRansacTreshold);
		txtLogging = (EditText) findViewById(R.id.txtLogging);
		
		for(int i = 0 ; i < NUM_OF_BUTTONS ; i++){
			mButtons[i].setOnClickListener(this);
		}
	}
	
	// UI Button click handler
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		//logd("Button clicked");
		
		if(txtWorkspaceName.getText().toString().equals("")){
			loge("Enter workspace name first!");
			return;
		}
		else{
			mWorkspaceName = txtWorkspaceName.getText().toString();
			if(!checkWorkspacePath()){
				logd("Skipping things because no appropriate workspace folder!");
			}
		}
		
		if(v.getId() == btnTakeReference.getId()){
			Intent cameraTaking = new Intent(this, ExternalApplication.class);
			File output = new File(mHomePath + mWorkspaceName, "reference.bmp");
			Bundle b = new Bundle();
			b.putString(BaseImageTaker.IMAGE_PATH, output.getAbsolutePath()); 
			cameraTaking.putExtras(b);
			// Open up the camera application to take picture
			startActivityForResult(cameraTaking, ACTION_TAKE_REFERENCE_IMAGE);
		}
		
	}

	
	/*
	 * Returns true if workspace path exist
	 * Will create one if no such folder exists
	 */
	public boolean checkWorkspacePath(){
		// String path
		String path = mHomePath;
		// File points to path
		File path_file;
		
		// Needs to have / at the end
		if(mHomePath.charAt(mHomePath.length() - 1) != '/'){
			path += "/";
		}
		path += mWorkspaceName;
		
		path_file = new File(path);
		
		if(!path_file.exists()){
			// Try to create the path if it doesn't exist
			if(!path_file.mkdirs()){
				loge("Error on creating the workspace folder!");
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Get the complete workspace path
	 */
	public String getWorkspacePath(){
		String out = mHomePath + mWorkspaceName;
		if(out.charAt(out.length() - 1) != '/')
			out += "/";
		return out;
	}
	
	public String getReferenceImagePath(){
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homography);

		// Init UI
		initWidgets();
		
		// This needs to be done first because many other objets
		// dependent on this global logger
		new GlobalLogger(this);
		
		// Initialize OpenCV engine
		ComputerVision cv = new ComputerVision(this, this, this);
		cv.initializeService();
	}
	

	@Override
	public void onInitServiceFinished() {
		// TODO Auto-generated method stub
		//logd("onInitServiceFinished()");
	}

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
			if(resultCode == RESULT_OK){
				String imagePath;
				imagePath = data.getExtras().getString(BaseImageTaker.IMAGE_PATH);
				logd("Reference image taken: " + imagePath);
			}
			else{
				
			}
			
			break;
		case ACTION_TAKE_TARGET_IMAGE:
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_homography, menu);
		return true;
	}

	@Override
	public void logd(String msg) {
		Log.d(TAG, msg);
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
