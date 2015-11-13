import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Proxy {
    
  public static void main(String[] args) {
    // check if given a port number
    if (args.length != 1) {
      System.out.println("Usage: java Server <port number>");
      System.exit(1);
    }
        
    try {
      httpProxy(Integer.parseInt(args[0]));
    } catch (NumberFormatException e) {
      // Argument is in incorrect format
      System.out.println("Incorrect port number");
    }
  }
    
  private static void httpProxy(int port) {
    try {
      // create a socket and bind with given port
      ServerSocket socket = new ServerSocket(port);
      // get local addr for print out
      String addr = socket.getLocalSocketAddress().toString();
      // get current time for print out
      Date d = new Date();
      // formate current time
      SimpleDateFormat ft = new SimpleDateFormat("dd MMM hh:mm:ss");
      System.out.println(ft.format(d) + " - Proxy listening on " + addr.substring(8));

      while (true) {
        // listen and accept connections
        Socket sock = socket.accept();
        // use Uhandler to handle each connection
        Thread thread = new Thread(new uHandler(sock));
        thread.start();
      }
    } catch (IOException e) {
      System.out.println(e);
    }
  }
  
  private static class uHandler implements Runnable {
    private Socket con;
    
    // constrctor
    public uHandler(Socket sock) {
      if(sock != null){
        con = sock;
      } else {
        throw new IllegalArgumentException();
      }
    }

    @Override
    public void run() {
    }
  }
}
