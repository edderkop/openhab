package org.openhab.binding.sharptv.internal.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SharpTvProxy {

	private static final int BUTTON_VOL_DOWN = 32;
	private static final int BUTTON_VOL_UP = 33;

	private final String host;
	private final int port;
	private final String user;
	private final String pass;

	public SharpTvProxy(String host, int port, String user, String pass) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.pass = pass;
	}

	public void setPower(boolean power) throws IOException {
		if (power) {
			sendCommands("POWR1   ");
		} else {
			sendCommands("POWR0   ");
		}
	}

	public void toggleInput() throws IOException {
		sendCommands("ITGD0   ");
	}

	public void setInput(int input) throws IOException {
		if (input == 0) {
			sendCommands("ITVD0");
		} else {
			sendCommands("IAVD" + input);
		}
	}

	public void setAvMode(int mode) throws IOException {
		sendCommands("AVMD" + mode);
	}

	public void setVolume(int volume) throws IOException {
		sendCommands("VOLM" + volume);
	}

	public void setPositionH(int val) throws IOException {
		sendCommands("HPOS" + val);
	}

	public void setPositionV(int val) throws IOException {
		sendCommands("VPOS" + val);
	}

	public void setPositionClock(int val) throws IOException {
		sendCommands("CLCK" + val);
	}

	public void setPositionPhase(int val) throws IOException {
		sendCommands("PHSE" + val);
	}

	public void setViewMode(int mode) throws IOException {
		sendCommands("WIDE" + mode);
	}

	public void toggleMute() throws IOException {
		sendCommands("MUTE0   ");
	}

	public void setMute(boolean mute) throws IOException {
		if (mute) {
			sendCommands("MUTE1   ");
		} else {
			sendCommands("MUTE2   ");
		}
	}

	public void setSurround(int surround) throws IOException {
		sendCommands("ACSU" + surround);
	}

	public void toggleAudioSelection() throws IOException {
		sendCommands("ACHA0   ");
	}

	public void setSleepTimer(int timerMode) throws IOException {
		sendCommands("OFTM" + timerMode);
	}

	public void setChannelAnalog(int channel) throws IOException {
		sendCommands("DCCH" + channel);
	}

	public void setChannelDigitalAir(int channel) throws IOException {
		sendCommands("DA2P" + channel);
	}

	public void setChannelDigitalCableTwoPart(int u, int l) throws IOException {
		sendCommands("DC2U" + u, "DC2L" + l);
	}
	
	public void setChannelDigitalCableOnePart(int channel) throws IOException {
		if(channel < 10000) {
			sendCommands("DC10"+channel);
		} else {
			sendCommands("DC11"+(10000-channel));
		}
	}

	public void channelUp() throws IOException {
		sendCommands("CHUP0   ");
	}

	public void channelDown() throws IOException {
		sendCommands("CHDW0   ");
	}

	public void toggleClosedCaption() throws IOException {
		sendCommands("CLCP0   ");
	}

	public void set3DMode(int mode) throws IOException {
		sendCommands("TDCH" + mode);
	}

	/**
	 * This function is not supported on every Sharp TV Model. I believe this
	 * started being supported sometime in the 2012 models. If it doesn't work
	 * for you, you may have luck updating your firmware.
	 * 
	 * @param button
	 * @throws IOException
	 */
	public void pressRemoteButton(int button) throws IOException {
		sendCommands("RCKY" + button);
	}

	/**
	 * This function relies on press remote button functionality which is not
	 * supported on every Sharp TV Model
	 * 
	 * @throws IOException
	 */
	public void volumeDown() throws IOException {
		pressRemoteButton(BUTTON_VOL_DOWN);
	}

	/**
	 * This function relies on press remote button functionality which is not
	 * supported on every Sharp TV Model
	 * 
	 * @throws IOException
	 */
	public void volumeUp() throws IOException {
		pressRemoteButton(BUTTON_VOL_UP);
	}

	public void sendCommands(String... commands) throws IOException {
		// normalize commands
		for (int i = 0; i < commands.length; i++) {
			String command = commands[i];
			if (command.length() > 8) {
				commands[i] = command.substring(0, 8);
			} else if (command.length() < 8) {
				StringBuilder sb = new StringBuilder();
				sb.append(command);
				for (int j = command.length(); j < 8; j++) {
					sb.append(" ");
				}
				commands[i] = sb.toString();
			}
		}

		Socket socket = null;
		try {
			// create connection
			socket = new Socket(host, port);
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
			// read login prompt
			readPrompt(in);
			// write user
			out.write((user + "\r").getBytes());
			out.flush();
			// read password prompt
			readPrompt(in);
			// write password
			out.write((pass + "\r").getBytes());
			out.flush();
			// read newline
			readLine(in);

			// write commands
			for (String command : commands) {
				out.write(command.getBytes());
				out.write("\r\n".getBytes());
			}
			out.flush();
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

	private String readPrompt(InputStream in) throws IOException {
		return readUntil(in, ':');
	}

	private String readLine(InputStream in) throws IOException {
		return readUntil(in, '\n');
	}

	private String readUntil(InputStream in, char c) throws IOException {
		StringBuilder sb = new StringBuilder();
		int val = in.read();
		while (val != -1 && val != c) {
			sb.append((char) val);
			val = in.read();
		}
		if (val == -1) {
			throw new IOException("Unexpected end of stream");
		}
		sb.append((char) val);
		return sb.toString();
	}

}
