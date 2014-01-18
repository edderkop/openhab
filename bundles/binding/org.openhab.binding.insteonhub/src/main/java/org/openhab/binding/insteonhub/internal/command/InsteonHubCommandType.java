/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.command;

/**
 * Insteon Hub Command type paired with their associated wire types.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public enum InsteonHubCommandType {
	ON(0x11), FAST_ON(0x12), OFF(0x13), FAST_OFF(0x14), BRIGHT(0x15), DIM(0x16), START_DIM_BRT(
			0x17), STOP_DIM_BRT(0x18), STATUS_REQUEST(0x19);
	private final int byteValue;

	private InsteonHubCommandType(int byteValue) {
		this.byteValue = byteValue;
	}

	public int getByteValue() {
		return byteValue;
	}
}