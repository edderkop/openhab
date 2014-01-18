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
 * Insteon Hub "Start Dim/Brt" commands need to know if it's a DIM event or BRT
 * event for the "Command 2" field.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubStartDimBrtCommand extends InsteonHubCommand {

	public enum InsteonHubStartDimBrtType {
		DIM, BRT;
	}

	private final InsteonHubStartDimBrtType dimBrtType;

	public InsteonHubStartDimBrtCommand(int deviceId,
			InsteonHubStartDimBrtType dimBrtType) {
		super(deviceId, InsteonHubCommandType.START_DIM_BRT);
		this.dimBrtType = dimBrtType;
	}

	@Override
	protected byte getCmd2() {
		if (dimBrtType == InsteonHubStartDimBrtType.DIM) {
			return 0;
		} else {
			return 1;
		}
	}
}
