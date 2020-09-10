package eu.gruning.discofox.task;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import eu.gruning.discofox.apiclient.DiscovergyApiEngine;
import eu.gruning.discofox.apiclient.aWATTarApiEngine;
import eu.gruning.discofox.apiobjects.Meter;
import eu.gruning.discofox.apiobjects.Reading;
import eu.gruning.discofox.apiobjects.aWATTarPricings;
import eu.gruning.discofox.internal.Configuration;
import eu.gruning.discofox.internal.Discotime;
import eu.gruning.discofox.mqtt.MqttEngine;

public class ReadingTask implements Runnable {

	private String taskname;
	private Meter meter;
	private DiscovergyApiEngine meterapiengine;
	private aWATTarApiEngine priceapiengine;
	private MqttEngine mqttengine;
	private Configuration config;
	private aWATTarPricings pricings = new aWATTarPricings();
	private static final Logger logger = LogManager.getLogger(ReadingTask.class);
	private int errorcounter = 0;
	private static boolean SUCCESS = true;
	private LocalTime aWATTarPricesForTomorrowAvailable = LocalTime.of(14, 0);

	public ReadingTask(Meter meter, Configuration config, DiscovergyApiEngine meterapiengine, aWATTarApiEngine priceapiengine, MqttEngine mqttengine) {
		this.meter = meter;
		this.taskname = "task-" + this.meter.getMeterId();
		this.meterapiengine = meterapiengine;
		this.priceapiengine = priceapiengine;
		this.mqttengine = mqttengine;
		this.config = config;
	}

	public void getaWATTarPrices() throws InterruptedException, IOException, URISyntaxException {
		long start = Discotime.beginOf(LocalDate.now());
		long end;
		// At 2 PM (14:00) aWATTar makes prices for the full next day available, adjust
		// end if after this point in time
		if (Discotime.LocalTimeGermany().isAfter(aWATTarPricesForTomorrowAvailable)) {
			end = Discotime.endOf(LocalDate.now().plusDays(1));
		} else {
			end = Discotime.endOf(LocalDate.now());
		}
		if (pricings.size() < 23 || pricings.getStartTimestamp() > start || pricings.getEndTimestamp() < end) {
			pricings = priceapiengine.query(start, end);
			logger.info("Got " + pricings.size() + " pricings from aWATTar (start " + pricings.getStartTimestamp() + " end " + pricings.getEndTimestamp() + ")");
			try {
				pricings.publish(mqttengine);
				logger.info("Published to topic " + pricings.getTopic());
			} catch (MqttException e) {
				logger.error("Publishing pricings failed: " + e.getMessage());
			}
		}
	}

	@Override
	public void run() {
		long watermark = 0;
		Thread.currentThread().setName(taskname);
		logger.info("Task '" + taskname + "' started");

		logger.info("Publishing meter " + meter.getMeterId() + " to topic " + meter.getTopic());
		try {
			meter.publish(mqttengine);
		} catch (MqttException e) {
			if (!errorHandler(e) == SUCCESS) {
				logger.error("Too many errors or non-recoverable situation, Task is being stopped.");
				return;
			}
		}
		// Looping forever in this task until interrupted
		while (true) {
			try {
				if (config.getaWATTarPrices()) {
					getaWATTarPrices();
				}
				Reading reading = meterapiengine.getLastReading(meter.getMeterId());
				String logline = "Got last reading for meter " + meter.getMeterId() + " (timestamp " + reading.getTime() + ", age " + reading.getAge() + "s";
				if (watermark < reading.getTime()) {
					// This is a new reading that needs to be published to MQTT
					watermark = reading.getTime();
					logline = logline.concat(", NEW)");
					logger.info(logline);
					if (config.getaWATTarPrices()) {
						reading.addPricing(pricings);
					}
					reading.publish(mqttengine);
					logger.info("Published " + reading + " to topic " + reading.getTopic());
				} else {
					// Got the same reading again from the API (not new, no need to publish)
					logline = logline.concat(")");
					logger.info(logline);
				}
				Thread.sleep(23000);
			} catch (Exception e) {
				if (!errorHandler(e) == SUCCESS) {
					logger.error("Too many errors or non-recoverable situation, Task is being stopped.");
					break;
				}
			}
		}
	}

	private boolean errorHandler(Exception e) {
		logger.error("Reading task '" + taskname + "' failed with exception: " + e.getMessage() + " (Class " + e.getClass().getName() + ")");
		errorcounter++;
		if (errorcounter >= 4) {
			return false;
		}

		if (e.getClass() == MqttException.class) {
			// Try to fix MQTT connection
			logger.error("MQTT connection has a problem. Trying to fix.");
			if (!mqttengine.isConnected(MqttEngine.RECONNECT)) {
				logger.error("MQTT connection is disconnected and cannot be reestablished");
				return false;
			}
		}
		return SUCCESS;
	}

	@Override
	public String toString() {
		return taskname;
	}

}