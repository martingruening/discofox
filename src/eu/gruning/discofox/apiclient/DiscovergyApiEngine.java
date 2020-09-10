package eu.gruning.discofox.apiclient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import eu.gruning.discofox.apiobjects.Meter;
import eu.gruning.discofox.apiobjects.Reading;
import eu.gruning.discofox.apiobjects.Readings;
import eu.gruning.discofox.internal.Configuration;

public class DiscovergyApiEngine {
	private final DiscovergyApi api;
	private final DiscovergyApiClient client;
	private final Type listMeterType = new TypeToken<List<Meter>>() {
	}.getType();
	private static final Logger logger = LogManager.getLogger(DiscovergyApiEngine.class);

	public DiscovergyApiEngine(Configuration config) throws IOException, InterruptedException, ExecutionException {
		this.api = new DiscovergyApi(config.getUser(), config.getPassword());
		this.client = new DiscovergyApiClient(api, config.getClientId());
		logger.info("Sucessfully connected to Discovergy API at " + config.getaWATTarBaseURL() + " with user " + config.getUser());
	}

	public List<Meter> getMeters() throws IOException, InterruptedException, ExecutionException {
		return new Gson().fromJson(client.executeRequest(client.createRequest(Verb.GET, "/meters"), 200).getBody(), listMeterType);
	}

	public Reading getLastReading(String meterId) throws IOException, InterruptedException, ExecutionException {
		OAuthRequest request = client.createRequest(Verb.GET, "/last_reading");
		request.addQuerystringParameter("meterId", meterId);
		Reading reading;
		try {
			reading = new Gson().fromJson(client.executeRequest(request, 200).getBody(), Reading.class);
			reading.setMeterId(meterId);
		} catch (JsonSyntaxException e) {
			// in case of syntax exception return empty object
			reading = new Reading();
		}
		return reading;
	}

	private Readings getReadingsInternal(String meterId, long start, long end) throws IOException, InterruptedException, ExecutionException {
		OAuthRequest request = client.createRequest(Verb.GET, "/readings");
		request.addQuerystringParameter("meterId", meterId);
		request.addQuerystringParameter("resolution", "raw");
		request.addQuerystringParameter("from", String.valueOf(start));
		request.addQuerystringParameter("to", String.valueOf(end));
		logger.debug("Getting readings for meter " + meterId + ", resolution: raw, from: " + start + " to " + end);
		String result = client.executeRequest(request, 200).getBody();
		logger.debug("Result: " + result);
		Readings readings = new Readings();
		try {
			Reading[] r = new Gson().fromJson(result, Reading[].class);
			readings.setReadings(Arrays.asList(r));
			readings.setMeterId(meterId);
		} catch (JsonSyntaxException e) {
			logger.info("JSON conversion of readings failed: " + e.getMessage());
			// in case of syntax exception return empty object
		}
		return readings;
	}

	public Readings getReadings(String meterId, long start, long end) throws IOException, InterruptedException, ExecutionException {
		// Handle situations where a day has 25 hours which can happen with switching to
		// daylight saving time
		// The Discovergy API will refuse any request with more than 24 hours between
		// start and end
		// In this special case we will first query 24 hours and then another time 1
		// hour
		if (end - start <= 86400000) {
			return getReadingsInternal(meterId, start, end);
		} else {
			Readings readings = getReadingsInternal(meterId, start, start + 86399999);
			readings.append(getReadingsInternal(meterId, start + 86400000, end));
			return readings;
		}
	}
}