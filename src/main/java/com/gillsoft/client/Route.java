package com.gillsoft.client;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public class Route implements Serializable {

	private static final long serialVersionUID = -7335027962679008301L;
	
	private String scheduleId;
	private String name;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
	private Date date;
	
	private List<RouteDetail> details;

	public String getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<RouteDetail> getDetails() {
		return details;
	}

	public void setDetails(List<RouteDetail> details) {
		this.details = details;
	}

}
