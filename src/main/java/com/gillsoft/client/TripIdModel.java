package com.gillsoft.client;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.gillsoft.model.AbstractJsonModel;

public class TripIdModel extends AbstractJsonModel {
	
	private static final long serialVersionUID = -7376845699198065470L;
	
	private String id;
	private String from;
	private String to;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	private Date date;
	
	public TripIdModel() {
		
	}

	public TripIdModel(String id, String from, String to, Date date) {
		super();
		this.id = id;
		this.from = from;
		this.to = to;
		this.date = date;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	@Override
	public TripIdModel create(String json) {
		return (TripIdModel) super.create(json);
	}

}
