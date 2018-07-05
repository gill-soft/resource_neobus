package com.gillsoft.client;

import java.io.Serializable;
import java.math.BigDecimal;

public class PriceDetail implements Serializable {

	private static final long serialVersionUID = -5474247651133026826L;
	
	private BigDecimal price;
	
	private int seats;

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

}
