package com.merchantflipper.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Derives a privacy-safe, per-account {@code profileId} from RuneLite's
 * {@code client.getAccountHash()}. The account hash is already a stable,
 * RSN-free identifier, but we run it through SHA-256 anyway so the value we
 * transmit is a one-way digest with no realistic path back to the numeric
 * account hash (let alone the display name) - defense in depth for the
 * "never transmit anything that identifies the player" requirement.
 */
public final class ProfileIdProvider
{
	private ProfileIdProvider()
	{
	}

	public static String deriveProfileId(long accountHash)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(Long.toString(accountHash).getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash)
			{
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			// SHA-256 is guaranteed available on every standard JVM; this is unreachable in practice.
			return Long.toHexString(accountHash);
		}
	}
}
