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
package org.openhab.binding.digitalstrom.internal.client.constants;

/**
 * @author	Alexander Betker
 * @since	1.3.0
 * @version	digitalSTROM-API 1.14.5
 */
public enum DeviceParameterClassEnum {
	
	/**
	 * communication specific parameters
	 */
	CLASS_0		(0),
	
	/**
	 * digitalSTROM device specific parameters
	 */
	CLASS_1		(1),
	
	/**
	 * function specific parameters
	 */
	CLASS_3		(3),
	
	/**
	 * sensor event table
	 */
	CLASS_6		(6),
	
	/**
	 * output status
	 * 
	 * possible OffsetParameters:
	 * - READ_OUTPUT
	 */
	CLASS_64	(64),
	
	/**
	 * read scene table
	 * use index/offset 0-127 
	 */
	CLASS_128	(128);
	
	private final int		classIndex;
	
	DeviceParameterClassEnum(int index) {
		this.classIndex = index;
	}
	
	public int getClassIndex() {
		return this.classIndex;
	}

}
