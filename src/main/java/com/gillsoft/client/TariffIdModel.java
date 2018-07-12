package com.gillsoft.client;

import java.math.BigDecimal;

import com.gillsoft.model.AbstractJsonModel;

public class TariffIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 6914701845642755939L;

	private BigDecimal price;
	
	public TariffIdModel() {
		
	}

	public TariffIdModel(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	@Override
	public TariffIdModel create(String json) {
		return (TariffIdModel) super.create(json);
	}

}
