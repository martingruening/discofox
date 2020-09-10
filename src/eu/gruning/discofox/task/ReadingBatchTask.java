package eu.gruning.discofox.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import eu.gruning.discofox.apiclient.DiscovergyApiEngine;
import eu.gruning.discofox.apiobjects.Meter;
import eu.gruning.discofox.apiobjects.Readings;
import eu.gruning.discofox.apiobjects.aWATTarPricings;
import eu.gruning.discofox.mqtt.MqttEngine;

public class ReadingBatchTask implements Runnable {

	private long start, end;
	private String taskname;
	private Meter meter;
	private DiscovergyApiEngine apiengine;
	private MqttEngine mqttengine;
	private Boolean dopricing = false;
	private aWATTarPricings pricings;

	private static final Logger logger = LogManager.getLogger(ReadingTask.class);

	public ReadingBatchTask(Meter meter, long start, long end, DiscovergyApiEngine apiengine, MqttEngine mqttengine) {
		this.meter = meter;
		this.apiengine = apiengine;
		this.mqttengine = mqttengine;
		this.start = start;
		this.end = end;
		this.taskname = "batchtask-" + this.meter.getMeterId() + "-" + start + "-" + end;
	}

	public ReadingBatchTask(Meter meter, long start, long end, aWATTarPricings pricings, DiscovergyApiEngine apiengine, MqttEngine mqttengine) {
		this(meter, start, end, apiengine, mqttengine);
		dopricing = true;
		this.pricings = pricings;
	}

	@Override
	public void run() {
		Thread.currentThread().setName(taskname);
		logger.info("Task '" + taskname + "' started");
		// Make sure the MQTT engine is connected, methods handles the necessary
		// reconnect
		if (!mqttengine.isConnected(MqttEngine.RECONNECT)) {
			logger.error("MQTT connection is disconnected and cannot be reestablished. Exiting Task.");
			return;
		}

		Readings readings = new Readings();
		try {
			readings = apiengine.getReadings(meter.getMeterId(), start, end);
			logger.info("Got " + readings.size() + " readings");
			if (readings.size() > 0) {
				if (dopricing) {
					logger.info("Merging prices with readings and publishing");
					readings.addPricing(pricings);
				} else {
					logger.info("Publishing");
				}
				readings.publish(mqttengine);
				logger.info("Published " + readings.size() + " readings to topic " + readings.getTopic());
			}
		} catch (Exception e) {
			logger.error("Reading task '" + taskname + "' failed with exception: " + e.getMessage() + " (Class " + e.getClass().getName() + ")");
			if (e.getClass() == MqttException.class) {
				logger.error("MQTT connection has a problem. Trying to fix.");
				// Try to fix MQTT connection
				if (!mqttengine.isConnected(MqttEngine.RECONNECT)) {
					logger.error("MQTT connection is disconnected and cannot be reestablished. Exiting Task.");
					return;
				} else {
					logger.info("MQTT connection is reestablished. Republishing.");
					try {
						readings.publish(mqttengine);
					} catch (MqttException f) {
					}
					logger.info("Published " + readings.size() + " readings to topic " + readings.getTopic());
				}
			}
		}
	}

}