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

import org.openhab.binding.insteonhub.internal.proxy.serial.InsteonHubSerialProxy;

/**
 * Used for debugging. Prints all bytes received from the hub in hex form.
 * Additionally ".." is printed for every second that elapses when nothing has
 * been received.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubStreamPrinter {

	private static final int LOAD_INTERVAL = 1000;
	private static long lastPrint = System.currentTimeMillis();
	private static boolean run = true;
	private static int lineLen;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: InsteonHubStreamPrinter <Hub IP>");
			System.exit(1);
		}
		String ip = args[0];

		new Thread(loadPrinter).start();

		try {
			final Socket socket = new Socket(ip,
					InsteonHubSerialProxy.DEFAULT_PORT);

			InputStream in = socket.getInputStream();
			int v;
			while ((v = in.read()) != -1) {
				print(String.format("%02X ", (byte) v));
			}
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Exiting...");
		run = false;
	}

	private static void print(String str) {
		System.out.print(str);
		lineLen += str.length();
		if (lineLen >= 100) {
			System.out.println();
			lineLen = 0;
		}
		lastPrint = System.currentTimeMillis();
	}

	private static final Runnable loadPrinter = new Runnable() {

		@Override
		public void run() {
			while (run) {
				if (System.currentTimeMillis() + LOAD_INTERVAL > lastPrint) {
					print(".. ");
				}
				try {
					Thread.sleep(LOAD_INTERVAL);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	};
}
