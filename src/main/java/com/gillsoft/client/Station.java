package com.gillsoft.client;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Station implements Serializable {

	private static final long serialVersionUID = 7121470317496562253L;
	
	private String stationId;
	private String name;
	private String description;
	private String group;

	@JsonProperty("isActive")
	private boolean active;

	public String getStationId() {
		return stationId;
	}

	public void setStationId(String stationId) {
		this.stationId = stationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
