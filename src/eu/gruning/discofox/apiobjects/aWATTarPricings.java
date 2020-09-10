package eu.gruning.discofox.apiobjects;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;

import eu.gruning.discofox.mqtt.MqttEngine;

public class aWATTarPricings {
	private String object;
	private String url;
	private List<aWATTarPricing> data = new ArrayList<>();

	public aWATTarPricings() {
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<aWATTarPricing> getPricings() {
		return data;
	}

	public void setPricings(List<aWATTarPricing> pricings) {
		this.data = pricings;
	}

	public int size() {
		return data.size();
	}

	public String getTopic() {
		return data.get(0).getTopic();
	}

	public void publish(MqttEngine mqttengine) throws MqttException {
		if (mqttengine.isConnected(MqttEngine.RECONNECT)) {
			for (aWATTarPricing pricing : data) {
				pricing.publish(mqttengine);
			}
		}
	}

	public long getStartTimestamp() {
		return data.get(0).getStartTimestamp();
	}

	public long getEndTimestamp() {
		return data.get(data.size() - 1).getEndTimestamp();
	}

	public aWATTarPricing getPriceUnit(long timestamp) {
		aWATTarPricing match = aWATTarPricing.NOT_FOUND;
		for (aWATTarPricing pricing : data) {
			if (timestamp >= pricing.getStartTimestamp() && timestamp <= pricing.getEndTimestamp()) {
				// Found a valid price period
				match = pricing;
				break;
			}
		}
		return match;
	}
}