package com.gillsoft.client;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.logging.SimpleRequestResponseLoggingInterceptor;
import com.gillsoft.util.RestTemplateUtil;
import com.google.common.base.Objects;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class RestClient {
	
	public static final String STATIONS_CACHE_KEY = "neobus.stations";
	public static final String ROUTE_CACHE_KEY = "neobus.route.";
	public static final String SCHEDULE_CACHE_KEY = "neobus.schedule.";
	public static final String PRICE_CACHE_KEY = "neobus.price.";
	public static final String TRIPS_CACHE_KEY = "neobus.trips.";
	
	private static final String STATUS_OK = "OK";
	private static final String STATUS_ERROR = "Error";
	private static final String NOT_LOGGED = "NOT LOGGED";
	
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm";
	
	public final static FastDateFormat dateFormat = FastDateFormat.getInstance(DATE_FORMAT);
	public final static FastDateFormat fullDateFormat = FastDateFormat.getInstance(FULL_DATE_FORMAT);
	
	private static final String LOGIN = "login";
	private static final String STATIONS = "station";
	private static final String ROUTE = "schedule/{0}/detailed";
	private static final String SCHEDULE = "schedule/{0}";
	private static final String SCHEDULE_FROM_TO = "schedule/{0}/from/{1}/to/{2}";
	private static final String PRICES = "price/from/{0}/to/{1}/cruise/{2}/details";

	@Autowired
    @Qualifier("RedisMemoryCache")
	private CacheHandler cache;
	
	private RestTemplate template;
	
	// для запросов поиска с меньшим таймаутом
	private RestTemplate searchTemplate;
	
	public RestClient() {
		template = createNewPoolingTemplate(Config.getRequestTimeout());
		searchTemplate = createNewPoolingTemplate(Config.getSearchRequestTimeout());
	}
	
	public RestTemplate createNewPoolingTemplate(int requestTimeout) {
		RestTemplate template = new RestTemplate(new BufferingClientHttpRequestFactory(
				RestTemplateUtil.createPoolingFactory(Config.getUrl(), 300, requestTimeout)));
		template.setInterceptors(Collections.singletonList(
				new SimpleRequestResponseLoggingInterceptor()));
		return template;
	}
	
	public void login() throws ResponseError {
		LoginRequest request = new LoginRequest();
		request.setLogin(Config.getLogin());
		request.setPassword(Config.getPassword());
		getResult(template, request, LOGIN, HttpMethod.POST,
				new ParameterizedTypeReference<Response<Object>>() { }, false);
	}
	
	@SuppressWarnings("unchecked")
	public List<Station> getStationsFromCache() throws IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.STATIONS_CACHE_KEY);
		params.put(RedisMemoryCache.IGNORE_AGE, true);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheStationsUpdateDelay());
		params.put(RedisMemoryCache.UPDATE_TASK, new StationsUpdateTask());
		return (List<Station>) cache.read(params);
	}
	
	public List<Station> getStations() throws ResponseError {
		return getResult(template, null, STATIONS, HttpMethod.GET,
				new ParameterizedTypeReference<Response<List<Station>>>() { }, true);
	}
	
	public List<ScheduleTrip> getSchedule(Date date) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(SCHEDULE, dateFormat.format(date)),
				HttpMethod.GET, new ParameterizedTypeReference<Response<List<ScheduleTrip>>>() { }, true);
	}
	
	public List<Trip> getTrips(String from, String to, Date date) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(SCHEDULE_FROM_TO, dateFormat.format(date), from, to),
				HttpMethod.GET, new ParameterizedTypeReference<Response<List<Trip>>>() { }, true);
	}
	
	public Price getTrips(String from, String to, String tripId) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(PRICES, from, to, tripId),
				HttpMethod.GET, new ParameterizedTypeReference<Response<Price>>() { }, true);
	}
	
	public Route getRoute(String tripId) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(ROUTE, tripId),
				HttpMethod.GET, new ParameterizedTypeReference<Response<Route>>() { }, true);
	}
	
	private void checkStatus(Response<?> response) throws ResponseError {
		if (response == null) {
			throw new ResponseError("Empty response");
		}
		if (Objects.equal(response.getStatus(), STATUS_ERROR)) {
			throw new ResponseError(response.getMessage());
		}
		if (!Objects.equal(response.getStatus(), STATUS_OK)) {
			throw new ResponseError("Unknown status");
		}
	}
	
	private <T> T getResult(RestTemplate template, Request request, String method, HttpMethod httpMethod,
			ParameterizedTypeReference<Response<T>> type, boolean checkLogged) throws ResponseError {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl() + method)
				.build().toUri();
		RequestEntity<Request> requestEntity = new RequestEntity<Request>(request, httpMethod, uri);
		ResponseEntity<Response<T>> response = template.exchange(requestEntity, type);
		Response<T> resultResponse = response.getBody();
		try {
			checkStatus(resultResponse);
		} catch (ResponseError e) {
			if (checkLogged) {
				if (e.getMessage() != null
						&& e.getMessage().toUpperCase().contains(NOT_LOGGED)) {
					login();
					return getResult(template, request, method, httpMethod, type, false);
				}
			}
		}
		return response.getBody().getData();
	}
	
	public static void main(String[] args) {
		RestClient client = new RestClient();
		try {
			for (Station station : client.getStations()) {
				System.out.println(station.getName());
			}
		} catch (ResponseError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public CacheHandler getCache() {
		return cache;
	}
	
	public static String getRouteCacheKey(String tripId) {
		return ROUTE_CACHE_KEY + tripId;
	}
	
	public static String getScheduleCacheKey(Date date) {
		return SCHEDULE_CACHE_KEY + date.getTime();
	}
	
}
