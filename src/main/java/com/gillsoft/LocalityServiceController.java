package com.gillsoft;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractLocalityService;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.Station;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.request.LocalityRequest;
import com.google.common.base.Objects;

@RestController
public class LocalityServiceController extends AbstractLocalityService {
	
	public static List<Locality> all;
	public static List<Locality> used;
	
	@Autowired
	private RestClient client;

	@Override
	public List<Locality> getAllResponse(LocalityRequest arg0) {
		createLocalities();
		return all;
	}

	@Override
	public Map<String, List<String>> getBindingResponse(LocalityRequest arg0) {
		return null;
	}

	@Override
	public List<Locality> getUsedResponse(LocalityRequest arg0) {
		createLocalities();
		return used;
	}
	
	@Scheduled(initialDelay = 60000, fixedDelay = 900000)
	public void createLocalities() {
		if (LocalityServiceController.all == null) {
			synchronized (LocalityServiceController.class) {
				if (LocalityServiceController.all == null) {
					boolean cacheError = true;
					do {
						try {
							List<Station> stations = client.getCachedStations();
							if (stations != null) {
								List<Locality> all = new CopyOnWriteArrayList<>();
								List<Locality> used = new CopyOnWriteArrayList<>();
								for (Station station : stations) {
									Locality locality = new Locality();
									locality.setId(station.getStationId());
									locality.setName(Lang.PL, station.getName());
									locality.setDetails(station.getDescription());
									all.add(locality);
									if (station.isActive()) {
										used.add(locality);
									}
								}
								LocalityServiceController.all = all;
								LocalityServiceController.used = used;
								cacheError = false;
							}
						} catch (IOCacheException e) {
							try {
								TimeUnit.MILLISECONDS.sleep(100);
							} catch (InterruptedException ie) {
							}
						}
					} while (cacheError);
				}
			}
		}
	}
	
	public static Locality getLocality(String id) {
		if (all == null) {
			return null;
		}
		for (Locality locality : all) {
			if (Objects.equal(id, locality.getId())) {
				return locality;
			}
		}
		return null;
	}

}
