package service;

import com.rtsoft.utils.Files;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * <p>A class used to ask a ServiceResponder for a service providing request</p>
 *
 * 
 * <p>Typical example could be:</p>
 * <blockquote><pre><code>
  static final String SERVICE_NAME = "discoveryDemo";
  ServiceBrowser browser = new ServiceBrowser(SERVICE_NAME);

  System.out.println("Browser started. Will search for 2 secs.");
  try {
    Thread.sleep(2000);
  } catch (InterruptedException ie) {
    // ignore
  }
  browser.stopSocket();
  VectArrayListServiceDescription> descriptors = browser.getResponses();
</code></pre></blockquote>
 * @see ServiceDescription
 * @see ServiceResponder
 * @author micruo
 */
public final class ServiceBrowser extends MulticastClient {
  private final ArrayList<ServiceDescription> descriptors = new ArrayList<>();
  private boolean sendedPacket = false;

  /**
   * Create a new ServiceBrowser
   * @param service
   * @throws IOException
   */
  public ServiceBrowser(ServiceDescription service) throws IOException {
    super(service);
    startSocket();
  }
  /**
   * Return the array of ServiceDescription provided by the service provider
   * @return array of ServiceDescription provided by the service provider
   */
  public ArrayList<ServiceDescription> getResponses() {
    try {
      synchronized(descriptors) {
        descriptors.wait();
      }
    } catch (InterruptedException ex) {
      Files.log(ex);
    }
    return descriptors;
  }

  private DatagramPacket getQueryPacket() {
    byte[] bytes = new byte[ServiceDescription.NAME_LENGTH + 1];
    System.arraycopy(ServiceDescription.getEncodedName(service.getInstanceName()), 0, bytes, 1, ServiceDescription.NAME_LENGTH);
    bytes[0] = QUERY_CODE;
    return new DatagramPacket(bytes, 0, bytes.length, service.getAddress(), service.getPort());
  }

  @Override
  protected void sendRequest() {
    if (!sendedPacket) {
      try {
        socket.send(getQueryPacket());
        sendedPacket = true;
      } catch (IOException ioe) {
      }
    }
  }

  @Override
  protected void getResponse(byte[] data) {
    if(data[0] == REPLY_CODE) {
      try {
        ServiceDescription descriptor = new ServiceDescription(data);
        synchronized(descriptors) {
          int pos = descriptors.indexOf(descriptor);
          if (pos > -1) {
            descriptors.remove(pos);
          }
          descriptors.add(descriptor);
          descriptors.notifyAll();
        }
      } catch (UnsupportedEncodingException | UnknownHostException ex) {
        Files.log(ex);
      }
    }
  }

}
