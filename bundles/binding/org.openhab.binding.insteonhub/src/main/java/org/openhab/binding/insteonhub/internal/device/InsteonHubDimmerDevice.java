/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.device;

import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.command.InsteonHubCommandType;
import org.openhab.binding.insteonhub.internal.command.InsteonHubOnCommand;
import org.openhab.binding.insteonhub.internal.command.InsteonHubStartDimBrtCommand;
import org.openhab.binding.insteonhub.internal.command.InsteonHubStartDimBrtCommand.InsteonHubStartDimBrtType;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubMsgConst;
import org.openhab.binding.insteonhub.internal.update.InsteonHubRecStdUpdate;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;

/**
 * A dimmer device. It responds to value checks by returning an analog value
 * 0-255, and supports {@link OnOffType}, {@link IncreaseDecreaseType},
 * {@link UpDownType}, {@link StopMoveType}, and {@link PercentType} commands.
 * Additionally, any {@link Command} who's toString method represents an integer
 * from 0-100 is also supported.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubDimmerDevice extends InsteonHubDevice {

	private static final float PCT_TO_LVL_MULTIPLIER = (255f / 100f);

	@Override
	protected void processOpenhabCommand(Command command) {
		if (command instanceof OnOffType) {
			setPower(command == OnOffType.ON);
		} else if (command instanceof UpDownType) {
			// Up/Down => Start Brighten/Dim
			if (command == UpDownType.UP) {
				startRampUp();
			} else {
				startRampDown();
			}
		} else if (command instanceof StopMoveType) {
			// Stop => Stop Brighten/Dim
			stopRamp();
		} else if (command instanceof IncreaseDecreaseType) {
			// Increase/Decrease => Incremental Brighten/Dim
			// Will not be called => taken care of by InsteonHubRampScheduler
			// which will map it to UpDownType and StopMoveType
		} else if (command instanceof PercentType) {
			// set level from 0 to 100 percent value
			setPercent(((PercentType) command).intValue());
		} else {
			// try to parse string as a precent value
			try {
				setPercent(Integer.parseInt(command.toString()));
			} catch (NumberFormatException e) {
				// bad command: ignore
			}
		}
	}

	private void setPower(boolean power) {
		if (power) {
			setPercent(100);
		} else {
			setPercent(0);
		}
	}

	private void setPercent(int percent) {
		int level = Math.round(percent * PCT_TO_LVL_MULTIPLIER);
		if (level == 0) {
			sendInsteonCommand(new InsteonHubCommand(getDeviceId(),
					InsteonHubCommandType.OFF));
		} else {
			sendInsteonCommand(new InsteonHubOnCommand(getDeviceId(), level));
		}
	}

	private void startRampUp() {
		sendInsteonCommand(new InsteonHubStartDimBrtCommand(getDeviceId(),
				InsteonHubStartDimBrtType.BRT));
	}

	private void startRampDown() {
		sendInsteonCommand(new InsteonHubStartDimBrtCommand(
				getDeviceId(), InsteonHubStartDimBrtType.DIM));
	}

	private void stopRamp() {
		sendInsteonCommand(new InsteonHubCommand(
				getDeviceId(), InsteonHubCommandType.STOP_DIM_BRT));
	}

	@Override
	protected void processInsteonUpdate(InsteonHubUpdate msg) {
		if (msg.getCode() == InsteonHubMsgConst.REC_CODE_INSTEON_STD_MSG) {
			InsteonHubRecStdUpdate stdMsg = (InsteonHubRecStdUpdate) msg;
			if(stdMsg.getFlags().isAck()) {
				// level response
				int ubyte = stdMsg.getCmd2();
				if (ubyte == 0) {
					postOpenhabUpdate(OnOffType.OFF);
				} else if (ubyte >= 254) {
					// yes, 254 is on purpose.
					// sometimes it ends up 254 when you press the switch
					postOpenhabUpdate(OnOffType.ON);
				} else {
					int percent = (int)(100 * (stdMsg.getCmd2() / 255f));
					postOpenhabUpdate(new PercentType(percent));
				}
			} else {
				// not a level response
				switch (stdMsg.getCmd1()) {
				case InsteonHubMsgConst.CMD1_STOP_DIM_BRT:
				case InsteonHubMsgConst.CMD1_BRT:
				case InsteonHubMsgConst.CMD1_DIM:
					// arbitrary: request actual value
					requestFromHub();
					break;
				case InsteonHubMsgConst.CMD1_OFF:
				case InsteonHubMsgConst.CMD1_OFF_FAST:
					// off
					postOpenhabUpdate(OnOffType.OFF);
					break;
				case InsteonHubMsgConst.CMD1_ON:
				case InsteonHubMsgConst.CMD1_ON_FAST:
					// level
					int percent = (int)(100 * (stdMsg.getCmd2() / 255f));
					postOpenhabUpdate(new PercentType(percent));
				default:
					break;
				}
			}
		}
	}

	@Override
	public void requestFromHub() {
		sendInsteonCommand(new InsteonHubCommand(
				getDeviceId(), InsteonHubCommandType.STATUS_REQUEST));
	}

}
