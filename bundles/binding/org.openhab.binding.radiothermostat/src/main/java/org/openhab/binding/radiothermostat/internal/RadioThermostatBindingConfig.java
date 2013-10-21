package org.openhab.binding.radiothermostat.internal;

import org.openhab.core.binding.BindingConfig;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * Radio Thermostat item binding configuration
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class RadioThermostatBindingConfig implements BindingConfig {

	public static final String KEY_BINDING_TYPE = "bindingType";
	public static final String KEY_DEVICE_UID = "uid";

	public enum BindingType {
		hvacMode, fanMode, targetTemperature, temperature, hold, fan;
	}

	private final String deviceUid;
	private final BindingType bindingType;
	private final String itemName;

	public RadioThermostatBindingConfig(String deviceUid, String bindingType,
			String itemName) throws BindingConfigParseException {
		this.deviceUid = deviceUid != null ? deviceUid
				: RadioThermostatBinding.DEFAULT_DEVICE_UID;
		this.bindingType = parseBindingType(bindingType);
		this.itemName = itemName;
	}

	private static BindingType parseBindingType(String str)
			throws BindingConfigParseException {
		if (str == null) {
			throw new BindingConfigParseException(KEY_BINDING_TYPE);
		}
		try {
			return BindingType.valueOf(str);
		} catch (Exception e) {
			throw new BindingConfigParseException("error parsing "
					+ KEY_BINDING_TYPE);
		}
	}

	public String getDeviceUid() {
		return deviceUid;
	}

	public BindingType getBindingType() {
		return bindingType;
	}

	public String getItemName() {
		return itemName;
	}
}
