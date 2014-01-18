/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.device;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;

/**
 * Manages {@link InsteonHubDevice}'s so they can be looked up by their by their
 * 3-byte Insteon device ID.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubDeviceManager {

	private final Map<Integer, InsteonHubDevice> devices = new LinkedHashMap<Integer, InsteonHubDevice>();
	private final InsteonHubBus bus;

	public InsteonHubDeviceManager(InsteonHubBus bus) {
		this.bus = bus;
	}

	public synchronized void clear() {
		devices.clear();
	}

	public synchronized void addDevice(int deviceId, InsteonHubDevice device) {
		InsteonHubDevice existing = devices.put(deviceId, device);
		if (existing != null) {
			existing.destroy();
		}
		device.setBus(bus);
	}

	public synchronized InsteonHubDevice removeDevice(int deviceId) {
		InsteonHubDevice existing = devices.remove(deviceId);
		if (existing != null) {
			existing.destroy();
		}
		return existing;
	}

	public synchronized InsteonHubDevice getDevice(int deviceId) {
		return devices.get(deviceId);
	}

	public synchronized void requestDeviceStates() {
		for (InsteonHubDevice device : devices.values()) {
			device.requestFromHub();
		}
	}

}
