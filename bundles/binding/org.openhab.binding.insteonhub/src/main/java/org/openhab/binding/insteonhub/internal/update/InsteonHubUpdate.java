/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.update;

/**
 * A generic {@link InsteonHubUpdate}. Implementations of this class will
 * contain more detailed information about the received message.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public abstract class InsteonHubUpdate {

	private final int deviceId;
	private final int code;

	public InsteonHubUpdate(final int deviceId, final int code) {
		this.deviceId = deviceId;
		this.code = code;
	}

	public int getDeviceId() {
		return deviceId;
	}

	public int getCode() {
		return code;
	}
	
}
