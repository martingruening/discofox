package eu.gruning.discofox.apiclient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import eu.gruning.discofox.apiobjects.aWATTarPricings;
import eu.gruning.discofox.internal.Configuration;

public class aWATTarApiEngine {

	private HttpClient client = HttpClient.newHttpClient();
	private HttpRequest request;
	private String apiurl = "https://api.awattar.de/v1/marketdata";
	private String agent = "discofox";
	private static final Logger logger = LogManager.getLogger(aWATTarApiEngine.class);

	private long callinterval = Instant.now().getEpochSecond();
	private long callcounter = 0;

	public aWATTarApiEngine(Configuration config) {
		this.apiurl = config.getaWATTarBaseURL();
		this.agent = config.getClientId();
	}

	public aWATTarPricings query(long start, long end) throws InterruptedException, IOException, URISyntaxException {
		return this.query(new URI(apiurl + "?start=" + start + "&end=" + end));
	}

	public aWATTarPricings query(long start) throws InterruptedException, IOException, URISyntaxException {
		return this.query(new URI(apiurl + "?start=" + start));
	}

	public aWATTarPricings query() throws InterruptedException, IOException, URISyntaxException {
		return this.query(new URI(apiurl));
	}

	// Make sure we don't exceed the fair use of the aWATTar API
	private void throttle() throws InterruptedException {
		double speed = (double) callcounter / (double) (Instant.now().getEpochSecond() - callinterval);
		// more than 1 API query per second? If yes, we will sleep for a second
		if (speed > 1) {
			logger.info("Throttling to not violate the aWATTar API Fair Use Policy (1 API call per second)");
			Thread.sleep(1000);
		}
	}

	private aWATTarPricings query(URI uri) throws InterruptedException, IOException {
		request = HttpRequest.newBuilder().GET().uri(uri).timeout(Duration.ofSeconds(10)).setHeader("User-Agent", agent).build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		callcounter++;
		aWATTarPricings pricings = new aWATTarPricings();

		if (response.statusCode() == 200) {
			logger.debug("Response Body: " + response.body());
			try {
				pricings = new Gson().fromJson(response.body(), aWATTarPricings.class);
			} catch (JsonSyntaxException e) {
				logger.info("JSON conversion of readings failed: " + e.getMessage());
			}
		} else {
			logger.error("aWATTar API returned status code " + response.statusCode());
			if (response.statusCode() == 429) {
				long exceededtime = Instant.now().getEpochSecond() - callinterval;
				logger.info("Status code 429 means we have exceeded the fair use of the API (60 Calls/Minute), did " + callcounter + " in " + exceededtime + " seconds");
				logger.info("Sleeping for " + (60 - exceededtime) + " seconds");
				Thread.sleep((60 - exceededtime) * 1000);
				callinterval = Instant.now().getEpochSecond();
				callcounter = 0;
				// Retry the query after sleeping
				return this.query(uri);
			}
		}
		throttle();
		return pricings;
	}
}