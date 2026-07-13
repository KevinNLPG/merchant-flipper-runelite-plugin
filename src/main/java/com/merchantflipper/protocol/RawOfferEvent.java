package com.merchantflipper.protocol;

/**
 * Wire model for the {@code event} payload of an {@code offer_event} message.
 * Field names and types are an exact contract shared with
 * {@code packages/shared/src/protocol/v1.ts} (rawOfferEventSchema) - do not
 * rename fields without updating both sides.
 */
public final class RawOfferEvent
{
	private final int protocolVersion;
	private final String eventId;
	private final long timestamp;
	private final String profileId;
	private final int slot;
	private final int itemId;
	private final String state;
	private final int totalQuantity;
	private final int filledQuantity;
	private final int offerPrice;
	private final int totalSpentOrReceived;

	public RawOfferEvent(
		int protocolVersion,
		String eventId,
		long timestamp,
		String profileId,
		int slot,
		int itemId,
		String state,
		int totalQuantity,
		int filledQuantity,
		int offerPrice,
		int totalSpentOrReceived)
	{
		this.protocolVersion = protocolVersion;
		this.eventId = eventId;
		this.timestamp = timestamp;
		this.profileId = profileId;
		this.slot = slot;
		this.itemId = itemId;
		this.state = state;
		this.totalQuantity = totalQuantity;
		this.filledQuantity = filledQuantity;
		this.offerPrice = offerPrice;
		this.totalSpentOrReceived = totalSpentOrReceived;
	}

	public int getProtocolVersion()
	{
		return protocolVersion;
	}

	public String getEventId()
	{
		return eventId;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public String getProfileId()
	{
		return profileId;
	}

	public int getSlot()
	{
		return slot;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getState()
	{
		return state;
	}

	public int getTotalQuantity()
	{
		return totalQuantity;
	}

	public int getFilledQuantity()
	{
		return filledQuantity;
	}

	public int getOfferPrice()
	{
		return offerPrice;
	}

	public int getTotalSpentOrReceived()
	{
		return totalSpentOrReceived;
	}
}
