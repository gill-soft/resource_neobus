package com.gillsoft.client;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public class RouteDetail implements Serializable {

	private static final long serialVersionUID = 1190600683100603892L;
	
	private String detailId;
	private String scheduleId;
	private String stationId;
	private boolean boardingAllowed;
	private boolean unboardingAllowed;

	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Date time;
	private int order;

	public String getDetailId() {
		return detailId;
	}

	public void setDetailId(String detailId) {
		this.detailId = detailId;
	}

	public String getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}

	public String getStationId() {
		return stationId;
	}

	public void setStationId(String stationId) {
		this.stationId = stationId;
	}

	public boolean isBoardingAllowed() {
		return boardingAllowed;
	}

	public void setBoardingAllowed(boolean boardingAllowed) {
		this.boardingAllowed = boardingAllowed;
	}

	public boolean isUnboardingAllowed() {
		return unboardingAllowed;
	}

	public void setUnboardingAllowed(boolean unboardingAllowed) {
		this.unboardingAllowed = unboardingAllowed;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
