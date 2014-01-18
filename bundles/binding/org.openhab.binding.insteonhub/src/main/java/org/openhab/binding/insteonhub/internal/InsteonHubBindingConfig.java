/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openhab.binding.insteonhub.internal.device.InsteonHubDeviceType;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;
import org.openhab.core.binding.BindingConfig;

/**
 * InsteonHub Binding Configuration
 * 
 * @author Eric Thill
 *
 * @since 1.4.0
 */
public class InsteonHubBindingConfig implements BindingConfig {

	public static final String KEY_BINDING_TYPE = "bindingType";
	public static final String KEY_HUB_ID = "hubid";
	public static final String KEY_DEVICE = "device";
	public static final String KEY_ON_VALUE = "onValue";
	public static final String KEY_OFF_VALUE = "offValue";
	public static final String KEY_OPEN_VALUE = "openValue";
	public static final String KEY_CLOSED_VALUE = "closedValue";

	public static InsteonHubBindingConfig parse(String itemName,
			String configStr) {
		Map<String, String> configMap = stringToMap(configStr);

		// parse hubId (default used if not present)
		String hubId = configMap.get(KEY_HUB_ID);
		if (hubId == null) {
			// no hubid defined => use default
			hubId = InsteonHubBinding.DEFAULT_HUB_ID;
		}

		// parse required device key
		String device = configMap.get(KEY_DEVICE);
		if (device == null) {
			throw new IllegalArgumentException(KEY_DEVICE
					+ " is not defined in " + configMap);
		}
		device = device.replace(".", "");

		// parse required bindingType key
		String bindingTypeStr = configMap.get(KEY_BINDING_TYPE);
		if (bindingTypeStr == null) {
			throw new IllegalArgumentException(KEY_BINDING_TYPE
					+ " is not defined in " + configMap);
		}
		InsteonHubDeviceType bindingType = InsteonHubDeviceType.parseIgnoreCase(bindingTypeStr);
		if (bindingType == null) {
			throw new IllegalArgumentException("Unknown value for "
					+ KEY_BINDING_TYPE + " '" + bindingTypeStr + "'");
		}

		int deviceId = InsteonHubByteUtil.deviceIdStringToInt(device);
		return new InsteonHubBindingConfig(itemName, hubId, deviceId, bindingType);
	}

	private static Map<String, String> stringToMap(String str) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		String[] keyValuePairs = str.split(",");
		for (String keyValuePair : keyValuePairs) {
			String key;
			String value;
			if (keyValuePair.contains("=")) {
				// parse the key and value
				String[] split = keyValuePair.split("=");
				key = split[0].trim();
				value = split[1].trim();
			} else {
				// treat this as a true/false flag to enable
				key = keyValuePair.trim();
				value = "true";
			}
			map.put(key, value);
		}
		return map;
	}

	private final String itemName;
	private final String hubId;
	private final int deviceId;
	private final InsteonHubDeviceType bindingType;

	public InsteonHubBindingConfig(String itemName,
			String hubId, int deviceId, InsteonHubDeviceType bindingType) {
		this.itemName = itemName;
		this.hubId = hubId;
		this.deviceId = deviceId;
		this.bindingType = bindingType;
	}

	public String getItemName() {
		return itemName;
	}

	public String getHubId() {
		return hubId;
	}
	
	public int getDeviceId() {
		return deviceId;
	}

	public InsteonHubDeviceType getBindingType() {
		return bindingType;
	}
	

}
