package org.openhab.binding.radiothermostat.internal.hardware;

public enum RadioThermostatFanMode {
	AUTO("0"), AUTO_CIRCULATE("1"), ON("2");

	private final String val;

	private RadioThermostatFanMode(String val) {
		this.val = val;
	}

	public static RadioThermostatFanMode parse(String val) {
		for (RadioThermostatFanMode mode : RadioThermostatFanMode.values()) {
			if (mode.val.equals(val)) {
				return mode;
			}
		}
		return null;
	}

	public String getVal() {
		return val;
	}
}
