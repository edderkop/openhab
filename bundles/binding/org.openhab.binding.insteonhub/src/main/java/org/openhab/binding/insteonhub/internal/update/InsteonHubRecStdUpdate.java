/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.update;

import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;

/**
 * Received Standard Message type 0x50. Most Insteon Device status messages use
 * this event type.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubRecStdUpdate extends InsteonHubUpdate {

	private final InsteonHubStdMsgFlags flags;
	private final int cmd1;
	private final int cmd2;

	public InsteonHubRecStdUpdate(int deviceId, InsteonHubStdMsgFlags flags,
			int cmd1, int cmd2) {
		super(deviceId, 0x50);
		this.flags = flags;
		this.cmd1 = cmd1;
		this.cmd2 = cmd2;
	}

	public InsteonHubStdMsgFlags getFlags() {
		return flags;
	}

	public int getCmd1() {
		return cmd1;
	}

	public int getCmd2() {
		return cmd2;
	}

	@Override
	public String toString() {
		return "InsteonHubRecStdMessage [flags=" + flags + ", cmd1="
				+ InsteonHubByteUtil.byteToHex(cmd1) + ", cmd2="
				+ InsteonHubByteUtil.byteToHex(cmd2) + ", getDeviceId()="
				+ InsteonHubByteUtil.deviceIdIntToString(getDeviceId())
				+ ", getCode()=" + InsteonHubByteUtil.byteToHex(getCode())
				+ "]";
	}

}
