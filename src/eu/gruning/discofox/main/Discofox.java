package eu.gruning.discofox.main;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import eu.gruning.discofox.apiclient.DiscovergyApiEngine;
import eu.gruning.discofox.apiclient.aWATTarApiEngine;
import eu.gruning.discofox.apiobjects.Meter;
import eu.gruning.discofox.apiobjects.aWATTarPricings;
import eu.gruning.discofox.internal.Configuration;
import eu.gruning.discofox.internal.Discotime;
import eu.gruning.discofox.internal.Version;
import eu.gruning.discofox.mqtt.MqttEngine;
import eu.gruning.discofox.task.ReadingBatchTask;
import eu.gruning.discofox.task.ReadingTask;

public class Discofox {

	private static final String configfile = "configuration.json";
	private static Configuration config;
	private static final Logger logger = LogManager.getLogger(Discofox.class);
	private static DiscovergyApiEngine meterapiengine;
	private static aWATTarApiEngine priceapiengine;
	private static MqttEngine mqttengine;
	private static List<Meter> meters;

	public static void readConfiguration() {
		logger.info("Startup Discofox " + new Version().getVersion());
		logger.info("Reading configfile '" + configfile + "'");
		// Try to read JSON-formatted configuration file
		try {
			Reader reader = Files.newBufferedReader(Paths.get(configfile));
			config = new Configuration(reader);
			reader.close();
			logger.info("Sucessfully read configfile");
		} catch (Exception e) {
			logger.fatal("Fatal error reading configfile: " + e.getMessage());
			System.exit(1);
		}
		// Check if all required configuration fields are populated
		if (!config.isSane()) {
			logger.fatal("Configuration malformed or incomplete");
			System.exit(2);
		}
	}

	private static void infiniteReadingLoop() throws InterruptedException {
		// Loop over all meters and start a single infinite reading task for each
		ExecutorService taskservice = Executors.newFixedThreadPool(meters.size());
		for (Meter meter : meters) {
			logger.info("Starting thread for meter " + meter.getMeterId());
			taskservice.submit(new ReadingTask(meter, config, meterapiengine, priceapiengine, mqttengine));
		}
		// This will wait for the tasks to end
		taskservice.shutdown();
		taskservice.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	private static void batchReadingLoop(LocalDate start, LocalDate end) throws InterruptedException, IOException, URISyntaxException {
		for (LocalDate date = start; date.isBefore(end) || date.isEqual(end); date = date.plusDays(1)) {
			long ts_midnight = Discotime.beginOf(date);
			long ts_endofday = Discotime.endOf(date);
			logger.info("Retrieving data for the full day " + date + " (timestamps " + ts_midnight + " to " + ts_endofday + ")");
			aWATTarPricings pricings = new aWATTarPricings();
			if (config.getaWATTarPrices()) {
				pricings = priceapiengine.query(ts_midnight, ts_endofday);
				logger.info("Got " + pricings.size() + " pricings from aWATTar");
				try {
					pricings.publish(mqttengine);
					logger.info("Published to topic " + pricings.getTopic());
				} catch (MqttException e) {
					logger.error("Publishing pricings failed: " + e.getMessage());
				}
			}
			ExecutorService taskservice = Executors.newFixedThreadPool(meters.size());
			for (Meter meter : meters) {
				logger.info("Publishing meter " + meter.getMeterId() + " to topic " + meter.getTopic());
				try {
					meter.publish(mqttengine);
				} catch (MqttException e) {
					logger.error("Publishing meter failed.");
				}
				if (config.getaWATTarPrices()) {
					logger.info("Starting batch thread for meter " + meter.getMeterId() + " with merged pricing");
					taskservice.submit(new ReadingBatchTask(meter, ts_midnight, ts_endofday, pricings, meterapiengine, mqttengine));
				} else {
					logger.info("Starting batch thread for meter " + meter.getMeterId());
					taskservice.submit(new ReadingBatchTask(meter, ts_midnight, ts_endofday, meterapiengine, mqttengine));
				}
			}

			// This will wait for the tasks to end
			taskservice.shutdown();
			taskservice.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		}
	}

	public static void main(String[] args) {
		readConfiguration();
		try {
			// Kick off the connection to the API
			logger.info("Connecting to to Discovergy API at " + config.getDiscovergyBaseURL() + " with user " + config.getUser());
			meterapiengine = new DiscovergyApiEngine(config);
			priceapiengine = new aWATTarApiEngine(config);
			logger.info("ConfigJSON: " + config.asJson());
			config.setaWATTarPrices(true);
			if (config.getaWATTarPrices()) {
				logger.info("aWATTar API is at " + config.getaWATTarBaseURL());
			}

			// Discover all the available meters and log some info
			meters = meterapiengine.getMeters();
			logger.info("Found meters: " + meters.size());
			if (meters.size() == 0) {
				logger.fatal("No meter found");
				System.exit(3);
			} else {
				for (Meter m : meters) {
					logger.info("Meter: " + m);
					logger.debug("Meter asJSON: " + m.asJson());
				}
			}

			// Build MQTT connection
			logger.info("Connecting to MQTT broker at " + config.getBroker());
			mqttengine = new MqttEngine(config.getBroker(), config.getClientId());
			if (mqttengine.isConnected()) {
				logger.info("Sucessfully connected to MQTT broker");
			} else {
				logger.fatal("Could not connect to MQTT broker. Exiting.");
				System.exit(99);
			}

			// When no arguments are passed discofox will poll all meters infinitely
			if (args.length == 0) {
				// Make sure that we have all prior readings for today by default
				logger.info("Step 1: Getting all prior readings for today to ensure completeness");
				batchReadingLoop(LocalDate.now(), LocalDate.now());
				// Kickoff the required infinite tasks
				logger.info("Step 2: Starting infinite reading loops");
				infiniteReadingLoop();
				logger.info("Tasks have completed. Exiting.");
				// Some cleanup to exit gracefully
				mqttengine.disconnect();
				System.exit(0);
			} else {
				try {
					LocalDate date1 = LocalDate.parse(args[0], DateTimeFormatter.BASIC_ISO_DATE);
					LocalDate date2 = date1;
					if (args.length > 1) {
						date2 = LocalDate.parse(args[1], DateTimeFormatter.BASIC_ISO_DATE);
					}
					// Kickoff the required tasks
					batchReadingLoop(date1, date2);
					logger.debug("Tasks have completed");
					mqttengine.disconnect();
					System.exit(0);
				} catch (DateTimeException e) {
					logger.error("Cannot parse date");
					System.out.println("Usage: discofox [DATE1] [DATE2]");
					System.out.println("When no arguments are passed discofox will read all meters infinitely (DEFAULT)");
					System.out.println("DATE1       : get all readings for {date1}, then exit (Batch mode)");
					System.out.println("DATE1 DATE2 : get all readings from {date1} to {date2}, then exit (Batch mode)");
					System.out.println("Dates should be in BASIC_ISO_DATE (YYYYMMDD) format, e.g. 20200729");
					System.exit(99);
				}
			}
		} catch (Exception e) {
			logger.fatal("Error: " + e.getMessage());
			System.exit(99);
		}
	}
}