package edu.uw.homographyanalyzer.parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Point;

import android.graphics.Bitmap;

/**
 * Container abstract class to represent a set of images that correspond to a complete Appliance
 * <b>It must contain a reference image that must be properly annotated</b> 
 * <b>It can contain a set of other images that are perspectie warps of reference image </b>
 * 
 * @author mhotan
 */
public abstract class ApplianceImageSet {

	/**
	 * Hidden data abstraction for containing data
	 * Features are only identified by 
	 */
	private final Map<String, List<Point>> mFeatures = new HashMap<String, List<Point>>();
	
	/**
	 * Adds a new feature to the appliance image set
	 * 
	 * @requires neither argument to be null, or points to have a size less then two
	 * @param featureName Name of feature
	 * @param points List of points
	 */
	public void addFeature(String featureName,  List<Point> points){
		if (featureName.length() == 0)
			throw new IllegalArgumentException("Feature name Empty: \"" + featureName +"\"");
		if (points.size() <= 2)
			throw new IllegalArgumentException("Not enough points for this feature Number of Points: " + points.size());
		
		List<Point> hiddenPoints = new LinkedList<Point>();
		for (Point p: points){
			hiddenPoints.add(p.clone());
		}
		mFeatures.put(featureName, hiddenPoints);
	}
	 
	/**
	 * Each image set contains a known set of features that are distinguishable on the appliance <b>
	 * To be able to reference the image shapes the all the names of the features must be known<b>
	 * This indicates that every feature name should be unique <b>
	 * 
	 * @return unmodifiable list of names of features, if no features exist list is empty
	 */
	public List<String> getFeatures(){
		return new LinkedList<String>(mFeatures.keySet());
	}
	
	/**
	 * Given a feature name found from call to getFeatures() it returns the list of corresponding points of the image
	 * <b>  The list of points returned will have more then two points 
	 * <b>  The point list will not be closed, each point
	 * 
	 * @param featureName
	 * @return null if feature does not exist, Empty list when feature is incomplete, else list of Points
	 */
	public List<Point> getShapePoints(final String featureName){
		if (!mFeatures.containsKey(featureName))
			return null;
		List<Point> points = new LinkedList<Point>(mFeatures.get(featureName));
		if (points.size() <= 2){
			points.clear();
			return points;
		}
		//Create copy of mutable list of points
		List<Point> hiddenPoints = new LinkedList<Point>();
		for (Point p: points){
			hiddenPoints.add(p.clone());
		}
		return hiddenPoints;
	}
	
	/**
	 * 
	 * @return reference image for this appliance
	 */
	public abstract Bitmap getReferenceImage();
	
	/**
	 * @return all non reference images
	 */
	public abstract Bitmap[] getNonReferenceImages();
	
	
}
