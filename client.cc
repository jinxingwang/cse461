#include <arpa/inet.h>
#include <assert.h>
#include <errno.h>
#include <netdb.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <iostream>

void Usage(char *progname);

bool LookupName(char *name,
                unsigned short port,
                struct sockaddr_storage *ret_addr,
                size_t *ret_addrlen);

bool Connect(const struct sockaddr_storage &addr,
             const size_t &addrlen,
             int *ret_fd);

int main(int argc, char **argv) {
  if (argc != 3) {
    Usage(argv[0]);
  }

  unsigned short port = 0;
  if (sscanf(argv[2], "%hu", &port) != 1) {
    Usage(argv[0]);
  }

  // Get an appropriate sockaddr structure.
  struct sockaddr_storage addr;
  size_t addrlen;
  if (!LookupName(argv[1], port, &addr, &addrlen)) {
    Usage(argv[0]);
  }

  // Connect to the remote host.
  int socket_fd;
  if (!Connect(addr, addrlen, &socket_fd)) {
    Usage(argv[0]);
  }

  // Read something from the remote host.
  char readbuf[256];
  int res;
  while (1) {
    res = read(socket_fd, readbuf, 255);
    if (res == 0) {
      std::cerr << "socket closed prematurely" << std::endl;
      close(socket_fd);
      return EXIT_FAILURE;
    }
    if (res == -1) {
      if (errno == EINTR)
        continue;
      std::cerr << "socket read failure: " << strerror(errno) << std::endl;
      close(socket_fd);
      return EXIT_FAILURE;
    }
    readbuf[res] = '\0';
    std::cout << readbuf;
    break;
  }

  // Write something to the remote host.
  while (1) {
    int wres = write(socket_fd, readbuf, res);
    if (wres == 0) {
      std::cerr << "socket closed prematurely" << std::endl;
      close(socket_fd);
      return EXIT_FAILURE;
    }
    if (wres == -1) {
      if (errno == EINTR)
        continue;
      std::cerr << "socket write failure: " << strerror(errno) << std::endl;
      close(socket_fd);
      return EXIT_FAILURE;
    }
    break;
  }

  // Clean up.
  close(socket_fd);
  return EXIT_SUCCESS;
}

void Usage(char *progname) {
  std::cerr << "usage: " << progname << " hostname port" << std::endl;
  exit(EXIT_FAILURE);
}

bool LookupName(char *name,
                unsigned short port,
                struct sockaddr_storage *ret_addr,
                size_t *ret_addrlen) {
  struct addrinfo hints, *results;
  int retval;

  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_UNSPEC;
  hints.ai_socktype = SOCK_STREAM;

  // Do the lookup by invoking getaddrinfo().
  if ((retval = getaddrinfo(name, NULL, &hints, &results)) != 0) {
    std::cerr << "getaddrinfo failed: ";
    std::cerr << gai_strerror(retval) << std::endl;
    return false;
  }

  // Set the port in the first result.
  if (results->ai_family == AF_INET) {
    struct sockaddr_in *v4addr = (struct sockaddr_in *) results->ai_addr;
    v4addr->sin_port = htons(port);
  } else if (results->ai_family == AF_INET6) {
    struct sockaddr_in6 *v6addr = (struct sockaddr_in6 *) results->ai_addr;
    v6addr->sin6_port = htons(port);
  } else {
    std::cerr << "getaddrinfo failed to provide an IPv4 or IPv6 address";
    std::cerr << std::endl;
    freeaddrinfo(results);
    return false;
  }

  // Return the first result.
  assert(results != NULL);
  memcpy(ret_addr, results->ai_addr, results->ai_addrlen);
  *ret_addrlen = results->ai_addrlen;

  // Clean up.
  freeaddrinfo(results);
  return true;
}

bool Connect(const struct sockaddr_storage &addr,
             const size_t &addrlen,
             int *ret_fd) {
  // Create the socket.
  int socket_fd = socket(addr.ss_family, SOCK_STREAM, 0);
  if (socket_fd == -1) {
    std::cerr << "socket() failed: " << strerror(errno) << std::endl;
    return false;
  }

  // Connect the socket to the remote host.
  int res = connect(socket_fd,
                    reinterpret_cast<const sockaddr *>(&addr),
                    addrlen);
  if (res == -1) {
    std::cerr << "connect() failed: " << strerror(errno) << std::endl;
    return false;
  }

  *ret_fd = socket_fd;
  return true;
}