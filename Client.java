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
	public static final int HEADER_SIZE = 12;
	public static final short STEP1 = 1;
	public static final short STEP2 = 2;

	public static void main(String[] args) throws Exception {
		////////////////////STAGE A//////////////////////////////////////////
		int portNum = 12235;
		InetAddress ip=InetAddress.getByName("attu2.cs.washington.edu");
    //InetAddress ip=InetAddress.getLocalHost();;
		DatagramSocket sock = new DatagramSocket();

		byte[] response = new byte[HEADER_SIZE + 16];
		String partA = "hello world\0";
		byte[] buf = createBuffer(partA.getBytes(), 0, STEP1);
		DatagramPacket packet = new DatagramPacket(buf, buf.length,
				ip, portNum);
		sock.send(packet);
		packet = new DatagramPacket(response, response.length);
		sock.receive(packet);
		int[] a2 = partA(response); // putting the 4 ints from server into int[]

		//	  for (int i = 0; i < a2.length; i++) {
		//		  System.out.println(a2[i]);
		//	  }

		//////////////////////STAGE B////////////////////////////////////////

		int numSent = 0;
		int send = a2[0];
		int len = a2[1];
		portNum = a2[2];
		int secretA = a2[3];
		sock.setSoTimeout(500); // .5s

		while (numSent < send) {
			// create packet
			buf = createBuffer(partB(numSent, len), secretA, STEP1);
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
				// if there's a timeout, try again
				continue;
			}
		}
		response = new byte[HEADER_SIZE + 8]; // 8 bytes -- 2 integers
		sock.receive(new DatagramPacket(response, response.length));
		ByteBuffer bb = ByteBuffer.wrap(response);
		// Header
		bb.getInt();
		bb.getInt();
		bb.getShort();
		bb.getShort();
		// reset the port number to the one given
		portNum = bb.getInt();
		int secretB = bb.getInt();
		// close the UDP socket
		sock.close();

		///////////////////////////STAGE C///////////////////////////////////
		Socket socket = new Socket(ip, portNum);
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		response = new byte[HEADER_SIZE + 16]; // 4 ints given to us this time
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
			byte[] b = createBuffer(buf, secretC, STEP1);
			out.write(b);
		}
		response = new byte[12 + 4]; // one integer. secretD plus header
		in.read(response);
		bb = ByteBuffer.wrap(response);
		int payloadLen =  bb.getInt();
	System.out.println("payload len of part d is:" + payloadLen);
		bb.getInt();
		bb.getInt();
		int secretD = bb.getInt();
		socket.close();
		in.close();
		out.close();
		System.out.println("The secrets:\nA: " + secretA);
		System.out.println("B: " + secretB);
		System.out.println("C: " + secretC);
		System.out.println("D: " + secretD);
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
		short studentNum = 456; // my student number is 1340456
		int paddedSize = content.length;
		while (paddedSize % 4 != 0) {
			paddedSize++;
		}
		ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + paddedSize);
		bb.putInt(content.length);
		bb.putInt(secret);
		bb.putShort(step);
		bb.putShort(studentNum);
		bb.put(content);
		return bb.array();
	} 
}
