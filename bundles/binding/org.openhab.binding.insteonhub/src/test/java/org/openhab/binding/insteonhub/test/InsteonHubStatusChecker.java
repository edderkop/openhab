/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.command.InsteonHubCommandType;
import org.openhab.binding.insteonhub.internal.proxy.serial.InsteonHubSerialProxy;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;

/**
 * Used for debugging. Sends a 0x50 status request message to the given device
 * via the given Hub. It then prints all bytes received from the Hub until the
 * application is closed.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubStatusChecker {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out
					.println("Usage: InsteonHubStreamPrinter <Hub IP> <Device>");
			System.exit(1);
		}
		String ip = args[0];
		String deviceStr = args[1];
		int deviceInt = InsteonHubByteUtil.deviceIdStringToInt(deviceStr);

		InsteonHubCommand command = new InsteonHubCommand(deviceInt, InsteonHubCommandType.STATUS_REQUEST);
		byte[] msgBuffer = command.buildSerialMessage();

		try {
			final Socket socket = new Socket(ip,
					InsteonHubSerialProxy.DEFAULT_PORT);

			System.out.println("Sending: "
					+ InsteonHubByteUtil.bytesToHex(msgBuffer, 0,
							msgBuffer.length));
			socket.getOutputStream().write(msgBuffer);
			socket.getOutputStream().flush();

			System.out.print("Received: ");
			InputStream in = socket.getInputStream();
			int v;
			while ((v = in.read()) != -1) {
				System.out.print(String.format("%02X ", (byte) v));
			}
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
