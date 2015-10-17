
import java.io.*;
import java.net.*;
import java.nio.*;
import java.lang.*;
import java.util.*;

public class Client {
  public static void main(String[] args) throws Exception {
	  int portNum = 12235;
	  InetAddress ip=InetAddress.getByName("attu2.cs.washington.edu");
	  DatagramSocket sock = new DatagramSocket();

	  byte[] response = new byte[256];
	  String partA = "hello world\0";
	  byte[] buf = createBuffer(partA.getBytes(), 0, (short)1);
	  DatagramPacket packet = new DatagramPacket(buf, buf.length,
			  ip, portNum);
	  sock.send(packet);
	  
	  packet = new DatagramPacket(response, response.length);
	  sock.receive(packet);
	  for (int i = 0; i < 20; i++) {
		  System.out.println("" + response[i]);
	  }
//	  String got = new String(packet.getData(), 0, packet.getLength());
//	  System.out.println("GOT " + packet.getData());
  }
  
  
  // creates the header and puts in
  public static byte[] createBuffer(byte[] content, int secret, short step) {
	  int headerSize = 12;
	  short studentNum = 456; // my student number is 1340456
	  int paddedSize = content.length;
	  while (paddedSize % 4 != 0) {
		  paddedSize++;
	  }
	  ByteBuffer bb = ByteBuffer.allocate(headerSize + paddedSize);
	  bb.putInt(content.length);
	  bb.putInt(secret);
	  bb.putShort(step);
	  bb.putShort(studentNum);
	  bb.put(content);
	  return bb.array();
  }
  
  
}