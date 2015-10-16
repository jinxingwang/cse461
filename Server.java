import java.io.*;
import java.net.*;
import java.nio.*;
import java.lang.*;
import java.util.*;

public class Server {
  public static void main(String[] args) throws Exception {
    Server s = new Server();
    s.runServer();
  }

  private int port = 11235;
  public void runServer() {
    try {
      DatagramSocket serverSocket = new DatagramSocket(port);

      while(1){
        byte[] buf = new byte[30];
        DatagramPacket packetUDP = new DatagramPacket(buf, buf.length);
        serverSocket.receive(packetUDP);
        Thread thread = new Thread(new serverConnection(serverSock, packet));
        thread.start();
      }

    } catch (Exception e) { e.printStackTrace(); }
  }

  public class serverConnection implements Runnable {
    private DatagramSocket socketUDP;
    private DatagramPacket packetUDP;
    private int pSecret = 0;

    serverConnection(DatagramSocket socketUDP, DatagramPacket packetUDP){
      this.socketUDP = socketUDP;
      this.packetUDP = packetUDP;
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

      if(!checkHeader(packetUDP.getData(), 12, (short)1)){
        return false;
      }
    }

    private boolean stageB(){
    }

    
    private boolean stageC(){

    }

    private boolean stageD(){

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

  }

}
