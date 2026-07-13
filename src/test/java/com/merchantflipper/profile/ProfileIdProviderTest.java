package com.merchantflipper.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProfileIdProviderTest
{
	@Test
	public void isDeterministicForTheSameAccountHash()
	{
		String first = ProfileIdProvider.deriveProfileId(123456789L);
		String second = ProfileIdProvider.deriveProfileId(123456789L);
		assertEquals(first, second);
	}

	@Test
	public void differsAcrossDifferentAccountHashes()
	{
		String a = ProfileIdProvider.deriveProfileId(1L);
		String b = ProfileIdProvider.deriveProfileId(2L);
		assertFalse(a.equals(b));
	}

	@Test
	public void isANonEmptyLowercaseHexSha256Digest()
	{
		String id = ProfileIdProvider.deriveProfileId(987654321L);
		assertNotNull(id);
		assertTrue("expected a 64-char lowercase hex SHA-256 digest but was: " + id, id.matches("[0-9a-f]{64}"));
	}

	@Test
	public void doesNotContainTheRawAccountHashAsASubstring()
	{
		long accountHash = 555555555555L;
		String id = ProfileIdProvider.deriveProfileId(accountHash);
		assertFalse(id.contains(Long.toString(accountHash)));
	}
}
