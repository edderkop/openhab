/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.proxy;

import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;

/**
 * Interface for an INSTEON Hub controller
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public interface InsteonHubProxy {

	/**
	 * Set the Insteon Hub bus to listen for commands and post updates
	 * 
	 * @param deviceManager
	 */
	void setBus(InsteonHubBus bus);
	
	/**
	 * Connect and start any internal threads
	 */
	void start();

	/**
	 * Disconnect and stop any internal threads
	 */
	void stop();

}
