import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;

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
      BufferedReader in = null;
      String request = null;
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
      in = new BufferedReader(new InputStreamReader(clientIn));
        request = null;
        while(request == null && clientSocket.isConnected()){
          try {
            request = in.readLine();
          } catch (IOException e) {
            try{
              clientIn.close();
              clientOut.close();
              clientSocket.close();
              System.out.println("non-connection closed");
            } catch (IOException e1) { /* failed */ }
            return;
          }
        }
        if(request.contains("GET") || request.contains("HEAD")
            || request.contains("POST") || request.contains("PUT")
            || request.contains("DELETE") || request.contains("TRACE")
            || request.contains("CONNECT")){
              // HTTP CONNECT tunneling
          if(request.substring(0,7).equalsIgnoreCase("connect")){
            tunnel(clientSocket,clientIn,clientOut,request);
          }else{
            non_connection(clientIn,clientOut,in,request);
          }
        }
      try{
        clientIn.close();
        clientOut.close();
        clientSocket.close();
        // System.out.println(" non-connection closed");
      } catch (IOException e) { /* failed */ }
    }

    private void non_connection(InputStream clientIn,OutputStream clientOut, BufferedReader in, String request) {
      String[] splitted;
      String url = "", editedReq, addr;
      Date d = new Date();
      SimpleDateFormat ft;
      Socket socket = null;
      InputStream hostIn = null;
      OutputStream hostOut = null;
      int portNum;
      byte[] req,buffer;

      // get addr and port
      splitted = request.split(" ");
      if(splitted.length < 2){
        System.out.println(request);
        System.out.println("wrong non-CONNECT request format");
        return;
      }
      addr = splitted[1];

      // formate current time
      ft = new SimpleDateFormat("dd MMM hh:mm:ss");

      // change version number to 1.0
      editedReq = request.substring(0, request.length() - 1);
      editedReq += "0\r\n";
      // Print out first line
      System.out.println(ft.format(d) + " - >>> "+editedReq.substring(0,editedReq.length() - 10));

      portNum = 80; // default if no other portnumbers are found
      try {
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
          }else if (request.length() >= 7 && request.substring(0, 6).equalsIgnoreCase("Host: ")) {
            String temp = request.substring(6);
            splitted = temp.split(":");
            url = splitted[0];
            if (splitted.length == 2) { // there is a portnumber given with the host
              portNum = Integer.parseInt(splitted[1]);
            }
          }
          // Add the newline at the end of the line
          editedReq += request+"\r\n"; // build up header to send to server
        }
        editedReq += "\r\n";
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (addr.substring(0, 5).equalsIgnoreCase("https")) {
        portNum = 443;
      }
      if(url == ""){
        url = addr;
      }
      try {
        socket = new Socket(url, portNum);
        hostIn = socket.getInputStream();
        hostOut = socket.getOutputStream();
      } catch (IOException e) {
        System.out.print("error in non-connection try to connect socket. url:"+url+"    port:"+portNum);
        e.printStackTrace();
      }

      req = editedReq.getBytes();
      try {
        // System.out.println(req);
        hostOut.write(req);
        hostOut.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }

      buffer = new byte[2048];
      int bytes_read;
      try {
        while((bytes_read = hostIn.read(buffer)) != -1) {
           // System.out.print(buffer);
           clientOut.write(buffer,0,bytes_read);
        }
        clientOut.flush();
      } catch (IOException e) {
        return;
      }
      return;
    }

    private void tunnel(Socket clientSocket, InputStream clientIn,OutputStream clientOut, String request) {
      String tunnelHost, temp, editedReq;
      int tunnelPort , i;
      Socket serverSocket = null;
      InputStream serverIn = null;
      OutputStream serverOut = null;
      byte[] rsp, buf;
      Date d = new Date();
      SimpleDateFormat ft;

      ft = new SimpleDateFormat("dd MMM hh:mm:ss");

      // change version number to 1.0
      editedReq = request.substring(0, request.length() - 1);
      editedReq += "0\r\n";
      // Print out first line
      System.out.println(ft.format(d) + " - >>> "+editedReq.substring(0,editedReq.length() - 10));
      // cut off Connect at beganing
      request = request.substring(8,request.length());

      // get addr and port
      String[] splitted = request.split(" ");
      String x = splitted[0];
      String[] splitted2 = x.split(":");
      if( splitted.length != 2){
        System.out.println("wrong CONNECT request format");
        return;
      }
      tunnelHost = splitted2[0];
      tunnelPort = Integer.parseInt(splitted2[1]);

      // try to connect to server
      try {
        serverSocket  = new Socket(tunnelHost, tunnelPort);
      }  catch (IOException e1) {}
      if(serverSocket.isConnected()){
        temp = "HTTP/1.0 200 OK\r\n\r\n";
        rsp = temp.getBytes();
        try {
          clientOut.write(rsp);
          clientOut.flush();
        }catch (IOException e) {}
      }else{
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

        Thread thread1 = new Thread(new wrder(clientIn,serverOut,serverSocket));
        thread1.start();
        Thread thread2 = new Thread(new wrder(serverIn,clientOut,clientSocket));
        thread2.start();

        while(serverSocket.isConnected() && clientSocket.isConnected()) {
        }
        try {
          serverIn.close();
          serverOut.close();
          serverSocket.close();
          System.out.println("server socket closed");
        } catch (IOException e) { /* failed */ }

    }
  }

  private static class wrder implements Runnable {
    private Socket socket = null;
    private InputStream In = null;
    private OutputStream Out = null;

    public wrder(InputStream x, OutputStream y, Socket z) {
      if(x != null && y != null && z != null){
        In = x;
        Out = y;
        socket = z;
      } else {
        throw new IllegalArgumentException();
      }
    }

    @Override
    public void run() {
      byte[] buf = new byte[2048];
      int i;
      try {
        while(((i = In.read(buf,0,buf.length))!=-1) && socket.isConnected() && ((i = In.read(buf,0,buf.length))!=0)){
          System.out.print(buf);
          Out.write(buf);
          Out.flush();
        }
      } catch(IOException e) {
        return;
        // e.printStackTrace();
      }
    }
  }
}

