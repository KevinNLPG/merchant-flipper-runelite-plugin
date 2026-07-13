package com.merchantflipper.net;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Thin adapter over OkHttp's WebSocket client so {@link ConnectionManager} can depend on a small
 * callback interface instead of the library's own listener directly. This class opens an
 * outbound connection only - it never listens on a socket or accepts inbound connections.
 *
 * <p>Uses OkHttp (not a separate WebSocket library) because {@code okhttp3} already ships as a
 * transitive dependency of the RuneLite client itself, so plugins get it for free without adding
 * anything to their own build.
 */
final class MerchantFlipperWebSocketClient
{
	interface Listener
	{
		void onSocketOpen();

		void onSocketMessage(String message);

		void onSocketClose(int code, String reason, boolean remote);

		void onSocketError(Exception ex);
	}

	// Shared across every instance (each reconnect attempt creates a new MerchantFlipperWebSocketClient):
	// an OkHttpClient owns its own thread/connection pools and is meant to be a long-lived singleton,
	// not recreated per connection attempt.
	private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
		.retryOnConnectionFailure(false)
		.build();

	private final URI serverUri;
	private final Listener listener;
	private final AtomicBoolean open = new AtomicBoolean(false);
	private volatile WebSocket webSocket;

	MerchantFlipperWebSocketClient(URI serverUri, Listener listener)
	{
		this.serverUri = serverUri;
		this.listener = listener;
	}

	void connect()
	{
		Request request = new Request.Builder().url(serverUri.toString()).build();
		webSocket = CLIENT.newWebSocket(request, new WebSocketListener()
		{
			@Override
			public void onOpen(WebSocket webSocket, Response response)
			{
				open.set(true);
				listener.onSocketOpen();
			}

			@Override
			public void onMessage(WebSocket webSocket, String text)
			{
				listener.onSocketMessage(text);
			}

			@Override
			public void onClosing(WebSocket webSocket, int code, String reason)
			{
				// Acknowledge the server-initiated close handshake, per OkHttp's documented contract.
				webSocket.close(code, reason);
			}

			@Override
			public void onClosed(WebSocket webSocket, int code, String reason)
			{
				open.set(false);
				listener.onSocketClose(code, reason, true);
			}

			@Override
			// response is nullable per WebSocketListener's contract (no HTTP response may exist
			// on a pure connection failure) - not annotated since no nullability-annotation
			// library is already on this plugin's classpath, and it's not worth adding one.
			public void onFailure(WebSocket webSocket, Throwable t, Response response)
			{
				open.set(false);
				listener.onSocketError(t instanceof Exception ? (Exception) t : new Exception(t));
				listener.onSocketClose(-1, t.getMessage(), true);
			}
		});
	}

	boolean isOpen()
	{
		return open.get();
	}

	void send(String message)
	{
		WebSocket ws = webSocket;
		if (ws != null)
		{
			ws.send(message);
		}
	}

	/**
	 * Named to match the previous Java-WebSocket-based adapter's API ({@link ConnectionManager}
	 * calls this expecting the socket to be fully released before it opens the next one), but
	 * OkHttp has no blocking-close equivalent. {@code cancel()} immediately and synchronously
	 * releases the connection without a graceful close handshake, which is appropriate here: this
	 * is called when we're already tearing down (shutdown or reconnect), not a case where telling
	 * the desktop app "goodbye" politely matters.
	 */
	void closeBlocking()
	{
		open.set(false);
		WebSocket ws = webSocket;
		if (ws != null)
		{
			ws.cancel();
		}
	}
}
