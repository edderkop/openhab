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

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.radiothermostat.RadioThermostatBindingProvider;
import org.openhab.binding.radiothermostat.internal.RadioThermostatBindingConfig.BindingType;
import org.openhab.binding.radiothermostat.internal.hardware.RadioThermostatFanMode;
import org.openhab.binding.radiothermostat.internal.hardware.RadioThermostatHvacMode;
import org.openhab.binding.radiothermostat.internal.hardware.RadioThermostatProxy;
import org.openhab.binding.radiothermostat.internal.hardware.RadioThermostatState;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Radio Thermostat binding. Handles all commands and polls configured devices
 * to process updates.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class RadioThermostatBinding extends
		AbstractActiveBinding<RadioThermostatBindingProvider> implements
		ManagedService {

	public static final String CONFIG_KEY_HOST = "host";
	public static final long DEFAULT_REFRESH_INTERVAL = 60000;
	public static final String DEFAULT_DEVICE_UID = "default";

	private static final String BINDING_NAME = "RadioThermostatBinding";

	private static final Logger logger = LoggerFactory
			.getLogger(RadioThermostatBinding.class);

	private long refreshInterval = DEFAULT_REFRESH_INTERVAL;

	// Map of proxies. key=deviceUid, value=proxy
	// Used to keep track of proxies
	private final Map<String, RadioThermostatProxy> proxies = new HashMap<String, RadioThermostatProxy>();

	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	@Override
	protected String getName() {
		return "RadioThermostat Refresh Service";
	}

	@Override
	protected void execute() {
		try {
			// Iterate through all proxy entries
			for (Map.Entry<String, RadioThermostatProxy> entry : proxies
					.entrySet()) {
				String deviceUid = entry.getKey();
				RadioThermostatProxy proxy = entry.getValue();
				// Send updates for this proxy
				sendUpdates(proxy, deviceUid);
			}
		} catch (Throwable t) {
			logger.error("Error polling devices for " + getName(), t);
		}
	}

	private void sendUpdates(RadioThermostatProxy proxy, String deviceUid) {
		// Get all item configurations belonging to this proxy
		Collection<RadioThermostatBindingConfig> configs = getDeviceConfigs(deviceUid);

		try {
			// Poll the state from the device
			RadioThermostatState state = proxy.getState();

			// Create state updates
			State thermostatModeUpdate = new StringType(state.getHvacMode()
					.toString());
			State fanModeUpdate = new StringType(state.getFanMode().toString());
			State targetTempUpdate = new DecimalType(
					state.getTargetTemperature());
			State tempUpdate = new DecimalType(state.getTemperature());
			State holdUpdate = state.isHold() ? OnOffType.ON : OnOffType.OFF;
			State fanUpdate = state.isFan() ? OnOffType.ON : OnOffType.OFF;

			// Send updates
			sendUpdate(configs, BindingType.hvacMode, thermostatModeUpdate);
			sendUpdate(configs, BindingType.fanMode, fanModeUpdate);
			sendUpdate(configs, BindingType.targetTemperature, targetTempUpdate);
			sendUpdate(configs, BindingType.temperature, tempUpdate);
			sendUpdate(configs, BindingType.hold, holdUpdate);
			sendUpdate(configs, BindingType.fan, fanUpdate);
		} catch (IOException e) {
			logger.warn(BINDING_NAME + " cannot communicate with "
					+ proxy.getHost() + " (uid: " + deviceUid + ")");
		}
	}

	private Collection<RadioThermostatBindingConfig> getDeviceConfigs(
			String deviceUid) {
		Map<String, RadioThermostatBindingConfig> items = new HashMap<String, RadioThermostatBindingConfig>();
		for (RadioThermostatBindingProvider provider : this.providers) {
			provider.getDeviceConfigs(deviceUid, items);
		}
		return items.values();
	}

	private void sendUpdate(Collection<RadioThermostatBindingConfig> configs,
			BindingType type, State state) {
		for (RadioThermostatBindingConfig config : configs) {
			if (config.getBindingType() == type) {
				eventPublisher.postUpdate(config.getItemName(), state);
			}
		}
	}

	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		RadioThermostatBindingConfig config = getConfigForItemName(itemName);
		if (config == null) {
			logger.error("Received command for unknown item '" + itemName + "'");
			return;
		}
		RadioThermostatProxy proxy = proxies.get(config.getDeviceUid());
		if (proxy == null) {
			logger.error(BINDING_NAME
					+ " received command for unknown device uid '"
					+ config.getDeviceUid() + "'");
			return;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug(BINDING_NAME + " processing command '" + command
					+ "' of type '" + command.getClass().getSimpleName()
					+ "' for item '" + itemName + "'");
		}

		try {
			BindingType type = config.getBindingType();
			if (type == BindingType.hvacMode) {
				proxy.setHvacMode(RadioThermostatHvacMode.valueOf(command
						.toString()));
				// Changing mode will change other parameters, send updates now
				sendUpdates(proxy, config.getDeviceUid());
			} else if (type == BindingType.fanMode) {
				proxy.setFanMode(RadioThermostatFanMode.valueOf(command
						.toString()));
			} else if (type == BindingType.targetTemperature) {
				if (command instanceof IncreaseDecreaseType
						|| command instanceof UpDownType) {
					float adjAmt;
					if (command == IncreaseDecreaseType.INCREASE
							|| command == UpDownType.UP) {
						adjAmt = .5f;
					} else {
						adjAmt = -.5f;
					}
					float newTarget = adjAmt
							+ proxy.getState().getTargetTemperature();
					proxy.setTargetTemperature(newTarget);
					// send new value as update
					State newState = new DecimalType(newTarget);
					eventPublisher.postUpdate(itemName, newState);
				} else {
					float temperature = Float.parseFloat(command.toString());
					proxy.setTargetTemperature(temperature);
				}
			} else if (type == BindingType.hold) {
				if (command instanceof OnOffType) {
					proxy.setHold(command == OnOffType.ON);
				}
			}
		} catch (IOException e) {
			logger.error(
					"Could not communicate with device at host "
							+ proxy.getHost(), e);
		} catch (Throwable t) {
			logger.error("Error processing command '" + command
					+ "' for item '" + itemName + "'", t);
		}
	}

	private RadioThermostatBindingConfig getConfigForItemName(String itemName) {
		for (RadioThermostatBindingProvider provider : this.providers) {
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
		logger.debug(BINDING_NAME + " updated");
		try {
			// Process device configuration
			if (config != null) {
				// process refresh interval
				String refreshIntervalString = (String) config.get("refresh");
				if (StringUtils.isNotBlank(refreshIntervalString)) {
					refreshInterval = Long.parseLong(refreshIntervalString);
				}
				// parse all configured receivers
				// ( radiothermostat:<uid>.host=10.0.0.2 )
				Enumeration<String> keys = config.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (key.endsWith(CONFIG_KEY_HOST)) {
						// parse host
						String host = (String) config.get(key);
						int separatorIdx = key.indexOf('.');
						// no uid => one device => use default UID
						String uid = separatorIdx == -1 ? DEFAULT_DEVICE_UID
								: key.substring(0, separatorIdx);
						// proxy is stateless. keep them in a map in the
						// binding.
						proxies.put(uid, new RadioThermostatProxy(host));
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
		logger.debug(BINDING_NAME + " activated");
	}

	@Override
	public void deactivate() {
		logger.debug(BINDING_NAME + " deactivated");
	}

}
