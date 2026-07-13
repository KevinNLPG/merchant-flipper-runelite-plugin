package com.merchantflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(MerchantFlipperConfig.GROUP)
public interface MerchantFlipperConfig extends Config
{
	String GROUP = "merchantflipper";

	@ConfigItem(
		keyName = "host",
		name = "Desktop app host",
		description = "Host the Merchant Flipper desktop app's local WebSocket is listening on. "
			+ "Should always stay 127.0.0.1 or localhost - this plugin never dials out anywhere else.",
		position = 0
	)
	default String host()
	{
		return "127.0.0.1";
	}

	@ConfigItem(
		keyName = "port",
		name = "Desktop app port",
		description = "Port the Merchant Flipper desktop app's local WebSocket is listening on.",
		position = 1
	)
	default int port()
	{
		return 47100;
	}

	@ConfigItem(
		keyName = "pairingToken",
		name = "Pairing token",
		description = "Paste the pairing token shown in the desktop app's Settings > RuneLite screen.",
		position = 2
	)
	default String pairingToken()
	{
		return "";
	}
}
