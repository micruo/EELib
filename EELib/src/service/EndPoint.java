/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import java.io.IOException;

/**
 *
 * @author micruo
 */
public abstract class EndPoint {
    /**
   * Send a message on this socket
   * @param m the message
   */
  public abstract void sendMsg(NetMsg m);
  private Receiver defaultReceiver;
  /**
   * Construct a new SocketThread that communicate on socket s
   * @param defaultReceiver the default Receiver
   */
  public EndPoint(Receiver defaultReceiver) {
    this.defaultReceiver = defaultReceiver;
  }
  /**
   * ovverride defaultReceiver
   * @param defaultReceiver 
   */
  public void setDefault(Receiver defaultReceiver) {
    this.defaultReceiver = defaultReceiver;
  }
  public void forceClose() {
  }
  public void start() {}
  public void receive(String m) throws IOException, ClassNotFoundException {
    if (m != null) {
      defaultReceiver.receive(this, m);
    }
    
  }
}
