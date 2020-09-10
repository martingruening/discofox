package eu.gruning.discofox.apiobjects;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import eu.gruning.discofox.mqtt.MqttEngine;

public class aWATTarPricing {

	public static aWATTarPricing NOT_FOUND = new aWATTarPricing(false);

	private long start_timestamp;
	private long end_timestamp;
	private double marketprice;
	private String unit;
	private String measurement = "EnergyPricing";

	@Expose(serialize = false, deserialize = false)
	private Boolean isvalid = true;

	public String getMeasurement() {
		return measurement;
	}

	public void setMeasurement(String measurement) {
		this.measurement = measurement;
	}

	public aWATTarPricing() {
	}

	public long getStartTimestamp() {
		return start_timestamp;
	}

	public void setStartTimestamp(long startTimestamp) {
		this.start_timestamp = startTimestamp;
	}

	public long getEndTimestamp() {
		return end_timestamp;
	}

	public void setEndTimestamp(long endTimestamp) {
		this.end_timestamp = endTimestamp;
	}

	public double getMarketprice() {
		return marketprice;
	}

	public void setMarketprice(double marketprice) {
		this.marketprice = marketprice;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getTopic() {
		return "awattar/pricing";
	}

	public void publish(MqttEngine mqtt) throws MqttException {
		mqtt.publish(this.getTopic(), this.asJson());
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

	public Boolean isValid() {
		return isvalid;
	}

	// Creates an pricing object with non-default validity
	public aWATTarPricing(Boolean valid) {
		this.isvalid = valid;
	}
}