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
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubMsgConst;
import org.openhab.binding.insteonhub.internal.update.InsteonHubRecStdUpdate;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;

/**
 * A switch device. It supports {@link OnOffType}. When the intended device
 * responds to status requests, non-zero values are treated as ON, while zero
 * values are treated as OFF.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubSwitchDevice extends InsteonHubDevice {

	@Override
	public void processOpenhabCommand(Command command) {
		if (command instanceof OnOffType) {
			setPower(command == OnOffType.ON);
		}
	}

	private void setPower(boolean power) {
		if (power) {
			sendInsteonCommand(new InsteonHubCommand(getDeviceId(),
					InsteonHubCommandType.FAST_ON));
		} else {
			sendInsteonCommand(new InsteonHubCommand(getDeviceId(),
					InsteonHubCommandType.FAST_ON));
		}
	}

	@Override
	public void processInsteonUpdate(InsteonHubUpdate msg) {
		if (msg.getCode() == InsteonHubMsgConst.REC_CODE_INSTEON_STD_MSG) {
			InsteonHubRecStdUpdate stdMsg = (InsteonHubRecStdUpdate) msg;
			if(stdMsg.getCmd1() == 0 && stdMsg.getFlags().isAck()) {
				// level response
				// FIXME is this right?
				int cmd2 = stdMsg.getCmd2();
				if (cmd2 == 0) {
					postOpenhabUpdate(OnOffType.OFF);
				} else {
					postOpenhabUpdate(OnOffType.ON);
				} 
			} else {
				switch (stdMsg.getCmd1()) {
				case InsteonHubMsgConst.CMD1_OFF:
				case InsteonHubMsgConst.CMD1_OFF_FAST:
					// off
					postOpenhabUpdate(OnOffType.OFF);
					break;
				case InsteonHubMsgConst.CMD1_ON:
				case InsteonHubMsgConst.CMD1_ON_FAST:
					// on
					postOpenhabUpdate(OnOffType.ON);
					break;
				case InsteonHubMsgConst.CMD1_LEVEL_RESPONSE:
					// parse value
					int ubyte = stdMsg.getCmd2();
					if (ubyte == 0) {
						postOpenhabUpdate(OnOffType.OFF);
					} else {
						// yes, 254 is on purpose.
						// sometimes it ends up 254 when you press the switch
						postOpenhabUpdate(OnOffType.ON);
					}
					break;
				default:
					break;
				}
			}
		}
	}

	@Override
	public void requestFromHub() {
		sendInsteonCommand(new InsteonHubCommand(getDeviceId(),
				InsteonHubCommandType.STATUS_REQUEST));
	}
}
