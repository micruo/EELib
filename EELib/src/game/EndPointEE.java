/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import com.google.gson.Gson;
import service.EndPoint;
import service.NetMsg;
import service.Receiver;
import com.rtsoft.utils.Files;
import java.io.IOException;
import javax.websocket.Session;

/**
 *
 * @author michele
 */
public class EndPointEE extends EndPoint {

  private final Session session;
  public EndPointEE(Session session, Receiver defaultReceiver) {
    super(defaultReceiver);
    this.session = session;
  }

  @Override
  public void sendMsg(NetMsg m) {
    try {
      session.getBasicRemote().sendText(new Gson().toJson(m));
    } catch (IOException ex) {
      Files.log(ex);
    }
  }
  
}
