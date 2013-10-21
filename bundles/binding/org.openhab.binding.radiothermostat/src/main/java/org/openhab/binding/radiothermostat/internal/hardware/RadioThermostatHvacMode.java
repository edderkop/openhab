package org.openhab.binding.radiothermostat.internal.hardware;

public enum RadioThermostatHvacMode {
	OFF("0"), HEAT("1"), COOL("2"), AUTO("3");
	
	private final String val;
	
	private RadioThermostatHvacMode(String val) {
		this.val = val;
	}
	
	public static RadioThermostatHvacMode parse(String val) {
		for(RadioThermostatHvacMode mode : RadioThermostatHvacMode.values()) {
			if(mode.val.equals(val)) {
				return mode;
			}
		}
		return null;
	}
	
	public String getVal() {
		return val;
	}
}
