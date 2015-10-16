nclude <arpa/inet.h>
#include <assert.h>
#include <errno.h>
#include <netdb.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#include <iostream>
#include <string>

using std::string;

void Usage(char *progname);
int  ListenTCP(char *portnum);
int  ListenUDP(char *portnum);
void *HandleClient(void *c_info);
bool ReadTCP(int fd, string *retstr);
bool ReadUDP(int fd, string *retstr);

int main(int argc, char **argv) {
  int port = atoi("12235");
  char buf[16];

  int listen_fd = ListenUDP(&port[0]);
  if (listen_fd <= 0) {
    // We failed to bind/listen to a socket.  Quit with failure.
    std::cerr << "Couldn't bind to any addresses." << std::endl;
    return EXIT_FAILURE;
  }
  
  clientlen = sizeof(clientaddr);
  // Loop forever, accepting client connections and dispatching them.
  while (1) {
    /*
     * recvfrom: receive a UDP datagram from a client
     */
    bzero(buf, BUFSIZE);
    ClientInfo *cinfo = new ClientInfo;
    cinfo->caddr_len = sizeof(cinfo->caddr);
    cinfo->fd = recvfrom(listen_fd, buf, 30, 0, reinterpret_cast<struct sockaddr *>(&(cinfo->caddr)), &(cinfo->caddr_len));
    if (cinfo->fd < 0){
      error("ERROR in recvfrom");
    }
    // check if the packet is vaild packet
    
    // make return packet
     
    // sendto: echo the input back to the client 
    
    cinfo->fd = sendto(listen_fd, buf, strlen(buf), 0, reinterpret_cast<struct sockaddr *>(&(cinfo->caddr)), &(cinfo->caddr_len));
    if (cinfo->fd < 0) {
      error("ERROR in sendto");
    }


    // Dispatch a thread to handle this file descriptor.
    pthread_t thr;
    assert(pthread_create(&thr, NULL, HandleClient, (void *) cinfo) == 0);
    assert(pthread_detach(thr) == 0);

  }

  // Close up shop.
  return EXIT_SUCCESS;
}

int ListenUDP(char *portnum){
  // Populate the "hints" addrinfo structure for getaddrinfo().
  // ("man addrinfo")
  struct addrinfo hints;
  memset(&hints, 0, sizeof(struct addrinfo));
  hints.ai_family = AF_UNSPEC;      // allow IPv4 or IPv6
  hints.ai_socktype = SOCK_DGRAM;  // stream
  hints.ai_flags = AI_PASSIVE;      // use wildcard "INADDR_ANY"
  hints.ai_protocol = IPPROTO_UDP;  // tcp protocol
  hints.ai_canonname = NULL;
  hints.ai_addr = NULL;
  hints.ai_next = NULL;

  // Use argv[1] as the string representation of our portnumber to
  // pass in to getaddrinfo().  getaddrinfo() returns a list of
  // address structures via the output parameter "result".
  struct addrinfo *result;
  int res = getaddrinfo(NULL, portnum, &hints, &result);

  // Did addrinfo() fail?
  if (res != 0) {
    std::cerr << "getaddrinfo() failed: ";
    std::cerr << gai_strerror(res) << std::endl;
    return -1;
  }

  // Loop through the returned address structures until we are able
  // to create a socket and bind to one.  The address structures are
  // linked in a list through the "ai_next" field of result.
  int listen_fd = -1;
  for (struct addrinfo *rp = result; rp != NULL; rp = rp->ai_next) {
    listen_fd = socket(rp->ai_family,
                       rp->ai_socktype,
                       rp->ai_protocol);
    if (listen_fd == -1) {
      // Creating this socket failed.  So, loop to the next returned
      // result and try again.
      std::cerr << "socket() failed: " << strerror(errno) << std::endl;
      listen_fd = -1;
      continue;
    }

    // Try binding the socket to the address and port number returned
    // by getaddrinfo().
    if (bind(listen_fd, rp->ai_addr, rp->ai_addrlen) == 0) {
      // Bind worked!  Print out the information about what
      // we bound to.
      PrintOut(listen_fd, rp->ai_addr, rp->ai_addrlen);
      break;
    }

    // The bind failed.  Close the socket, then loop back around and
    // try the next address/port returned by getaddrinfo().
    close(listen_fd);
    listen_fd = -1;
  }
  // Free the structure returned by getaddrinfo().
  freeaddrinfo(result);
  return listen_fd;
}
