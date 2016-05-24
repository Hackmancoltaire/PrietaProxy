import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;

import com.walkertribe.ian.util.BoolState;
import com.walkertribe.ian.enums.*;
import com.walkertribe.ian.iface.*;

import com.walkertribe.ian.world.*;
import com.walkertribe.ian.vesseldata.*;

import com.walkertribe.ian.protocol.*;
import com.walkertribe.ian.protocol.core.*;
import com.walkertribe.ian.protocol.core.setup.*;
import com.walkertribe.ian.protocol.core.world.*;
import com.walkertribe.ian.protocol.core.helm.*;
import com.walkertribe.ian.protocol.core.comm.*;
import com.walkertribe.ian.protocol.core.weap.*;

public class ProxyListener {
	public ArtemisNetworkInterface server;
	public ArtemisNetworkInterface client;
	public OutputStream out;
	public SystemManager mgr;
	
	public float maximumEnergy = 1000;

	public ProxyListener(ArtemisNetworkInterface server, ArtemisNetworkInterface client, OutputStream serialOut, SystemManager systemMgr) {
		this.mgr = systemMgr;
		this.server = server;
		this.client = client;
		this.out = serialOut;
		
		server.addListener(this);
		client.addListener(this);
		server.start();
		client.start();
	}
	
	@Listener
	public void onConnectSuccess(ConnectionSuccessEvent event) {
		System.out.println("Connected");
	}
	
	@Listener
	public void onDisconnect(DisconnectEvent event) {
		server.stop();
		client.stop();
		System.out.println("Disconnect: " + event);
	}
	
	@Listener
	public void onPacket(ArtemisPacket pkt) {
		ConnectionType type = pkt.getConnectionType();
		ArtemisNetworkInterface dest = type == ConnectionType.SERVER ? client : server;
		dest.send(pkt);
		
		if (type != ConnectionType.SERVER) {
			//System.out.println(type + "> " + pkt);
		}
	}
	
	@Listener
	public void onUpdatePacket(ObjectUpdatePacket pkt) {
		if (mgr != null) {
			ArtemisPlayer player = mgr.getPlayerShip(0);
			
			if (player != null) {
				if (player.getEnergy() > maximumEnergy) { maximumEnergy = player.getEnergy(); }
				
				// Player Energy
				float playerEnergyPercentage = player.getEnergy() / maximumEnergy;
				
				if (playerEnergyPercentage > 0.76) {
					new SerialWriter(this.out, "H");
				} else if (playerEnergyPercentage <= 0.76 && playerEnergyPercentage > 0.66) {
					new SerialWriter(this.out, "h");
				} else if (playerEnergyPercentage <= 0.66 && playerEnergyPercentage > 0.43) {
					new SerialWriter(this.out, "M");
				} else if (playerEnergyPercentage <= 0.43 && playerEnergyPercentage > 0.33) {
					new SerialWriter(this.out, "m");
				} else if (playerEnergyPercentage <= 0.33 && playerEnergyPercentage > 0.25) {
					new SerialWriter(this.out, "L");
				} else if (playerEnergyPercentage <= 0.25 && playerEnergyPercentage > 0.05) {
					new SerialWriter(this.out, "l");
				} else if (playerEnergyPercentage <= 0.05) {
					new SerialWriter(this.out, "x");
				} else { }
				
				// Torpedos
				boolean homingLoaded = false;
				boolean mineLoaded = false;
				boolean empLoaded = false;
				boolean nukeLoaded = false;
				
				for (int i=0; i < Artemis.MAX_TUBES; i++) {
					if (player.getTubeContents(i) == OrdnanceType.HOMING) { homingLoaded = true; }
					else if (player.getTubeContents(i) == OrdnanceType.MINE) { mineLoaded = true; }
					else if (player.getTubeContents(i) == OrdnanceType.EMP) { empLoaded = true; }
					else if (player.getTubeContents(i) == OrdnanceType.NUKE) { nukeLoaded = true; }
				}
				
				if (homingLoaded) {
					new SerialWriter(this.out, "T");
				} else if (player.getTorpedoCount(OrdnanceType.HOMING) != -1) {
					new SerialWriter(this.out, "t" + player.getTorpedoCount(OrdnanceType.HOMING));
				}
				
				if (mineLoaded) {
					new SerialWriter(this.out, "I");
				} else if (player.getTorpedoCount(OrdnanceType.MINE) != -1) {
					new SerialWriter(this.out, "i" + player.getTorpedoCount(OrdnanceType.MINE));
				}

				if (empLoaded) {
					new SerialWriter(this.out, "E");
				} else if (player.getTorpedoCount(OrdnanceType.EMP) != -1) {
					new SerialWriter(this.out, "e" + player.getTorpedoCount(OrdnanceType.EMP));
				}
				
				if (nukeLoaded) {
					new SerialWriter(this.out, "N");
				} else if (player.getTorpedoCount(OrdnanceType.NUKE) != -1) {
					new SerialWriter(this.out, "n" + player.getTorpedoCount(OrdnanceType.NUKE));
				}
				
				// Targeting
				if (player.getWeaponsTarget() > 1) { new SerialWriter(this.out, "A"); }
				else { new SerialWriter(this.out, "a"); }
				
				// Shields
				if (player.getShieldsState().getBooleanValue()) { new SerialWriter(this.out, "S"); }
				else { new SerialWriter(this.out, "s"); }
				
				// Red Alert
				if (player.getAlertStatus() == AlertStatus.RED) { new SerialWriter(this.out, "R"); }
				else { new SerialWriter(this.out, "r"); }
				
				// Movement
				if (player.getVelocity() > 0.0) { new SerialWriter(this.out, "U"); }
				else { new SerialWriter(this.out, "u"); }
			} else {
				System.out.print(".");
			}
		} else {
			System.out.println("No manager");
		}
		
		ConnectionType type = pkt.getConnectionType();
		ArtemisNetworkInterface dest = type == ConnectionType.SERVER ? client : server;
		dest.send(pkt);
	}
	
	/*
	@Listener
	public void onShields(ToggleShieldsPacket pkt) {
		
		if (mgr != null) {
			ArtemisPlayer player = mgr.getPlayerShip(0);
			
			if (player != null) {
		 
				if (player.getShieldsState().getBooleanValue()) {
					new SerialWriter(this.out, "s");
				} else {
					new SerialWriter(this.out, "S");
				}
			} else {
				System.out.println("Still waiting for player data...");
			}
		} else {
			System.out.println("No manager");
		}
		
	}
	*/
}