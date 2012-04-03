
package j9p.example;

import j9p.ns.handlers.ProcessFile;


public class Counter implements ProcessFile.Listener {

	private long counter = 0;
	
	public void asInput(byte[] data, long offset) {
		counter += data.length;
	}

	public byte[] getOutput(long offset, int count) {
		if (offset != 0) return null;
		return (counter + "\n").getBytes();
	}
}
