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
 * On commands also need the 0-255 level value to send in the "Command 2" field.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubFastOnCommand extends InsteonHubCommand {

	private final int level;

	public InsteonHubFastOnCommand(int deviceId, int level) {
		super(deviceId, InsteonHubCommandType.FAST_ON);
		this.level = level;
	}

	public int getLevel() {
		return level;
	}

	@Override
	protected byte getCmd2() {
		return (byte) level;
	}
}
