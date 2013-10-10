package org.openhab.binding.radiothermostat.internal.hardware;

public class RadioThermostatState {

	private final RadioThermostatHvacMode thermostatMode;
	private final RadioThermostatFanMode fanMode;
	private final float temperature;
	private final float targetTemperature;
	private final boolean hold;
	private final boolean fan;

	public RadioThermostatState(RadioThermostatHvacMode thermostatMode, RadioThermostatFanMode fanMode,
			float temperature, float targetTemperature, boolean hold, boolean fan) {
		this.thermostatMode = thermostatMode;
		this.fanMode = fanMode;
		this.temperature = temperature;
		this.targetTemperature = targetTemperature;
		this.hold = hold;
		this.fan = fan;
	}

	public RadioThermostatHvacMode getHvacMode() {
		return thermostatMode;
	}

	public RadioThermostatFanMode getFanMode() {
		return fanMode;
	}

	public float getTemperature() {
		return temperature;
	}

	public float getTargetTemperature() {
		return targetTemperature;
	}

	public boolean isHold() {
		return hold;
	}
	
	public boolean isFan() {
		return fan;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("thermostatMode=");
		sb.append(thermostatMode);
		sb.append(" fanMode=");
		sb.append(fanMode);
		sb.append(" temperature=");
		sb.append(temperature);
		sb.append(" targetTemperature=");
		sb.append(targetTemperature);
		sb.append(" hold=");
		sb.append(hold);
		sb.append(" fan=");
		sb.append(fan);
		return sb.toString();
	}
}
