package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TicketIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 5620665150690960973L;

	private String id;
	private TripIdModel tripIdModel;
	
	public TicketIdModel() {
		
	}

	public TicketIdModel(String id, TripIdModel tripIdModel) {
		this.id = id;
		this.tripIdModel = tripIdModel;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TripIdModel getTripIdModel() {
		return tripIdModel;
	}

	public void setTripIdModel(TripIdModel tripIdModel) {
		this.tripIdModel = tripIdModel;
	}

	@Override
	public TicketIdModel create(String json) {
		return (TicketIdModel) super.create(json);
	}
}
