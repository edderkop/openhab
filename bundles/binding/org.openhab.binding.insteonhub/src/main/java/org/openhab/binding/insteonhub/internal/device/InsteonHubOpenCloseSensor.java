/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.device;

import org.openhab.binding.insteonhub.internal.proxy.InsteonHubMsgConst;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.binding.insteonhub.internal.update.InsteonHubRecStdUpdate;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.Command;

/**
 * An OnOff Sensor is a generic sensor type that parses On/Off or OnFast/OffFast
 * events from the Insteon Hub. Off events mean OPEN, On events mean CLOSED.
 * Device status requests will be attempted, but may not be supported by the
 * intended hardware; In which case, status requests will not update the known
 * state.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubOpenCloseSensor extends InsteonHubDevice {

	@Override
	public void processOpenhabCommand(Command command) {
		// Read-Only
	}

	@Override
	public void processInsteonUpdate(InsteonHubUpdate msg) {
		if (msg.getCode() == InsteonHubMsgConst.REC_CODE_INSTEON_STD_MSG) {
			InsteonHubRecStdUpdate stdMsg = (InsteonHubRecStdUpdate) msg;
			switch (stdMsg.getCmd1()) {
			case InsteonHubMsgConst.CMD1_OFF:
			case InsteonHubMsgConst.CMD1_OFF_FAST:
				// off
				postOpenhabUpdate(OpenClosedType.CLOSED);
				break;
			case InsteonHubMsgConst.CMD1_ON:
			case InsteonHubMsgConst.CMD1_ON_FAST:
				// on
				postOpenhabUpdate(OpenClosedType.OPEN);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void requestFromHub() {
		// Insteon Sensors are broadcast-only
		// TODO The hub caches this value... Any way to get it?
	}
}
