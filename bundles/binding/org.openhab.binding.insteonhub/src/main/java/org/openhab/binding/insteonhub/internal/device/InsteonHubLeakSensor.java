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
import org.openhab.binding.insteonhub.internal.update.InsteonHubRecStdUpdate;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;

/**
 * Handles leak sensor broadcast events. Insteon Sensors are broadcast only, so
 * device status requests will not be attempted.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubLeakSensor extends InsteonHubDevice {

	@Override
	public void processOpenhabCommand(Command command) {
		// Read-Only
	}

	@Override
	public void processInsteonUpdate(InsteonHubUpdate msg) {
		if (msg.getCode() == InsteonHubMsgConst.REC_CODE_INSTEON_STD_MSG) {
			InsteonHubRecStdUpdate stdMsg = (InsteonHubRecStdUpdate) msg;
			switch (stdMsg.getCmd1()) {
			case InsteonHubMsgConst.CMD1_ON:
				if (stdMsg.getCmd2() == 0x02) {
					// on
					postOpenhabUpdate(OnOffType.ON);
				} else if (stdMsg.getCmd2() == 0x01) {
					// off
					postOpenhabUpdate(OnOffType.OFF);
				}
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
