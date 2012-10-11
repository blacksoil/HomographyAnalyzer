package edu.uw.homographyanalyzer.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import edu.uw.homographyanalyzer.global.GlobalLogger;

/*
 * Base class of all picture-taker activities.
 * This class is meant to standardize the process so
 * a new implementation would agree to the same pattern.
 * 
 * This class is designed as an independent Activity
 * So a user would invoke it through startActivity()
 * and obtain the path of the picture taken through 
 * the corresponding construct. NOTE that returning the 
 * Bitmap immediately DOESN'T work due to Google's bug?
 * (I wasted some time just to figure this out!!)
 * 
 * The path of the resulting image is returned
 * via an Intent passed through onActivityResult()
 * with key name = "IMAGE_PATH"
 * (eg. intent.getExtras().getString("IMAGE_PATH") )
 * 
 */
public abstract class BaseImageTaker extends Activity {
	// The path to where the image result should be stored
	// This is passed when this activity is created
	
	// This is the bundle's key
	public static final String IMAGE_PATH = "IMAGE_PATH";
	// This is the actual path
	private String mPath;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mPath = getIntent().getExtras().getString("IMAGE_PATH");
		if(mPath.equals("")){
			throw new IllegalArgumentException("BaseImageTaker requires a path" +
					"							to save the result image!");
		}
	}

	/*
	 * The path where the taken image should be stored
	 */
	protected String getImagePath(){
		return mPath;
	}
	
	/*
	 * Quit the activity and returns the image path
	 */
	protected void finishAndReturnImagePath() {
		GlobalLogger.getInstance().logd("Picture taken successfully!");
		Intent result = new Intent();
		result.putExtra(IMAGE_PATH, getImagePath());
		this.setResult(RESULT_OK, result);
		this.finish();
	}

	/*
	 * Quit the application but not returning the image path as the capture was
	 * failed
	 */
	protected void finishFail() {
		GlobalLogger.getInstance().logd("Picture taking failed!");
		setResult(RESULT_CANCELED);
		finish();
	}
}
