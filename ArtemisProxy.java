import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;

//import ProxyListener;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.OutputStream;

import asg.cliche.Command;
import asg.cliche.ShellFactory;

import net.dhleong.acl.util.BoolState;
import net.dhleong.acl.enums.*;
import net.dhleong.acl.iface.*;

import net.dhleong.acl.world.*;
import net.dhleong.acl.vesseldata.*;

import net.dhleong.acl.protocol.*;
import net.dhleong.acl.protocol.core.*;
import net.dhleong.acl.protocol.core.setup.*;
import net.dhleong.acl.protocol.core.world.*;
import net.dhleong.acl.protocol.core.helm.*;
import net.dhleong.acl.protocol.core.comm.*;
import net.dhleong.acl.protocol.core.weap.*;

public class ArtemisProxy implements Runnable {

	public static ThreadedArtemisNetworkInterface server;
	public static SystemManager mgr;
	public InputStream in;
	public OutputStream out;
	public static String serialDevice;
	
	public static void main(String[] args) {
		mgr = new SystemManager();
		String serverAddr = args[0];
		serialDevice = args[1];
		
		int port = 2010;
		ArtemisProxy mainProxy = new ArtemisProxy(port, serverAddr);
		
		try {
			// Load the vessel data, otherwise it will crash during certain packets
			VesselData.load(new File("."));
		} catch (VesselDataException ex) {
			ex.printStackTrace();
		}
		
		try {
			new Thread(mainProxy).start();
			ShellFactory.createConsoleShell("proxy", "", mainProxy).commandLoop();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public class SerialReader implements Runnable {
		InputStream in;
		
		public SerialReader (InputStream in) {
			this.in = in;
		}
		
		public void run () {
			byte[] buffer = new byte[1024];
			int len = -1;
			String commandString = "";
			try {
				while ((len = this.in.read(buffer)) > -1) {
					//System.out.println("Length: " + len);
					
					if (!commandString.contains("\n")) {
						commandString = commandString.concat(new String(buffer,0,len)).trim();
					} else {
						commandString = new String(buffer,0,len).trim();
					}
					
					switch (Integer.parseInt(commandString)) {
						case 1		: shields(); break;
						case 2		: redAlert(); break;
						case 3		: allStop(); break;
						case 4 		: setMainScreen("TACTICAL"); break;
						case 5 		: setMainScreen("LONG_RANGE"); break;
						case 6		: setMainScreen("STATUS"); break;
						case 7		: setMainScreen("FORE"); break;
						case 8		: setMainScreen("AFT"); break;
						case 9		: setMainScreen("Port"); break;
						case 10		: setMainScreen("Starboard"); break;
						default: System.out.println(commandString); break;
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private int port;
	private String serverAddr;
	private int serverPort = 2010;
	
	public ArtemisProxy(int port, String serverAddr) {
		this.port = port;
		int colonPos = serverAddr.indexOf(':');
		
		if (colonPos == -1) {
			this.serverAddr = serverAddr;
		} else {
			this.serverAddr = serverAddr.substring(0, colonPos);
			serverPort = Integer.parseInt(serverAddr.substring(colonPos + 1));
		}

		// START SERIAL MONITORING
		try {
			
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("/dev/" + serialDevice);
			if ( portIdentifier.isCurrentlyOwned() ) {
				System.out.println("Error: Port is currently in use");
			} else {
				CommPort commPort = portIdentifier.open("ArtemisProxy",2000);
				
				if (commPort instanceof SerialPort) {
					SerialPort serialPort = (SerialPort) commPort;
					serialPort.setSerialPortParams(115200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
					
					in = serialPort.getInputStream();
					out = serialPort.getOutputStream();
					
					(new Thread(new SerialReader(in))).start();

				} else {
					System.out.println("Error: Only serial ports are handled by this example.");
				}
			}
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// END SERIAL
	
	}
	
	@Override
	public void run() {
		// Client code
		/*
		try {
			server = new ThreadedArtemisNetworkInterface(serverAddr, serverPort);
			server.addListener(this);
			server.addListener(mgr);
			server.start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		*/

		ServerSocket listener = null;
		
		try {
			listener = new ServerSocket(this.port);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			
			 //if (listener != null && !listener.isClosed()) {
			//	try {
			 //listener.close();
			//	} catch (IOException ex) {
			 //ex.printStackTrace();
			//	}
			 //}
		 
		}
		
		while(true) {
			try {
				//listener.setSoTimeout(0);
				
				System.out.println("Listening for connections on port " + this.port + "...");
				Socket skt = listener.accept();
				
				System.out.println("Received connection from " + skt.getRemoteSocketAddress());
				ThreadedArtemisNetworkInterface client = new ThreadedArtemisNetworkInterface(skt, ConnectionType.CLIENT);
				client.setParsePackets(true);
				
				System.out.println("Connecting to server at " + serverAddr + ":" + serverPort + "...");
				server = new ThreadedArtemisNetworkInterface(serverAddr, serverPort);
				server.setParsePackets(true);
				server.addListener(mgr);
				
				new ProxyListener(server, client, out, mgr);
				System.out.println("Connection established.");
				
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	
	}
	
	// Client listeners
	/*
	@Listener
	public void onConnectSuccess(ConnectionSuccessEvent event) {
		server.send(new SetConsolePacket(Console.HELM, true));
		server.send(new ReadyPacket());
		System.out.println("Connected");
	}
	
	@Listener
	public void onPacket(WeapPlayerUpdatePacket pkt) {
		System.out.println(pkt);
	}
	
	@Listener
	public void onDisconnect(DisconnectEvent event) {
		System.out.println("Disconnected: " + event.getCause());
	}
	@Listener
	public void onPacket(ToggleShieldsPacket pkt) {
		new SerialWriter(out, serialCommands.SHIELD, "up");
		System.out.println("Recognized Shield Packet");
	}
*/
	
	@Command
	public String shields() {
		if (server.isConnected()) {
			ToggleShieldsPacket packet = new ToggleShieldsPacket();
			server.send(packet);
			
			return "Toggling shields...";
		} else {
			System.out.println("Cannot send shield toggle: Not connected");
			return "No server";
		}
	}
	
	@Command
	public String redAlert() {
		if (server.isConnected()) {
			ToggleRedAlertPacket packet = new ToggleRedAlertPacket();
			server.send(packet);
			return "RED ALERT";
		} else {
			System.out.println("Cannot send red alert: Not connected");
			return "No server";
		}
	}

	@Command
	public String setMainScreen(String screen) {
		// FORE, PORT, STARBOARD, AFT, TACTICAL, LONG_RANGE, STATUS
		if (server.isConnected()) {
			SetMainScreenPacket packet = new SetMainScreenPacket(MainScreenView.valueOf(screen));
			server.send(packet);
			return "Set Main Screen to something";
		} else {
			System.out.println("Cannot change main screen: Not connected");
			return "No server";
		}
	}
	
	@Command
	public String togglePerspective() {
		if (server.isConnected()) {
			TogglePerspectivePacket packet = new TogglePerspectivePacket();
			server.send(packet);

			return "Changed view screen perspective";
		} else {
			System.out.println("Cannot change main screen: Not connected");
			return "No server";
		}
	}

	// Only impulse works. Client seems to take priority for warp
	@Command
	public String allStop() {
		if (server.isConnected()) {
			HelmSetWarpPacket packetB = new HelmSetWarpPacket(0);
			server.send(packetB);
			HelmSetImpulsePacket packet = new HelmSetImpulsePacket(0);
			server.send(packet);
			
			return "ALL STOP";
		} else {
			System.out.println("Cannot bring the ship to a halt: Not connected");
			return "No server";
		}
	}
	
	// Doesn't work. Client seems to take priority
	@Command
	public String setWarp(int warp) {
		if (server.isConnected()) {
			HelmSetWarpPacket packet = new HelmSetWarpPacket(warp);
			server.send(packet);
			
			return "Set Warp: " + warp;
		} else {
			System.out.println("Cannot bring the ship to a halt: Not connected");
			return "No server";
		}
	}
	
	@Command
	public String torpedoStatus() {
		if (server.isConnected()) {
			ArtemisPlayer player = mgr.getPlayerShip(0);
			
			//System.out.println(player);

			System.out.println("Homing: " + player.getTorpedoCount(OrdnanceType.HOMING));
			System.out.println("Nuke: " + player.getTorpedoCount(OrdnanceType.NUKE));
			System.out.println("EMP: " + player.getTorpedoCount(OrdnanceType.EMP));
			System.out.println("Mine: " + player.getTorpedoCount(OrdnanceType.MINE));
			
			return "";
		} else {
			return "No Server";
		}
	}
	
}