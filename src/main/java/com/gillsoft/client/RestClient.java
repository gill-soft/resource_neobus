package com.gillsoft.client;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.logging.SimpleRequestResponseLoggingInterceptor;
import com.gillsoft.util.RestTemplateUtil;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class RestClient {
	
	public static final String STATIONS_CACHE_KEY = "neobus.stations";
	public static final String ROUTE_CACHE_KEY = "neobus.route.";
	public static final String SCHEDULE_CACHE_KEY = "neobus.schedule.";
	public static final String PRICE_CACHE_KEY = "neobus.price.";
	public static final String TRIPS_CACHE_KEY = "neobus.trips.";
	
	private static final String STATUS_OK = "OK";
	private static final String STATUS_ERROR = "ERROR";
	private static final String NOT_LOGGED = "NOT LOGGED";
	
	public static final String TIME_FORMAT = "HH:mm";
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm";
	
	public final static FastDateFormat timeFormat = FastDateFormat.getInstance(TIME_FORMAT);
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
	
	private HttpHeaders headers = new HttpHeaders();
	
	private RestTemplate template;
	
	// для запросов поиска с меньшим таймаутом
	private RestTemplate searchTemplate;
	
	public RestClient() {
		template = createNewPoolingTemplate(Config.getRequestTimeout());
		searchTemplate = createNewPoolingTemplate(Config.getSearchRequestTimeout());
	}
	
	public RestTemplate createNewPoolingTemplate(int requestTimeout) {
		RestTemplate template = new RestTemplate(new BufferingClientHttpRequestFactory(
				RestTemplateUtil.createPoolingFactory(Config.getUrl(), 300, requestTimeout, true, true)));
		template.setInterceptors(Collections.singletonList(
				new SimpleRequestResponseLoggingInterceptor()));
		return template;
	}
	
	private void login() throws ResponseError {
		LoginRequest request = new LoginRequest();
		request.setLogin(Config.getLogin());
		request.setPassword(Config.getPassword());
		getResult(template, request, LOGIN, HttpMethod.POST,
				new ParameterizedTypeReference<Response<Object>>() { }, false);
	}
	
	@SuppressWarnings("unchecked")
	public List<Station> getCachedStations() throws IOCacheException {
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
	
	@SuppressWarnings("unchecked")
	public List<ScheduleTrip> getCachedSchedule(Date date) throws ResponseError {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getScheduleCacheKey(date));
		params.put(RedisMemoryCache.UPDATE_TASK, new ScheduleUpdateTask(date));
		try {
			return (List<ScheduleTrip>) checkCache(cache.read(params));
		} catch (IOCacheException e) {
			throw new CacheProcessing(e.getMessage());
		} catch (ResponseError e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseError(e.getMessage());
		}
	}
	
	public List<ScheduleTrip> getSchedule(Date date) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(SCHEDULE, dateFormat.format(date)),
				HttpMethod.GET, new ParameterizedTypeReference<Response<List<ScheduleTrip>>>() { }, true);
	}
	
	@SuppressWarnings("unchecked")
	public List<Trip> getCachedTrips(String from, String to, Date date) throws ResponseError {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getTripsCacheKey(date, from, to));
		params.put(RedisMemoryCache.UPDATE_TASK, new TripsUpdateTask(from, to, date));
		try {
			return (List<Trip>) checkCache(cache.read(params));
		} catch (IOCacheException e) {
			throw new CacheProcessing(e.getMessage());
		} catch (ResponseError e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseError(e.getMessage());
		}
	}
	
	public List<Trip> getTrips(String from, String to, Date date) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(SCHEDULE_FROM_TO, dateFormat.format(date), from, to),
				HttpMethod.GET, new ParameterizedTypeReference<Response<List<Trip>>>() { }, true);
	}
	
	public Price getCachedPrice(String from, String to, String tripId, Date departure) throws ResponseError {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getPriceCacheKey(tripId, from, to));
		params.put(RedisMemoryCache.UPDATE_TASK, new PriceUpdateTask(tripId, from, to, departure));
		try {
			return (Price) checkCache(cache.read(params));
		} catch (IOCacheException e) {
			throw new CacheProcessing(e.getMessage());
		} catch (ResponseError e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseError(e.getMessage());
		}
	}
	
	public Price getPrice(String from, String to, String tripId) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(PRICES, from, to, tripId),
				HttpMethod.GET, new ParameterizedTypeReference<Response<Price>>() { }, true);
	}
	
	public Route getCachedRoute(String tripId) throws ResponseError {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getRouteCacheKey(tripId));
		params.put(RedisMemoryCache.UPDATE_TASK, new RouteUpdateTask(tripId));
		try {
			return (Route) checkCache(cache.read(params));
		} catch (IOCacheException e) {
			throw new CacheProcessing(e.getMessage());
		} catch (ResponseError e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseError(e.getMessage());
		}
	}
	
	public Route getRoute(String tripId) throws ResponseError {
		return getResult(searchTemplate, null, MessageFormat.format(ROUTE, tripId),
				HttpMethod.GET, new ParameterizedTypeReference<Response<Route>>() { }, true);
	}
	
	private Object checkCache(Object value) throws ResponseError {
		if (value instanceof ResponseError) {
			throw (ResponseError) value;
		} else {
			return value;
		}
	}
	
	private void checkStatus(Response<?> response) throws ResponseError {
		if (response == null) {
			throw new ResponseError("Empty response");
		}
		if (Objects.equals(response.getStatus().toUpperCase(), STATUS_ERROR)) {
			throw new ResponseError(response.getMessage());
		}
		if (!Objects.equals(response.getStatus().toUpperCase(), STATUS_OK)) {
			throw new ResponseError("Unknown status");
		}
	}
	
	private <T> T getResult(RestTemplate template, Request request, String method, HttpMethod httpMethod,
			ParameterizedTypeReference<Response<T>> type, boolean checkLogin) throws ResponseError {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl() + method).build().toUri();
		RequestEntity<Request> requestEntity = new RequestEntity<Request>(request, headers, httpMethod, uri);
		ResponseEntity<Response<T>> response = template.exchange(requestEntity, type);
		
		// создаем хедер с куками только для логина
		if (Objects.equals(method, LOGIN)) {
			createHeaders(response.getHeaders().getValuesAsList(HttpHeaders.SET_COOKIE));
		}
		Response<T> resultResponse = response.getBody();
		try {
			checkStatus(resultResponse);
		} catch (ResponseError e) {
			
			// проверяем авторизацию
			if (checkLogin) {
				if (e.getMessage() != null
						&& e.getMessage().toUpperCase().contains(NOT_LOGGED)) {
					HttpHeaders copy = this.headers;
					
					// чтобы не дергать логин несколько раз подряд
					synchronized (RestClient.class) {
						
						// если хедер изменился, то логин выполнен
						if (copy == this.headers) {
							login();
						}
					}
					// вызываем тот же запрос с новым хедером авторизации но без проврки логина
					return getResult(template, request, method, httpMethod, type, false);
				}
			}
			throw e;
		}
		return response.getBody().getData();
	}
	
	private void createHeaders(List<String> cookies) {
		headers = new HttpHeaders();
		Map<String, String> cookiesMap = new HashMap<>();
		for (String cookie : cookies) {
			if (cookie.contains("=")) {
				String[] params = cookie.split("=");
				cookiesMap.put(params[0], params[1]);
			}
		}
		for (Entry<String, String> cookie : cookiesMap.entrySet()) {
			headers.add(HttpHeaders.COOKIE, String.join("=", cookie.getKey(), cookie.getValue()));
		}
	}

	public CacheHandler getCache() {
		return cache;
	}
	
	public static String getRouteCacheKey(String tripId) {
		return ROUTE_CACHE_KEY + tripId;
	}
	
	public static String getScheduleCacheKey(Date date) {
		return SCHEDULE_CACHE_KEY + DateUtils.truncate(date, Calendar.DATE).getTime();
	}
	
	public static String getPriceCacheKey(String tripId, String from, String to) {
		return PRICE_CACHE_KEY + String.join(";", tripId, from, to);
	}
	
	public static String getTripsCacheKey(Date date, String from, String to) {
		return TRIPS_CACHE_KEY + String.join(";",
				String.valueOf(DateUtils.truncate(date, Calendar.DATE).getTime()), from, to);
	}
	
	public static RestClientException createUnavailableMethod() {
		return new RestClientException("Method is unavailable");
	}
	
}
