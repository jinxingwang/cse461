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
    private Socket servSock = null;
    
    
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
      InputStream clientIn = null;
      OutputStream clientOut = null;
      try {
        clientIn = con.getInputStream();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      try {
        clientOut = con.getOutputStream();
      } catch (IOException e1) {
        e1.printStackTrace();
      }

      // Buffered reader to read header line by line
      BufferedReader in = new BufferedReader(new InputStreamReader(clientIn));
      String request = "";
      try {
        request = in.readLine();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // change version number to 1.0
      System.out.println("The request is " + request);
      String editedReq = request.substring(0, request.length() - 1);
      editedReq += "0";
      // Print out first line
      System.out.println(">>> " + editedReq);

      // HTTP CONNECT tunneling
      if(editedReq.substring(0,7).equalsIgnoreCase("connect")){
        
      }

      String url = "";
      int portNum = 80; // default if no other portnumbers are found
      try {
        int c = 0;
        while (true) {
          request = in.readLine();
          // clientIn was making the request==null never happen, and yet never
          // was giving new lines. This is the only way I found to break out the loop
          if (request == null || request.trim().length() == 0) break;
          // Change connection to close
          if (request.length() >= 12 && request.substring(0, 12).equalsIgnoreCase("Connection: ")) {
            request = "Connection: close";
          // save the host in an url so we can connect via tcp
          } else if (request.length() >= 7 && request.substring(0, 6).equalsIgnoreCase("Host: ")) {
            // change to 6, because index 6 is beganing of addr, if 7 that will take first 'w' of of the addr
            String temp = request.substring(6);
            String[] splitted = temp.split(":");
            url = splitted[0];
            if (splitted.length == 2) { // there is a portnumber given with the host
              portNum = Integer.parseInt(splitted[1]);
            }
          }
          // Add the newline at the end of the line
          editedReq += "\r\n";
          editedReq += request; // build up header to send to server
        }
        editedReq += "\r\n\r\n";
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (url.substring(0, 5).equalsIgnoreCase("https")) {
    	  portNum = 443;
      }
      Socket socket = null;
      InputStream hostIn = null;
      OutputStream hostOut = null;

      try {
        socket = new Socket(url, portNum);
        hostIn = socket.getInputStream();
        hostOut = socket.getOutputStream();
      } catch (IOException e) {
        e.printStackTrace();
      }
/*      // OutputStreamWriter toServer = new OutputStreamWriter(hostOut);
      // Send the header to the url.
      for (int i = 0; i < editedReq.length(); i++) {
        try {
          System.out.print(editedReq.charAt(i));
           toServer.write((int)editedReq.charAt(i));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }*/

      // OutputStreamWriter toServer = new OutputStreamWriter(hostOut);
      // Send the header to the url.
     byte[] req = editedReq.getBytes();
      try {
        hostOut.write(req, 0, req.length);
        hostOut.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
      while (true) tunnel(hostOut, clientOut, hostIn, clientIn);

//      byte[] buffer = new byte[2048];
//      int bytes_read;
//      try {
//        while((bytes_read = hostIn.read(buffer)) != -1) {
////          System.out.print(buffer);
//           clientOut.write(buffer, 0, bytes_read);
//           clientOut.flush();
//        }
//      }catch (IOException e) {}
//      try {
//          while((bytes_read = clientIn.read(buffer)) != -1) {
//            System.out.print(buffer);
//             clientIn.read(buffer, 0, bytes_read);
//          }
//      }catch (IOException e) {}
  
//      for (int i = 0; i < 2048; i++) {
//    	  System.out.print("" + (char)buffer[i]);
//      }
    }
    private void tunnel(OutputStream hostOut, OutputStream clientOut,
    		InputStream hostIn, InputStream clientIn) {
        byte[] buffer = new byte[2048];
        int bytes_read;
        // write out to client from server
        try {
          while((bytes_read = hostIn.read(buffer)) != -1) {
//            System.out.print(buffer);
             clientOut.write(buffer, 0, bytes_read);
             clientOut.flush();
          }
        }catch (IOException e) {}
        // write out to host from client
        try {
            while((bytes_read = clientIn.read(buffer)) != -1) {
              System.out.print(buffer);
               hostOut.write(buffer, 0, bytes_read);
               hostOut.flush();
            }
        }catch (IOException e) {}
    }
  }
}
