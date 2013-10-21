package org.openhab.binding.yamahareceiver.internal.hardware;

public class YamahaReceiverState {
	
	private final boolean power;
	private final String input;
	private final String surroundProgram;
	private final float volume;
	private final boolean mute;
	
	public YamahaReceiverState(boolean power, String input, String surroundProgram, float volume, boolean mute) {
		this.power = power;
		this.input = input;
		this.surroundProgram = surroundProgram;
		this.volume = volume;
		this.mute = mute;
	}
	
	public boolean isPower() {
		return power;
	}
	
	public String getInput() {
		return input;
	}
	
	public String getSurroundProgram() {
		return surroundProgram;
	}
	
	public float getVolume() {
		return volume;
	}
	
	public boolean isMute() {
		return mute;
	}
}
