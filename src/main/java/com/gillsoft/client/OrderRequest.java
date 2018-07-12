package com.gillsoft.client;

import java.math.BigDecimal;

public class OrderRequest {

	private String name;
	private String surname;
	private String email;
	private String phone;
	private String remarks;
	private int seats;
	private String cruiseId;
	private BigDecimal price;
	private String stationId1;
	private String stationId2;
	private String offerId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public int getSeats() {
		return seats;
	}

	public void setSeats(int seats) {
		this.seats = seats;
	}

	public String getCruiseId() {
		return cruiseId;
	}

	public void setCruiseId(String cruiseId) {
		this.cruiseId = cruiseId;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
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

	public String getOfferId() {
		return offerId;
	}

	public void setOfferId(String offerId) {
		this.offerId = offerId;
	}

}
