package com.merchantflipper.net;

import com.merchantflipper.buffer.EventBuffer;
import com.merchantflipper.protocol.EventAckMessage;
import com.merchantflipper.protocol.HeartbeatMessage;
import com.merchantflipper.protocol.HelloAckMessage;
import com.merchantflipper.protocol.HelloMessage;
import com.merchantflipper.protocol.IncomingMessage;
import com.merchantflipper.protocol.OfferEventMessage;
import com.merchantflipper.protocol.ProfileAnnounceMessage;
import com.merchantflipper.protocol.Protocol;
import com.merchantflipper.protocol.ProtocolCodec;
import com.merchantflipper.protocol.RawOfferEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Owns the outbound WebSocket connection to the desktop app: connects as a
 * client only (never listens), sends {@code hello} on open, reconnects with
 * exponential backoff on any drop, sends a heartbeat every 15s while
 * connected, replays the local {@link EventBuffer} once accepted, and stops
 * retrying (rather than spamming a rejected pairing token) once the desktop
 * app has explicitly rejected it.
 *
 * <p>This class deliberately contains no game-automation logic of any kind:
 * it only ever sends the handful of plugin -> desktop message types defined
 * in the wire protocol, and only ever dials out to the configured
 * host/port - it never binds or listens on a socket itself.
 */
public final class ConnectionManager implements MerchantFlipperWebSocketClient.Listener
{
	private static final long HEARTBEAT_INTERVAL_MILLIS = 15_000L;

	private final EventBuffer buffer;
	private final BackoffCalculator backoff = new BackoffCalculator();
	private final String pluginVersion;
	private final StatusListener statusListener;

	private ScheduledExecutorService executor;
	private volatile MerchantFlipperWebSocketClient socket;
	private volatile ScheduledFuture<?> reconnectFuture;
	private volatile ScheduledFuture<?> heartbeatFuture;
	private volatile boolean running;
	private volatile boolean tokenRejected;
	private volatile boolean connectedAndAccepted;
	private volatile String host;
	private volatile int port;
	private volatile String pairingToken;
	private volatile String currentProfileId;

	public interface StatusListener
	{
		void onConnected();

		void onDisconnected();

		void onRejected(String reason);
	}

	public ConnectionManager(EventBuffer buffer, String pluginVersion, StatusListener statusListener)
	{
		this.buffer = buffer;
		this.pluginVersion = pluginVersion;
		this.statusListener = statusListener;
	}

	/** Starts (or restarts) the connection lifecycle with the given config values. */
	public synchronized void start(String host, int port, String pairingToken)
	{
		this.host = host;
		this.port = port;
		this.pairingToken = pairingToken;
		this.tokenRejected = false;
		this.running = true;
		this.executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, "merchant-flipper-ws");
			t.setDaemon(true);
			return t;
		});
		backoff.reset();
		connectNow();
	}

	/** Applies new config (host/port/token changed) and reconnects immediately with a fresh backoff. */
	public synchronized void reconfigure(String host, int port, String pairingToken)
	{
		this.host = host;
		this.port = port;
		this.pairingToken = pairingToken;
		this.tokenRejected = false;
		backoff.reset();
		closeSocketQuietly();
		if (running)
		{
			connectNow();
		}
	}

	/** Shuts the connection down permanently (plugin shutDown()). Safe to call more than once. */
	public synchronized void stop()
	{
		running = false;
		cancelReconnect();
		cancelHeartbeat();
		closeSocketQuietly();
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
	}

	private void cancelReconnect()
	{
		ScheduledFuture<?> f = reconnectFuture;
		if (f != null)
		{
			f.cancel(true);
			reconnectFuture = null;
		}
	}

	private void cancelHeartbeat()
	{
		ScheduledFuture<?> f = heartbeatFuture;
		if (f != null)
		{
			f.cancel(true);
			heartbeatFuture = null;
		}
	}

	private void closeSocketQuietly()
	{
		MerchantFlipperWebSocketClient s = socket;
		socket = null;
		connectedAndAccepted = false;
		cancelHeartbeat();
		if (s != null)
		{
			s.closeBlocking();
		}
	}

	private void connectNow()
	{
		if (!running || tokenRejected)
		{
			return;
		}
		try
		{
			URI uri = new URI("ws://" + host + ":" + port);
			MerchantFlipperWebSocketClient client = new MerchantFlipperWebSocketClient(uri, this);
			socket = client;
			client.connect();
		}
		catch (URISyntaxException e)
		{
			scheduleReconnect();
		}
	}

	private void scheduleReconnect()
	{
		if (!running || tokenRejected)
		{
			return;
		}
		ScheduledExecutorService ex = executor;
		if (ex == null || ex.isShutdown())
		{
			return;
		}
		long delay = backoff.nextDelayMillis();
		reconnectFuture = ex.schedule(this::connectNow, delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onSocketOpen()
	{
		backoff.reset();
		HelloMessage hello = new HelloMessage(Protocol.VERSION, pluginVersion, pairingToken);
		send(ProtocolCodec.toJson(hello));
		startHeartbeat();
	}

	@Override
	public void onSocketMessage(String message)
	{
		IncomingMessage incoming = IncomingMessage.parse(message);
		if (incoming == null)
		{
			return;
		}

		switch (incoming.getType())
		{
			case HELLO_ACK:
				handleHelloAck(incoming.getHelloAck());
				break;
			case EVENT_ACK:
				handleEventAck(incoming.getEventAck());
				break;
			case HEARTBEAT:
			case UNKNOWN:
			default:
				// Server heartbeats just confirm liveness; unknown types are ignored for forward compatibility.
				break;
		}
	}

	private void handleHelloAck(HelloAckMessage ack)
	{
		if (ack.isAccepted())
		{
			connectedAndAccepted = true;
			backoff.reset();
			if (statusListener != null)
			{
				statusListener.onConnected();
			}
			if (currentProfileId != null)
			{
				sendRaw(new ProfileAnnounceMessage(currentProfileId, System.currentTimeMillis()));
			}
			replayBuffer();
		}
		else
		{
			tokenRejected = true;
			connectedAndAccepted = false;
			if (statusListener != null)
			{
				String reason = ack.getReason() != null ? ack.getReason() : "Pairing token rejected by desktop app";
				statusListener.onRejected(reason);
			}
			closeSocketQuietly();
		}
	}

	private void handleEventAck(EventAckMessage ack)
	{
		if (ack == null || ack.getEventId() == null)
		{
			return;
		}
		try
		{
			buffer.acknowledge(ack.getEventId());
		}
		catch (IOException e)
		{
			// Best effort - if persistence fails we simply retry the ack removal on the next successful write.
		}
	}

	@Override
	public void onSocketClose(int code, String reason, boolean remote)
	{
		boolean wasAccepted = connectedAndAccepted;
		connectedAndAccepted = false;
		cancelHeartbeat();
		if (wasAccepted && statusListener != null)
		{
			statusListener.onDisconnected();
		}
		if (running && !tokenRejected)
		{
			scheduleReconnect();
		}
	}

	@Override
	public void onSocketError(Exception ex)
	{
		// The underlying library always follows an error with onClose(), which drives the actual
		// reconnect scheduling; nothing additional to do here.
	}

	private void startHeartbeat()
	{
		ScheduledExecutorService ex = executor;
		if (ex == null || ex.isShutdown())
		{
			return;
		}
		heartbeatFuture = ex.scheduleAtFixedRate(
			() -> sendRaw(new HeartbeatMessage(System.currentTimeMillis())),
			HEARTBEAT_INTERVAL_MILLIS,
			HEARTBEAT_INTERVAL_MILLIS,
			TimeUnit.MILLISECONDS);
	}

	private void replayBuffer()
	{
		for (RawOfferEvent event : buffer.pendingInOrder())
		{
			sendRaw(new OfferEventMessage(event));
		}
	}

	/**
	 * Called by the plugin whenever the current account's profileId is known/changes. Sends
	 * {@code profile_announce} immediately if already connected+accepted; otherwise the new id is
	 * simply remembered and will be announced as soon as a hello is accepted.
	 */
	public void updateProfileId(String profileId)
	{
		if (profileId == null || profileId.equals(currentProfileId))
		{
			return;
		}
		currentProfileId = profileId;
		if (connectedAndAccepted)
		{
			sendRaw(new ProfileAnnounceMessage(profileId, System.currentTimeMillis()));
		}
	}

	/**
	 * Buffers {@code event} durably and, if currently connected+accepted, also sends it live
	 * immediately. The event stays in the buffer until its matching {@code event_ack} arrives,
	 * regardless of whether the live send happened.
	 */
	public void sendOfferEvent(RawOfferEvent event)
	{
		try
		{
			buffer.append(event);
		}
		catch (IOException e)
		{
			// Even if local persistence failed, still attempt the best-effort live send below.
		}
		if (connectedAndAccepted)
		{
			sendRaw(new OfferEventMessage(event));
		}
	}

	private void sendRaw(Object message)
	{
		send(ProtocolCodec.toJson(message));
	}

	private void send(String json)
	{
		MerchantFlipperWebSocketClient s = socket;
		if (s != null && s.isOpen())
		{
			s.send(json);
		}
	}
}
