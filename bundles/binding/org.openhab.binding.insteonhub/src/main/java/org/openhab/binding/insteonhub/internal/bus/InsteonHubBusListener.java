/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.bus;

import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Callbacks to listen to an {@link InsteonHubBus}
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public interface InsteonHubBusListener {
	void onSendOpenhabCommand(int deviceId, Command command);
	void onPostOpenhabUpdate(int deviceId, State update);
	void onSendInsteonCommand(int deviceId, InsteonHubCommand command);
	void onPostInsteonUpdate(int deviceId, InsteonHubUpdate update);
}
