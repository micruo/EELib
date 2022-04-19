package service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import javax.swing.JOptionPane;

/**
 * A class thay describe a service: it contains its name, its net address and port.
 * @author micruo
 */
public class ServiceDescription implements Comparable<ServiceDescription> {
  /**
   * Maximal lenght of a service name
   */
  public static final int NAME_LENGTH = 15;
  private static final int START_ADDRESS = 16;
  public static final int PORT = 8081;
  public static final String MULTICAST = "233.12.12.1";

  private final String serviceName;
  private int port;
  private InetAddress address;

  /**
   * Create a new ServiceDescription instance
   * @param instanceName service name
   * @param address net address
   * @param port net port
   */
  public ServiceDescription(String instanceName, InetAddress address, int port ) {
    this.serviceName = instanceName;
    this.address = address;
    this.port = port;
  }

  /**
   * Create a new ServiceDescription instance
   * @param instanceName service name
   * @param address name of the net address
   * @param port net port
   * @throws java.net.UnknownHostException
   */
  public ServiceDescription(String instanceName, String address, int port ) throws UnknownHostException {
    this(instanceName, InetAddress.getByName(address), port);
  }

  ServiceDescription(byte[] data) throws UnsupportedEncodingException, UnknownHostException {
    int len = IntStream.range(0, NAME_LENGTH).filter(l -> data[l + 1] == 0).findFirst().getAsInt();
    serviceName = URLDecoder.decode(new String(data, 1, len), "UTF-8");
    if (serviceName == null || serviceName.length() == 0) {
      throw new UnsupportedEncodingException();
    }

    address = InetAddress.getByAddress(Arrays.copyOfRange(data,
      START_ADDRESS, START_ADDRESS + 4));

    port = (data[START_ADDRESS + 4] << 24) + ((data[START_ADDRESS + 5] & 0xFF) << 16) +
      ((data[START_ADDRESS + 6] & 0xFF) << 8) + (data[START_ADDRESS + 7] & 0xFF);


  }

  static byte[] getEncodedName(String name) {
    try {
      byte[] b = Arrays.copyOf(URLEncoder.encode(name, "UTF-8").getBytes(), NAME_LENGTH);
      return b;
    } catch (UnsupportedEncodingException uee) {
      return null;
    }
  }
  /**
   * Return the net address for this service
   * @return net address
   */
  public InetAddress getAddress() {
    return address;
  }

  /**
   * Return the service name
   * @return service name
   */
  public String getInstanceName() {
    return serviceName;
  }

  /**
   * Return the net port for this service
   * @return net port
   */
  public int getPort() {
    return port;
  }

  byte[] getDescription() {
    byte[] buffer = new byte[MulticastClient.DATAGRAM_LENGTH];
    System.arraycopy(getEncodedName(serviceName), 0, buffer, 1, NAME_LENGTH);
    System.arraycopy(getAddress().getAddress(), 0, buffer, START_ADDRESS, 4);
    buffer[START_ADDRESS + 4] = (byte) (port >>> 24);
    buffer[START_ADDRESS + 5] = (byte) (port >>> 16);
    buffer[START_ADDRESS + 6] = (byte) (port >>> 8);
    buffer[START_ADDRESS + 7] = (byte) port;
    return buffer;
  }

  @Override
  public String toString() {
    return getInstanceName() + " " +  getAddress().getHostAddress() + " " + getPort();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ServiceDescription)) {
      return false;
    }
    ServiceDescription descriptor = (ServiceDescription) o;
    return descriptor.getInstanceName().equals(getInstanceName());
  }

  @Override
  public int hashCode() {
    return getInstanceName().hashCode();
  }

  @Override
  public int compareTo(ServiceDescription sd) throws ClassCastException {
    if (sd == null) {
      throw new NullPointerException();
    }
    if (sd == this) {
      return 0;
    }

    return getInstanceName().compareTo(sd.getInstanceName());
  }
  public EndPoint getGames(final BrowserCreator bc, Object session) throws IOException {
    System.setProperty("java.net.preferIPv4Stack" , "true");
    Socket s;
    if(getAddress().isMulticastAddress()) {
      ServiceBrowser browser = new ServiceBrowser(this);

      ArrayList<ServiceDescription> descriptors = browser.getResponses();
      browser.stopSocket();
      if (descriptors.isEmpty())
        throw new IOException("errore di connessione");
      s = new Socket(descriptors.get(0).getAddress(), descriptors.get(0).getPort());
    } else {
      s = new Socket(getAddress(), getPort());
    }
    return new SocketThread(s, bc) {

      @Override
      protected void closed() {
        JOptionPane.showMessageDialog(bc, "Connessione persa");
      }

    };
  }
}
