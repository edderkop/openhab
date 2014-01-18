/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.device;

import org.openhab.binding.insteonhub.internal.InsteonHubBindingConfig;
import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;
import org.openhab.binding.insteonhub.internal.bus.InsteonHubBusListener;
import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A device is responsible for parsing OpenHab commands and sending them to the
 * Insteon Hub, as well as receiving messages from the Insteon Hub and
 * forwarding them to the OpenHab bus.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public abstract class InsteonHubDevice {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private InsteonHubBus bus;
	private int deviceId;
	private InsteonHubBindingConfig config;
	
	public final void destroy() {
		bus.removeListener(busListener);
	}
	
	public final void setConfig(InsteonHubBindingConfig config) {
		this.config = config;
	}
	
	public final InsteonHubBindingConfig getConfig() {
		return config;
	}
	
	public final void setDeviceId(int deviceId) {
		this.deviceId = deviceId;
	}

	public final void setBus(InsteonHubBus bus) {
		this.bus = bus;
		bus.addListener(busListener);
	}

	protected final int getDeviceId() {
		return deviceId;
	}

	protected final void postOpenhabUpdate(State update) {
		bus.postOpenhabUpdate(deviceId, update);
	}
	
	protected final void sendInsteonCommand(InsteonHubCommand command) {
		bus.sendInsteonCommand(deviceId, command);
	}
	
	private final InsteonHubBusListener busListener = new InsteonHubBusListener() {

		@Override
		public void onSendOpenhabCommand(int deviceId, Command command) {
			if(InsteonHubDevice.this.deviceId == deviceId) {
				if(logger.isDebugEnabled()) {
					logger.debug("Processing " + InsteonHubByteUtil.deviceIdIntToString(deviceId) + " = " + command);
				}
				processOpenhabCommand(command);
			}
		}

		@Override
		public void onPostOpenhabUpdate(int deviceId, State update) {
			// device's are responsible for posting this information
		}

		@Override
		public void onSendInsteonCommand(int deviceId, InsteonHubCommand command) {
			// device's are responsible for posting this information
		}

		@Override
		public void onPostInsteonUpdate(int deviceId, InsteonHubUpdate update) {
			if(InsteonHubDevice.this.deviceId == deviceId) {
				if(logger.isDebugEnabled()) {
					logger.debug("Processing " + InsteonHubByteUtil.deviceIdIntToString(deviceId) + " = " + update);
				}
				processInsteonUpdate(update);
			}
		}
		
	};

	protected abstract void processOpenhabCommand(Command command);

	protected abstract void processInsteonUpdate(InsteonHubUpdate msg);

	public abstract void requestFromHub();
}
