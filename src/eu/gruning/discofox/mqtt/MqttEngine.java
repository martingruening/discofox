package eu.gruning.discofox.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttEngine {
	private final String broker;
	private final String clientId;
	private final MemoryPersistence persistence = new MemoryPersistence();
	private final MqttClient client;
	private final int qos = 2;
	private final MqttConnectOptions connOpts;
	public static final Boolean RECONNECT = true;

	public MqttEngine(String broker, String clientId) throws MqttException {
		this.broker = broker;
		this.clientId = clientId;

		// Setup MQTT Client connection
		client = new MqttClient(this.broker, this.clientId, persistence);
		connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		client.connect(connOpts);
	}

	public void publish(String topic, String content) throws MqttException {
		MqttMessage message = new MqttMessage(content.getBytes());
		message.setQos(qos);
		client.publish(topic, message);
	}

	public void disconnect() throws MqttException {
		client.disconnect();
	}

	public void connect() {
		if (!client.isConnected()) {
			try {
				client.connect(connOpts);
			} catch (Exception e) {
			}
		}
	}

	public boolean isConnected() {
		return client.isConnected();
	}

	public boolean isConnected(Boolean reconnect) {
		if (reconnect == RECONNECT) {
			for (int i = 0; i < 3; i++) {
				this.connect();
				if (client.isConnected()) {
					break;
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
			}
		}
		return client.isConnected();
	}
}