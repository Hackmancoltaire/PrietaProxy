import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;

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
	public void onUpdatePacket(MainPlayerUpdatePacket pkt) {
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
				if (player.getRedAlertState().getBooleanValue()) { new SerialWriter(this.out, "R"); }
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