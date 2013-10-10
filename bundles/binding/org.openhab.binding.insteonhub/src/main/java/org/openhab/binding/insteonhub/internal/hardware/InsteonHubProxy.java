package org.openhab.binding.insteonhub.internal.hardware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

public class InsteonHubProxy {

	public static final int DEFAULT_PORT = 25105;
	private static final int LEVEL_MAX = 255;
	private static final int LEVEL_MIN = 0;

	private static final String COMMAND_ON_RAMP = "11";
	private static final String COMMAND_ON_FAST = "12";
	private static final String COMMAND_OFF_RAMP = "13";
	private static final String COMMAND_OFF_FAST = "14";
	private static final String COMMAND_STATUS = "19";
	private static final String STATUS_PREFIX = "<X D=\"";

	private final String host;
	private final int port;
	private final String credentials;

	public InsteonHubProxy(String host, String user, String pass) {
		this(host, DEFAULT_PORT, user, pass);
	}

	public InsteonHubProxy(String host, int port, String user, String pass) {
		this.host = host;
		this.port = port;
		this.credentials = Base64.encodeBase64String((user + ":" + pass)
				.getBytes());
	}

	public String getConnectionString() {
		return host + ":" + port;
	}
	
	public void setPower(String device, boolean power, boolean ramp)
			throws IOException {
		setLevel(device, power ? LEVEL_MAX : LEVEL_MIN, ramp);
	}
	
	public void setLevel(String device, int level) throws IOException {
		setLevel(device, level, true);
	}

	public boolean isPower(String device) throws IOException {
		return getLevel(device) > 0;
	}
	
	public int getLevel(String device) throws IOException {
		StringBuilder url = new StringBuilder();
		url.append("/sx.xml?");
		url.append(device);
		url.append("=");
		url.append(COMMAND_STATUS);
		url.append("00");

		String response = httpGet(url.toString());
		
		int startIdx = response.indexOf(STATUS_PREFIX);
		if (startIdx == -1) {
			throw new IOException("Unexpected response: " + response);
		}
		
		try {
			// ignore up to xml node value
			String hex = response.substring(startIdx + STATUS_PREFIX.length());
			// ignore after xml node value
			hex = hex.substring(0, hex.indexOf('"'));
			// only pay attention to last 2 digits
			hex = hex.substring(hex.length()-2);
			// parse the hex value
			int value = Hex.decodeHex(hex.toCharArray())[0];
			if (value < 0) {
				value += 256;
			}
			return value;
		} catch(Exception e) {
			throw new IOException("Unexpected response: " + response);
		}
	}

	private String setLevel(String device, int level, boolean ramp)
			throws IOException {
		String levelHex = byteToHex((byte) level);
		String command;
		if ("00".equals(levelHex)) {
			command = ramp ? COMMAND_OFF_RAMP : COMMAND_OFF_FAST;
		} else if ("FF".equals(levelHex)) {
			command = ramp ? COMMAND_ON_RAMP : COMMAND_ON_FAST;
		} else {
			// "dim" values must ramp
			command = COMMAND_ON_RAMP;
		}

		StringBuilder url = new StringBuilder();
		url.append("/sx.xml?");
		url.append(device);
		url.append("=");
		url.append(command);
		url.append(levelHex);

		return httpGet(url.toString());
	}

	private String httpGet(String urlSuffix) throws IOException {
		URLConnection connection = null;
		InputStream is = null;
		try {
			connection = new URL("http://" + getConnectionString() + urlSuffix)
					.openConnection();
			connection.setRequestProperty("Authorization", "Basic "
					+ credentials);

			is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} catch (Exception e) {
			throw new IOException("Could not handle http get", e);
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	private static final String byteToHex(byte b) {
		return new String(Hex.encodeHex(new byte[] { b })).toUpperCase();
	}
}
