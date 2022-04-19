/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import service.NetMsg;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 *
 * @author michele
 */
public class MsgEncoder implements Encoder.BinaryStream<NetMsg> {

  @Override
  public void encode(NetMsg arg0, OutputStream arg1) throws EncodeException, IOException {
		ObjectOutputStream os = new ObjectOutputStream(arg1);
		os.writeObject(arg0);
  }

  @Override
  public void init(EndpointConfig config) {
  }

  @Override
  public void destroy() {
  }
  
}
