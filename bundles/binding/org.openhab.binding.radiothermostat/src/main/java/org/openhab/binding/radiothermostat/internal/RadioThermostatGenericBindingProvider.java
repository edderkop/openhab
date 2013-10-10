/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2010-2013, openHAB.org <admin@openhab.org>
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with Eclipse (or a modified version of that library),
 * containing parts covered by the terms of the Eclipse Public License
 * (EPL), the licensors of this Program grant you additional permission
 * to convey the resulting work.
 */
package org.openhab.binding.radiothermostat.internal;

import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.radiothermostat.RadioThermostatBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class RadioThermostatGenericBindingProvider extends
		AbstractGenericBindingProvider implements RadioThermostatBindingProvider {

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "radiothermostat";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item,
			String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		if (bindingConfig != null) {
			// parse configuration
			Map<String, String> props = parseProperties(bindingConfig);
			String id = props.get(RadioThermostatBindingConfig.KEY_DEVICE_UID);
			String bindingType = props
					.get(RadioThermostatBindingConfig.KEY_BINDING_TYPE);
			// create configuration
			RadioThermostatBindingConfig config = new RadioThermostatBindingConfig(
					id, bindingType, item.getName());
			// add binding configuration
			addBindingConfig(item, config);
		}
	}

	public static Map<String, String> parseProperties(String config) {
		Map<String, String> props = new HashMap<String, String>();
		String[] tokens = config.trim().split(",");
		for (String token : tokens) {
			token = token.trim();
			String[] confStatement = token.split("=");
			String key = confStatement[0];
			String value = confStatement[1];
			props.put(key, value);
		}
		return props;
	}

	@Override
	public RadioThermostatBindingConfig getItemConfig(String itemName) {
		return ((RadioThermostatBindingConfig) bindingConfigs.get(itemName));
	}

	@Override
	public void getDeviceConfigs(String deviceUid,
			Map<String, RadioThermostatBindingConfig> configs) {
		for (BindingConfig config : bindingConfigs.values()) {
			RadioThermostatBindingConfig tstatConfig = (RadioThermostatBindingConfig) config;
			if (tstatConfig.getDeviceUid().equals(deviceUid)) {
				configs.put(tstatConfig.getItemName(), tstatConfig);
			}
		}
	}

}
