/*
 * Jinxing Wang (1468230) and Tad Perry (1340456)
 *
 * CSE 461 Project 1 Stage 1
 */
import java.io.*;
import java.net.*;
import java.nio.*;
import java.lang.*;
import java.util.*;

// This class handles the Server side of Project 1
public class Server {
  private static Random randy = new Random();
  private static final int TIMEOUT = 3000;
  private static final int HEADER_LEN = 12;
  private static final int START_PORT = 12235;

  public static void main(String[] args) throws Exception {
    Server s = new Server();
    s.runServer();
  }

  // run server with muti thread
  public void runServer() {
    try {
      DatagramSocket serverSocket = new DatagramSocket(START_PORT);

      while(true){
        byte[] buf = new byte[100];
        DatagramPacket packetUDP = new DatagramPacket(buf, buf.length);
        serverSocket.receive(packetUDP);
        Thread thread = new Thread(new ServerConnection(serverSocket, packetUDP));
        thread.start();
      }
    } catch (Exception e) { e.printStackTrace(); }
  }

  public class ServerConnection implements Runnable {
    private InetAddress ipAddress;
    private DatagramSocket socketUDP;
    private DatagramPacket packetUDP;
    private ServerSocket serverSock;
    private Socket tcpSock;
    private short sid;
    private byte c;
    private int tcpPort, port, len, num, secretA, secretB, secretC, secretD, pSecret = 0;

    private void closeTCP() {
      try {
      serverSock.close();
      tcpSock.close();
      } catch (Exception e) {};
    }

    ServerConnection(DatagramSocket s, DatagramPacket p) {
      socketUDP = s;
      packetUDP = p;
    }

    // override run fun for thread to run
    @Override
    public void run () {
      if (!stageA())
        return;
      pSecret = secretA;
      if (!stageB())
        return;
      pSecret = secretB;
      if (!stageC()) {
        closeTCP();
        return;
      }
      pSecret = secretC;
      if (!stageD()) {
        closeTCP();
        return;
      }
    }

    private boolean stageA() {
      // in stage A right now
      if (!checkHeader(packetUDP.getData(), 12, (short) 1))
        return false;
      ipAddress = packetUDP.getAddress();
      xPacket p = new xPacket(ByteBuffer.wrap(packetUDP.getData()));
      // start revieved message
      bytesToHex(packetUDP.getData());
      if ("hello world".equals(new String(p.payload).substring(0,11))
            && pSecret == p.secret)
      {
        sid = p.sid;
        port = randy.nextInt(49151);
        len = randy.nextInt(80);
        num = randy.nextInt(10) + 1;
        secretA = randy.nextInt();

        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(num);
        buf.putInt(len);
        buf.putInt(port);
        buf.putInt(secretA);

        sendUDP(generatePacket((short) 2, buf.array(), buf.array().length), packetUDP.getPort());
        return true;
      }
      return false;
    }

    private boolean stageB() {
      // starting stage B
      try {
        socketUDP = new DatagramSocket(port);
        //don't ack the first packet.
        socketUDP.receive(packetUDP);
      } catch (Exception e) {e.printStackTrace();}

      ByteBuffer ackbuf = ByteBuffer.allocate(4);
      int packetId = 0;
      while (packetId < num) {
        try { socketUDP.receive(packetUDP); } catch (Exception e) {e.printStackTrace();}
        if (!checkHeader(packetUDP.getData(), len + 4, (short) 1))
          return false;
        xPacket pack = new xPacket(ByteBuffer.wrap(packetUDP.getData()));
        if (checkPacket(pack, packetId, len + 4, (byte)0)) {
          if (randy.nextInt(10) > 1) {
            ackbuf.putInt(0, packetId++);
            sendUDP(generatePacket((short) 1, ackbuf.array(), ackbuf.array().length), packetUDP.getPort());
          }
        }
        else {
          return false;
        }
      }

      ByteBuffer buf = ByteBuffer.allocate(8);
      tcpPort = randy.nextInt(49151);
      secretB = randy.nextInt();

      buf.putInt(tcpPort);
      buf.putInt(secretB);
      try {
        serverSock = new ServerSocket(tcpPort);
        serverSock.setSoTimeout(TIMEOUT);
      } catch(Exception e) {e.printStackTrace(); return false;}
      sendUDP(generatePacket((short) 2, buf.array(), buf.array().length), packetUDP.getPort());
      return true;
    }

    private boolean stageC() {
      // starting stage C
      try {
        // accepting tcpsocket
        tcpSock = serverSock.accept();
      } catch (Exception e) {
    	  e.printStackTrace();
    	  return false;
      }

      ByteBuffer buf = ByteBuffer.allocate(16);
      len = randy.nextInt(80);
      num = randy.nextInt(10) + 1;
      secretC = randy.nextInt();
      c = (byte) randy.nextInt(256);

      buf.putInt(num);
      buf.putInt(len);
      buf.putInt(secretC);
      buf.put(c);

      try {
	  sendBytes(generatePacket((short) 2, buf.array(), 13));
      } catch (Exception e) {e.printStackTrace();}
      return true;
    }

    private boolean stageD() {
      // staring stage D
      xPacket pack = null;
      try {
        for (int i = 0; i < num; i++) {
          byte[] res = readBytes((((HEADER_LEN + len) + 3) / 4) * 4);
          if (res == null || !checkHeader(res, len, (short) 1))
            return false;
          pack = new xPacket(ByteBuffer.wrap(res));
          // pack.print();
          if (!checkPacket(pack, -1, len, c))
            return false;
        }
      } catch (Exception e) {
    	  e.printStackTrace();
    	  return false;
      }

      secretD = randy.nextInt();
      ByteBuffer buf = ByteBuffer.allocate(4);

      buf.putInt(secretD);
      sendBytes(generatePacket((short) 2, buf.array(), buf.array().length));
      return true;
    }

    private byte[] generatePacket(short step, byte[] payload, int plength) {
      int len = ((plength + 3) / 4) * 4;
      ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN + len);
      buf.putInt(plength);
      buf.putInt(pSecret);
      buf.putShort(step);
      buf.putShort(sid);
      buf.put(payload);
      return buf.array();
    }

   private boolean checkPacket(xPacket pack, int packetId, int expectedLength, byte c) {
      ByteBuffer payload = ByteBuffer.wrap(pack.payload);
      if (packetId > 0) {
        int packId = payload.getInt();
        if (packId != packetId)
          return false;
      }
      for (int i = 0; i < len; i++)
        if (payload.get() != c)
          return false;
      return true;
    }

   private boolean checkHeader(byte[] packet, int exPayloadLen, short exStep) {
     // extract header
     ByteBuffer header = ByteBuffer.allocate(12);
     if(packet.length < 12)
	     return false;

     byte[] subPacket = Arrays.copyOfRange(packet, 0, 12);
     bytesToHex(subPacket);
     header.put(subPacket);
     // need to reset pointer
     header.rewind();

     int length = header.getInt();
     if (length != exPayloadLen)
       return false;
     int secret = header.getInt();
     if (secret != pSecret)
       return false;
     short step = header.getShort();
     if (step != exStep)
       return false;
     return true;
   }

    public void sendUDP(byte[] sendData, int port) {
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
      // data are:
      bytesToHex(sendData);

      try {
        socketUDP.send(sendPacket);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void sendBytes(byte[] bytes) {
      try {
        // sedning data here
        bytesToHex(bytes);
        OutputStream out = tcpSock.getOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.write(bytes, 0, bytes.length);
        dos.flush();
      } catch (Exception e) {e.printStackTrace();}
    }

    public byte[] readBytes(int len) throws IOException {
      InputStream in = tcpSock.getInputStream();
      DataInputStream dis = new DataInputStream(in);
      byte[] data = new byte[len];
      try {
          dis.readFully(data);
      } catch(Exception e) {
          return null;
      }
      return data;
    }

    private class xPacket {
      public int length;
      public int secret;
      public short step;
      public short sid;
      public byte[] payload;

      xPacket(ByteBuffer b) {
        length = b.getInt();
        secret = b.getInt();
        step = b.getShort();
        sid = b.getShort();
        payload = new byte[length];
        b.get(payload, 0, length);
      }

      public void print() {
        System.out.println(
          "length: " + length
        + "\nsecret: " + secret
        + "\n step: " + step
        + "\n sid: " + sid);
        bytesToHex(payload);
      }
    }
  }

  public static void bytesToHex(byte[] bytes) {
    char[] hexArray = "0123456789ABCDEF".toCharArray();
    char[] hexChars = new char[bytes.length * 2];
    int v;
    for ( int j = 0; j < bytes.length; j++ ) {
        v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
  }
}

