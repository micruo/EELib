/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import java.io.IOException;
import java.io.Writer;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 *
 * @author micheleruotolo
 */
public class TextEncoder implements Encoder.TextStream<String> {


  @Override
  public void init(EndpointConfig config) {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void encode(String object, Writer writer) throws EncodeException, IOException {
    writer.write(object);
    writer.flush();
  }

  
}
