package com.gillsoft.client;

import java.util.List;

import com.gillsoft.model.AbstractJsonModel;

public class OrderIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = -1362021293089343288L;
	
	private List<TicketIdModel> tickets;
	
	public OrderIdModel() {
		
	}

	public OrderIdModel(List<TicketIdModel> tickets) {
		this.tickets = tickets;
	}

	public List<TicketIdModel> getTickets() {
		return tickets;
	}

	public void setTickets(List<TicketIdModel> tickets) {
		this.tickets = tickets;
	}
	
	@Override
	public OrderIdModel create(String json) {
		return (OrderIdModel) super.create(json);
	}

}
