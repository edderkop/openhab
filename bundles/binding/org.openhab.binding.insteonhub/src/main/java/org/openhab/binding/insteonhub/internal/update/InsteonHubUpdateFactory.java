/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.update;

import org.openhab.binding.insteonhub.internal.device.InsteonHubDeviceManager;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubMsgConst;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;

/**
 * Handles messages received from the insteon hub. Message are recieved as raw
 * byte buffers and are translated info an instance of {@link InsteonHubUpdate}
 * . Successfully parsed messages are then passed to the
 * {@link InsteonHubDeviceManager} where they are routed to the appropriate
 * device to be processed.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubUpdateFactory {

	public InsteonHubUpdate createUpdate(byte[] msg) {
		byte cmd = msg[1];

		if (cmd == InsteonHubMsgConst.REC_CODE_INSTEON_STD_MSG) {
			// INSTEON Standard Message
			// parse the message
			int deviceId = InsteonHubByteUtil.readDeviceId(msg, 2);
			InsteonHubStdMsgFlags flags = new InsteonHubStdMsgFlags(
					InsteonHubByteUtil.byteToUnsignedInt(msg[8]));
			int cmd1 = msg[9] & 0xFF;
			int cmd2 = msg[10] & 0xFF;
			InsteonHubRecStdUpdate stdMsg = new InsteonHubRecStdUpdate(
					deviceId, flags, cmd1, cmd2);

			// return standard message
			return stdMsg;
		}
		
		return null;
	}
}
