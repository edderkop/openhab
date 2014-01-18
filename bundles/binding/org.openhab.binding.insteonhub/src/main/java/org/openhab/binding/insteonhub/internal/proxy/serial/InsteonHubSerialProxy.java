/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.proxy.serial;

import java.io.IOException;
import java.net.Socket;

import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;
import org.openhab.binding.insteonhub.internal.bus.InsteonHubBusListener;
import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubProxy;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdateFactory;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link InsteonHubProxy} implementation that uses the Serial API to
 * communicate with the INSTEON Hub. This proxy takes care of the connection and
 * translating method calls to queue-able commands. The transport will take care
 * of actually sending and receiving the messages.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class InsteonHubSerialProxy implements InsteonHubProxy {

	private static final Logger logger = LoggerFactory
			.getLogger(InsteonHubSerialProxy.class);

	public static final int DEFAULT_PORT = 9761;
	private static final long RETRY_INTERVAL_SECONDS = 30;
	private static final long MILLIS_PER_SECOND = 1000;

	private final InsteonHubUpdateFactory updateFactory = new InsteonHubUpdateFactory();
	private final InsteonHubSerialTransport transport;
	private final String host;
	private final int port;
	
	private volatile Socket socket;
	private volatile Connecter connecter;
	private InsteonHubBus bus;
	
	public InsteonHubSerialProxy(String host) {
		this(host, DEFAULT_PORT);
	}

	public InsteonHubSerialProxy(String host, int port) {
		this.host = host;
		this.port = port;
		transport = new InsteonHubSerialTransport(transportCallbacks, getConnectionString());
	}
	
	@Override
	public void setBus(InsteonHubBus bus) {
		this.bus = bus;
		bus.addListener(busListener);
	}

	@Override
	public synchronized void start() {
		reconnect();
	}

	protected synchronized void reconnect() {
		// if not currently connecting => reconnect
		if (connecter == null) {
			// stop and cleanup existing connection
			stop();
			// asynchronously reconnect
			connecter = new Connecter();
			new Thread(connecter, getConnectionString() + " connecter").start();
		}
	}
	
	private String getConnectionString() {
		return host + ":" + port;
	}

	@Override
	public synchronized void stop() {
		// tell all threads to gracefully exit
		connecter = null;
		transport.stop();
		// try to close the socket
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}
	
	private final InsteonHubBusListener busListener = new InsteonHubBusListener() {

		@Override
		public void onSendOpenhabCommand(int deviceId, Command command) {
			// ignore
		}

		@Override
		public void onPostOpenhabUpdate(int deviceId, State update) {
			// ignore
		}

		@Override
		public void onSendInsteonCommand(int deviceId, InsteonHubCommand command) {
			sendCommandToHub(command);
		}

		@Override
		public void onPostInsteonUpdate(int deviceId, InsteonHubUpdate update) {
			// proxy's transport will be posting this
		}
		
	};
	
	private void sendCommandToHub(InsteonHubCommand command) {
		if (!transport.isStarted()) {
			logger.info("Not sending message - Not connected to Hub");
			return;
		}
		transport.enqueueCommand(command.buildSerialMessage());
	}

	private final InsteonHubSerialTransportCallbacks transportCallbacks = new InsteonHubSerialTransportCallbacks() {
		
		@Override
		public void onReceived(byte[] msg) {
			InsteonHubUpdate update = updateFactory.createUpdate(msg);
			if(update != null) {
				bus.postInsteonUpdate(update.getDeviceId(), update);
			}
		}
		
		@Override
		public void onDisconnect() {
			logger.error("Insteon Hub " + getConnectionString() + " Connection lost");
			reconnect();
		}
	};
	

	private class Connecter implements Runnable {
		@Override
		public void run() {
			while (connecter == this) {
				try {
					// try connecting
					socket = new Socket(host, port);
					// no IOException => success => start send/rec threads
					transport.start(socket.getInputStream(),
							socket.getOutputStream());
					connecter = null;
					logger.info("Connected to Insteon Hub: " + host + ":"
							+ port);
					return;
				} catch (IOException e) {
					// connection failure => log and retry in a bit
					logger.warn("Could not connect to Insteon Hub. Will retry connection in "
							+ RETRY_INTERVAL_SECONDS + " seconds...");
					try {
						Thread.sleep(RETRY_INTERVAL_SECONDS * MILLIS_PER_SECOND);
					} catch (InterruptedException e1) {
						// ignore
					}
				}
			}
		}
	}
	
}
