package eu.gruning.discofox.internal;

public class Version {
	public String getVersion() {
		return this.getClass().getPackage().getImplementationVersion();
	}
}