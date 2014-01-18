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
 * The different types of supported Insteon devices. Note that "DIMMER" is used
 * for both dimming in-wall and plug-in modules, and "SWITCH" is used for both
 * on/off in-wall and plug-in modules.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public enum InsteonHubDeviceType {
	DIMMER, SWITCH, OPEN_CLOSE_SENSOR, LEAK_SENSOR, MOTION_SENSOR, SMOKE_BRIDGE;
	public static InsteonHubDeviceType parseIgnoreCase(String str) {
		for (InsteonHubDeviceType type : values()) {
			if (type.toString().equalsIgnoreCase(str)) {
				return type;
			}
		}
		return null;
	}
}