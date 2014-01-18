/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.util;


/**
 * Utility functions for dealing with hex/buffers/bytes
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class InsteonHubByteUtil {
	
	public static int byteToUnsignedInt(byte b) {
		int i = b;
		if (i < 0) {
			i += 256;
		}
		return i;
	}
	
	public static boolean checkBit(int value, int idx) {
		return (value >>> idx) != 0;
	}
	
	public static int deviceIdStringToInt(String idStr) {
		idStr = idStr.replace(".", "");
		if(idStr.length() != 6) {
			throw new IllegalArgumentException("Invalid device ID '" + idStr + "'");
		}
		int idInt = 0;
		for(int i = 0; i < idStr.length(); i++) {
			idInt <<= 4;
			idInt += Character.digit(idStr.charAt(i), 16);
		}
		return idInt;
	}
	
	public static String deviceIdIntToString(int idInt) {
		StringBuilder sb = new StringBuilder();
		sb.append(Character.forDigit((idInt >>> 20) & 0xF, 16));
		sb.append(Character.forDigit((idInt >>> 16) & 0xF, 16));
		sb.append(".");
		sb.append(Character.forDigit((idInt >>> 12) & 0xF, 16));
		sb.append(Character.forDigit((idInt >>> 8) & 0xF, 16));
		sb.append(".");
		sb.append(Character.forDigit((idInt >>> 4) & 0xF, 16));
		sb.append(Character.forDigit((idInt) & 0xF, 16));
		return sb.toString();
	}
	
	public static void writeDeviceId(int id, byte[] dest, int destOff) {
		dest[destOff] = (byte)((id >>> 16) & 0xFF);
		dest[destOff+1] = (byte)((id >>> 8) & 0xFF);
		dest[destOff+2] = (byte)(id & 0xFF);
	}
	
	public static int readDeviceId(byte[] src, int off) {
		int id = 0;
		for(int i = 0; i < 3; i++) {
			id <<= 8;
			id += (src[off+i] & 0xFF);
		}
		return id;
	}
	
	public static String bytesToHex(byte[] buf, int off, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
	        sb.append(String.format("%02X ", buf[off+i]));
	    }
		return sb.toString();
	}
	
	public static String byteToHex(int b) {
		StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X ", b));
		return sb.toString();
	}
	
}
