package com.gillsoft.client;

import java.io.Serializable;

public class ScheduleTrip implements Serializable {

	private static final long serialVersionUID = 4744807582823256962L;
	
	private String scheduleId;
	private String name;

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

}
