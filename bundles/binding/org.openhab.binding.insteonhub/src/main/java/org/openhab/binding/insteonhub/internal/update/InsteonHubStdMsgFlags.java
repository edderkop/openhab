/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.update;

import org.openhab.binding.insteonhub.internal.proxy.InsteonHubMsgConst;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;

/**
 * Helper class that parses the {@link InsteonHubRecStdUpdate} 0x50's flags
 * bitset.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubStdMsgFlags {

	private final boolean ack;
	private final boolean broadcast;
	private final boolean extended;
	private final boolean group;

	public InsteonHubStdMsgFlags(int bits) {
		ack = InsteonHubByteUtil.checkBit(bits,
				InsteonHubMsgConst.STD_FLAG_BIT_ACK);
		broadcast = InsteonHubByteUtil.checkBit(bits,
				InsteonHubMsgConst.STD_FLAG_BIT_BC_OR_NAK);
		extended = InsteonHubByteUtil.checkBit(bits,
				InsteonHubMsgConst.STD_FLAG_BIT_EXT);
		group = InsteonHubByteUtil.checkBit(bits,
				InsteonHubMsgConst.STD_FLAG_BIT_GROUP);
	}

	public boolean isAck() {
		return ack;
	}

	public boolean isBroadcast() {
		return broadcast;
	}

	public boolean isExtended() {
		return extended;
	}

	public boolean isGroup() {
		return group;
	}

	@Override
	public String toString() {
		return "InsteonHubStdMsgFlags [ack=" + ack + ", broadcast=" + broadcast
				+ ", extended=" + extended + ", group=" + group + "]";
	}

}