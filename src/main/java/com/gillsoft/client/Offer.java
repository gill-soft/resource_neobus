package com.gillsoft.client;

import java.math.BigDecimal;

public class Offer {

	private String priceId;
	private String station1;
	private String station2;
	private BigDecimal price;
	private int seats;
	private boolean promotions;
	private String offerId;

	public String getPriceId() {
		return priceId;
	}

	public void setPriceId(String priceId) {
		this.priceId = priceId;
	}

	public String getStation1() {
		return station1;
	}

	public void setStation1(String station1) {
		this.station1 = station1;
	}

	public String getStation2() {
		return station2;
	}

	public void setStation2(String station2) {
		this.station2 = station2;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public int getSeats() {
		return seats;
	}

	public void setSeats(int seats) {
		this.seats = seats;
	}

	public boolean isPromotions() {
		return promotions;
	}

	public void setPromotions(boolean promotions) {
		this.promotions = promotions;
	}

	public String getOfferId() {
		return offerId;
	}

	public void setOfferId(String offerId) {
		this.offerId = offerId;
	}

}
