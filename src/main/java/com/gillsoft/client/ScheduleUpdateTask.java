package com.gillsoft.client;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.gillsoft.cache.AbstractUpdateTask;
import com.gillsoft.util.ContextProvider;

public class ScheduleUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = 4155606685422091417L;
	
	private Date date;

	public ScheduleUpdateTask(Date date) {
		this.date = DateUtils.truncate(date, Calendar.DATE);
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			List<ScheduleTrip> schedule = client.getSchedule(date);
			
			// время жизни кэша расписания до начала следующих суток
			Date liveDate = DateUtils.addDays(date, 1);
			long timeToLive = liveDate.getTime() - System.currentTimeMillis();
			
			writeObject(client.getCache(), RestClient.getScheduleCacheKey(date), schedule,
					timeToLive, Config.getCacheScheduleUpdateDelay());
		} catch (ResponseError e) {
			
			// ошибку тоже кладем в кэш
			writeObject(client.getCache(), RestClient.getScheduleCacheKey(date), e,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}

}
