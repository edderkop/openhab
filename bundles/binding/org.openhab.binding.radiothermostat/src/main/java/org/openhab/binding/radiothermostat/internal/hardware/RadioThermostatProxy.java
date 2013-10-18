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
package org.openhab.binding.radiothermostat.internal.hardware;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Radio Thermostat proxy used to control a Radio Thermostat using HTTP/Json
 * 
 * @author Eric Thill
 */
public class RadioThermostatProxy {

	private final String host;
	private final JSONParser parser = new JSONParser();

	public RadioThermostatProxy(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	/*
	 * Notes --- tstate identifies current heat/cool mode. it may not be
	 * supported by all models/versions. if thermostat is in "auto" mode, tstate
	 * should exist and we will be required to identify if "t_cool" or "t_heat"
	 * should be used to get the current target temperature. if tstate does not
	 * exist in the message, the logic will fall back on using the set tmode to
	 * determine if it is in heating or cooling mode.
	 */
	public RadioThermostatState getState() throws IOException {
		String jsonStr = httpGet("/tstat");
		try {
			JSONObject json = (JSONObject) parser.parse(jsonStr);
			float temp = Float.parseFloat(json.get("temp").toString());
			RadioThermostatHvacMode tstate = json.containsKey("tstate") ? RadioThermostatHvacMode
					.parse(json.get("tstate").toString()) : null;
			RadioThermostatHvacMode tmode = RadioThermostatHvacMode.parse(json
					.get("tmode").toString());
			RadioThermostatFanMode fmode = RadioThermostatFanMode.parse(json
					.get("fmode").toString());
			boolean fan = "1".equals(json.get("fstate").toString());
			boolean hold = "1".equals(json.get("hold").toString());
			float targetTemp = temp;
			if (tstate == RadioThermostatHvacMode.COOL
					|| tmode == RadioThermostatHvacMode.COOL) {
				targetTemp = parseCoolTarget(json);
			} else if (tstate == RadioThermostatHvacMode.HEAT
					|| tmode == RadioThermostatHvacMode.HEAT) {
				targetTemp = parseHeatTarget(json);
			}
			return new RadioThermostatState(tmode, fmode, temp, targetTemp,
					hold, fan);
		} catch (ParseException e) {
			throw new IOException("Unexpected response", e);
		}
	}

	private float parseCoolTarget(JSONObject json) {
		if (json.containsKey("t_cool")) {
			return Float.parseFloat(json.get("t_cool").toString());
		} else if (json.containsKey("a_cool")) {
			return Float.parseFloat(json.get("a_cool").toString());
		} else {
			return -1;
		}
	}

	private float parseHeatTarget(JSONObject json) {
		if (json.containsKey("t_heat")) {
			return Float.parseFloat(json.get("t_heat").toString());
		} else if (json.containsKey("a_heat")) {
			return Float.parseFloat(json.get("a_heat").toString());
		} else {
			return -1;
		}
	}

	/**
	 * Set the mode of the thermostat (OFF, HEAT, COOL, AUTO)
	 * 
	 * @param mode
	 * @throws IOException
	 */
	public void setHvacMode(RadioThermostatHvacMode mode) throws IOException {
		if (mode != null) {
			httpPost("/tstat", "{\"tmode\":" + mode.getVal() + "}");
		}
	}

	/**
	 * Set the fan mode (AUTO, ON)
	 * 
	 * @param mode
	 * @throws IOException
	 */
	public void setFanMode(RadioThermostatFanMode mode) throws IOException {
		if (mode != null) {
			httpPost("/tstat", "{\"fmode\":" + mode.getVal() + "}");
		}
	}

	/**
	 * Set the current target temperature
	 * 
	 * @param temperature
	 * @throws IOException
	 */
	public void setTargetTemperature(float temperature) throws IOException {
		RadioThermostatState state = getState();
		RadioThermostatHvacMode tmode = state.getHvacMode();
		if (tmode == RadioThermostatHvacMode.COOL) {
			httpPost("/tstat", "{\"t_cool\":" + temperature + "}");
		} else if (tmode == RadioThermostatHvacMode.HEAT) {
			httpPost("/tstat", "{\"t_heat\":" + temperature + "}");
		} else if (tmode == RadioThermostatHvacMode.AUTO) {
			httpPost("/tstat", "{\"t_heat\":" + temperature + ", \"t_cool\":"
					+ temperature + "}");
		}
	}

	/**
	 * Set hold on or off
	 * 
	 * @param hold
	 *            true=ON, false=OFF
	 * @throws IOException
	 */
	public void setHold(boolean hold) throws IOException {
		int val = hold ? 1 : 0;
		httpPost("/tstat", "{\"hold\":" + val + "}");
	}

	private String httpGet(String urlSuffix) throws IOException {
		URLConnection connection = null;
		InputStream is = null;
		try {
			connection = new URL("http://" + host + urlSuffix).openConnection();
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

	private void httpPost(String urlSuffix, String message) throws IOException {
		HttpURLConnection connection = null;
		try {
			URL url = new URL("http://" + host + urlSuffix);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Length",
					Integer.toString(message.length()));

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send post
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(message);
			wr.flush();
			wr.close();

			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
		} catch (Exception e) {
			throw new IOException("Could not handle http post", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}