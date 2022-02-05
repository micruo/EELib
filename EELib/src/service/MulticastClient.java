/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package service;

import com.rtsoft.utils.Files;
import java.io.IOException;
import java.net.*;

/**
 * Base class for ServiceResponder and ServiceBrowser: it eventually send a DatagramPacket on a Multicast socket,
 * and listen for DatagramPacket response.
 * @author micruo
 */
public abstract class MulticastClient implements Runnable {
  static final int DATAGRAM_LENGTH = 24;
  static final byte QUERY_CODE = 95;
  static final byte REPLY_CODE = 64;

  final ServiceDescription service;
  DatagramSocket socket;
  private boolean shouldRun = true;
  private Thread myThread;

  /**
   * Create a new MulticastClient
   * @param service
   * @throws IOException
   */
  public MulticastClient(ServiceDescription service) throws IOException {
    this.service = service;
    socket = new MulticastSocket(service.getPort());
    ((MulticastSocket)socket).joinGroup(service.getAddress());
    socket.setSoTimeout(250);
  }
  void startSocket() {
    if (myThread == null) {
      shouldRun = true;
      myThread = new Thread(this);
      myThread.start();
    }
  }

  /**
   * Stop the socket activity
   */
  public void stopSocket() {
    if (myThread != null) {
      shouldRun = false;
      myThread.interrupt();
      myThread = null;
    }
  }

  /**
   * Callback called to send a request packet
   */
  protected abstract void sendRequest();
  /**
   * Callback called when a packet is received
   * @param data the packet content
   */
  protected abstract void getResponse(byte[] data);
  @Override
  public void run() {
    while (shouldRun) {

      /* listen (briefly) for a reply packet */
      try {
        sendRequest();
        byte[] buf = new byte[DATAGRAM_LENGTH];
        DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
        socket.receive(receivedPacket); // note timeout in effect

        /* notes on behavior of descriptors.indexOf(...)
         * ServiceDescriptor objects check for 'equals()'
         * based only on the instanceName field. An update
         * to a descriptor implies we should replace an
         * entry if we already have one. (Instead of bothing
         * with the details to determine new vs. update, just
         * quickly replace any current descriptor.)
         */

        if(receivedPacket.getData() != null)
          getResponse(receivedPacket.getData());

      } catch (SocketTimeoutException ste) {
        /* ignored; this exception is by design to
         * break the blocking from socket.receive */
      } catch (IOException ioe) {
        Files.log("MulticastClient", "run", ioe);
      }
    }
  }
}
