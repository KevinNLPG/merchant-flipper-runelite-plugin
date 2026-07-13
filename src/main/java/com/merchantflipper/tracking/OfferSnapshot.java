package com.merchantflipper.tracking;

import java.util.Objects;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

/**
 * Immutable value object capturing everything about a GE offer slot that we
 * care about for change detection: {@code state}, {@code itemId},
 * {@code filledQuantity} (offer.getQuantitySold()), {@code totalQuantity},
 * {@code price}, and {@code spent}. Two snapshots are equal only if every one
 * of these fields matches.
 */
public final class OfferSnapshot
{
	private final GrandExchangeOfferState state;
	private final int itemId;
	private final int filledQuantity;
	private final int totalQuantity;
	private final int price;
	private final int spent;

	public OfferSnapshot(
		GrandExchangeOfferState state,
		int itemId,
		int filledQuantity,
		int totalQuantity,
		int price,
		int spent)
	{
		this.state = state;
		this.itemId = itemId;
		this.filledQuantity = filledQuantity;
		this.totalQuantity = totalQuantity;
		this.price = price;
		this.spent = spent;
	}

	public static OfferSnapshot fromOffer(GrandExchangeOffer offer)
	{
		return new OfferSnapshot(
			offer.getState(),
			offer.getItemId(),
			offer.getQuantitySold(),
			offer.getTotalQuantity(),
			offer.getPrice(),
			offer.getSpent());
	}

	public GrandExchangeOfferState getState()
	{
		return state;
	}

	public int getItemId()
	{
		return itemId;
	}

	public int getFilledQuantity()
	{
		return filledQuantity;
	}

	public int getTotalQuantity()
	{
		return totalQuantity;
	}

	public int getPrice()
	{
		return price;
	}

	public int getSpent()
	{
		return spent;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof OfferSnapshot))
		{
			return false;
		}
		OfferSnapshot that = (OfferSnapshot) o;
		return itemId == that.itemId
			&& filledQuantity == that.filledQuantity
			&& totalQuantity == that.totalQuantity
			&& price == that.price
			&& spent == that.spent
			&& state == that.state;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(state, itemId, filledQuantity, totalQuantity, price, spent);
	}

	@Override
	public String toString()
	{
		return "OfferSnapshot{state=" + state + ", itemId=" + itemId + ", filledQuantity=" + filledQuantity
			+ ", totalQuantity=" + totalQuantity + ", price=" + price + ", spent=" + spent + '}';
	}
}
