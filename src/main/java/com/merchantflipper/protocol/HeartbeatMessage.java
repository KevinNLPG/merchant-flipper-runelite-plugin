package com.merchantflipper.protocol;

/** Both directions, periodic keepalive. */
public final class HeartbeatMessage
{
	private String type = "heartbeat";
	private long timestamp;

	/** No-arg constructor used by Gson during deserialization. */
	public HeartbeatMessage()
	{
	}

	public HeartbeatMessage(long timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getType()
	{
		return type;
	}

	public long getTimestamp()
	{
		return timestamp;
	}
}
