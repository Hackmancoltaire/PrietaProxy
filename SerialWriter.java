import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;

import java.io.IOException;

public class SerialWriter implements Runnable {
	OutputStream out;
	
	public SerialWriter(OutputStream out, String outText) {
		this.out = out;
		
		try {
			if (outText != "") {
				//System.out.print(outText);
				this.out.write(outText.getBytes());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run () { }
}
