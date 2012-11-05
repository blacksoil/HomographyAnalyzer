package edu.uw.homographyanalyzer.parser;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Point;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.util.Log;

public class XMLTestImageSet extends ApplianceImageSet {

	private static final String TAG = "XMLTestImageSet";

	private int mReferenceImageID;

	/**
	 * 
	 * @param featureXMLID
	 * @param imageIDs
	 * @throws IOException For an IO Exception that occurs while parsing ID
	 * @throws XmlPullParserException Illformatted XML format
	 */
	public XMLTestImageSet(Activity activity, int featureXMLID, int referenceImageID) 
			throws IOException, XmlPullParserException{

		if (referenceImageID > 0)
			mReferenceImageID = referenceImageID;
		else
			throw new IllegalArgumentException("Illegal Reference ID");

		////////////////////////////////////////////
		// Parse the XML for obtaining features
		Resources res = activity.getResources();
		XmlResourceParser xpp = res.getXml(featureXMLID);
		xpp.next();

		int eventType = xpp.getEventType();
		StringBuffer stringBuffer = new StringBuffer();
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if(eventType == XmlPullParser.START_DOCUMENT)
			{
				stringBuffer.append("--- Start XML ---");
			}
			else if(eventType == XmlPullParser.START_TAG)
			{
				//				stringBuffer.append("\nSTART_TAG: "+ xpp.getName() + " Text: " + xpp.getText());
				String object = xpp.getName();

				// Iterate through all the tags 
				// find all the objects
				if (object.equals("object")){
					int objectType = xpp.next();
					String tagName = xpp.getName(); 

					//Variables to store object variables
					String objectName = null;
					List<Point> fPoints = new LinkedList<Point>();

					// WHile have not reached end tag for object
					while (!(objectType == XmlPullParser.END_TAG && 
							object.equals(tagName))){

						stringBuffer.append("\nObject Start: " + xpp.getName());

						// If the current tag is the name object
						if (objectType == XmlPullParser.START_TAG &&
								tagName.equals("name")){
							if (xpp.next() == XmlPullParser.TEXT){
								objectName = xpp.getText().trim();
							}
							else objectName = null;
						}

						stringBuffer.append("\nObject Name: " + xpp.getName());

						// For every point within the object add to list
						if (objectType == XmlPullParser.START_TAG &&
								tagName != null && tagName.equals("pt")){

							int ptType = xpp.next();
							String ptTagName = xpp.getName(); 

							// Iterate through the point and look for x and y
							Point p  = new Point();
							p.x = -1;
							p.y = -1;
							while (!(ptType == XmlPullParser.END_TAG &&
									ptTagName != null && ptTagName.equals("pt"))){

								// If is X coordinate
								if (ptType == XmlPullParser.START_TAG &&
										ptTagName != null && ptTagName.equals("x")){
									if (xpp.next() == XmlPullParser.TEXT){
										String text = xpp.getText();
										text = text.replace("\n", "");
										p.x = Double.parseDouble(text);
									}
								}

								// If is Y coordinate
								if (ptType == XmlPullParser.START_TAG &&
										ptTagName != null && ptTagName.equals("y")){
									if (xpp.next() == XmlPullParser.TEXT){
										String text = xpp.getText();
										text = text.replace("\n", "");
										p.y = Double.parseDouble(text);
									}
								}

								//Check if the point is complete

								ptType = xpp.next();
								ptTagName = xpp.getName(); 

								//If end of point has been reached 
								// check if point is valid
								// Create a new one
								if (ptType == XmlPullParser.END_TAG &&
										ptTagName != null && ptTagName.equals("pt")){

									if (p.x != -1 && p.y != -1){
										// valid point so add to list
										stringBuffer.append("\nObject Feature Point: " + p);
										fPoints.add(p);
									}

									p  = new Point();
									p.x = -1;
									p.y = -1;
								}
							}
						}

						// Update the tag name and tag id number
						objectType = xpp.next();
						tagName = xpp.getName(); 
					}

					stringBuffer.append("\nObject END: " + xpp.getName());

					// Check for name 
					if (objectName == null){
						Log.e(TAG, "Object had no name");
					}
					else if (fPoints.size() <= 2){
						Log.e(TAG, "Size of Feature points to small: " + fPoints.size());
					} else {
						//Finally add the feature to points
						addFeature(objectName, fPoints);
					}
				}
			}
			eventType = xpp.next();
		}
		stringBuffer.append("\n--- End XML ---");
		Log.i(TAG, "event name: " + stringBuffer);
	}

	@Override
	public Bitmap getReferenceImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bitmap[] getNonReferenceImages() {
		// TODO Auto-generated method stub
		return null;
	}

}
