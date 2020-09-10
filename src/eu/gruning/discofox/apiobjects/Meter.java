package eu.gruning.discofox.apiobjects;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.Gson;

import eu.gruning.discofox.mqtt.MqttEngine;

public class Meter {
	public String getSerialNumber() {
		return serialNumber;
	}

	public String getFullSerialNumber() {
		return fullSerialNumber;
	}

	public Map<String, String> getLocation() {
		return location;
	}

	public String getAdministrationNumber() {
		return administrationNumber;
	}

	public String getType() {
		return type;
	}

	public String getMeasurementType() {
		return measurementType;
	}

	public String getLoadProfileType() {
		return loadProfileType;
	}

	public int getScalingFactor() {
		return scalingFactor;
	}

	public int getCurrentScalingFactor() {
		return currentScalingFactor;
	}

	public int getVoltageScalingFactor() {
		return voltageScalingFactor;
	}

	public int getInternalMeters() {
		return internalMeters;
	}

	public long getFirstMeasurementTime() {
		return firstMeasurementTime;
	}

	public long getLastMeasurementTime() {
		return lastMeasurementTime;
	}

	public String getMeterId() {
		return meterId;
	}

	public String getManufacturerId() {
		return manufacturerId;
	}

	public String getMeasurement() {
		return measurement;
	}

	@Override
	public String toString() {
		return ("ID " + meterId + ", Measurement " + measurementType + ", Serial " + serialNumber);

	}

	public String getTopic() {
		return "discovergy/meters";
	}

	public void publish(MqttEngine mqtt) throws MqttException {
		mqtt.publish(this.getTopic(), this.asJson());
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

	private String meterId;
	private String manufacturerId;
	private String serialNumber;
	private String fullSerialNumber;
	private Map<String, String> location;
	private String administrationNumber;
	private String type;
	private String measurementType;
	private String loadProfileType;
	private int scalingFactor;
	private int currentScalingFactor;
	private int voltageScalingFactor;
	private int internalMeters;
	private long firstMeasurementTime;
	private long lastMeasurementTime;
	private String measurement = "Meter";

}
