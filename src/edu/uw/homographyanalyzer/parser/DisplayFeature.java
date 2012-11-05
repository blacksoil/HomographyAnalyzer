package edu.uw.homographyanalyzer.parser;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;

/**
 * <p>Contains the base contents for a distinguishable feature
 * inside an image. </p> 
 * 
 * The image is associated with values that correspond to different attributes and labels.  
 * it is up to any client class that implements subclasses of this to decide the value of the attributes.
 * 
 * 
 * @author mhotan
 */
public abstract class DisplayFeature {
	
	private static final String EMPTY_STRING = "";
	
	/**
	 * Name of the feature 
	 */
	protected String mName;
	
	/**
	 * General description of this feature.
	 */
	protected String mDescription;
	
	/**
	 * List of points of this feature that describe its overall shape.
	 */
	protected List<Point> mPoints;
	
	/**
	 * Identification number of this feature
	 */
	protected int mID;
	
	/**
	 * @return name of this feature
	 */
	public String getName(){
		return mName;
	}
	
	/**
	 * @return  the description of this feature, Or empty string
	 */
	public String getDescription(){
		return mDescription == null ? EMPTY_STRING : mDescription;
	}
	
	/**
	 * @return Copy of the list of  
	 */
	public List<Point> getPoints(){
		assert mPoints != null: "list of points null";
		
		// Create a mutable copy of contained points
		ArrayList<Point> points = new ArrayList<Point>(mPoints.size());
		for (Point p: mPoints){
			points.add(new Point(p.x, p.y));
		}
		
		return points;
	}
	
	/**
	 * @return Identification number of the feature
	 */
	public int getID(){
		return mID;
	}
	
}
