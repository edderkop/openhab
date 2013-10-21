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
package org.openhab.binding.sharptv.internal;

import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.sharptv.SharpTvBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class SharpTvGenericBindingProvider extends
		AbstractGenericBindingProvider implements SharpTvBindingProvider {

	private static final Logger logger = LoggerFactory
			.getLogger(SharpTvGenericBindingProvider.class);

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "sharptv";
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
		Map<String, String> props = parseProperties(bindingConfig);
		String uid = props.get(SharpTvBindingConfig.KEY_DEVICE_UID);
		String bindingType = props.get(SharpTvBindingConfig.KEY_BINDING_TYPE);

		if (uid == null) {
			logger.error("uid not defined - check configuration");
			return;
		} else if (bindingType == null) {
			logger.error("bindingType not defined - check configuration");
			return;
		}

		SharpTvBindingConfig config = new SharpTvBindingConfig(uid,
				bindingType, item.getName());
		addBindingConfig(item, config);
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
	public SharpTvBindingConfig getItemConfig(String itemName) {
		return ((SharpTvBindingConfig) bindingConfigs.get(itemName));
	}

}
