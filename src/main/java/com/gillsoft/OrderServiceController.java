package com.gillsoft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractOrderService;
import com.gillsoft.client.Offer;
import com.gillsoft.client.OrderIdModel;
import com.gillsoft.client.ResponseError;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.TariffIdModel;
import com.gillsoft.client.Ticket;
import com.gillsoft.client.TicketIdModel;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Price;
import com.gillsoft.model.RestError;
import com.gillsoft.model.Segment;
import com.gillsoft.model.ServiceItem;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.request.OrderRequest;
import com.gillsoft.model.response.OrderResponse;

@RestController
public class OrderServiceController extends AbstractOrderService {
	
	@Autowired
	private RestClient client;

	@Override
	public OrderResponse createResponse(OrderRequest request) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();
		response.setCustomers(request.getCustomers());
		
		Map<String, Locality> localities = new HashMap<>();
		Map<String, Segment> segments = new HashMap<>();
		List<ServiceItem> resultItems = new ArrayList<>();
		
		OrderIdModel orderId = new OrderIdModel(new ArrayList<>());
		
		for (ServiceItem item : request.getServices()) {
			TripIdModel tripId = new TripIdModel().create(item.getSegment().getId());
			TariffIdModel tariffId = new TariffIdModel().create(item.getPrice().getTariff().getId());
			ServiceItem resItem;
			try {
				Offer offer = client.lockSeats(tripId.getFrom(), tripId.getTo(), tripId.getId());
				Ticket ticket = client.create(request.getCustomers().get(item.getCustomer().getId()), tripId.getFrom(),
						tripId.getTo(), tripId.getId(), tariffId.getPrice(), offer.getOfferId());
				
				TicketIdModel ticketId = new TicketIdModel(ticket.getTicketId(), tripId);
				resItem = createService(ticket, ticketId, localities, segments);
				resItem.setCustomer(item.getCustomer());
				resultItems.add(resItem);
				orderId.getTickets().add(ticketId);
			} catch (ResponseError e) {
				item.setError(new RestError(e.getMessage()));
				resultItems.add(item);
			}
		}
		response.setOrderId(orderId.asString());
		response.setCustomers(request.getCustomers());
		response.setLocalities(localities);
		response.setSegments(segments);
		response.setServices(resultItems);
		return response;
	}
	
	private ServiceItem createService(Ticket ticket, TicketIdModel ticketId, Map<String, Locality> localities,
			Map<String, Segment> segments) {
		ServiceItem resItem = new ServiceItem();
		resItem.setId(ticketId.asString());
		resItem.setNumber(ticket.getTicketNumber());
		
		Price price = new Price();
		price.setCurrency(Currency.PLN);
		Tariff tariff = new Tariff();
		tariff.setId(new TariffIdModel(ticket.getPrice()).asString());
		tariff.setValue(ticket.getPrice());
		price.setTariff(tariff);
		resItem.setPrice(price);
		
		Segment segment = new Segment();
		segment.setNumber(ticket.getCruiseName());
		segment.setDeparture(SearchServiceController.addStation(localities, ticket.getStationId1()));
		segment.setArrival(SearchServiceController.addStation(localities, ticket.getStationId2()));
		
		String segmentId = ticketId.getTripIdModel().asString();
		segments.put(segmentId, segment);
		
		Segment itemSegment = new Segment();
		itemSegment.setId(segmentId);
		resItem.setSegment(itemSegment);
		
		return resItem;
	}

	@Override
	public OrderResponse addServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse removeServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse updateCustomersResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse getResponse(String orderId) {

		// преобразовываем ид заказа в объкт
		OrderIdModel orderIdModel = new OrderIdModel().create(orderId);

		OrderResponse response = getTickets(orderIdModel.getTickets());
		response.setId(orderId);
		
		return response;
	}

	@Override
	public OrderResponse getServiceResponse(String serviceId) {
		return getTickets(Collections.singletonList(new TicketIdModel().create(serviceId)));
	}
	
	private OrderResponse getTickets(List<TicketIdModel> tickets) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();
		Map<String, Locality> localities = new HashMap<>();
		Map<String, Segment> segments = new HashMap<>();

		// преобразовываем ид заказа в объкт
		List<ServiceItem> resultItems = new ArrayList<>(tickets.size());
		
		// выкупаем заказы и формируем ответ
		for (TicketIdModel ticket : tickets) {
			ServiceItem serviceItem = new ServiceItem();
			serviceItem.setId(ticket.asString());
			try {
				Ticket resTicket = client.get(ticket.getId());
				createService(resTicket, ticket, localities, segments);
			} catch (ResponseError e) {
				serviceItem.setError(new RestError(e.getMessage()));
			}
			resultItems.add(serviceItem);
		}
		response.setLocalities(localities);
		response.setSegments(segments);
		response.setServices(resultItems);
		return response;
	}

	@Override
	public OrderResponse bookingResponse(String orderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse confirmResponse(String orderId) {
		return operationResponse(orderId, true);
	}
	
	private void addServiceItems(List<ServiceItem> resultItems, TicketIdModel ticket, boolean confirmed, RestError error) {
		ServiceItem serviceItem = new ServiceItem();
		serviceItem.setId(ticket.asString());
		serviceItem.setConfirmed(confirmed);
		serviceItem.setError(error);
		resultItems.add(serviceItem);
	}
	
	public OrderResponse operationResponse(String orderId, boolean confirm) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();

		// преобразовываем ид заказа в объкт
		OrderIdModel orderIdModel = new OrderIdModel().create(orderId);
		List<ServiceItem> resultItems = new ArrayList<>(orderIdModel.getTickets().size());
		
		// выкупаем заказы и формируем ответ
		for (TicketIdModel ticket : orderIdModel.getTickets()) {
			try {
				if (confirm) {
					client.confirm(ticket.getId());
				} else {
					client.cancel(ticket.getId());
				}
				addServiceItems(resultItems, ticket, true, null);
			} catch (ResponseError e) {
				addServiceItems(resultItems, ticket, false, new RestError(e.getMessage()));
			}
		}
		response.setOrderId(orderId);
		response.setServices(resultItems);
		return response;
	}

	@Override
	public OrderResponse cancelResponse(String orderId) {
		return operationResponse(orderId, false);
	}

	@Override
	public OrderResponse prepareReturnServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse returnServicesResponse(OrderRequest request) {
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>(request.getServices().size()));
		for (ServiceItem serviceItem : request.getServices()) {
			TicketIdModel model = new TicketIdModel().create(serviceItem.getId());
			try {
				client.cancel(model.getId());
				serviceItem.setConfirmed(true);
			} catch (ResponseError e) {
				serviceItem.setError(new RestError(e.getMessage()));
			}
			response.getServices().add(serviceItem);
		}
		return response;
	}

	@Override
	public OrderResponse getPdfDocumentsResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

}
