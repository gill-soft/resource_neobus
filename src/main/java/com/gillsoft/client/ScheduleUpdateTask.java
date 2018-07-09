package com.gillsoft.client;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

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
			
			writeObject(RestClient.getScheduleCacheKey(date), schedule, false,
					timeToLive, Config.getCacheScheduleUpdateDelay());
		} catch (ResponseError e) {
			
			// ошибку тоже кладем в кэш
			writeObject(RestClient.getScheduleCacheKey(date), e, true,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}

}
