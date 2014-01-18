/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.device;

/**
 * Creates the appropraite {@link InsteonHubDevice} instance given the device ID
 * and {@link InsteonHubDeviceType}
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubDeviceFactory {

	public InsteonHubDevice createDevice(int deviceId,
			InsteonHubDeviceType deviceType) {
		InsteonHubDevice device = null;
		switch (deviceType) {
		case DIMMER:
			device = new InsteonHubDimmerDevice();
			break;
		case SWITCH:
			device = new InsteonHubSwitchDevice();
			break;
		case OPEN_CLOSE_SENSOR:
			device = new InsteonHubOpenCloseSensor();
			break;
		case LEAK_SENSOR:
			device = new InsteonHubLeakSensor();
			break;
		case MOTION_SENSOR:
		case SMOKE_BRIDGE:
			device = new InsteonHubDigitalSensor();
			break;
		}
		device.setDeviceId(deviceId);
		return device;
	}
}
