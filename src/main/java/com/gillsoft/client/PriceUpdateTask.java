package com.gillsoft.client;

import java.util.Date;

import com.gillsoft.util.ContextProvider;

public class PriceUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = -4495492649276633125L;
	
	private String tripId;
	private String from;
	private String to;
	private Date departure;
	
	public PriceUpdateTask(String tripId, String from, String to, Date departure) {
		this.tripId = tripId;
		this.from = from;
		this.to = to;
		this.departure = departure;
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			writeObject(RestClient.getPriceCacheKey(tripId, from, to), client.getPrice(from, to, tripId), false,
					getTimeToLive(), Config.getCacheScheduleUpdateDelay());
		} catch (ResponseError e) {
			
			// ошибку тоже кладем в кэш
			writeObject(RestClient.getPriceCacheKey(tripId, from, to), e, true,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}
	
	// время жизни до момента отправления
	private long getTimeToLive() {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		return departure.getTime() - System.currentTimeMillis();
	}

}
