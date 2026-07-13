package com.merchantflipper;

import com.google.inject.Provides;
import com.merchantflipper.buffer.EventBuffer;
import com.merchantflipper.net.ConnectionManager;
import com.merchantflipper.profile.ProfileIdProvider;
import com.merchantflipper.protocol.Protocol;
import com.merchantflipper.protocol.RawOfferEvent;
import com.merchantflipper.tracking.OfferSnapshot;
import com.merchantflipper.tracking.OfferStateTracker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Read-only sensor plugin: observes the player's own Grand Exchange offers
 * via RuneLite's {@link GrandExchangeOfferChanged} event and reports changes
 * to the local Merchant Flipper desktop app over a WebSocket the plugin
 * opens as a *client* to 127.0.0.1. This plugin never clicks, types, places,
 * modifies, or cancels any offer, and never binds/listens on a socket
 * itself - see PRIVACY.md.
 */
@Slf4j
@PluginDescriptor(
	name = "Merchant Flipper",
	description = "Reports your own Grand Exchange offer changes to the local Merchant Flipper desktop app "
		+ "(127.0.0.1 only). Read-only: never automates anything in-game.",
	tags = {"grand exchange", "ge", "flipping", "trading", "merchant"}
)
public class MerchantFlipperPlugin extends Plugin
{
	public static final String PLUGIN_VERSION = "0.1.0";
	private static final String BUFFER_FILE_NAME = "offer-event-buffer.ndjson";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MerchantFlipperConfig config;

	private OfferStateTracker offerStateTracker;
	private ConnectionManager connectionManager;

	@Override
	protected void startUp() throws Exception
	{
		offerStateTracker = new OfferStateTracker();

		EventBuffer buffer;
		try
		{
			buffer = new EventBuffer(resolveBufferFile());
		}
		catch (IOException e)
		{
			log.warn("Merchant Flipper: could not open the RuneLite-dir event buffer file, "
				+ "falling back to a temp-dir buffer for this session", e);
			buffer = new EventBuffer(resolveFallbackBufferFile());
		}

		connectionManager = new ConnectionManager(buffer, PLUGIN_VERSION, new ConnectionManager.StatusListener()
		{
			@Override
			public void onConnected()
			{
				postChatMessage("Connected to the Merchant Flipper desktop app.");
			}

			@Override
			public void onDisconnected()
			{
				postChatMessage("Disconnected from the Merchant Flipper desktop app - retrying...");
			}

			@Override
			public void onRejected(String reason)
			{
				postChatMessage("Pairing rejected (" + reason + "). Paste a fresh pairing token into the "
					+ "Merchant Flipper plugin settings, then toggle the plugin off and on.");
			}
		});

		connectionManager.start(config.host(), config.port(), config.pairingToken());
	}

	@Override
	protected void shutDown() throws Exception
	{
		if (connectionManager != null)
		{
			connectionManager.stop();
			connectionManager = null;
		}
		offerStateTracker = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MerchantFlipperConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (connectionManager != null)
		{
			connectionManager.reconfigure(config.host(), config.port(), config.pairingToken());
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (connectionManager == null || offerStateTracker == null)
		{
			return;
		}

		GrandExchangeOffer offer = event.getOffer();
		int slot = event.getSlot();

		OfferSnapshot snapshot = OfferSnapshot.fromOffer(offer);
		Optional<OfferSnapshot> changed = offerStateTracker.update(slot, snapshot);
		if (!changed.isPresent())
		{
			return;
		}

		String profileId = ProfileIdProvider.deriveProfileId(client.getAccountHash());
		connectionManager.updateProfileId(profileId);

		RawOfferEvent rawEvent = new RawOfferEvent(
			Protocol.VERSION,
			UUID.randomUUID().toString(),
			System.currentTimeMillis(),
			profileId,
			slot,
			snapshot.getItemId(),
			snapshot.getState().name(),
			snapshot.getTotalQuantity(),
			snapshot.getFilledQuantity(),
			snapshot.getPrice(),
			snapshot.getSpent());

		connectionManager.sendOfferEvent(rawEvent);
	}

	@Provides
	MerchantFlipperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MerchantFlipperConfig.class);
	}

	private Path resolveBufferFile()
	{
		File dir = new File(RuneLite.RUNELITE_DIR, "merchant-flipper");
		return new File(dir, BUFFER_FILE_NAME).toPath();
	}

	private Path resolveFallbackBufferFile()
	{
		// Used only if the real RuneLite dir couldn't be created/written to; buffering simply
		// won't survive a restart in that unlikely case, but live events still flow normally.
		File dir = new File(System.getProperty("java.io.tmpdir"), "merchant-flipper-fallback");
		return new File(dir, BUFFER_FILE_NAME).toPath();
	}

	private void postChatMessage(String message)
	{
		clientThread.invoke(() ->
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Merchant Flipper] " + message, null));
	}
}
