/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.command;

import org.openhab.binding.insteonhub.internal.proxy.InsteonHubMsgConst;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubProxy;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;

/**
 * A command that an {@link InsteonHubProxy} can serialize and send to the
 * Insteon Hub over the wire.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubCommand {

	private static final byte DEFAULT_FLAG = 0x0F;

	private final int deviceId;
	private final InsteonHubCommandType commandType;

	public InsteonHubCommand(int deviceId, InsteonHubCommandType commandType) {
		this.deviceId = deviceId;
		this.commandType = commandType;
	}

	public int getDeviceId() {
		return deviceId;
	}

	public InsteonHubCommandType getCommandType() {
		return commandType;
	}

	public byte[] buildSerialMessage() {
		byte[] msgBuffer = new byte[8];
		// populate message
		msgBuffer[0] = InsteonHubMsgConst.STX;
		msgBuffer[1] = InsteonHubMsgConst.SND_CODE_SEND_INSTEON_STD_OR_EXT_MSG;
		InsteonHubByteUtil.writeDeviceId(deviceId, msgBuffer, 2);
		msgBuffer[5] = DEFAULT_FLAG;
		msgBuffer[6] = (byte) commandType.getByteValue();
		msgBuffer[7] = getCmd2();
		return msgBuffer;
	}

	protected byte getCmd2() {
		return 0;
	}
}
