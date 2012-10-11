package edu.uw.homographyanalyzer.camera;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import edu.uw.homographyanalyzer.global.GlobalLogger;

/*
 * Take a picture using an external application.
 * This class extends BaseImageTaker. Take a look at the class description
 * to get an idea on how to use it
 */
public class ExternalApplication extends BaseImageTaker {
	private final static int ACTIVITY_RESULT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		GlobalLogger.getInstance().logd(
				"Camera directory: " + getImagePath());
		// Start camera intent
		takePicture();
	}

	/*
	 * Runs an external intent to take a picture.
	 */
	private void takePicture() {
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(getImagePath())));
		startActivityForResult(i, ACTIVITY_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Picture is taken!
		if (resultCode == RESULT_OK) {
			File outFile = new File(getImagePath());
			try {
				if(!outFile.exists()){
					throw new FileNotFoundException();
				}
				
				// Return and quit
				finishAndReturnImagePath();
			} catch (FileNotFoundException e) {
				GlobalLogger.getInstance().loge(
						"ExternalApplication.java: temporary file not found = " + 
						outFile.getAbsolutePath());
				finishFail();
			}
		} else {
			finishFail();
		}
	}

}
