package com.merchantflipper.tracking;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Before;
import org.junit.Test;

public class OfferStateTrackerTest
{
	private OfferStateTracker tracker;

	@Before
	public void setUp()
	{
		tracker = new OfferStateTracker();
	}

	private OfferSnapshot snapshot(GrandExchangeOfferState state, int itemId, int filled, int total, int price, int spent)
	{
		return new OfferSnapshot(state, itemId, filled, total, price, spent);
	}

	@Test
	public void firstObservationOfASlotIsAlwaysEmitted()
	{
		Optional<OfferSnapshot> result = tracker.update(0, snapshot(GrandExchangeOfferState.EMPTY, 0, 0, 0, 0, 0));
		assertTrue(result.isPresent());
	}

	@Test
	public void identicalRepeatIsSuppressed()
	{
		tracker.update(0, snapshot(GrandExchangeOfferState.BUYING, 4151, 3, 10, 1500, 4500));
		Optional<OfferSnapshot> second =
			tracker.update(0, snapshot(GrandExchangeOfferState.BUYING, 4151, 3, 10, 1500, 4500));
		assertFalse("an identical redundant snapshot (e.g. RuneLite re-firing on login) must be suppressed",
			second.isPresent());
	}

	@Test
	public void everyStepOfAPartialFillProgressionIsEmitted()
	{
		Optional<OfferSnapshot> first =
			tracker.update(0, snapshot(GrandExchangeOfferState.BUYING, 4151, 0, 10, 1500, 0));
		Optional<OfferSnapshot> second =
			tracker.update(0, snapshot(GrandExchangeOfferState.BUYING, 4151, 3, 10, 1500, 4500));
		Optional<OfferSnapshot> third =
			tracker.update(0, snapshot(GrandExchangeOfferState.BOUGHT, 4151, 10, 10, 1500, 15000));

		assertTrue(first.isPresent());
		assertTrue(second.isPresent());
		assertTrue(third.isPresent());
	}

	@Test
	public void multiplePartialSellsOfTheSamePositionAreEachEmitted()
	{
		tracker.update(0, snapshot(GrandExchangeOfferState.SELLING, 4151, 0, 10, 1500, 0));
		Optional<OfferSnapshot> sell1 =
			tracker.update(0, snapshot(GrandExchangeOfferState.SELLING, 4151, 4, 10, 1500, 6000));
		Optional<OfferSnapshot> sell2 =
			tracker.update(0, snapshot(GrandExchangeOfferState.SELLING, 4151, 7, 10, 1500, 10500));
		Optional<OfferSnapshot> sold =
			tracker.update(0, snapshot(GrandExchangeOfferState.SOLD, 4151, 10, 10, 1500, 15000));

		assertTrue(sell1.isPresent());
		assertTrue(sell2.isPresent());
		assertTrue(sold.isPresent());
	}

	@Test
	public void cancelledOfferWithAPartialFillIsReportedExactlyOnce()
	{
		tracker.update(0, snapshot(GrandExchangeOfferState.SELLING, 4151, 4, 10, 1500, 6000));

		Optional<OfferSnapshot> cancelled =
			tracker.update(0, snapshot(GrandExchangeOfferState.CANCELLED_SELL, 4151, 4, 10, 1500, 6000));
		Optional<OfferSnapshot> repeatedCancel =
			tracker.update(0, snapshot(GrandExchangeOfferState.CANCELLED_SELL, 4151, 4, 10, 1500, 6000));

		assertTrue("a cancel-after-partial-fill must still be reported", cancelled.isPresent());
		assertFalse("a redundant re-fire of the same cancelled state must be suppressed", repeatedCancel.isPresent());
	}

	@Test
	public void emptyAfterCollectingIsReportedExactlyOnce()
	{
		tracker.update(0, snapshot(GrandExchangeOfferState.SOLD, 4151, 10, 10, 1500, 15000));

		Optional<OfferSnapshot> empty1 = tracker.update(0, snapshot(GrandExchangeOfferState.EMPTY, 0, 0, 0, 0, 0));
		Optional<OfferSnapshot> empty2 = tracker.update(0, snapshot(GrandExchangeOfferState.EMPTY, 0, 0, 0, 0, 0));

		assertTrue("the EMPTY transition after collection must be reported", empty1.isPresent());
		assertFalse("a redundant EMPTY re-fire (e.g. RuneLite's login snapshot burst) must be suppressed",
			empty2.isPresent());
	}

	@Test
	public void slotsAreTrackedIndependentlyOfEachOther()
	{
		tracker.update(0, snapshot(GrandExchangeOfferState.BUYING, 4151, 0, 10, 1500, 0));
		Optional<OfferSnapshot> slot1First =
			tracker.update(1, snapshot(GrandExchangeOfferState.BUYING, 4151, 0, 10, 1500, 0));

		assertTrue("a different slot with identical values is still a first observation for that slot",
			slot1First.isPresent());
	}

	@Test
	public void resetForgetsAllPriorState()
	{
		tracker.update(0, snapshot(GrandExchangeOfferState.BUYING, 4151, 0, 10, 1500, 0));
		tracker.reset();
		Optional<OfferSnapshot> afterReset =
			tracker.update(0, snapshot(GrandExchangeOfferState.BUYING, 4151, 0, 10, 1500, 0));
		assertTrue("after reset(), the next observation is treated as a first observation again",
			afterReset.isPresent());
	}
}
