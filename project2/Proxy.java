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
    private Socket clientSocket;
    
    // constrctor
    public uHandler(Socket sock) {
      if(sock != null){
        clientSocket = sock;
      } else {
        throw new IllegalArgumentException();
      }
    }
    
    @Override
    public void run() {
      InputStream clientIn = null;
      OutputStream clientOut = null;
      try {
        clientIn = clientSocket.getInputStream();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      try {
        clientOut = clientSocket.getOutputStream();
      } catch (IOException e1) {
        e1.printStackTrace();
      }

      // Buffered reader to read header line by line
      BufferedReader in = new BufferedReader(new InputStreamReader(clientIn));
      while(true){
      String url = "";
      String request = null;
      while(request == null){
        try {
          request = in.readLine();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      

      // HTTP CONNECT tunneling
      if(request.substring(0,7).equalsIgnoreCase("connect")){
        System.out.println(request);
        tunnel(clientIn,clientOut,request);
        // close socket
        System.out.println(" tunneling connection closed");
        break;
      }
      
      // get addr and port
      String[] splitted = request.split(" ");
      if( splitted.length < 2){
        System.out.println("wrong CONNECT request format");
        continue;
      }
      url = splitted[1];

      // get current time for print out
      Date d = new Date();
      // formate current time
      SimpleDateFormat ft = new SimpleDateFormat("dd MMM hh:mm:ss");

      // change version number to 1.0
      // System.out.println("The request is " + request);
      String editedReq = request.substring(0, request.length() - 1);
      editedReq += "0";
      // Print out first line
      if(editedReq.substring(0,3).equalsIgnoreCase("get")){
        System.out.println(ft.format(d) + " - >>> " + editedReq.substring(0,editedReq.length() - 8));
      }

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
          } else if (request.length() >= 17 && request.substring(0, 17).equalsIgnoreCase("Proxy-connection:")) {
            request = "Proxy-connection: close";
          // save the host in an url so we can connect via tcp
          } 
          /*else if (request.length() >= 7 && request.substring(0, 6).equalsIgnoreCase("Host: ")) {
            String temp = request.substring(6);
            String[] splitted = temp.split(":");
            url = splitted[0];
            if (splitted.length == 2) { // there is a portnumber given with the host
              portNum = Integer.parseInt(splitted[1]);
            }
          }*/
          // Add the newline at the end of the line
          editedReq += "\r\n";
          editedReq += request; // build up header to send to server
        }
        editedReq += "\r\n\r\n";
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (!url.contains("http")) {
        continue;
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
      }
     
     byte[] req = editedReq.getBytes();
      try {
        System.out.println(req);
        hostOut.write(req);
        hostOut.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }

      byte[] buffer = new byte[2048];
      int bytes_read;
      try {
        while((bytes_read = hostIn.read(buffer)) != -1) {
//          System.out.print(buffer);
           clientOut.write(buffer);
           clientOut.flush();
        }
      } catch (IOException e) {}
      try{
        clientIn.close();
        clientOut.close();
        clientSocket.close(); 
        System.out.println(" non-connection closed");
      } catch (IOException e) { /* failed */ }

    }
    }
    private void tunnel(InputStream clientIn,OutputStream clientOut, String request) {
      String tunnelHost, temp;
      int tunnelPort , buf;
      Socket serverSocket = null;
      InputStream serverIn = null;
      OutputStream serverOut = null;
      byte[] rsp;

      // cut off Connect at beganing
      request = request.substring(8,request.length());
      
      // get addr and port
      String[] splitted = request.split(":");
      if( splitted.length != 2){
        System.out.println("wrong CONNECT request format");
        return;
      }
      tunnelHost = splitted[0];
      tunnelPort = Integer.parseInt(splitted[1]);

      // try to connect to server
      try {
        serverSocket  = new Socket(tunnelHost, tunnelPort);
        temp = "HTTP/1.0 200 OK\r\n\r\n";
        rsp = temp.getBytes();
        try {
          clientOut.write(rsp);
          clientOut.flush();
        }catch (IOException e) {}
      }  catch (IOException e1) {
        temp = "HTTP/1.0 502 Bad Gateway\r\n\r\n";
        rsp = temp.getBytes();
        try {
          clientOut.write(rsp);
          clientOut.flush();
        } catch (IOException e) {}
        return;
      }
      
      try {
        serverIn = serverSocket.getInputStream();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      try {
        serverOut = serverSocket.getOutputStream();
      } catch (IOException e1) {
        e1.printStackTrace();
      }

      try {
        while((clientIn.available() > 0) || (serverIn.available() > 0)) {
          buf = clientIn.read();
          while(buf != -1) {
            serverOut.write(buf);
            buf = clientIn.read();
          }
          serverOut.flush();
          buf = serverIn.read();
          while(buf != -1){
            clientOut.write(buf);
          }
          clientOut.flush();
          if((clientIn.available() == 0) && (serverIn.available() == 0)){
            // wait 100ms for respons.
            try {
              Thread.sleep(100);
            } catch(InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          }
        }
      } catch(IOException e) {
        e.printStackTrace(System.err);
      }finally {
        try {
          serverIn.close();
          serverOut.close();
          serverSocket.close();
          System.out.print("server socket closed");
        } catch (IOException e) { /* failed */ }
      } 

    }
  }
}
