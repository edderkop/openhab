/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.bus;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.openhab.binding.insteonhub.internal.InsteonHubBinding;
import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDevice;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubProxy;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the bus that is used for all communication with Insteon
 * Devices/Proxies/Hubs/etc. The flow of communication basically goes as
 * follows: The Binding maps openHAB itemName's to insteon ID's, and then sends
 * openHAB commands to the bus paired with the correct Insteon ID.
 * {@link InsteonHubDevice}'s each listen for openHAB {@link Command}'s
 * associated with their assigned device ID. Each {@link InsteonHubDevice} is
 * responsible to translating these openHAB commands into
 * {@link InsteonHubCommand}'s, and publishes these Insteon commands back to the
 * bus. {@link InsteonHubProxy}'s each listen for Insteon commands, serialize
 * these commands, and send them over the wire to the Insteon Hub. The
 * {@link InsteonHubProxy}'s are also responsible for listening to messages from
 * the Insteon Hub, parsing them into {@link InsteonHubUpdate}'s, and publishing
 * these updates to the bus. The {@link InsteonHubDevice}'s are then responsible
 * for listening to Insteon updates associated with their device ID, translating
 * these Insteon updates into openHAB {@link State} updates, and publishing
 * these openHAB updates back to the bus. The {@link InsteonHubBinding} is then
 * responsible for mapping these openHAB updates to the appropriately configured
 * item names, and publishing it to the openHAB event bus.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubBus {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final BlockingQueue<BusEvent> eventQueue = new LinkedBlockingQueue<BusEvent>();
	private final Set<InsteonHubBusListener> listeners = new LinkedHashSet<InsteonHubBusListener>();
	// avoids concurrent access exception when trying to add/remove listeners
	// during callbacks
	private final Set<InsteonHubBusListener> listenersToAdd = new LinkedHashSet<InsteonHubBusListener>();
	private final Set<InsteonHubBusListener> listenersToRemove = new LinkedHashSet<InsteonHubBusListener>();
	private volatile BusProcessor processor;

	public synchronized void start() {
		processor = new BusProcessor();
		new Thread(processor, "Insteon Hub Bus").start();
		logger.debug("InsteonHub Bus Started");
	}

	public synchronized void stop() {
		processor = null;
		eventQueue.add(new BusEvent());
	}

	public void postOpenhabUpdate(final int deviceId, final State update) {
		BusEvent event = new BusEvent();
		event.type = BusEventType.OPENHAB_UPDATE;
		event.deviceId = deviceId;
		event.openhabUpdate = update;
		eventQueue.add(event);
	}

	public void sendOpenhabCommand(final int deviceId, final Command command) {
		BusEvent event = new BusEvent();
		event.type = BusEventType.OPENHAB_COMMAND;
		event.deviceId = deviceId;
		event.openhabCommand = command;
		eventQueue.add(event);
	}

	public void postInsteonUpdate(final int deviceId,
			final InsteonHubUpdate update) {
		BusEvent event = new BusEvent();
		event.type = BusEventType.INSTEON_UPDATE;
		event.deviceId = deviceId;
		event.insteonUpdate = update;
		eventQueue.add(event);
	}

	public void sendInsteonCommand(final int deviceId,
			final InsteonHubCommand command) {
		BusEvent event = new BusEvent();
		event.type = BusEventType.INSTEON_COMMAND;
		event.deviceId = deviceId;
		event.insteonCommand = command;
		eventQueue.add(event);
	}

	public synchronized void addListener(InsteonHubBusListener listener) {
		listenersToAdd.add(listener);
	}

	public synchronized void removeListener(InsteonHubBusListener listener) {
		listenersToRemove.add(listener);
	}

	public static class BusEvent {
		public BusEventType type;
		public int deviceId;
		public Command openhabCommand;
		public State openhabUpdate;
		public InsteonHubCommand insteonCommand;
		public InsteonHubUpdate insteonUpdate;
	}

	private enum BusEventType {
		OPENHAB_COMMAND, OPENHAB_UPDATE, INSTEON_COMMAND, INSTEON_UPDATE;
	}

	private class BusProcessor implements Runnable {

		@Override
		public void run() {
			while (processor == this) {
				try {
					BusEvent event = eventQueue.take();
					handleEvent(event);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			logger.debug("InsteonHub Bus Stopped");
		}
	};

	private synchronized void handleEvent(BusEvent event) {
		BusEventType type = event.type;
		for (InsteonHubBusListener listener : listenersToAdd) {
			listeners.add(listener);
		}
		listenersToAdd.clear();
		for (InsteonHubBusListener listener : listenersToRemove) {
			listeners.remove(listener);
		}
		listenersToRemove.clear();
		for (InsteonHubBusListener listener : listeners) {
			try {
				if (type == BusEventType.INSTEON_COMMAND) {
					listener.onSendInsteonCommand(event.deviceId,
							event.insteonCommand);
				} else if (type == BusEventType.INSTEON_UPDATE) {
					listener.onPostInsteonUpdate(event.deviceId,
							event.insteonUpdate);
				} else if (type == BusEventType.OPENHAB_COMMAND) {
					listener.onSendOpenhabCommand(event.deviceId,
							event.openhabCommand);
				} else if (type == BusEventType.OPENHAB_UPDATE) {
					listener.onPostOpenhabUpdate(event.deviceId,
							event.openhabUpdate);
				}
			} catch (Throwable t) {
				logger.error("Error processing event", t);
			}
		}
	}
}
