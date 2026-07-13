package com.merchantflipper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * NOT a JUnit test (no {@code @Test} methods, so the Gradle {@code test} task ignores it).
 * This is the standard RuneLite solo-dev entry point for running/debugging a single plugin
 * inside a real, full RuneLite client without needing the Plugin Hub. Run it via
 * {@code ./gradlew run} (see build.gradle's {@code run} task) or directly from an IDE.
 */
public class MerchantFlipperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MerchantFlipperPlugin.class);
		RuneLite.main(args);
	}
}
