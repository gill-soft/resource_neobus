package com.gillsoft.client;

import java.io.Serializable;
import java.util.List;

public class Price implements Serializable {

	private static final long serialVersionUID = -1043884951559815628L;
	
	private String stationId1;
	private String stationId2;
	private String cruise;
	private List<PriceDetail> details;

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

	public String getCruise() {
		return cruise;
	}

	public void setCruise(String cruise) {
		this.cruise = cruise;
	}

	public List<PriceDetail> getDetails() {
		return details;
	}

	public void setDetails(List<PriceDetail> details) {
		this.details = details;
	}

}
