package com.merchantflipper.protocol;

/**
 * plugin -> desktop, sent once after hello_ack(accepted=true) and again if the profile
 * changes (e.g. player switches OSRS account while client stays open).
 */
public final class ProfileAnnounceMessage
{
	private final String type = "profile_announce";
	private final String profileId;
	private final long timestamp;

	public ProfileAnnounceMessage(String profileId, long timestamp)
	{
		this.profileId = profileId;
		this.timestamp = timestamp;
	}

	public String getType()
	{
		return type;
	}

	public String getProfileId()
	{
		return profileId;
	}

	public long getTimestamp()
	{
		return timestamp;
	}
}
