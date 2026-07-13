package com.merchantflipper.protocol;

/** plugin -> desktop, one per actual GE offer state change. */
public final class OfferEventMessage
{
	private final String type = "offer_event";
	private final RawOfferEvent event;

	public OfferEventMessage(RawOfferEvent event)
	{
		this.event = event;
	}

	public String getType()
	{
		return type;
	}

	public RawOfferEvent getEvent()
	{
		return event;
	}
}
