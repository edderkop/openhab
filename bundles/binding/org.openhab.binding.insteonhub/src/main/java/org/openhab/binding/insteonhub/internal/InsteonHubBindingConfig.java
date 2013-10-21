package org.openhab.binding.insteonhub.internal;

import org.openhab.core.binding.BindingConfig;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * Insteon Hub item binding configuration
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class InsteonHubBindingConfig implements BindingConfig {

	public static final String KEY_BINDING_TYPE = "bindingType";
	public static final String KEY_HUB_ID = "hubid";
	public static final String KEY_DEVICE = "device";
	
	public enum BindingType {
		digital, dimmer;
	}

	private final String hubId;
	private final String device;
	private final BindingType bindingType;
	private final String itemName;

	public InsteonHubBindingConfig(String hubId, String device, String bindingType,
			String itemName) throws BindingConfigParseException {
		this.hubId = hubId != null ? hubId
				: InsteonHubBinding.DEFAULT_HUB_ID;
		if(device == null) {
			throw new IllegalArgumentException("device cannot be null");
		}
		this.device = device;
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

	public String getHubId() {
		return hubId;
	}
	
	public String getDevice() {
		return device;
	}

	public BindingType getBindingType() {
		return bindingType;
	}

	public String getItemName() {
		return itemName;
	}
}
