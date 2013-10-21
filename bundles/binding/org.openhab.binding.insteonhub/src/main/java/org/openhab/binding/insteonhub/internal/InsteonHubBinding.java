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
package org.openhab.binding.insteonhub.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.insteonhub.InsteonHubBindingProvider;
import org.openhab.binding.insteonhub.internal.InsteonHubBindingConfig.BindingType;
import org.openhab.binding.insteonhub.internal.hardware.InsteonHubProxy;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Insteon Hub binding. Handles all commands and polls configured devices to
 * process updates.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class InsteonHubBinding extends
		AbstractActiveBinding<InsteonHubBindingProvider> implements
		ManagedService {

	public static final String CONFIG_KEY_HOST = "host";
	public static final String CONFIG_KEY_PORT = "port";
	public static final String CONFIG_KEY_USER = "user";
	public static final String CONFIG_KEY_PASS = "pass";

	public static final long DEFAULT_REFRESH_INTERVAL = 60000;
	public static final String DEFAULT_HUB_ID = "default";

	private static final Logger logger = LoggerFactory
			.getLogger(InsteonHubBinding.class);

	private long refreshInterval = DEFAULT_REFRESH_INTERVAL;

	// Map of proxies. key=hubId, value=proxy
	// Used to keep track of proxies
	private final Map<String, InsteonHubProxy> proxies = new HashMap<String, InsteonHubProxy>();

	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	@Override
	protected String getName() {
		return "InsteonHub Refresh Service";
	}

	@Override
	protected void execute() {
		try {
			// Iterate through all proxies
			for (Map.Entry<String, InsteonHubProxy> entry : proxies.entrySet()) {
				String deviceUid = entry.getKey();
				InsteonHubProxy receiverProxy = entry.getValue();
				sendUpdates(receiverProxy, deviceUid);
			}
		} catch (Throwable t) {
			logger.error("Error polling devices for " + getName(), t);
		}
	}

	private void sendUpdates(InsteonHubProxy proxy, String hubid) {
		// Get all item configurations belonging to this proxy
		Collection<InsteonHubBindingConfig> configs = getHubConfigs(hubid);
		for (InsteonHubBindingConfig config : configs) {
			try {
				int level = proxy.getLevel(config.getDevice());
				if (config.getBindingType() == BindingType.digital) {
					State update = level > 0 ? OnOffType.ON : OnOffType.OFF;
					eventPublisher.postUpdate(config.getItemName(), update);
				} else if (config.getBindingType() == BindingType.dimmer) {
					State update = new DecimalType(level);
					eventPublisher.postUpdate(config.getItemName(), update);
				}
			} catch (IOException e) {
				warnCommunicationFailure(proxy, config);
				break;
			}
		}
	}

	private Collection<InsteonHubBindingConfig> getHubConfigs(String hubId) {
		Map<String, InsteonHubBindingConfig> items = new HashMap<String, InsteonHubBindingConfig>();
		for (InsteonHubBindingProvider provider : this.providers) {
			provider.getHubConfigs(hubId, items);
		}
		return items.values();
	}

	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		InsteonHubBindingConfig config = getConfigForItemName(itemName);
		if (config == null) {
			logger.error("Received command for unknown item '" + itemName + "'");
			return;
		}
		InsteonHubProxy proxy = proxies.get(config.getHubId());
		if (proxy == null) {
			logger.error("Received command for unknown hub id '"
					+ config.getHubId() + "'");
			return;
		}

		try {
			BindingType type = config.getBindingType();

			if (type == BindingType.digital) {
				if (command instanceof OnOffType) {
					proxy.setPower(config.getDevice(), command == OnOffType.ON,
							false);
				}
			} else if (type == BindingType.dimmer) {
				if (command instanceof IncreaseDecreaseType
						|| command instanceof UpDownType) {
					// increase or decrease by 30/255
					int level = proxy.getLevel(config.getDevice());
					if (command == IncreaseDecreaseType.INCREASE
							|| command == UpDownType.UP) {
						level += 30;
					} else {
						level -= 30;
					}
					if (level < 0) {
						level = 0;
					} else if (level > 255) {
						level = 255;
					}
					proxy.setLevel(config.getDevice(), level);
				} else if (command instanceof PercentType) {
					// set level from percent
					byte percentByte = ((PercentType) command).byteValue();
					float percent = percentByte * .01f;
					int level = (int) (255 * percent);
					proxy.setLevel(config.getDevice(), level);
				} else {
					// set level from value
					int level = Integer.parseInt(command.toString());
					proxy.setLevel(config.getDevice(), level);
				}
			}
		} catch (IOException e) {
			warnCommunicationFailure(proxy, config);
		} catch (Throwable t) {
			logger.error("Error processing command '" + command
					+ "' for item '" + itemName + "'", t);
		}
	}

	private void warnCommunicationFailure(InsteonHubProxy proxy,
			InsteonHubBindingConfig config) {
		logger.warn("Cannot communicate with " + proxy.getConnectionString()
				+ " (hubid: " + config.getHubId() + ", device:"
				+ config.getDevice() + ")");
	}

	private InsteonHubBindingConfig getConfigForItemName(String itemName) {
		for (InsteonHubBindingProvider provider : this.providers) {
			if (provider.getItemConfig(itemName) != null) {
				return provider.getItemConfig(itemName);
			}
		}
		return null;
	}

	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		logger.debug("internalReceiveCommand() is called!");
	}

	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		try {
			// Process device configuration
			if (config != null) {
				String refreshIntervalString = (String) config.get("refresh");
				if (StringUtils.isNotBlank(refreshIntervalString)) {
					refreshInterval = Long.parseLong(refreshIntervalString);
				}
				// parse all configured receivers
				// ( insteonhub:<hubid>.host=10.0.0.2 )
				// ( insteonhub:<hubid>.user=username )
				// ( insteonhub:<hubid>.pass=password )
				Enumeration<String> keys = config.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (key.endsWith(CONFIG_KEY_HOST)) {
						// parse host
						String host = (String) config.get(key);
						int separatorIdx = key.indexOf('.');
						String hubId, keyPrefix;
						if (separatorIdx == -1) {
							// no prefix/hubid => one hub => use default hub ID
							hubId = DEFAULT_HUB_ID;
							keyPrefix = "";
						} else {
							// prefix => use it as the hub ID
							hubId = key.substring(0, separatorIdx);
							keyPrefix = hubId + ".";
						}
						String user = (String) config.get(keyPrefix
								+ CONFIG_KEY_USER);
						String pass = (String) config.get(keyPrefix
								+ CONFIG_KEY_PASS);
						String portStr = (String) config.get(keyPrefix
								+ CONFIG_KEY_PORT);
						int port = StringUtils.isBlank(portStr) ? InsteonHubProxy.DEFAULT_PORT
								: Integer
										.parseInt(config.get(
												keyPrefix + CONFIG_KEY_PORT)
												.toString());
						// Add proxy to map
						proxies.put(hubId, new InsteonHubProxy(host, port,
								user, pass));
					}
				}
				setProperlyConfigured(true);
			}
		} catch (Throwable t) {
			logger.error("Error configuring " + getName(), t);
		}
	}

	@Override
	public void activate() {

	}

	@Override
	public void deactivate() {

	}

}
