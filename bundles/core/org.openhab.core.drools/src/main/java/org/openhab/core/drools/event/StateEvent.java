/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2010-2013, openHAB.org <admin@openhab.org>
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with Eclipse (or a modified version of that library),
 * containing parts covered by the terms of the Eclipse Public License
 * (EPL), the licensors of this Program grant you additional permission
 * to convey the resulting work.
 */
package org.openhab.core.drools.event;

import org.openhab.core.items.Item;
import org.openhab.core.types.State;

/**
 * This class is used as a fact in rules to inform about received status updates on the openHAB event bus.
 * 
 * @author Kai Kreuzer
 * @since 0.7.0
 *
 */
public class StateEvent extends RuleEvent {

	protected boolean changed;
	protected State oldState;
	protected State newState;

	public StateEvent(Item item, State oldState, State newState) {
		super(item);
		this.oldState = oldState;
		this.newState = newState;
		this.changed = !oldState.equals(newState);
	}

	public StateEvent(Item item, State state) {
		this(item, state, state);
	}
	
	public boolean isChanged() {
		return changed;
	}

	public State getOldState() {
		return oldState;
	}

	public State getNewState() {
		return newState;
	}	
}
