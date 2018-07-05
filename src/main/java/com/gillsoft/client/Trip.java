package com.gillsoft.client;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public class Trip implements Serializable {
	
	private static final long serialVersionUID = -6616547381960272738L;
	
	private String scheduleId;
	private String stationId1;
	private String stationId2;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Date time1;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Date time2;

	public String getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}

	public String getStationId1() {
		return stationId1;
	}

	public void setStationId1(String stationId1) {
		this.stationId1 = stationId1;
	}

	public String getStationId2() {
		return stationId2;
	}

	public void setStationId2(String stationId2) {
		this.stationId2 = stationId2;
	}

	public Date getTime1() {
		return time1;
	}

	public void setTime1(Date time1) {
		this.time1 = time1;
	}

	public Date getTime2() {
		return time2;
	}

	public void setTime2(Date time2) {
		this.time2 = time2;
	}

}
