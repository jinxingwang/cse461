import java.io.*;
import java.net.*;
import java.nio.*;
import java.lang.*;
import java.util.*;

public class Server {
  private static Random rnum = new Random();
  private static final int HEADER_LEN = 12;
  private static final int TIMEOUT = 3000;
  private static final int START_PORT = 12235;

  public static void main(String[] args) throws Exception {
    Server s = new Server();
    s.runServer();
  }

  private int port = 11235;
  public void runServer() {
    try {
      DatagramSocket serverSocket = new DatagramSocket(port);

      while(true){
        byte[] buf = new byte[30];
        DatagramPacket packetUDP = new DatagramPacket(buf, buf.length);
        serverSocket.receive(packetUDP);
        Thread thread = new Thread(new serverConnection(serverSocket, packetUDP));
        thread.start();
      }

    } catch (Exception e) { e.printStackTrace(); }
  }

  public class serverConnection implements Runnable {
    private InetAddress ipAddress;
    private DatagramSocket socketUDP;
    private DatagramPacket packetUDP;
    private ServerSocket serverSock;
    private Socket tcpSock;
    private short sid;
    private byte c;
    private int tcpPort, port, len, num, secretA, secretB, secretC, secretD, pSecret = 0;

    serverConnection(DatagramSocket socketUDP, DatagramPacket packetUDP){
      this.socketUDP = socketUDP;
      this.packetUDP = packetUDP;
    }

    private void closeTCP() {
      try {
      serverSock.close();
      tcpSock.close();
      } catch (Exception e) {};
    }

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
    
    private boolean stageA(){
      System.out.println("+++++++++++++++++++++++++++++Stage A+++++++++++++++++++++++++++++");

      if(!checkHeader(packetUDP.getData(), 12, (short) 1)){
        return false;
      }
      ipAddress = packetUDP.getAddress();
      xPacket xp = new xPacket(ByteBuffer.wrap(packetUDP.getData()));
      bytesToHex(packetUDP.getData());
      if ("hello world".equals(new String(xp.payload).substring(0,11)) && pSecret == xp.secret){
        sid = xp.sid;
        // >49151 are private ports
        port = rnum.nextInt(49152);
        len = rnum.nextInt(80);
        num = rnum.nextInt(10) + 1;
        secretA = rnum.nextInt();

        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(num);
        buf.putInt(len);
        buf.putInt(port);
        buf.putInt(secretA);
        
        sendUDP(createBuffer(buf.array(), buf.array().length, (short) 2), packetUDP.getPort());
        return true;
      }
      return false;
    }

    private boolean stageB(){
      System.out.println("+++++++++++++++++++++++++++++Stage B+++++++++++++++++++++++++++++");
      
      ByteBuffer ackbuf = ByteBuffer.allocate(4);
      int packetId = 0;
      while(packetId < num){
        try { socketUDP.receive(packetUDP); } catch (Exception e) {e.printStackTrace();}
        if (!checkHeader(packetUDP.getData(), len + 4, (short) 1)){
          return false;
        }
        xPacket xp = new xPacket(ByteBuffer.wrap(packetUDP.getData()));
        if(!checkPacket(xp, packetId, (byte) 0 )){
          if (rnum.nextInt(10) != 6) {
            ackbuf.putInt(0, packetId++);
            sendUDP(createBuffer(ackbuf.array(), ackbuf.array().length, (short) 1), packetUDP.getPort());
          }
        }else{
          return false;
        }
      }

      ByteBuffer buf = ByteBuffer.allocate(8);
      tcpPort = rnum.nextInt(49152);
      secretB = rnum.nextInt();

      buf.putInt(tcpPort);
      buf.putInt(secretB);

      try {
        serverSock = new ServerSocket(tcpPort);
        serverSock.setSoTimeout(TIMEOUT);
      } catch(Exception e) {e.printStackTrace(); return false;}
      sendUDP(createBuffer(buf.array(), buf.array().length, (short) 2), packetUDP.getPort());
      return true;
    }

    
    private boolean stageC(){
      System.out.println("+++++++++++++++++++++++++++++Stage C+++++++++++++++++++++++++++++");
      try {
        System.out.println("tcp Port: " + tcpPort);
        tcpSock = serverSock.accept();
      } catch (Exception e) {
    	  e.printStackTrace();
    	  return false;
      }

      ByteBuffer buf = ByteBuffer.allocate(16);
      len = rnum.nextInt(80);
      num = rnum.nextInt(10) + 1;
      secretC = rnum.nextInt();
      c = (byte) 'c';

      buf.putInt(num);
      buf.putInt(len);
      buf.putInt(secretC);
      buf.put(c);

      try {
        sendBytes(createBuffer(buf.array(), 13, (short) 2));
      } catch (Exception e) {e.printStackTrace();}
      return true;
    }

    private boolean stageD(){
      System.out.println("+++++++++++++++++++++++++++++Stage A+++++++++++++++++++++++++++++");
      xPacket packet = null;
      return true; 
    }

    public byte[] createBuffer(byte[] content, int len, short step) {
      len = ((len+3)/4)*4;
      ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN + len);
      buf.putInt(len);
      buf.putInt(pSecret);
      buf.putShort(step);
      buf.putShort(sid);
      buf.put(content);
      return buf.array();
    }

    private boolean checkHeader(byte[] packet, int pLen, short step) {
      // buffer header
      ByteBuffer header = ByteBuffer.allocate(12);
      if(packet.length < 12){
	      return false;
      }

      byte[] headerBuf = Arrays.copyOfRange(packet, 0, 12);
      bytesToHex(headerBuf);
      header.put(headerBuf);

      header.rewind();

      int length = header.getInt();
      if(length != pLen){
        return false;
      }

      int secret = header.getInt();
      if(secret != pSecret){
        return false;
      }
      
      short s = header.getShort();
      if(s != step){
        return false;
      }

      return true;
    }

    private boolean checkPacket(xPacket packet, int packetId, byte c) {
      ByteBuffer payload = ByteBuffer.wrap(packet.payload);
      int packId = payload.getInt();
      if (packId != packetId){
        return false;
      }

      for (int i = 0; i < len; i++){
        if (payload.get() != c){
          return false;
        }
      }
      return true;
    }

    public void sendUDP(byte[] sendData, int port) {
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
      System.out.println("sendData: ");
      bytesToHex(sendData);
      System.out.println("port = " +  port);
      System.out.println("sendPacket addr. = " + sendPacket.getAddress());
      try {
        socketUDP.send(sendPacket);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void sendBytes(byte[] bytes) {
      try {
        System.out.println("sending : ");
        bytesToHex(bytes);
        OutputStream out = tcpSock.getOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.write(bytes, 0, bytes.length);
        dos.flush();
      } catch (Exception e) {e.printStackTrace();}
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
    String str = new String(hexChars);
    for(int i = 0; i < str.length(); i+=2) {
        System.out.print(str.charAt(i));
        if(i + 1 < str.length()) {
          System.out.print(str.charAt(i + 1));
        }
        System.out.print(" ");
    }
    System.out.println();
  }
}
