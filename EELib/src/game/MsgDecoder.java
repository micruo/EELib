/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import service.NetMsg;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 *
 * @author michele
 */
public class MsgDecoder implements Decoder.BinaryStream<NetMsg> {

    @Override
    public NetMsg decode(InputStream arg0) throws DecodeException, IOException {
    	try {
    		ObjectInputStream is = new ObjectInputStream(arg0);
    		return (NetMsg) is.readObject();
      } catch (ClassNotFoundException e) {
        return null;
      }
    }
    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }
    
  }
