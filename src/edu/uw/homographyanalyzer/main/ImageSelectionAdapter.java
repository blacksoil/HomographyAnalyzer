package edu.uw.homographyanalyzer.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.global.GlobalLogger;

/**
 * Adapter used to cycle through images added to by client services
 * @author mhotan
 *
 */
public class ImageSelectionAdapter extends BaseAdapter {

	private static final String TAG = "ImageSelectionAdapter";
	
	// Owning context of this adapter
	private Context mContext;
	
	// 
	public static final int POSITION_BASE = 0;
	public static final int POSITION_QUERY = 1;
	
	
	HashMap<Integer, ImageView> toShowMap;
	HashMap<Integer, Uri> uriMap;
	
	protected List<Bitmap> mBitMaps;
	
	//Default Search Image
	protected final Bitmap mPlaceHolder;
	
	private static int default_search_id = R.drawable.ic_action_search;
	private int defaultItemBackground;
	
	/**
	 * Adpater for managing images for Homography
	 * @param galleryContext Context that contains gallery of images
	 */
	public ImageSelectionAdapter(Context galleryContext) {
		mContext = galleryContext;	
		mBitMaps = new ArrayList<Bitmap>(2);
		mPlaceHolder = BitmapFactory.decodeResource(mContext.getResources(), default_search_id);
		
		// Set the gallery item backgrounf image
		TypedArray styleAttrs = mContext.obtainStyledAttributes(R.styleable.PicGallery);
		// Get background Resource
		defaultItemBackground = styleAttrs.getResourceId(
				R.styleable.PicGallery_android_galleryItemBackground, 0);
		//Recycle Attribute
		styleAttrs.recycle();
		
		// Add default images
		reset();
	}

	
	@Override
	public int getCount() {
		return mBitMaps.size();
	}

	/**
	 * Returns the BIT Map associated with 
	 */
	@Override
	public Object getItem(int position) {
		if (position < 0 || position >= mBitMaps.size())
			return null;
		else 
			return mBitMaps.get(position);
	}

	/**
	 * Returns Integer values of resource ID of image
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//create the view
	    ImageView imageView = new ImageView(mContext);
	    //specify the bitmap at this position in the array
	    imageView.setImageBitmap(mBitMaps.get(position));
	    //set layout options
	    imageView.setLayoutParams(new Gallery.LayoutParams(300, 200));
	    //scale type within view area
	    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
	    //set default gallery item background
	    imageView.setBackgroundResource(defaultItemBackground);
	    //return the view
	    return imageView;
	}

	/**
	 * Sets the image at position
	 * @param image Image to show 
	 * @param position position to show image at
	 */
	public void setImage(Bitmap image, int position) {
		setImageWithoutNotif(image, position);
		notifyDataSetChanged();
	}
	
	/**
	 * Adds with notifying any owning view
	 * @param image
	 * @param position
	 */
	private void setImageWithoutNotif(Bitmap image, int position){
		if (image == null){
			GlobalLogger.getInstance().loge(TAG + "[setImage] NULL Image attempted");
			return;
		}
		// Because default size of images is 2 it possible to remove and 
		// replace image
		// If position requested is outside of bounds then append to end
		if (position >= mBitMaps.size())
			mBitMaps.add(image);
		// If position index is negative append to the end
		else if (position < 0)
			mBitMaps.add(0,image);
		// Within bounds remove image
		else {
			mBitMaps.remove(position);
			mBitMaps.add(position, image);
		}
	}
	
	/**
	 * Appends image to the end
	 * @param image
	 */
	public void addImageToEnd(Bitmap image){
		setImage(image, mBitMaps.size());
	}
	
	/**
	 * Appends image to the end
	 * @param image
	 */
	public void addAllImagesToEnd(List<Bitmap> images){
		for (int i = 0; i < images.size(); ++i)
			setImageWithoutNotif(images.get(i), mBitMaps.size());
		notifyDataSetChanged();
	}
	
	public void removeAllExtraImages(){
		Bitmap ref = mBitMaps.get(0);
		Bitmap other = mBitMaps.get(1);
		mBitMaps.clear();
		addImageToEnd(ref);
		addImageToEnd(other);
	}
	
	/**
	 * Resets the image thumbnails to show the search button
	 */
	public void reset(){
		// Reset image to look 
		mBitMaps.clear();
	}
	
	public boolean isDefaultImage(int pos){
		if (pos < 0 || pos >= mBitMaps.size())
			return false;
		else 
			return mBitMaps.get(pos) == mPlaceHolder;
	}
	
	public Bitmap getImage(int pos){
		if (pos < 0 || pos >= mBitMaps.size())
			return null;
		else 
			return mBitMaps.get(pos);
	}

}
