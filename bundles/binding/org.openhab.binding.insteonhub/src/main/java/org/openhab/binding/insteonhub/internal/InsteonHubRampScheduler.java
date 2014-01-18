/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Insteon Hub isn't good at responding to a ton of arbitrary DIM/BRT commands
 * in a row. This translates them to Up/Down/Stop commands by keeping track of
 * timeouts for each device.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubRampScheduler {

	private static final int TIMEOUT_INTERVAL = 400;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<Integer, DimInfo> dimInfos = new LinkedHashMap<Integer, DimInfo>();
	private final ReadWriteLock dimInfosLock = new ReentrantReadWriteLock();

	private final InsteonHubBus bus;
	private volatile ScheduleProcessor scheduleProcessor;

	public InsteonHubRampScheduler(InsteonHubBus bus) {
		this.bus = bus;
	}

	public void increase(int deviceId) {
		DimInfo info = getOrCreateDimInfo(deviceId);
		if (info.timeout.getAndSet(System.currentTimeMillis()
				+ TIMEOUT_INTERVAL) == 0) {
			bus.sendOpenhabCommand(deviceId, UpDownType.UP);
			if (logger.isDebugEnabled()) {
				logger.debug("Started Ramp UP for "
						+ InsteonHubByteUtil.deviceIdIntToString(deviceId));
			}
		}
	}

	public void decrease(int deviceId) {
		DimInfo info = getOrCreateDimInfo(deviceId);
		if (info.timeout.getAndSet(System.currentTimeMillis()
				+ TIMEOUT_INTERVAL) == 0) {
			bus.sendOpenhabCommand(deviceId, UpDownType.DOWN);
			if (logger.isDebugEnabled()) {
				logger.debug("Started Ramp DOWN for "
						+ InsteonHubByteUtil.deviceIdIntToString(deviceId));
			}
		}
	}

	private DimInfo getOrCreateDimInfo(int deviceId) {
		dimInfosLock.readLock().lock();
		DimInfo info;
		try {
			info = dimInfos.get(deviceId);
		} finally {
			dimInfosLock.readLock().unlock();
		}
		if (info == null) {
			try {
				// double check eliminates writeLock 99% of the time
				dimInfosLock.writeLock().lock();
				info = dimInfos.get(deviceId);
				if (info == null) {
					info = new DimInfo();
					dimInfos.put(deviceId, info);
				}
			} finally {
				dimInfosLock.writeLock().unlock();
			}
		}
		return info;
	}

	public synchronized void start() {
		scheduleProcessor = new ScheduleProcessor();
		new Thread(scheduleProcessor, "InsteonHub Ramp Scheduler").start();
	}

	public synchronized void stop() {
		scheduleProcessor = null;
	}

	private class ScheduleProcessor implements Runnable {
		@Override
		public void run() {
			while (scheduleProcessor == this) {
				long curTime = System.currentTimeMillis();
				dimInfosLock.readLock().lock();
				try {
					// check all timeouts
					for (Map.Entry<Integer, DimInfo> entry : dimInfos
							.entrySet()) {
						// parse from entry
						int deviceId = entry.getKey();
						long timeout = entry.getValue().timeout.get();
						// check if timeout is set and has elapsed
						if (timeout > 0 && curTime > timeout) {
							// timeout elapsed => reset timeout and stop dim/brt
							entry.getValue().timeout.set(0);
							bus.sendOpenhabCommand(deviceId, StopMoveType.STOP);
							if (logger.isDebugEnabled()) {
								logger.debug("Stopped Ramp for "
										+ InsteonHubByteUtil
												.deviceIdIntToString(deviceId));
							}
						}
					}
				} finally {
					dimInfosLock.readLock().unlock();
				}
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	private static class DimInfo {
		public AtomicLong timeout = new AtomicLong();
		// public boolean dimming;
	}

}
