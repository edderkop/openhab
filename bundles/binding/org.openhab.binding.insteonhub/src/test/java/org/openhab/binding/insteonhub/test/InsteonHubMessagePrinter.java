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
import java.net.Socket;

import org.openhab.binding.insteonhub.internal.proxy.serial.InsteonHubSerialProxy;
import org.openhab.binding.insteonhub.internal.proxy.serial.InsteonHubSerialTransport;
import org.openhab.binding.insteonhub.internal.proxy.serial.InsteonHubSerialTransportCallbacks;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;

/**
 * Used for debugging. Parses and prints all messages received from the given
 * Insteon Hub.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubMessagePrinter {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: InsteonHubMessagePrinter <Hub IP>");
			System.exit(1);
		}
		String ip = args[0];
		final Socket socket = new Socket(ip, InsteonHubSerialProxy.DEFAULT_PORT);

		InsteonHubSerialTransportCallbacks callbacks = new InsteonHubSerialTransportCallbacks() {
			@Override
			public void onDisconnect() {
				System.out.println("Connection Lost!");
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
				}
			}

			@Override
			public void onReceived(byte[] msg) {
				System.out.println(InsteonHubByteUtil.bytesToHex(msg, 0,
						msg.length));
			}

		};

		InsteonHubSerialTransport transport = new InsteonHubSerialTransport(
				callbacks, "InsteonHub");
		transport.start(socket.getInputStream(), socket.getOutputStream());
	}
}
