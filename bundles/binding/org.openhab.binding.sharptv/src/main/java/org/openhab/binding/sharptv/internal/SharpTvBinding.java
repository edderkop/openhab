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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.sharptv.SharpTvBindingProvider;
import org.openhab.binding.sharptv.internal.SharpTvBindingConfig.BindingType;
import org.openhab.binding.sharptv.internal.hardware.SharpTvProxy;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sharp TV binding. Handles all commands and polls configured devices to
 * process updates.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class SharpTvBinding extends
		AbstractActiveBinding<SharpTvBindingProvider> implements ManagedService {

	public static final String CONFIG_KEY_HOST = "host";
	public static final String CONFIG_KEY_PORT = "port";
	public static final String CONFIG_KEY_USER = "user";
	public static final String CONFIG_KEY_PASS = "pass";
	public static final String DEFAULT_DEVICE_UID = "default";

	private static final int MAX_VOLUME = 60;

	private static final Logger logger = LoggerFactory
			.getLogger(SharpTvBinding.class);

	// Map of proxies. key=deviceUid, value=proxy
	// Used to keep track of proxies
	private final Map<String, SharpTvProxy> proxies = new HashMap<String, SharpTvProxy>();

	@Override
	protected long getRefreshInterval() {
		// Sharp TVs do not support retrieval of state
		return 0;
	}

	@Override
	protected String getName() {
		return "SharpTv Refresh Service";
	}

	@Override
	protected void execute() {
		// Sharp TVs do not support retrieval of state
	}

	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		SharpTvBindingConfig config = getConfigForItemName(itemName);
		if (config == null) {
			logger.error("Received command for unknown item '" + itemName + "'");
			return;
		}
		SharpTvProxy proxy = proxies.get(config.getDeviceUid());
		if (proxy == null) {
			logger.error("Received command for unknown device uid '"
					+ config.getDeviceUid() + "'");
			return;
		}

		try {
			BindingType type = config.getBindingType();
			if (type == BindingType.audioSelectionToggle) {
				proxy.toggleAudioSelection();
			} else if (type == BindingType.avMode) {
				int value = Integer.parseInt(command.toString());
				proxy.setAvMode(value);
			} else if (type == BindingType.channelAnalog) {
				int channel = Integer.parseInt(command.toString());
				proxy.setChannelAnalog(channel);
			} else if (type == BindingType.channelDigitalAir) {
				int channel = Integer.parseInt(command.toString());
				proxy.setChannelDigitalAir(channel);
			} else if (type == BindingType.channelDigitalCableOnePart) {
				int channel = Integer.parseInt(command.toString());
				proxy.setChannelDigitalCableOnePart(channel);
			} else if (type == BindingType.channelDigitalCableTwoPart) {
				String channel = command.toString();
				String[] split = channel.split(".");
				if (split.length == 1) {
					proxy.setChannelDigitalCableTwoPart(
							Integer.parseInt(channel), 0);
				} else {
					proxy.setChannelDigitalCableTwoPart(
							Integer.parseInt(split[0]),
							Integer.parseInt(split[1]));
				}
			} else if (type == BindingType.channelUpDown) {
				if (command == IncreaseDecreaseType.INCREASE
						|| command == UpDownType.UP) {
					proxy.channelUp();
				} else if (command == IncreaseDecreaseType.DECREASE
						|| command == UpDownType.DOWN) {
					proxy.channelDown();
				}
			} else if (type == BindingType.closedCaptionToggle) {
				proxy.toggleClosedCaption();
			} else if (type == BindingType.input) {
				int input = Integer.parseInt(command.toString());
				proxy.setInput(input);
			} else if (type == BindingType.inputToggle) {
				proxy.toggleInput();
			} else if (type == BindingType.mute) {
				if (command instanceof OnOffType) {
					boolean mute = command == OnOffType.ON;
					proxy.setMute(mute);
				}
			} else if (type == BindingType.muteToggle) {
				proxy.toggleMute();
			} else if (type == BindingType.power) {
				if (command instanceof OnOffType) {
					boolean power = command == OnOffType.ON;
					proxy.setPower(power);
				}
			} else if (type == BindingType.remoteButton) {
				int button = Integer.parseInt(command.toString());
				proxy.pressRemoteButton(button);
			} else if (type == BindingType.sendCommand) {
				proxy.sendCommands(command.toString());
			} else if (type == BindingType.sleepTimer) {
				int timerMode = Integer.parseInt(command.toString());
				proxy.setSleepTimer(timerMode);
			} else if (type == BindingType.surround) {
				int surround = Integer.parseInt(command.toString());
				proxy.setSurround(surround);
			} else if (type == BindingType.threeD) {
				int mode = Integer.parseInt(command.toString());
				proxy.set3DMode(mode);
			} else if (type == BindingType.viewMode) {
				int mode = Integer.parseInt(command.toString());
				proxy.setViewMode(mode);
			} else if (type == BindingType.volume) {
				if (command instanceof DecimalType
						|| command instanceof StringType) {
					int volume = Integer.parseInt(command.toString());
					proxy.setVolume(volume);
				} else if (command instanceof PercentType) {
					float percent = .01f * ((PercentType) command).byteValue();
					proxy.setVolume((int) (MAX_VOLUME * percent));
				} else if (command instanceof IncreaseDecreaseType
						|| command instanceof UpDownType) {
					if (command == IncreaseDecreaseType.INCREASE
							|| command == UpDownType.UP) {
						proxy.volumeUp();
					} else if (command == IncreaseDecreaseType.DECREASE
							|| command == UpDownType.DOWN) {
						proxy.volumeDown();
					}
				}
			}
		} catch (IOException e) {
			logger.error("Could not communicate with Sharp TV", e);
		} catch (Throwable t) {
			logger.error("Error processing command '" + command
					+ "' for item '" + itemName + "'", t);
		}

		// Sharp TV has no way of retriving state from the device
		// To keep the GUI true to that fact, set item state to
		// UnDef after updating it.
		eventPublisher.postUpdate(itemName, UnDefType.UNDEF);
	}

	private SharpTvBindingConfig getConfigForItemName(String itemName) {
		for (SharpTvBindingProvider provider : this.providers) {
			if (provider.getItemConfig(itemName) != null) {
				return provider.getItemConfig(itemName);
			}
		}
		return null;
	}

	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		try {
			if (config != null) {
				// parse all configured receivers
				// ( sharptv:<uid>.host=10.0.0.2 )
				// ( sharptv:<uid>.port=12345 )
				// ( sharptv:<uid>.user=username )
				// ( sharptv:<uid>.pass=password )
				Enumeration<String> keys = config.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (key.endsWith(CONFIG_KEY_HOST)) {
						// found a host key. parse UID and associate with the rest
						// of the keys for this device
						String host = (String) config.get(key);
						int separatorIdx = key.indexOf('.');
						Integer port;
						String uid, user, pass;
						if (separatorIdx == -1) {
							// format: sharptv:host => use default UID
							uid = DEFAULT_DEVICE_UID;
							String portStr = (String) config.get(CONFIG_KEY_PORT);
							port = portStr != null ? Integer.parseInt(portStr)
									: null;
							user = (String) config.get(CONFIG_KEY_USER);
							pass = (String) config.get(CONFIG_KEY_PASS);
						} else {
							// format: sharptv:<uid>:host => parse UID from key
							uid = key.substring(0, separatorIdx);
							String keyPrefix = uid + ".";
							String portStr = (String) config.get(keyPrefix
									+ CONFIG_KEY_PORT);
							port = portStr != null ? Integer.parseInt(portStr)
									: null;
							user = (String) config.get(keyPrefix + CONFIG_KEY_USER);
							pass = (String) config.get(keyPrefix + CONFIG_KEY_PASS);
						}
						if (port == null || user == null || pass == null) {
							String passHidden = pass != null ? "(hidden)" : null;
							logger.error("Invalid configuration: host=" + host
									+ " port=" + port + " user=" + user + " pass="
									+ passHidden);
						} else {
							// proxy is stateless. keep them in a map in the
							// binding.
							proxies.put(uid, new SharpTvProxy(host, port, user,
									pass));
							setProperlyConfigured(true);
						}
					}
				}
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
