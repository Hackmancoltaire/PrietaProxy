import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.OutputStream;

import asg.cliche.Command;
import asg.cliche.ShellFactory;

import net.dhleong.acl.enums.ConnectionType;
import net.dhleong.acl.iface.ArtemisNetworkInterface;
import net.dhleong.acl.iface.DisconnectEvent;
import net.dhleong.acl.iface.Listener;
import net.dhleong.acl.iface.ThreadedArtemisNetworkInterface;
import net.dhleong.acl.protocol.*;
import net.dhleong.acl.protocol.core.ToggleShieldsPacket;
import net.dhleong.acl.protocol.core.comm.ToggleRedAlertPacket;

public class ArtemisProxy implements Runnable {

	public static ThreadedArtemisNetworkInterface server;
	
	public static void main(String[] args) {
		String serverAddr = args[0];
		int port = args.length > 1 ? Integer.parseInt(args[1]) : 2010;
		ArtemisProxy mainProxy = new ArtemisProxy(port, serverAddr);
		
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
				while ( ( len = this.in.read(buffer)) > -1 ) {
					System.out.println("Length: " + len);
					
					if (!commandString.contains("\n")) {
						commandString = commandString.concat(new String(buffer,0,len)).trim();
					} else {
						commandString = new String(buffer,0,len).trim();
					}
					
					if (commandString.equals("Toggle")) {
						shields();
						commandString = "";
					}
				}
			}
			catch ( IOException e ) {
				e.printStackTrace();
			}
		}
	}
	
	private int port;
	private String serverAddr;
	private int serverPort = 2010;
	//private PacketPersistingProxyDebugger debugger;
	
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
			
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("/dev/tty.usbserial-A700dWq6");
			if ( portIdentifier.isCurrentlyOwned() ) {
				System.out.println("Error: Port is currently in use");
			} else {
				CommPort commPort = portIdentifier.open("ArtemisProxy",2000);
				
				if ( commPort instanceof SerialPort ) {
					SerialPort serialPort = (SerialPort) commPort;
					serialPort.setSerialPortParams(115200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
					
					InputStream in = serialPort.getInputStream();
					OutputStream out = serialPort.getOutputStream();
					
					(new Thread(new SerialReader(in))).start();
				}
				else
				{
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
		ServerSocket listener = null;
		
		try {
			listener = new ServerSocket(this.port);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			/*
			if (listener != null && !listener.isClosed()) {
				try {
					listener.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			*/
		}
		
		while (true) {
			try {
			//listener.setSoTimeout(0);
			
			System.out.println("Listening for connections on port " + this.port + "...");
			Socket skt = listener.accept();
			
			System.out.println("Received connection from " + skt.getRemoteSocketAddress());
			ThreadedArtemisNetworkInterface client = new ThreadedArtemisNetworkInterface(skt, ConnectionType.CLIENT);
			
			client.setParsePackets(true);
			System.out.println("Connecting to server at " + serverAddr + ":" + serverPort + "...");
			server = new ThreadedArtemisNetworkInterface(serverAddr, serverPort);
			server.setParsePackets(false);
			
			new ProxyListener(server, client);
			System.out.println("Connection established.");
			
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public class ProxyListener {
		public ArtemisNetworkInterface server;
		public ArtemisNetworkInterface client;
		
		private ProxyListener(ArtemisNetworkInterface server, ArtemisNetworkInterface client) {
			this.server = server;
			this.client = client;
			server.addListener(this);
			client.addListener(this);
			server.start();
			client.start();
		}
		
		@Listener
		public void onDisconnect(DisconnectEvent event) {
			server.stop();
			client.stop();
			System.out.println("Disconnect: " + event);
			
			if (event.getException() != null) {
				event.getException().printStackTrace();
			}
		}

		@Listener
		public void onPacket(ArtemisPacket pkt) {
			ConnectionType type = pkt.getConnectionType();
			ArtemisNetworkInterface dest = type == ConnectionType.SERVER ? client : server;
			dest.send(pkt);
			
			if (type != ConnectionType.SERVER) {
				System.out.println(type + "> " + pkt);
			}
		}
		
	}
	
	@Command
	public String shields() {
		if (server != null && !server.isConnected()) {
		
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
		ToggleRedAlertPacket packet = new ToggleRedAlertPacket();
		server.send(packet);
		return "RED ALERT";
	}

	
}