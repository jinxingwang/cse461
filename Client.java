/*
 * Jinxing Wang and Tad Perry
 * 
 * CSE 461 Project 1 Stage 1
 */

import java.io.*;
import java.net.*;
import java.nio.*;
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
//	  sock.close();
	  int[] a2 = partA(response);
//	  for (int i = 0; i < a2.length; i++) {
//		  System.out.println(a2[i]);
//	  }
	  int numSent = 0;
	  int send = a2[0];
	  int len = a2[1];
	  portNum = a2[2];
	  int secretA = a2[3];
	  
	  //////////////////////part2/////////////////////////////////////////
//	  sock = new DatagramSocket();
	  sock.setSoTimeout(500); // .5s

//	  sock.connect(ip, portNum);
//	  response = new byte[16]; // header + int size
	  // DatagramPacket resPack = new DatagramPacket(response, response.length);
	  while (numSent < send) {
		  // create packet
		  buf = createBuffer(partB(numSent, len), secretA, (short)1);
		  packet = new DatagramPacket(buf, buf.length, ip, portNum);
		  sock.send(packet);
		  // send packet
		  try {
			  sock.receive(new DatagramPacket(response, response.length));
			  numSent++;
			  ByteBuffer temp = ByteBuffer.wrap(response);
			  temp.getInt();
			  temp.getInt();
			  temp.getInt();
//			  System.out.println(temp.getInt());  // For debug. See if counts up by 1
		  } catch (SocketTimeoutException e) {
			  continue;
		  }
	  }
	  response = new byte[20];
	  sock.receive(new DatagramPacket(response, response.length));
	  ByteBuffer bb = ByteBuffer.wrap(response);
	  // Header
	  bb.getInt();
	  bb.getInt();
	  bb.getShort();
	  bb.getShort();
	  // data sent
	  portNum = bb.getInt();
	  int secretB = bb.getInt();
//	  System.out.println(portNum);
//	  System.out.println(secretB);
	  sock.close();
	  Socket socket = new Socket(ip, portNum);
	  InputStream in = socket.getInputStream();
	  OutputStream out = socket.getOutputStream();
	  response = new byte[12 + 16]; // 4 ints given to us this time
	  in.read(response);
	  bb = ByteBuffer.wrap(response);
	  // Header
	  bb.getInt();
	  bb.getInt();
	  bb.getInt();
	  // num2 len2 secretC and char c
	  
	  numSent = bb.getInt();
	  len = bb.getInt();
	  int secretC = bb.getInt();
	  // stage d
	  byte c = (byte)bb.getChar();
	  buf = new byte[len];
	  Arrays.fill(buf, c);
	  for (int i = 0; i < numSent; i++) {
		  byte[] b = createBuffer(buf, secretC, (short)1);
		  out.write(b);
	  }
	  response = new byte[12 + 4]; // one integer. secretD plus header
	  in.read(response);
	  bb = ByteBuffer.wrap(response);
	  bb.getInt();
	  bb.getInt();
	  bb.getInt();
	  int secretD = bb.getInt();
	  socket.close();
	  in.close();
	  out.close();
	  System.out.println("I did it all yo if this is read " + secretD);
  }
  
  
  
  
  // returns byte array of content meant to be sent in packet b1
  public static byte[] partB(int packet_id, int len) {
	  ByteBuffer bb = ByteBuffer.allocate(len + 4);
	  bb.putInt(packet_id);
	  return bb.array();
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