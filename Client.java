/*
 * Jinxing Wang and Tad Perry
 * 
 * CSE 461 Project 1 Stage 1
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.lang.*;
import java.util.*;

// This class handles the Client side of Project 1
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
	  int[] a2 = partA(response);
	  for (int i = 0; i < a2.length; i++) {
		  System.out.println(a2[i]);
	  }
  }
  
  
  // Takes the byte array received from the server and parses.
  // The server responds with a UDP packet containing 4 integers. The
  // integer array returned contains has those 4 integers.
  public static int[] partA(byte[] response) {
	  ByteBuffer bb = ByteBuffer.wrap(response);
	  int payload_len = bb.getInt();
	  int psecret = bb.getInt();
	  short step = bb.getShort();
	  if (step != 2) {
		  System.out.println("Something is wrong, step = " + step + " when it should equal 2");
	  }
	  short id = bb.getShort();
	  int[] res = new int[4];
	  for (int i = 0; i < res.length; i++) {
		  res[i] = bb.getInt();
	  }
	  return res;
  }
  
  
  
  // creates the header and pads the content returning a new byte[]
  // in the proper format. The secret is 4bytes (int), the step
  // is 2 bytes (short).
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