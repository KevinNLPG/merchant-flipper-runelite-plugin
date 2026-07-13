package com.merchantflipper.protocol;

/** desktop -> plugin, response to {@link HelloMessage}. Only ever received, never constructed to send. */
public final class HelloAckMessage
{
	private String type;
	private int protocolVersion;
	private boolean accepted;
	private String reason;

	/** No-arg constructor used by Gson during deserialization. */
	public HelloAckMessage()
	{
	}

	public HelloAckMessage(int protocolVersion, boolean accepted, String reason)
	{
		this.type = "hello_ack";
		this.protocolVersion = protocolVersion;
		this.accepted = accepted;
		this.reason = reason;
	}

	public String getType()
	{
		return type;
	}

	public int getProtocolVersion()
	{
		return protocolVersion;
	}

	public boolean isAccepted()
	{
		return accepted;
	}

	public String getReason()
	{
		return reason;
	}
}
