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
    	String editedReq = request.substring(0, request.length() - 1);
    	editedReq += "0";
    	// Print out first line
    	System.out.println(">>> " + editedReq);
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
					String temp = request.substring(7);
					String[] splitted = temp.split(":");
					url = splitted[0];
					if (splitted.length == 2) { // there is a portnumber given with the host
						portNum = Integer.parseInt(splitted[1]);
					}
				}
				if (!request.equals("\n"))
					editedReq += "\n";
				editedReq += request; // build up header to send to server
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Socket socket = null;
		InputStream hostIn = null;
		OutputStream hostOut = null;
		//////////////////////////////////////////////////////////////////////
		try {
			socket = new Socket(url, portNum);
			hostIn = socket.getInputStream();
			hostOut = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		OutputStreamWriter toServer = new OutputStreamWriter(hostOut);
		// Send the header to the url.
		for (int i = 0; i < editedReq.length(); i++) {
			try {
				System.out.print(editedReq.charAt(i));
				toServer.write((int)editedReq.charAt(i));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			toServer.write((int)'\n');
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		
		try {
			System.out.println(" I am here " + hostIn.read());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		in = new BufferedReader(new InputStreamReader(hostIn));
		try {
			in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
  }
}
