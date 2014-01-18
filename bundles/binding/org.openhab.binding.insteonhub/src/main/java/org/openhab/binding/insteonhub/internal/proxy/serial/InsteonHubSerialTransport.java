/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.proxy.serial;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.insteonhub.internal.proxy.InsteonHubMsgConst;
import org.openhab.binding.insteonhub.internal.update.InsteonHubStdMsgFlags;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class does all of the heaving lifting for serial I/O communication with
 * the Insteon Hub.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class InsteonHubSerialTransport {

	public static final String SYSPROP_SERIAL_DUMP_FILE = "InsteonHub.serialDumpFile";

	private static final Logger logger = LoggerFactory
			.getLogger(InsteonHubSerialTransport.class);

	private final BlockingQueue<byte[]> commandQueue = new LinkedBlockingQueue<byte[]>();
	private final InsteonHubSerialTransportCallbacks callbacks;
	private final String threadNamePrefix;
	private volatile Listener listener;
	private volatile Sender sender;
	private InputStream inputStream;
	private OutputStream outputStream;
	private Writer cacheDumpWriter;

	public InsteonHubSerialTransport(
			InsteonHubSerialTransportCallbacks callbacks,
			String threadNamePrefix) {
		this.callbacks = callbacks;
		this.threadNamePrefix = threadNamePrefix;
	}

	public synchronized boolean isStarted() {
		return inputStream != null;
	}

	public synchronized void start(InputStream in, OutputStream out) {
		this.inputStream = in;
		this.outputStream = out;
		listener = new Listener();
		sender = new Sender();
		String serialDumpFilePath = System
				.getProperty(SYSPROP_SERIAL_DUMP_FILE);
		if (serialDumpFilePath != null) {
			File serialDumpFile = new File(serialDumpFilePath);
			try {
				cacheDumpWriter = new FileWriter(serialDumpFile);
			} catch (IOException e) {
				logger.error("Could not create serialDumpFile '"
						+ serialDumpFile.getAbsolutePath() + "'", e);
			}
		}
		new Thread(listener, threadNamePrefix + " listener").start();
		new Thread(sender, threadNamePrefix + " sender").start();
	}

	public synchronized void stop() {
		if (cacheDumpWriter != null) {
			try {
				cacheDumpWriter.close();
			} catch (IOException e) {
				// ignore
			}
		}
		cacheDumpWriter = null;
		inputStream = null;
		outputStream = null;
		listener = null;
		sender = null;
	}

	public void enqueueCommand(byte[] msg) {
		commandQueue.add(msg);
	}

	// Takes commands off the command queue and sends them to the Hub.
	private class Sender implements Runnable {
		@Override
		public void run() {
			try {
				// check run condition
				while (sender == this) {
					// take message off queue
					byte[] msg = null;
					try {
						msg = commandQueue.poll(5, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						// ignore: msg will be null and not processed
					}

					// process message
					if (msg != null) {
						outputStream.write(msg);
						outputStream.flush();
					}
				}
			} catch (Throwable t) {
				// thread will stop
				logger.error("Failure writing to " + threadNamePrefix, t);
			}
		}
	};

	// Listens for messages from the Hub and passes them to callback
	private class Listener implements Runnable {
		@Override
		public void run() {
			try {
				while (listener == this) {
					// read next messages
					byte[] msg = readMsg();
					// if msg was read, pass to handleMessage
					if (msg != null) {
						callbacks.onReceived(msg);
					}
				}
			} catch (Throwable t) {
				logger.error("Failure writing to " + threadNamePrefix, t);
				callbacks.onDisconnect();
			}
		}

		private byte[] readMsg() throws IOException {
			// read to 0x02 "start of message"
			byte b;
			while ((b = readByte()) != InsteonHubMsgConst.STX) {
				if (logger.isInfoEnabled()) {
					logger.info("Ignoring non STX byte: " + b);
				}
			}
			// read command type byte
			byte cmd = readByte();

			// based on command type, figure out number of messages to read
			Integer msgSize = InsteonHubMsgConst.REC_MSG_SIZES.get(cmd);
			if (msgSize == null) {
				logger.info("Received unknown command type '" + cmd + "'");
				return null;
			}
			byte[] msg = new byte[msgSize];
			msg[0] = InsteonHubMsgConst.STX;
			msg[1] = cmd;
			fillBuffer(msg, 2);

			if (cmd == InsteonHubMsgConst.SND_CODE_SEND_INSTEON_STD_OR_EXT_MSG) {
				InsteonHubStdMsgFlags flags = new InsteonHubStdMsgFlags(msg[5]);
				if (flags.isExtended()) {
					// read 14 more bytes and add them to the end of the msg
					byte[] extendedBytes = new byte[14];
					fillBuffer(extendedBytes, 0);
					byte[] extMsg = new byte[msg.length + extendedBytes.length];
					System.arraycopy(msg, 0, extMsg, 0, msg.length);
					System.arraycopy(extendedBytes, 0, extMsg, msg.length,
							extendedBytes.length);
					msg = extMsg;
				} else if(msg[8] == 0x02) {
					// TODO what the heck is this?
					byte[] ignore = new byte[8];
					fillBuffer(ignore, 0);
					msg[8] = InsteonHubMsgConst.ACK;
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Received Message from INSTEON Hub: "
						+ InsteonHubByteUtil.bytesToHex(msg, 0, msg.length));
			}

			if (cacheDumpWriter != null) {
				cacheDumpWriter.flush();
			}

			return msg;
		}
	}

	private void fillBuffer(byte[] buf, int off) throws IOException {
		for (int i = off; i < buf.length; i++) {
			buf[i] = readByte();
		}
	}

	private byte readByte() throws IOException {
		int v = inputStream.read();
		if (v == -1) {
			throw new IOException("Unexpected end of stream");
		}
		if (cacheDumpWriter != null) {
			cacheDumpWriter.write(InsteonHubByteUtil.byteToHex(v));
		}
		return (byte) v;
	}

}
