package com.gillsoft.client;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.gillsoft.util.ContextProvider;

public class TripsUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = -2909115302393210804L;
	
	private String from;
	private String to;
	private Date date;

	public TripsUpdateTask(String from, String to, Date date) {
		super();
		this.from = from;
		this.to = to;
		this.date = DateUtils.truncate(date, Calendar.DATE);
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			List<Trip> trips = client.getTrips(from, to, date);
			writeObject(RestClient.getTripsCacheKey(date, from, to), trips, false,
					getTimeToLive(trips), Config.getCacheTripUpdateDelay());
		} catch (ResponseError e) {
			
			// ошибку тоже кладем в кэш
			writeObject(RestClient.getTripsCacheKey(date, from, to), e, true,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}
	
	// время жизни до момента самого позднего отправления
	private long getTimeToLive(List<Trip> trips) {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		long max = 0;
		for (Trip trip : trips) {
			if (trip.getTime1().getTime() > max) {
				max = trip.getTime1().getTime();
			}
		}
		return max - System.currentTimeMillis();
	}

}
