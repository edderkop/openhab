/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.proxy.serial;

/**
 * Callbacks when using an {@link InsteonHubSerialTransport}. Callbacks include
 * when a message is received, and when the program becomes disconnected from
 * the Hub, in which case no more message will be received.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public interface InsteonHubSerialTransportCallbacks {
	void onDisconnect();

	void onReceived(byte[] msg);
}
