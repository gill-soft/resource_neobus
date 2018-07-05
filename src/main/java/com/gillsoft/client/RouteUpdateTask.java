package com.gillsoft.client;

import com.gillsoft.util.ContextProvider;

public class RouteUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = -1891438087701386742L;
	
	private String tripId;
	
	public RouteUpdateTask(String tripId) {
		this.tripId = tripId;
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			Route route = client.getRoute(tripId);
			
			// кладем маршрут в кэш
			writeObject(RestClient.getRouteCacheKey(tripId), route, false,
					getTimeToLive(route), Config.getCacheRouteUpdateDelay());
		} catch (ResponseError e) {
			
			// ошибку тоже кладем в кэш
			writeObject(RestClient.getRouteCacheKey(tripId), e, true,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}
	
	private long getTimeToLive(Route route) {
		if (Config.getCacheRouteTimeToLive() != 0) {
			return Config.getCacheRouteTimeToLive();
		}
		long max = 0;
		for (RouteDetail detail : route.getDetails()) {
			if (detail.getTime().getTime() > max) {
				max = detail.getTime().getTime();
			}
		}
		return max - System.currentTimeMillis();
	}

}
