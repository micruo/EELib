package service;

import com.rtsoft.utils.Files;
import com.rtsoft.utils.Utils;
import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * A class enable to listen for service providing requests, sending a response with the description of
 * the service offered.
 * <p>Typical example could be:</p>
 * <blockquote><pre><code>
    ServiceResponder responder;
    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket();
      serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
    } catch (IOException ioe) {
      System.err.println("Could not bind a server socket to a free port: " + ioe);
      System.exit(1);
    }

    ServiceDescription[] descriptors = {
      new ServiceDescription("demo", serverSocket.getInetAddress(), serverSocket.getLocalPort())
    };
    try {
      responder = new ServiceResponder("demoServer", descriptors);
    } catch (IOException ex) {
      System.exit(1);
    }
</code></pre></blockquote>
 * @see ServiceDescription
 * @see ServiceBrowser
 * @author micruo
 */
public final class ServiceResponder extends MulticastClient  {

  private final ServiceDescription[] descriptors;

  /**
   * Create a new ServiceResponder built on an array of ServiceDescription instances
   * @param service
   * @param descriptors ServiceDescription instances
   * @throws IOException
   */
  private ServiceResponder(ServiceDescription service, ServiceDescription[] descriptors) throws IOException {
    super(service);
    this.descriptors = descriptors;
    addShutdownHandler();
    startSocket();
  }
  private static InetAddress localAddr() throws SocketException {
    return Utils.asStream(NetworkInterface.getNetworkInterfaces()).flatMap(e -> Utils.asStream(e.getInetAddresses())).
      filter(a -> a instanceof Inet4Address && ! a.getHostAddress().equals("127.0.0.1")).findFirst().orElse(null);
  }

  
  static ServerSocket createServer(String serviceName) throws IOException {
    System.setProperty("java.net.preferIPv4Stack" , "true");
    ServiceDescription svc = new ServiceDescription(serviceName, ServiceDescription.MULTICAST, ServiceDescription.PORT);
    ServerSocket ss;
    ss = new ServerSocket(0, 50, localAddr());
    new ServiceResponder(svc, 
      new ServiceDescription[] {
      new ServiceDescription(svc.getInstanceName(), ss.getInetAddress(), ss.getLocalPort())
      });
    return ss;
  }
  /**
   * ServiceDescription instances associated with this ServiceResponder
   * @return a ServiceDescription instance associated with this ServiceResponder
   */
  public ServiceDescription[] getDescriptors() {
    return descriptors;
  }

  private void addShutdownHandler() {
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        stopSocket();
      }
    });
  }

  @Override
  protected void sendRequest() {
  }

  @Override
  protected void getResponse(byte[] data) {
    if ((data[0] != QUERY_CODE) ||
      !Arrays.equals(ServiceDescription.getEncodedName(service.getInstanceName()),
      Arrays.copyOfRange(data, 1, 1 + ServiceDescription.NAME_LENGTH))) {
      return;
    }

    for(ServiceDescription sd : descriptors) {
      byte[] bytes = sd.getDescription();
      bytes[0] = REPLY_CODE;
      DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length, service.getAddress(), service.getPort());

      try {
        socket.send(packet);
      } catch (IOException ioe) {
        Files.log(ioe);
        /* resume operation */
      }
    }
  }
}
