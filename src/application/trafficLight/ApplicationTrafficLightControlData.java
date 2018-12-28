package application.trafficLight;

import java.io.Serializable;

import model.MapPoint;

/**
 * This class is used to represent the traffic light control app data which is send from the car to the
 * master traffic light
 * @author Andreea
 *
 */
public class ApplicationTrafficLightControlData implements Serializable {
	private String msg;
	private Long carId;
	private Long wayId;
	private int direction;
	private MapPoint mapPoint;
	private long timeStop;
	
	public long getTimeStop() {
		return timeStop;
	}
	public void setTimeStop(long timeStop) {
		this.timeStop = timeStop;
	}
	public MapPoint getMapPoint() {
		return mapPoint;
	}
	public void setMapPoint(MapPoint mapPoint) {
		this.mapPoint = mapPoint;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public Long getCarId() {
		return carId;
	}
	public void setCarId(Long carId) {
		this.carId = carId;
	}
	public Long getWayId() {
		return wayId;
	}
	public void setWayId(Long wayId) {
		this.wayId = wayId;
	}
	public int getDirection() {
		return direction;
	}
	public void setDirection(int direction) {
		this.direction = direction;
	}
}
