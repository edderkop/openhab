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
package org.openhab.binding.milight.internal;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.milight.MilightBindingProvider;
import org.openhab.binding.milight.internal.MilightBindingConfig.BindingType;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	

/**
 * This binding is able to do the following tasks with the Milight system:
 * <ul>
 * <li>Switching bulbs on and off.</li>
 * <li>Change color temperature of a bulb, what results in a white color.</li>
 * <li>Change the brightness of a bulb without changing the color.</li>
 * <li>Change the RGB values of a bulb.</li>
 * </ul>
 * 
 * @author Hans-Joerg Merk
 * @author Kai Kreuzer
 * @since 1.3.0
 */
public class MilightBinding extends AbstractBinding<MilightBindingProvider> implements ManagedService {

	private static final Logger logger = LoggerFactory.getLogger(MilightBinding.class);

	/** RegEx to validate a config <code>'^(.*?)\\.(host|port)$'</code> */
	private static final Pattern EXTRACT_CONFIG_PATTERN = Pattern.compile("^(.*?)\\.(host|port)$");

	private final static int DEFAULT_PORT = 50000;

	protected Map<String, DeviceConfig> deviceConfigs = new HashMap<String, DeviceConfig>();

    protected Map<String, String> bridgeIpConfig = new HashMap<String, String>();
    
    protected Map<String, Integer> bridgePortConfig = new HashMap<String, Integer>();
    
    protected Map<String, PercentType> dimmerState = new HashMap<String, PercentType>();

	public MilightBinding() {
	}

	public void activate() {
	}
	
	public void deactivate() {
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		super.internalReceiveCommand(itemName, command);
		
		MilightBindingConfig deviceConfig = getConfigForItemName(itemName);
		
		if (deviceConfig == null) {
			return;
		}

		try {
			int bulb = deviceConfig.getChannelNumber();
			String bridgeId = deviceConfig.getDeviceId();
			
			if (deviceConfig.getCommandType().equals(BindingType.brightness) ||
				deviceConfig.getCommandType().equals(BindingType.rgb)) {
				if (OnOffType.ON.equals(command)) {
					sendOn(bulb, bridgeId);
					if(deviceConfig.getCommandType().equals(BindingType.brightness)) {
						Thread.sleep(100);
						sendFull(bulb, bridgeId);
					}
				} else if (OnOffType.OFF.equals(command)) {
					sendOff(bulb, bridgeId);
				}
				if (IncreaseDecreaseType.INCREASE.equals(command)) {
					sendOn(bulb, bridgeId);
					Thread.sleep(100);
					PercentType newValue = sendIncrease(bulb, bridgeId);
					eventPublisher.postUpdate(itemName, newValue);
				} else if (IncreaseDecreaseType.DECREASE.equals(command)) {
					PercentType newValue = sendDecrease(bulb, bridgeId);
					eventPublisher.postUpdate(itemName, newValue);
				} else if (command instanceof PercentType) {
					sendPercent(bulb, bridgeId, (PercentType) command, BindingType.brightness);
				}
			}
			if (deviceConfig.getCommandType().equals(BindingType.nightMode)) {
				logger.debug("milight: item is of type nightMode");
				if (OnOffType.ON.equals(command)) {
					sendNightMode(bulb, bridgeId);
				}
				if (OnOffType.OFF.equals(command)) {
					sendOff(bulb, bridgeId);
				}

			}
			if (deviceConfig.getCommandType().equals(BindingType.colorTemperature)) {
				logger.debug("milight: item is of type warm/cold white");
				if (OnOffType.ON.equals(command)) {
					sendPercent(bulb, bridgeId, PercentType.HUNDRED, BindingType.colorTemperature);
				} else if (OnOffType.OFF.equals(command)) {
					sendPercent(bulb, bridgeId, PercentType.ZERO, BindingType.colorTemperature);
				} else if (IncreaseDecreaseType.INCREASE.equals(command)) {
					PercentType newValue = sendWarmer(bulb, bridgeId);
					eventPublisher.postUpdate(itemName, newValue);
				} else if (IncreaseDecreaseType.DECREASE.equals(command)) {
					PercentType newValue = sendCooler(bulb, bridgeId);
					eventPublisher.postUpdate(itemName, newValue);
				} else if (command instanceof PercentType) {
					sendPercent(bulb, bridgeId, (PercentType) command, BindingType.colorTemperature);
				}
			}
			if (deviceConfig.getCommandType().equals(BindingType.discoMode)) {
				logger.debug("milight: item is of type discoMode");
				if (IncreaseDecreaseType.INCREASE.equals(command)) {
					sendDiscoModeUp(bulb, bridgeId);
				} else if (IncreaseDecreaseType.DECREASE.equals(command)) {
					sendDiscoModeDown(bulb, bridgeId);
				} else if (command instanceof PercentType) {
					sendPercent(bulb, bridgeId, (PercentType) command, BindingType.discoMode);
				}
			}
			if (deviceConfig.getCommandType().equals(BindingType.discoSpeed)) {
				logger.debug("milight: item is of type discoSpeed");
				if (IncreaseDecreaseType.INCREASE.equals(command)) {
					sendIncreaseSpeed(bulb, bridgeId);
				} else if (IncreaseDecreaseType.DECREASE.equals(command)) {
					sendDecreaseSpeed(bulb, bridgeId);
				} else if (command instanceof PercentType) {
					sendPercent(bulb, bridgeId, (PercentType) command, BindingType.colorTemperature);
				}
			}
			if (deviceConfig.getCommandType().equals(BindingType.rgb)) {
				if (command instanceof HSBType) {
					sendColor(command, bridgeId);
				}
	        }
		} catch (Exception e) {
	            logger.error("milight: Failed to send {} command ", deviceConfig.getCommandType(), e);
	        }
	}

	private void sendPercent(int bulb, String bridgeId, PercentType command, MilightBindingConfig.BindingType type) {
		if(BindingType.brightness.equals(type) && command.equals(PercentType.ZERO)) {
			sendOff(bulb, bridgeId);
			return;
		}
		if(BindingType.brightness.equals(type) && command.equals(PercentType.HUNDRED)) {
			sendFull(bulb, bridgeId);
			return;
		}
		PercentType oldPercent = getCurrentState(bulb, bridgeId, type);
		if(oldPercent.equals(PercentType.ZERO)) sendOn(bulb, bridgeId);
		try {
			if (command.compareTo(oldPercent) > 0) {
				int repeatCount = (command.intValue() - oldPercent.intValue()) / 10;
				for(int i = 0; i <= repeatCount; i++) {
					Thread.sleep(100);
					if(BindingType.brightness.equals(type)) {
						sendIncrease(bulb, bridgeId);
					} else if(BindingType.colorTemperature.equals(type)) {
						sendWarmer(bulb, bridgeId);
					} else if(BindingType.discoSpeed.equals(type)) {
						sendIncreaseSpeed(bulb, bridgeId);
					} else if(BindingType.discoMode.equals(type)) {
						sendDiscoModeUp(bulb, bridgeId);
					}
				}
			} else if (command.compareTo(oldPercent) < 0) {
				int repeatCount = (oldPercent.intValue() - command.intValue()) / 10;
				for(int i = 0; i <= repeatCount; i++) {
					Thread.sleep(100);
					if(BindingType.brightness.equals(type)) {
						sendDecrease(bulb, bridgeId);
					} else if(BindingType.colorTemperature.equals(type)) {
						sendCooler(bulb, bridgeId);
					} else if(BindingType.discoSpeed.equals(type)) {
						sendDecreaseSpeed(bulb, bridgeId);
					} else if(BindingType.discoMode.equals(type)) {
						sendDiscoModeDown(bulb, bridgeId);
					}
				}
			}
			// store dimmerValue
			setCurrentState(bulb, bridgeId, command, type);
		} catch(InterruptedException e) {
			logger.debug("Sleeping thread has been interrupted.");
		}
	}

	private PercentType getCurrentState(int bulb, String bridgeId, BindingType type) {
		PercentType percent = dimmerState.get(bridgeId + bulb + type);
		if(percent == null) {
			percent = PercentType.ZERO;
		}
		return percent;
	}

	private void setCurrentState(int bulb, String bridgeId, PercentType command, BindingType type) {
		dimmerState.put(bridgeId + bulb + type, command);
	}

	private PercentType sendIncrease(int bulb, String bridgeId) {
		String messageBytes = null;
		switch (bulb) {
		// increase brightness of white bulbs
		case 0 :
		case 1 :
		case 2 :
		case 3 :
		case 4 :
			messageBytes  = "3C:00:55";
			break;
		// increase brightness of rgb bulbs
		case 5 :
			messageBytes = "23:00:55";
			break;
		}
		int currentPercent = getCurrentState(bulb, bridgeId, BindingType.brightness).intValue();
		if(currentPercent==0) {
			try {
				sendOn(bulb, bridgeId);
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		int newPercent = currentPercent + 10;
		if(newPercent > 100) {
			newPercent = 100;
		}
		PercentType newValue = new PercentType(newPercent);
		sendMessage(messageBytes, bridgeId);
		setCurrentState(bulb, bridgeId, newValue, BindingType.brightness);
		return newValue;
	}

	private PercentType sendDecrease(int bulb, String bridgeId) {
		String messageBytes = null;
		switch (bulb) {
		// decrease brightness of white bulbs
		case 0 :
		case 1 :
		case 2 :
		case 3 :
		case 4 :
			messageBytes  = "34:00:55";
			break;
			// decrease brightness of rgb bulbs
		case 5 :
			messageBytes = "24:00:55";
			break;
		}
		int newPercent = getCurrentState(bulb, bridgeId, BindingType.brightness).intValue() - 10;
		if(newPercent < 0) {
			newPercent = 0;
		}
		PercentType newValue = new PercentType(newPercent);
		if(newValue.equals(PercentType.ZERO)) {
			sendOff(bulb, bridgeId);
		} else {
			sendMessage(messageBytes, bridgeId);
		}
		setCurrentState(bulb, bridgeId, newValue, BindingType.brightness);
		return newValue;
	}

	private PercentType sendWarmer(int bulb, String bridgeId) {
		int newPercent = getCurrentState(bulb, bridgeId, BindingType.brightness).intValue() + 10;
		if(newPercent > 100) {
			newPercent = 100;
		}
		PercentType newValue = new PercentType(newPercent);
		String messageBytes = "3E:00:55";
		sendMessage(messageBytes, bridgeId);
		setCurrentState(bulb, bridgeId, newValue, BindingType.brightness);
		return newValue;
	}

	private PercentType sendCooler(int bulb, String bridgeId) {
		int newPercent = getCurrentState(bulb, bridgeId, BindingType.brightness).intValue() - 10;
		if(newPercent < 0) {
			newPercent = 0;
		}
		PercentType newValue = new PercentType(newPercent);
		String messageBytes = "3F:00:55";
		sendMessage(messageBytes, bridgeId);
		setCurrentState(bulb, bridgeId, newValue, BindingType.brightness);
		return newValue;
	}

	private void sendDiscoModeUp(int bulb, String bridgeId) {
		String messageBytes = "27:00:55";
		sendMessage(messageBytes, bridgeId);
	}

	private void sendDiscoModeDown(int bulb, String bridgeId) {
		String messageBytes = "28:00:55";
		sendMessage(messageBytes, bridgeId);
	}

	private void sendIncreaseSpeed(int bulb, String bridgeId) {
		String messageBytes = "25:00:55";
		sendMessage(messageBytes, bridgeId);
	}

	private void sendDecreaseSpeed(int bulb, String bridgeId) {
		String messageBytes = "26:00:55";
		sendMessage(messageBytes, bridgeId);
	}

	private void sendNightMode(int bulb, String bridgeId) {
		String messageBytes = null;
		switch (bulb) {
		case 0 :
			// message nightMode all white bulbs
			messageBytes = "B9:00:55";
			break;
		case 1 :
			// message nightMode white bulb channel 1
			messageBytes = "BB:00:55";
			break;
		case 2 :
			// message nightMode white bulb channel 2
			messageBytes = "B3:00:55";
			break;
		case 3 :
			// message nightMode white bulb channel 3
			messageBytes = "BA:00:55";
			break;
		case 4 :
			// message nightMode white bulb channel 4
			messageBytes = "B6:00:55";
			break;
		}
		sendMessage(messageBytes, bridgeId);
	}

	private void sendFull(int bulb, String bridgeId) {
		String messageBytes = null;
		switch (bulb) {
		case 0 :
			// message fullBright all white bulbs
			messageBytes = "B5:00:55";
			break;
		case 1 :
			// message fullBright white bulb channel 1
			messageBytes = "B8:00:55";
			break;
		case 2 :
			// message fullBright white bulb channel 2
			messageBytes = "BD:00:55";
			break;
		case 3 :
			// message fullBright white bulb channel 3
			messageBytes = "B7:00:55";
			break;
		case 4 :
			// message fullBright white bulb channel 4
			messageBytes = "B2:00:55";
			break;
		}
		sendMessage(messageBytes, bridgeId);
		setCurrentState(bulb, bridgeId, PercentType.HUNDRED, BindingType.brightness);
	}

	private void sendOn(int bulb, String bridgeId) {
		String messageBytes = null;
		switch (bulb) {
		case 0 :
			// message all white bulbs ON
			messageBytes = "35:00:55";
			break;
		case 1 :
			// message white bulb channel 1 ON
			messageBytes = "38:00:55";
			break;
		case 2 :
			// message white bulb channel 2 ON
			messageBytes = "3D:00:55";
			break;
		case 3 :
			// message white bulb channel 3 ON
			messageBytes = "37:00:55";
			break;
		case 4 :
			// message white bulb channel 4 ON
			messageBytes = "32:00:55";
			break;
		case 5 :
			// message rgb bulbs ON
			messageBytes = "22:00:55";
			break;
		}
		sendMessage(messageBytes, bridgeId);
	}

	private void sendOff(int bulb, String bridgeId) {
		String messageBytes = null;
		switch (bulb) {
		case 0 :
			// message all white bulbs OFF
			messageBytes = "39:00:55";
			break;
		case 1 :
			// message white bulb channel 1 OFF
			messageBytes = "3B:00:55";
			break;
		case 2 :
			// message white bulb channel 2 OFF
			messageBytes = "33:00:55";
			break;
		case 3 :
			// message white bulb channel 3 OFF
			messageBytes = "3A:00:55";
			break;
		case 4 :
			// message white bulb channel 4 OFF
			messageBytes = "36:00:55";
			break;
		case 5 :
			// message rgb bulbs OFF
			messageBytes = "21:00:55";
			break;
			}
		sendMessage(messageBytes, bridgeId);
		setCurrentState(bulb, bridgeId, PercentType.ZERO, BindingType.brightness);
	}
	
	private void sendColor(Command command, String bridgeId) {
		HSBType hsbCommand = (HSBType) command;
		DecimalType hue = hsbCommand.getHue();
		
		// we have to map [0,360] to [0,0xFF], where red equals hue=0 and the milight color 0xB0 (=176)
		Integer milightColorNo = (256 + 176 - (int) (hue.floatValue() / 360.0 * 255.0)) % 256;
		String messageBytes = "20:" + Integer.toHexString(milightColorNo) + ":55";
		
		sendMessage(messageBytes, bridgeId);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveUpdate() is called!");
	}

	protected void sendMessage(String messageBytes, String bridgeId) {
		
		String bridgeIp = bridgeIpConfig.get(bridgeId);
		Integer bridgePort = bridgePortConfig.get(bridgeId);
		
		if (bridgePort == null) {
			bridgePort = DEFAULT_PORT;
		}
		
		try {
			byte[] buffer = getMessageBytes(messageBytes);
	        
			InetAddress IPAddress = InetAddress.getByName(bridgeIp);
	        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IPAddress, bridgePort);
	        DatagramSocket datagramSocket = new DatagramSocket();
	        datagramSocket.send(packet);
	        datagramSocket.close();
	        logger.debug("Sent packet '{}' to bridge '{}' ({}:{})", new Object[] { messageBytes, bridgeId, bridgeIp, bridgePort });
        }
        catch (Exception e) {
            logger.error("Failed to send Message to '{}': ", new Object[] { bridgeIp, e.getMessage()});
        }
	}
	

	private byte[] getMessageBytes(String messageBytes) {
		if (messageBytes == null) {
			logger.error("messageBytes must not be null");
		}
    	
        byte[] buffer = new byte[3];
        String[] hex = messageBytes.split("(\\:|\\-)");
        
    	int hexIndex = 0;
        for (hexIndex = 0; hexIndex < 3; hexIndex++) {
            buffer[hexIndex] = (byte) Integer.parseInt(hex[hexIndex], 16);
        }
        return buffer;
    }
		
	/**
	* Lookup of the configuration of the named item.
	* 
 	* @param itemName
 	*            The name of the item.
 	* @return The configuration, null otherwise.
 	*/
	private MilightBindingConfig getConfigForItemName(String itemName) {
		for (MilightBindingProvider provider : this.providers) {
			if (provider.getItemConfig(itemName) != null) {
				return provider.getItemConfig(itemName);
			}
		}
		return null;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		if (config != null) {
			Enumeration<String> keys = config.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();

				// the config-key enumeration contains additional keys that we
				// don't want to process here ...
				if ("service.pid".equals(key)) {
					continue;
				}

				Matcher matcher = EXTRACT_CONFIG_PATTERN.matcher(key);

				if (!matcher.matches()) {
					logger.debug("given config key '" + key
						+ "' does not follow the expected pattern '<id>.<host|port>'");
					continue;
				}

				matcher.reset();
				matcher.find();

				String deviceId = matcher.group(1);

				DeviceConfig deviceConfig = deviceConfigs.get(deviceId);

				if (deviceConfig == null) {
					deviceConfig = new DeviceConfig(deviceId);
					deviceConfigs.put(deviceId, deviceConfig);
				}

				String configKey = matcher.group(2);
				String value = (String) config.get(key);

				if ("host".equals(configKey)) {
					deviceConfig.host = value;
					bridgeIpConfig.put(deviceId, value);
				} else if ("port".equals(configKey)) {
					deviceConfig.port = Integer.valueOf(value);
					bridgePortConfig.put(deviceId, Integer.valueOf(value));
				} else {
					throw new ConfigurationException(configKey, "the given configKey '" + configKey + "' is unknown");
				}
			}
		}
	}

	/**
	 * Internal data structure which carries the connection details of one
	 * device (there could be several)
	 */
	static class DeviceConfig {
		
		String host;
		int port = DEFAULT_PORT;
		
		String deviceId;

		public DeviceConfig(String deviceId) {
			this.deviceId = deviceId;
		}

		public String getHost(){
			return host;
		}
		
		public int getPort(){
			return port;
		}
		
		@Override
		public String toString() {
			return "Device [id=" + deviceId + ", host=" + host + ", port=" + port + "]";
		}
	}
}