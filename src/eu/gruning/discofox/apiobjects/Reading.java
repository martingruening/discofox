package eu.gruning.discofox.apiobjects;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.Gson;

import eu.gruning.discofox.mqtt.MqttEngine;

public class Reading {
	private String meterId;
	private long time;
	private Map<String, Long> values;
	private double price;
	private String unit;

	public Reading() {
	}

	public long getTime() {
		return time;
	}

	public Date getDate() {
		return new Date(time);
	}

	public Map<String, Long> getValues() {
		return values;
	}

	public String getMeterId() {
		return meterId;
	}

	public void setMeterId(String meterId) {
		this.meterId = meterId;
	}

	// Return age in Seconds
	public long getAge() {
		return Instant.now().getEpochSecond() - time / 1000;
	}

	public String getTopic() {
		return "discovergy/" + meterId + "/readings";
	}

	public void publish(MqttEngine mqttengine) throws MqttException {
		if (mqttengine.isConnected(MqttEngine.RECONNECT)) {
			mqttengine.publish(this.getTopic(), this.asJson());
		}
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

	public void addPricing(aWATTarPricings pricings) {
		aWATTarPricing pricing = pricings.getPriceUnit(time);
		if (pricing.isValid()) {
			setUnit(pricing.getUnit());
			setPrice(pricing.getMarketprice());
		}
	}

	@Override
	public String toString() {
		return meterId + "/" + time;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}
}