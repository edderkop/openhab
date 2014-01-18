/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal;

import java.util.Dictionary;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.insteonhub.InsteonHubBindingProvider;
import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;
import org.openhab.binding.insteonhub.internal.bus.InsteonHubBusListener;
import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDevice;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDeviceFactory;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDeviceManager;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubProxy;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubProxyFactory;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubProxyManager;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.IncreaseDecreaseType;
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

	private static final Logger logger = LoggerFactory
			.getLogger(InsteonHubBinding.class);

	public static final String DEFAULT_HUB_ID = "_default";
	private static final long DEFAULT_REFRESH_INTERVAL = 60000;
	private static final String BINDING_NAME = "InsteonHubBinding";

	private final InsteonHubDeviceFactory deviceFactory = new InsteonHubDeviceFactory();
	private final InsteonHubBus insteonHubBus = new InsteonHubBus();
	private final InsteonHubDeviceManager deviceManager = new InsteonHubDeviceManager(insteonHubBus);
	private final InsteonHubProxyManager proxyManager = new InsteonHubProxyManager(insteonHubBus);
	private final InsteonHubRampScheduler rampScheduler = new InsteonHubRampScheduler(insteonHubBus);

	private long refreshInterval = DEFAULT_REFRESH_INTERVAL;
	private volatile boolean activated;

	public InsteonHubBinding() {
		insteonHubBus.addListener(insteonHubBusListener);
	}
	
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
		logger.debug(BINDING_NAME + " execute");
		for (InsteonHubBindingProvider provider : this.providers) {
			for (InsteonHubBindingConfig config : provider.getConfigs()) {
				checkDevice(config.getDeviceId());
			}
		}
		deviceManager.requestDeviceStates();
		logger.debug(BINDING_NAME + " execute complete");
	}

	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		InsteonHubBindingConfig config = getConfigForItemName(itemName);
		if (config != null) {
			if (command instanceof IncreaseDecreaseType) {
				// Increase/Decrease => Incremental Brighten/Dim
				if (command == IncreaseDecreaseType.INCREASE) {
					rampScheduler.increase(config.getDeviceId());
				} else {
					rampScheduler.decrease(config.getDeviceId());
				}
			} else {
				insteonHubBus.sendOpenhabCommand(config.getDeviceId(), command);
			}
		} else {
			logger.error("Received command for unknown item '" + itemName + "'");
		}

		if (logger.isDebugEnabled()) {
			logger.debug(BINDING_NAME + " processing command '" + command
					+ "' of type '" + command.getClass().getSimpleName()
					+ "' for item '" + itemName + "'");
		}
	}

	private InsteonHubBindingConfig getConfigForItemName(String itemName) {
		for (InsteonHubBindingProvider provider : this.providers) {
			InsteonHubBindingConfig config;
			if ((config = provider.getItemConfig(itemName)) != null) {
				return config;
			}
		}
		return null;
	}
	
	private InsteonHubBindingConfig getConfigForDeviceId(int deviceId) {
		for (InsteonHubBindingProvider provider : this.providers) {
			InsteonHubBindingConfig config;
			if ((config = provider.getDeviceConfig(deviceId)) != null) {
				return config;
			}
		}
		return null;
	}
	
	private final InsteonHubBusListener insteonHubBusListener = new InsteonHubBusListener() {
		
		@Override
		public void onSendOpenhabCommand(int deviceId, Command command) {
			checkDevice(deviceId);
			if(logger.isDebugEnabled()) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				logger.debug("openHAB Command: " + deviceString + " = " + command);
			}
		}
		
		@Override
		public void onSendInsteonCommand(int deviceId, InsteonHubCommand command) {
			if(logger.isDebugEnabled()) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				byte[] buf = command.buildSerialMessage();
				String msg = InsteonHubByteUtil.bytesToHex(buf, 0, buf.length);
				logger.debug("InsteonHub Command: " + deviceString + " = " + msg);
			}
		}
		
		@Override
		public void onPostOpenhabUpdate(int deviceId, State update) {
			InsteonHubBindingConfig config = getConfigForDeviceId(deviceId);
			if(config != null) {
				eventPublisher.postUpdate(config.getItemName(), update);
			}
			if(logger.isDebugEnabled()) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				logger.debug("openHAB Update: " + deviceString + " = " + update);
			}
		}
		
		@Override
		public void onPostInsteonUpdate(int deviceId, InsteonHubUpdate update) {
			checkDevice(deviceId);
			if(logger.isDebugEnabled()) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				logger.debug("InsteonHub Update: " + deviceString + " = " + update);
			}
		}
		
	};
	
	private void checkDevice(int deviceId) {
		InsteonHubDevice device = deviceManager.getDevice(deviceId);
		InsteonHubBindingConfig config = getConfigForDeviceId(deviceId);
		if(config == null) {
			if(device != null) {
				// remove deleted device
				deviceManager.removeDevice(deviceId);
			}
		} else if(device == null || device.getConfig() != config) {
			// need to create or re-create device
			if(logger.isDebugEnabled()) {
				logger.debug("Creating InsteonHubDevice " + InsteonHubByteUtil.deviceIdIntToString(deviceId));
			}
			device = deviceFactory.createDevice(config.getDeviceId(), config.getBindingType());
			device.setConfig(config);
			deviceManager.addDevice(config.getDeviceId(), device);
		}
	}

	@Override
	public synchronized void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		logger.debug(BINDING_NAME + " updated");
		try {
			// Process device configuration
			if (config != null) {
				String refreshIntervalString = (String) config.get("refresh");
				if (StringUtils.isNotBlank(refreshIntervalString)) {
					refreshInterval = Long.parseLong(refreshIntervalString);
				}

				// Stop and forget all existing proxies
				proxyManager.stopProxies();
				proxyManager.clear();

				// Load new proxies
				Map<String, InsteonHubProxy> newProxies = InsteonHubProxyFactory
						.createInstances(config, insteonHubBus);
				for (Map.Entry<String, InsteonHubProxy> entry : newProxies
						.entrySet()) {
					String hubId = entry.getKey();
					InsteonHubProxy proxy = entry.getValue();
					proxyManager.addProxy(hubId, proxy);
				}

				// If activated, start proxies now
				if (activated) {
					proxyManager.startProxies();
				}

				// Set properly configured
				setProperlyConfigured(true);
			}
		} catch (Throwable t) {
			logger.error("Error configuring " + getName(), t);
			setProperlyConfigured(false);
		}
	}

	@Override
	public synchronized void activate() {
		activated = true;
		proxyManager.startProxies();
		insteonHubBus.start();
		rampScheduler.start();
		logger.debug(BINDING_NAME + " activated");
	}

	@Override
	public synchronized void deactivate() {
		activated = false;
		proxyManager.stopProxies();
		insteonHubBus.stop();
		rampScheduler.stop();
		logger.debug(BINDING_NAME + " deactivated");
	}

	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		if (logger.isTraceEnabled()) {
			logger.trace(BINDING_NAME + " received update for '" + itemName
					+ "' of type '" + newState.getClass().getSimpleName()
					+ "' with value '" + newState + "'");
		}
		// ignore
	}

}
