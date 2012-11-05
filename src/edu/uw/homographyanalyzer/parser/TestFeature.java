package edu.uw.homographyanalyzer.parser;

import java.util.ArrayList;

import android.graphics.Point;

/**
 * Test Feature for just checking the ability to read in source data.
 * @author mhotan
 */
public class TestFeature extends DisplayFeature {
	
	private static int DEFAULT_NUM_POINTS = 12; //TODO Verify this number is appropiate
	
	/**
	 * Debug tool intended to be used to help see who wrote this 
	 */
	private final String mOwnerName;
	
	/**
	 * Name and ID should be unique to the corresponding image. It is the client responsibility to do
	 * @param name associated with this feature
	 * @param id associated with this feature 
	 */
	public TestFeature(String featureName, String owner, int id){
		mName = featureName;
		mOwnerName = owner;
		mID = id;
		mPoints = new ArrayList<Point>(DEFAULT_NUM_POINTS);
	}
	
	/**
	 * Adds point that corresponds to the outer lying shape
	 * @param p 
	 */
	public void addPoint(Point p){
		
	}
	
	/**
	 * Removes point from corresponding feature
	 * @param p
	 * @return
	 */
	public boolean removePoint(Point p){
		if (!mPoints.contains(p)) return false;
		mPoints.remove(p); return true;
	}
	
}
