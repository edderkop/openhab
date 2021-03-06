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
package org.openhab.binding.nikobus.internal.util;

import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.HIGH_BYTE;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.LOW_BYTE;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_CHANGE_ACK;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_CHANGE_CMD;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_CHANGE_GROUP_1;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_CHANGE_GROUP_2;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_REQUEST_ACK;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_REQUEST_CMD;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_REQUEST_GROUP_1;
import static org.openhab.binding.nikobus.internal.config.SwitchModuleChannelGroup.STATUS_REQUEST_GROUP_2;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.nikobus.internal.NikobusBinding;
import org.openhab.binding.nikobus.internal.core.NikobusCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzer for Nikobus Switch module with 12 channels (05-000-02).
 * 
 * This analyzer will try to determine all possible command combinations and
 * their corresponding checksum.
 * 
 * To accomplish this, a brute force approach is used and all possible
 * combinations are sent to the Nikobus. When a positive reply is received for
 * one of the commands, it's checksum is stored in the command cache.
 * 
 * Using this analyzer will result in thousands of commands being sent to the
 * nikobus. Use with care.
 * 
 * @author Davy Vanherbergen
 * @since 1.3.0
 */
public class SwitchModuleAnalyzer {

	private static Logger log = LoggerFactory.getLogger(SwitchModuleAnalyzer.class);

	private String moduleAddress;

	private CommandCache cache;

	private int group = 1;

	private NikobusBinding binding;

	private String statusRequestGroup;

	private String statusChangeGroup;

	private AnalysisType type;

	/**
	 * Initialize new analyzer for a given channel group.
	 * 
	 * @param group
	 *            1 or 2, representing channels 1-6 or 7-12 respectively.
	 */
	public SwitchModuleAnalyzer(int group) {
		this.group = group;

		if (this.group == 1) {
			statusRequestGroup = STATUS_REQUEST_GROUP_1;
			statusChangeGroup = STATUS_CHANGE_GROUP_1;
		} else {
			statusRequestGroup = STATUS_REQUEST_GROUP_2;
			statusChangeGroup = STATUS_CHANGE_GROUP_2;
		}
	}

	private enum AnalysisType {
		GUESS, COUNT, VERIFY
	}

	/**
	 * Find the checksum value for the status request command.
	 * 
	 * @throws Exception
	 */
	private void analyzeStatusRequestChecksum() throws Exception {

		// guess checksum for status request command
		String baseCommand = STATUS_REQUEST_CMD + CRCUtil.appendCRC(statusRequestGroup + moduleAddress);

		String ack = STATUS_REQUEST_ACK + statusRequestGroup;
		
		boolean found = guessChecksum(baseCommand, ack);
		if (type != AnalysisType.GUESS) {
			int count = found ? 1 : 0;
			log.info("Detected {}/1 status request command for group {}", count, group);
			return;
		}
		
		if (!found) {
			throw new Exception(
				"Status Request Command not found. Module Address is incorrect or incompatible. Aborting scan.");
		}
	}

	/**
	 * Guess checksums for all 64 possible combinations of a 6 channel module
	 * group.
	 * 
	 * @throws Exception
	 */
	private void analyzeStatusChangeChecksums() {

		int count = 0;

		// check all possible combinations
		for (int i = 0; i < 64; i++) {

			StringBuilder command = new StringBuilder();

			command.append(statusChangeGroup);
			command.append(moduleAddress);
			command.append(((i & (1 << 0)) != 0) ? HIGH_BYTE : LOW_BYTE);
			command.append(((i & (1 << 1)) != 0) ? HIGH_BYTE : LOW_BYTE);
			command.append(((i & (1 << 2)) != 0) ? HIGH_BYTE : LOW_BYTE);
			command.append(((i & (1 << 3)) != 0) ? HIGH_BYTE : LOW_BYTE);
			command.append(((i & (1 << 4)) != 0) ? HIGH_BYTE : LOW_BYTE);
			command.append(((i & (1 << 5)) != 0) ? HIGH_BYTE : LOW_BYTE);
			command.append(HIGH_BYTE);

			String baseCommand = STATUS_CHANGE_CMD
					+ CRCUtil.appendCRC(command.toString());
			String ack = STATUS_CHANGE_ACK + statusChangeGroup;

			if (guessChecksum(baseCommand, ack)) {
				count++;
			}
		}

		log.info("Detected {}/64 combinations for group {}", count, group);
	}

	/**
	 * Analyze all status request and status change commands.
	 */
	public void analyze(String address, AnalysisType type) throws Exception {

		this.type = type;
		moduleAddress = address.toUpperCase();

		if (address == null || address.length() != 4) {
			throw new Exception("Invalid module address");
		}

		analyzeStatusRequestChecksum();
		analyzeStatusChangeChecksums();
	}

	/**
	 * Guess checksums for all status request and status change commands.
	 */
	public void analyze(String address) throws Exception {
		analyze(address, AnalysisType.GUESS);
	}

	/**
	 * Counts the available checksums in the cache for the given module.
	 */
	public void count(String address) throws Exception {
		analyze(address, AnalysisType.COUNT);
	}

	/**
	 * Tests all the commands in the cache for the given module.
	 */
	public void verify(String address) throws Exception {
		analyze(address, AnalysisType.VERIFY);
	}

	/**
	 * Verify if a checksum in the cache is working correctly by re-testing the
	 * command. If the command doesn't work, it is removed from the cache.
	 * 
	 * @param command
	 *            serial command
	 * @param checksum
	 *            checksum to append to command
	 * @param expectedAck
	 * @return true if command works.
	 */
	private boolean verifyChecksum(String command, String checksum, String expectedAck) {

		try {
			// let's not flood the nikobus system with tasks..
			Thread.sleep(500);
			// send command
			binding.sendCommand(new NikobusCommand(command + checksum, expectedAck, 2000));
			return true;
		} catch (Exception e) {
			// no ack received, checksum is incorrect..
			log.error("Found invalid checksum {} = {}", command, checksum);
			// cache.put(command, null);
			return false;
		}
	}

	/**
	 * Try all 256 possible combinations for a checksum until one is found which
	 * works. If no match is found, a second attempt done is but during this
	 * attempt, commands will be sent slower.
	 * 
	 * @param command
	 *            command for which to find the checksum
	 * @param expectedAck
	 *            ACK expected from Nikobus which indicates a valid command was
	 *            sent.
	 * @return true if command was found
	 */
	private boolean guessChecksum(String command, String expectedAck) {

		String checksum = cache.get(command);

		if (checksum != null) {
			if (type == AnalysisType.VERIFY) {
				return verifyChecksum(command, checksum, expectedAck);
			} else {
				return true;
			}
		} else {
			if (type != AnalysisType.GUESS) {
				return false;
			}
		}

		long start = System.currentTimeMillis();
		boolean found = false;

		int i;

		for (i = 0; i < 256; i++) {

			// send 10 commands/second to try and locate the correct checksum
			checksum = StringUtils.leftPad(Integer.toHexString(i).toUpperCase(), 2, "0");

			try {
				binding.sendCommand(new NikobusCommand(command
						+ checksum, expectedAck, 100));
				// if we get here, we got a async positive response
				// to one of the last commands sent.
				log.info("Found possible match.");
				break;
			} catch (Exception e) {
				// no match found, move on to next one..
			}
		}

		for (int j = i - 5; j <= i; j++) {
			// slow down search to find the exact match
			checksum = StringUtils.leftPad(Integer.toHexString(j).toUpperCase(), 2, "0");

			try {
				binding.sendCommand(new NikobusCommand(command + checksum, expectedAck, 400));
				log.info("Found checksum in {} ms using {} attempts.",
						(System.currentTimeMillis() - start), (i + j + 1));
				cache.put(command, checksum);
				found = true;
				break;
			} catch (Exception e) {
				// no match found, move on to next one..
			}
		}

		if (!found) {
			log.error("Could not determine checksum for {}", command);
		}
		return found;
	}

	/**
	 * @param binding
	 */
	public void setBinding(NikobusBinding binding) {
		this.binding = binding;
		this.cache = binding.getCache();
	}

}
