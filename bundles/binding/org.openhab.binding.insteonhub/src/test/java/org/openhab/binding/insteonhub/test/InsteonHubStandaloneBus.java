package org.openhab.binding.insteonhub.test;

import org.openhab.binding.insteonhub.internal.bus.InsteonHubBus;
import org.openhab.binding.insteonhub.internal.bus.InsteonHubBusListener;
import org.openhab.binding.insteonhub.internal.command.InsteonHubCommand;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDevice;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDeviceFactory;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDeviceManager;
import org.openhab.binding.insteonhub.internal.device.InsteonHubDeviceType;
import org.openhab.binding.insteonhub.internal.proxy.InsteonHubProxyManager;
import org.openhab.binding.insteonhub.internal.proxy.serial.InsteonHubSerialProxy;
import org.openhab.binding.insteonhub.internal.update.InsteonHubUpdate;
import org.openhab.binding.insteonhub.internal.util.InsteonHubByteUtil;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

public class InsteonHubStandaloneBus {

	private static final String PROXY_NAME = "DEFAULT";
	
	public static void main(String[] args) {
		if(args.length < 2) {
			System.out.println("Use: InsteonHubStandaloneBus <HubIP> <dev1_type>:<dev1_ID> <dev2_type>:<dev2_ID> ...");
			System.out.println("Example: InsteonHubStandaloneBus 10.0.0.2 DIMMER:12.A1.D2 MOTION_SENSOR:54:2A:01 SWITCH:84:A3:91");
			System.exit(1);
		}

		InsteonHubBus bus = new InsteonHubBus();
		bus.start();

		InsteonHubProxyManager proxyManager = new InsteonHubProxyManager(bus);
		InsteonHubSerialProxy proxy = new InsteonHubSerialProxy(args[0]);
		proxyManager.addProxy(PROXY_NAME, proxy);
		proxyManager.startProxies();
		
		InsteonHubDeviceManager deviceManager = new InsteonHubDeviceManager(bus);
		InsteonHubDeviceFactory deviceFactory = new InsteonHubDeviceFactory();
		for(int i = 1; i < args.length; i++) {
			String[] split = args[i].split("\\:");
			InsteonHubDeviceType deviceType = InsteonHubDeviceType.valueOf(split[0]);
			int deviceId = InsteonHubByteUtil.deviceIdStringToInt(split[1]);
			InsteonHubDevice device = deviceFactory.createDevice(deviceId, deviceType);
			deviceManager.addDevice(deviceId, device);
		}
		
		bus.addListener(new InsteonHubBusListener() {
			@Override
			public void onSendOpenhabCommand(int deviceId, Command command) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				System.out.println("openHAB Command: " + deviceString + " = " + command);
			}
			@Override
			public void onSendInsteonCommand(int deviceId, InsteonHubCommand command) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				byte[] buf = command.buildSerialMessage();
				String msg = InsteonHubByteUtil.bytesToHex(buf, 0, buf.length);
				System.out.println("InsteonHub Command: " + deviceString + " = " + msg);
			}
			@Override
			public void onPostOpenhabUpdate(int deviceId, State update) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				System.out.println("openHAB Update: " + deviceString + " = " + update);
			}
			@Override
			public void onPostInsteonUpdate(int deviceId, InsteonHubUpdate update) {
				String deviceString = InsteonHubByteUtil.deviceIdIntToString(deviceId);
				System.out.println("InsteonHub Update: " + deviceString + " = " + update);
			}
		});
	}
}
