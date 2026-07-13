package com.merchantflipper.net;

import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Thin adapter over the Java-WebSocket client so {@link ConnectionManager}
 * can depend on a small callback interface instead of the library's own
 * abstract class directly. This class opens an outbound connection only -
 * it never listens on a socket or accepts inbound connections.
 */
final class MerchantFlipperWebSocketClient extends WebSocketClient
{
	interface Listener
	{
		void onSocketOpen();

		void onSocketMessage(String message);

		void onSocketClose(int code, String reason, boolean remote);

		void onSocketError(Exception ex);
	}

	private final Listener listener;

	MerchantFlipperWebSocketClient(URI serverUri, Listener listener)
	{
		super(serverUri);
		this.listener = listener;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata)
	{
		listener.onSocketOpen();
	}

	@Override
	public void onMessage(String message)
	{
		listener.onSocketMessage(message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote)
	{
		listener.onSocketClose(code, reason, remote);
	}

	@Override
	public void onError(Exception ex)
	{
		listener.onSocketError(ex);
	}
}
