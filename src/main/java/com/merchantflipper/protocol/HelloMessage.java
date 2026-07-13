package com.merchantflipper.protocol;

/** plugin -> desktop, sent once per connection immediately after opening the socket. */
public final class HelloMessage
{
	private final String type = "hello";
	private final int protocolVersion;
	private final String pluginVersion;
	private final String pairingToken;

	public HelloMessage(int protocolVersion, String pluginVersion, String pairingToken)
	{
		this.protocolVersion = protocolVersion;
		this.pluginVersion = pluginVersion;
		this.pairingToken = pairingToken;
	}

	public String getType()
	{
		return type;
	}

	public int getProtocolVersion()
	{
		return protocolVersion;
	}

	public String getPluginVersion()
	{
		return pluginVersion;
	}

	public String getPairingToken()
	{
		return pairingToken;
	}
}
