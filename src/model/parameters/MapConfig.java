package model.parameters;

import java.awt.geom.Point2D;

public interface MapConfig {
	String getMapFilename();
	String getCity();
	String getTracesPath();
	String getPartialResultsPath();
	String getCorrectionAlgResults();
	String getInterpolateAlgResults();
	String getPartialGraphFilename();
	String getPartialStreetsFilename();
	String getStreetsFilename();
	String getTrafficLightsFilename();
	String getIndexTableFilename();
	String getTracesListFilename();
	String getTrafficLightsLoaded();
	String getGeneratedTracesListFilename();
	String getAccessPointsFilename();
	String getCongestionGraphFilename();
	Integer getN();
	Integer getM();
	Point2D getMapCentre();
	Integer getBaseColumn();
	Integer getBaseRow();
	Integer getQuot();
	long getInterpolateStartTime();
	long getInterpolateInterval();
	String getResultsJunctionsDetectionAlgPath();
	String getSortedRoadData();
	String getSortedRoadSpeeds();
	double getAreaMinLat();
	double getAreaMaxLat();
	double getAreaMinLon();
	double getAreaMaxLon();
	String getPRGraphFilename();
}