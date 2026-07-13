package com.merchantflipper.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BackoffCalculatorTest
{
	@Test
	public void firstDelayEqualsInitialDelay()
	{
		BackoffCalculator backoff = new BackoffCalculator(1000, 60000);
		assertEquals(1000L, backoff.nextDelayMillis());
	}

	@Test
	public void delaysDoubleUntilTheyHitTheCap()
	{
		BackoffCalculator backoff = new BackoffCalculator(1000, 60000);
		long[] expected = {1000, 2000, 4000, 8000, 16000, 32000, 60000, 60000, 60000};
		for (long exp : expected)
		{
			assertEquals(exp, backoff.nextDelayMillis());
		}
	}

	@Test
	public void resetRestartsFromTheInitialDelay()
	{
		BackoffCalculator backoff = new BackoffCalculator(1000, 60000);
		backoff.nextDelayMillis();
		backoff.nextDelayMillis();
		backoff.nextDelayMillis();
		backoff.reset();
		assertEquals(1000L, backoff.nextDelayMillis());
	}

	@Test
	public void neverExceedsTheConfiguredMax()
	{
		BackoffCalculator backoff = new BackoffCalculator(1000, 60000);
		for (int i = 0; i < 50; i++)
		{
			assertTrue(backoff.nextDelayMillis() <= 60000);
		}
	}

	@Test
	public void respectsCustomInitialAndMaxValues()
	{
		BackoffCalculator backoff = new BackoffCalculator(500, 4000);
		assertEquals(500L, backoff.nextDelayMillis());
		assertEquals(1000L, backoff.nextDelayMillis());
		assertEquals(2000L, backoff.nextDelayMillis());
		assertEquals(4000L, backoff.nextDelayMillis());
		assertEquals(4000L, backoff.nextDelayMillis());
	}
}
