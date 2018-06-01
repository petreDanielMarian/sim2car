package application.trafficLight;

import java.io.Serializable;

public class ApplicationTrafficLightControlData implements Serializable {
	private String msg;
	private Long carId;
	private Long wayId;
	private int direction;
	
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
