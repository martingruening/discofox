package eu.gruning.discofox.internal;

import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class Configuration {
	private String DiscovergyBaseURL = "https://api.discovergy.com/public/v1";
	private String aWATTarBaseURL = "https://api.awattar.de/v1/marketdata";
	private String clientId = "discofox";
	private String User;
	private String Password;
	private Boolean Debug = false;
	private String Broker = "tcp://localhost:1883";
	@SerializedName("aWATTarPrices")
	private Boolean aWATTarPrices = false;

	public Boolean getaWATTarPrices() {
		return aWATTarPrices;
	}

	public void setaWATTarPrices(Boolean aWATTarPrices) {
		this.aWATTarPrices = aWATTarPrices;
	}

	public String getBroker() {
		return Broker;
	}

	public void setBroker(String broker) {
		Broker = broker;
	}

	public String getDiscovergyBaseURL() {
		return DiscovergyBaseURL;
	}

	public String getaWATTarBaseURL() {
		return aWATTarBaseURL;
	}

	public String getUser() {
		return User;
	}

	public String getPassword() {
		return Password;
	}

	public Boolean getDebug() {
		return Debug;
	}

	public void setDiscovergyBaseURL(String baseURL) {
		this.DiscovergyBaseURL = baseURL;
	}

	public void setaWATTarBaseURL(String baseURL) {
		this.aWATTarBaseURL = baseURL;
	}

	public void setUser(String user) {
		User = user;
	}

	public void setPassword(String password) {
		Password = password;
	}

	public void setDebug(Boolean debug) {
		Debug = debug;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Configuration() {
	}

	public Configuration(Configuration config) {
		this.DiscovergyBaseURL = config.DiscovergyBaseURL;
		this.aWATTarBaseURL = config.aWATTarBaseURL;
		this.clientId = config.clientId;
		this.Debug = config.Debug;
		this.Password = config.Password;
		this.User = config.User;
		this.Broker = config.Broker;
	}

	public Configuration(Reader reader) {
		this(new Gson().fromJson(reader, Configuration.class));
	}

	public Configuration(String json) {
		this(new Gson().fromJson(json, Configuration.class));
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

	public String asPrettyJson() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}

	public boolean isSane() {
		return (DiscovergyBaseURL != null && aWATTarBaseURL != null && clientId != null && Debug != null && Password != null && User != null && Broker != null);
	}
}
