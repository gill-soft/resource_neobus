package com.gillsoft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.gillsoft.abstract_rest_service.SimpleAbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.client.CacheProcessing;
import com.gillsoft.client.Price;
import com.gillsoft.client.PriceDetail;
import com.gillsoft.client.ResponseError;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.RouteDetail;
import com.gillsoft.client.ScheduleTrip;
import com.gillsoft.client.TariffIdModel;
import com.gillsoft.client.Trip;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.client.TripPackage;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Document;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.RequiredField;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ReturnCondition;
import com.gillsoft.model.Route;
import com.gillsoft.model.RoutePoint;
import com.gillsoft.model.Seat;
import com.gillsoft.model.SeatsScheme;
import com.gillsoft.model.Segment;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.TripContainer;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;

@RestController
public class SearchServiceController extends SimpleAbstractTripSearchService<TripPackage> {
	
	@Autowired
	private RestClient client;
	
	@Autowired
	@Qualifier("MemoryCacheHandler")
	private CacheHandler cache;

	@Override
	public TripSearchResponse initSearchResponse(TripSearchRequest request) {
		return simpleInitSearchResponse(cache, request);
	}
	
	@Override
	public void addInitSearchCallables(List<Callable<TripPackage>> callables, String[] pair, Date date) {
		callables.add(() -> {
			TripPackage tripPackage = new TripPackage();
			tripPackage.setRequest(TripSearchRequest.createRequest(pair, date));
			searchTrips(tripPackage);
			return tripPackage;
		});
	}
	
	/*
	 * Получение рейсов с кэша.
	 */
	private void searchTrips(TripPackage tripPackage) {
		tripPackage.setInProgress(false);
		try {
			TripSearchRequest request = tripPackage.getRequest();
			
			// ищем по очереди расписание, рейсы, цены, маршруты
			List<ScheduleTrip> schedule = client.getCachedSchedule(request.getDates().get(0));
			if (schedule != null) {
				tripPackage.setSchedule(schedule);
				
				// если списка нет, то значит поиск был в процессе и можно брать результат с кэша
				if (tripPackage.getTrips() == null) {
					List<Trip> trips = client.getCachedTrips(request.getLocalityPairs().get(0)[0],
							request.getLocalityPairs().get(0)[1], request.getDates().get(0));
					if (!trips.isEmpty()) {
						tripPackage.setTrips(new CopyOnWriteArrayList<>());
						tripPackage.getTrips().addAll(trips);
					}
				}
				// если рейсы есть, то проверяем стоимость и маршрут и добавляем, если их нет
				if (tripPackage.getTrips() != null) {
					
					// для каждого рейса ищем стоимость и маршрут
					for (Trip trip : tripPackage.getTrips()) {
						if (trip.getRoute() == null) {
							try {
								trip.setRoute(client.getCachedRoute(trip.getScheduleId()));
							} catch (Exception e) {
							}
						}
						if (trip.getPrice() == null) {
							try {
								trip.setPrice(client.getCachedPrice(request.getLocalityPairs().get(0)[0],
										request.getLocalityPairs().get(0)[1], trip.getScheduleId(), trip.getTime1()));
							} catch (Exception e) {
								if (e instanceof CacheProcessing) {
									tripPackage.setInProgress(true);
								}
							}
						}
					}
				}
			}
		} catch (ResponseError e) {
			processError(tripPackage, e);
		}
	}
	
	/*
	 * Отлавливаем ошибку типа CacheProcessing только для расписания и списка рейсов.
	 * Эта ошибка означает, что кэш еще формируется и результат будет позже.
	 */
	private void processError(TripPackage tripPackage, ResponseError e) {
		if (e instanceof CacheProcessing) {
			tripPackage.setInProgress(true);
		} else {
			tripPackage.setError(e);
			tripPackage.setInProgress(false);
		}
	}
	
	@Override
	public void addNextGetSearchCallablesAndResult(List<Callable<TripPackage>> callables, Map<String, Vehicle> vehicles,
			Map<String, Locality> localities, Map<String, Organisation> organisations, Map<String, Segment> segments,
			List<TripContainer> containers, TripPackage tripPackage) {
		
		addSearchResult(localities, segments, containers, tripPackage);
		if (tripPackage.isInProgress()) {
			
			// добавляем следующую таску 
			callables.add(() -> {
				searchTrips(tripPackage);
				return tripPackage;
			});
		}
	}

	@Override
	public TripSearchResponse getSearchResultResponse(String searchId) {
		return simpleGetSearchResponse(cache, searchId);
	}
	
	private void addSearchResult(Map<String, Locality> localities, Map<String, Segment> segments,
			List<TripContainer> containers, TripPackage tripPackage) {
		TripContainer container = new TripContainer();
		container.setRequest(tripPackage.getRequest());
		if (tripPackage.getError() != null) {
			container.setError(new RestError(tripPackage.getError().getMessage()));
		}
		if (tripPackage.getTrips() != null) {
			List<com.gillsoft.model.Trip> trips = new ArrayList<>();
			for (int i = tripPackage.getTrips().size() - 1; i >= 0; i--) {
				Trip trip = tripPackage.getTrips().get(i);
				if (trip.getPrice() != null) {
					com.gillsoft.model.Trip resTrip = new com.gillsoft.model.Trip();
					
					// добавляем сегменты
					resTrip.setId(addSegment(localities, segments, tripPackage, trip));
					trips.add(resTrip);
					
					tripPackage.getTrips().remove(i);
				}
			}
			container.setTrips(trips);
		}
		containers.add(container);
	}
	
	private String addSegment(Map<String, Locality> localities, Map<String, Segment> segments, TripPackage tripPackage,
			Trip trip) {
		String key = new TripIdModel(trip.getScheduleId(), trip.getStationId1(), trip.getStationId2(), trip.getTime1()).asString();
		Segment segment = new Segment();
		
		// проставляем номера рейсов
		for (ScheduleTrip scheduleTrip : tripPackage.getSchedule()) {
			if (Objects.equals(scheduleTrip.getScheduleId(), trip.getScheduleId())) {
				segment.setNumber(scheduleTrip.getName());
				break;
			}
		}
		segment.setDepartureDate(trip.getTime1());
		segment.setArrivalDate(trip.getTime2());
		
		segment.setDeparture(addStation(localities, trip.getStationId1()));
		segment.setArrival(addStation(localities, trip.getStationId2()));
		
		// стоимость
		addPrice(trip.getPrice(), segment);
		
		// маршрут
		segment.setRoute(createRoute(localities, trip.getRoute()));
		
		segments.put(key, segment);
		return key;
	}
	
	private void addPrice(Price price, Segment segment) {
		com.gillsoft.model.Price tripPrice = new com.gillsoft.model.Price();
		PriceDetail maxDetail = null;
		for (PriceDetail detail : price.getDetails()) {
			if (maxDetail == null
					|| maxDetail.getPrice().compareTo(detail.getPrice()) < 0) {
				maxDetail = detail;
			}
		}
		tripPrice.setCurrency(Currency.PLN);
		tripPrice.setAmount(maxDetail.getPrice());
		tripPrice.setTariff(createTariff(maxDetail));
		
		segment.setPrice(tripPrice);
	}
	
	private Route createRoute(Map<String, Locality> localities, com.gillsoft.client.Route route) {
		if (route == null) {
			return null;
		}
		Route tripRoute = new Route();
		tripRoute.setName(route.getName());
		tripRoute.setNumber(route.getScheduleId());
		
		Collections.sort(route.getDetails(), (route1, route2) -> {
			return route1.getTime().compareTo(route2.getTime());
		});
		tripRoute.setPath(new ArrayList<>());
		Date first = route.getDetails().get(0).getTime();
		for (RouteDetail routeDetail : route.getDetails()) {
			RoutePoint point = new RoutePoint();
			point.setArrivalDay(Days.daysBetween(
					new LocalDate(first), new LocalDate(routeDetail.getTime())).getDays());
			point.setId(routeDetail.getDetailId());
			point.setLocality(addStation(localities, routeDetail.getStationId()));
			point.setDepartureTime(RestClient.timeFormat.format(routeDetail.getTime()));
			tripRoute.getPath().add(point);
		}
		return tripRoute;
	}
	
	public static Locality addStation(Map<String, Locality> localities, String id) {
		Locality fromDict = LocalityServiceController.getLocality(id);
		if (fromDict == null) {
			return null;
		}
		if (localities == null) {
			return fromDict;
		}
		String fromDictId = fromDict.getId();
		try {
			fromDict = fromDict.clone();
			fromDict.setId(null);
		} catch (CloneNotSupportedException e) {
		}
		Locality locality = localities.get(fromDictId);
		if (locality == null) {
			localities.put(fromDictId, fromDict);
		}
		return new Locality(fromDictId);
	}

	@Override
	public Route getRouteResponse(String tripId) {
		try {
			TripIdModel model = new TripIdModel().create(tripId);
			com.gillsoft.client.Route route = client.getCachedRoute(model.getId());
			return createRoute(null, route);
		} catch (Exception e) {
			throw new RestClientException(e.getMessage());
		}
	}

	@Override
	public SeatsScheme getSeatsSchemeResponse(String tripId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Seat> getSeatsResponse(String tripId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Tariff> getTariffsResponse(String tripId) {
		TripIdModel model = new TripIdModel().create(tripId);
		try {
			Price price = client.getCachedPrice(model.getFrom(), model.getTo(), model.getId(), model.getDate());
			List<Tariff> tariffs = new ArrayList<>();
			for (PriceDetail detail : price.getDetails()) {
				tariffs.add(createTariff(detail));
			}
			return tariffs;
		} catch (Exception e) {
			throw new RestClientException(e.getMessage());
		}
	}
	
	private Tariff createTariff(PriceDetail detail) {
		Tariff tariff = new Tariff();
		tariff.setId(new TariffIdModel(detail.getPrice()).asString());
		tariff.setValue(detail.getPrice());
		tariff.setAvailableCount(detail.getSeats());
		return tariff;
	}

	@Override
	public List<RequiredField> getRequiredFieldsResponse(String tripId) {
		List<RequiredField> fields = new ArrayList<>();
		fields.add(RequiredField.NAME);
		fields.add(RequiredField.SURNAME);
		fields.add(RequiredField.PHONE);
		fields.add(RequiredField.EMAIL);
		return fields;
	}

	@Override
	public List<Seat> updateSeatsResponse(String tripId, List<Seat> seats) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<ReturnCondition> getConditionsResponse(String tripId, String tariffId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Document> getDocumentsResponse(String tripId) {
		throw RestClient.createUnavailableMethod();
	}

}
