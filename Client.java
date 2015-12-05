/*
 * Jinxing Wang (1468230) and Tad Perry (1340456)
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
//        InetAddress ip=InetAddress.getLocalHost();
        InetAddress ip=InetAddress.getByName("attu2.cs.washington.edu");

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

        //      for (int i = 0; i < a2.length; i++) {
        //          System.out.println(a2[i]);
        //      }

        //////////////////////STAGE B////////////////////////////////////////

        int numSent = 0;
        int send = a2[0];
        int len = a2[1];
        portNum = a2[2];
        int secretA = a2[3];
        System.out.println("The secrets:\nA: " + secretA);

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
                checkHeader(temp, 4, secretA, STEP1);
                //              System.out.println(temp.getInt());  // For debug. See if counts up by 1
            } catch (SocketTimeoutException e) {
                // if there's a timeout, try again
                continue;
            }
        }
        response = new byte[HEADER_SIZE + 8]; // 8 bytes -- 2 integers
        sock.receive(new DatagramPacket(response, response.length));
        ByteBuffer bb = ByteBuffer.wrap(response);
        // Header
        checkHeader(bb, 8, secretA, STEP2);

        // reset the port number to the one given
        portNum = bb.getInt();
        int secretB = bb.getInt();
        System.out.println("B: " + secretB);

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
        checkHeader(bb, 13, secretB, STEP2);

        // num2 len2 secretC and char c

        numSent = bb.getInt();
        len = bb.getInt();
        int secretC = bb.getInt();
        System.out.println("C: " + secretC);

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
        checkHeader(bb, 4, secretC, STEP2);

        int secretD = bb.getInt();
        socket.close();
        in.close();
        out.close();
        System.out.println("D: " + secretD);
    }




    // returns byte array of content meant to be sent in packet b1
    public static byte[] partB(int packet_id, int len) {
        ByteBuffer bb = ByteBuffer.allocate(len + 4);
        bb.putInt(packet_id);
        return bb.array();
    }

    public static void checkHeader(ByteBuffer b, int length, int psecret, short step) {
        int l = b.getInt();
        int ps = b.getInt();
        int s = b.getShort();
        b.getShort(); // the student number, which I am not checking.
        if (l != length) {
            System.out.println("Packet length was " + l + " instead of " + length);
        }
        if (ps != psecret) {
            System.out.println("Psecret was " + ps + " instead of " + psecret);
        }
        if (s != step) {
            System.out.println("Packet length was " + s + " instead of " + step);
        }
    }



    // Takes the byte array received from the server and parses.
    // The server responds with a UDP packet containing 4 integers. The
    // integer array returned contains has those 4 integers.
    public static int[] partA(byte[] response) {
        ByteBuffer bb = ByteBuffer.wrap(response);
        checkHeader(bb, 16, 0, STEP2);
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
