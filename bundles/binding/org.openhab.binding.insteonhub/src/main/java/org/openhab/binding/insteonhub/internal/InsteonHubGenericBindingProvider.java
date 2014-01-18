/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.insteonhub.InsteonHubBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class InsteonHubGenericBindingProvider extends
		AbstractGenericBindingProvider implements InsteonHubBindingProvider {

	private final Map<String, InsteonHubBindingConfig> configsByItemName = new HashMap<String, InsteonHubBindingConfig>();
	private final Map<Integer, InsteonHubBindingConfig> configsByDeviceId = new HashMap<Integer, InsteonHubBindingConfig>();

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "insteonhub";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {

	}

	@Override
	public InsteonHubBindingConfig getItemConfig(String itemName) {
		return configsByItemName.get(itemName);
	}

	@Override
	public InsteonHubBindingConfig getDeviceConfig(int deviceId) {
		return configsByDeviceId.get(deviceId);
	}

	@Override
	public Collection<InsteonHubBindingConfig> getConfigs() {
		return new ArrayList<InsteonHubBindingConfig>(configsByItemName.values());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void processBindingConfiguration(String context,
			Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		if (bindingConfig != null) {
			// parse binding configuration
			InsteonHubBindingConfig config = InsteonHubBindingConfig.parse(
					item.getName(), bindingConfig);
			// add binding configuration
			addBindingConfig(item, config);
			// add to maps
			configsByItemName.put(config.getItemName(), config);
			configsByDeviceId.put(config.getDeviceId(), config);
		}
	}

}
