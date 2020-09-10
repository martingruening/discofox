package eu.gruning.discofox.apiobjects;

import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;

import eu.gruning.discofox.mqtt.MqttEngine;

public class Readings {
	private List<Reading> readings;

	public Readings() {
	}

	public List<Reading> getReadings() {
		return readings;
	}

	public long size() {
		return readings.size();
	}

	public void setReadings(List<Reading> readings) {
		this.readings = readings;
	}

	public void setMeterId(String meterId) {
		for (Reading r : readings) {
			r.setMeterId(meterId);
		}
	}

	public String getTopic() {
		return readings.get(0).getTopic();
	}

	public void publish(MqttEngine mqttengine) throws MqttException {
		for (Reading r : readings) {
			r.publish(mqttengine);
		}
	}

	public void addPricing(aWATTarPricings pricings) {
		for (Reading r : readings) {
			r.addPricing(pricings);
		}
	}

	public void append(Readings readings) {
		this.readings.addAll(readings.readings);
	}
}