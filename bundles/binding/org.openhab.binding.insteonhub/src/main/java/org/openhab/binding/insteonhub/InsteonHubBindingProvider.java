/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub;

import java.util.Collection;

import org.openhab.binding.insteonhub.internal.InsteonHubBindingConfig;
import org.openhab.core.binding.BindingProvider;

/**
 * Insteon Hub BindingProvider interface
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public interface InsteonHubBindingProvider extends BindingProvider {

	/**
	 * Gets the configuration associated with the given {@code itemName}
	 * 
	 * @param itemName The Item Name
	 * @return The InsteonHubBindingConfig, or null if it didn't exist
	 */
	InsteonHubBindingConfig getItemConfig(String itemName);
	
	/**
	 * Gets the configuration associated with the given {@code deviceId}
	 * 
	 * @param deviceId The Device ID
	 * @return The InsteonHubBindingConfig, or null if it didn't exist
	 */
	InsteonHubBindingConfig getDeviceConfig(int deviceId);
	
	/**
	 * Gets all currently configured binding configs
	 * 
	 * @return A collection of all configs
	 */
	Collection<InsteonHubBindingConfig> getConfigs();

}
