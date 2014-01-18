/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.insteonhub.internal.proxy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all of the created {@link InsteonHubProxy}'s by name. Also supports
 * batch starting/stopping of all managed proxies.
 * 
 * @author Eric Thill
 * @since 1.4.1
 */
public class InsteonHubProxyManager {

	public static final String DEFAULT_PROXY_NAME = "_DEFAULT";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, InsteonHubProxy> proxies = new LinkedHashMap<String, InsteonHubProxy>();
	private final InsteonHubBus bus;
	
	public InsteonHubProxyManager(InsteonHubBus bus) {
		this.bus = bus;
	}
	
	public synchronized void clear() {
		proxies.clear();
	}

	public synchronized void addProxy(String proxyName, InsteonHubProxy proxy) {
		InsteonHubProxy existing = proxies.put(proxyName, proxy);
		if(existing != null) {
			existing.stop();
		}
		proxy.setBus(bus);
	}

	public synchronized InsteonHubProxy removeProxy(String proxyName) {
		InsteonHubProxy existing = proxies.remove(proxyName);
		if(existing != null) {
			existing.stop();
		}
		return existing;
	}

	public synchronized void startProxies() {
		for (InsteonHubProxy proxy : proxies.values()) {
			proxy.start();
			logger.debug("Starting Insteon Hub proxy " + proxy);
		}
	}

	public synchronized void stopProxies() {
		for (InsteonHubProxy proxy : proxies.values()) {
			proxy.stop();
		}
	}

}
