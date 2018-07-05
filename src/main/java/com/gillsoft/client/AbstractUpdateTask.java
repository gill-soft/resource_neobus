package com.gillsoft.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.util.ContextProvider;

public abstract class AbstractUpdateTask implements Runnable, Serializable {

	private static final long serialVersionUID = -4445951612165534860L;

	protected void writeObject(String key, Object cachedObject, boolean error, long timeToLive, long updateDelay) {
		if (cachedObject != null) {
			Map<String, Object> params = new HashMap<>();
			params.put(RedisMemoryCache.OBJECT_NAME, key);
			if (error) {
				
				// ошибку тоже кладем в кэш
				params.put(RedisMemoryCache.UPDATE_TASK, this);
				params.put(RedisMemoryCache.TIME_TO_LIVE, Config.getCacheErrorTimeToLive());
				params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheErrorUpdateDelay());
				
			} else {
				// время жизни кэша
				params.put(RedisMemoryCache.TIME_TO_LIVE, timeToLive);
				
				// обновляем, если время жизни больше времени обновления
				if (timeToLive > Config.getCacheRouteUpdateDelay()) {
					params.put(RedisMemoryCache.UPDATE_TASK, this);
					params.put(RedisMemoryCache.UPDATE_DELAY, updateDelay);
				}
			}
			try {
				RestClient client = ContextProvider.getBean(RestClient.class);
				client.getCache().write(cachedObject, params);
			} catch (IOCacheException e) {
			}
		}
	}

}
