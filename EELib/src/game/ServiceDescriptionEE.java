/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import com.rtsoft.service.BrowserCreator;
import com.rtsoft.service.EndPoint;
import com.rtsoft.service.ServiceDescription;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 *
 * @author michele
 */
public class ServiceDescriptionEE extends ServiceDescription {
  
  public ServiceDescriptionEE(String instanceName, String address, Object ep) throws IOException {
    super(instanceName, (InetAddress)null, 0);
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    try {
      container.connectToServer(ep, new URI(address));
    } catch (URISyntaxException | DeploymentException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public EndPoint getGames(BrowserCreator bc, Object session) throws IOException {
    return new EndPointEE((Session)session, bc);
  }
  
}
