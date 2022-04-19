/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 *
 * @author micheleruotolo
 */
public class TextDecoder implements Decoder.TextStream<String> {


  @Override
  public void init(EndpointConfig config) {
  }

  @Override
  public void destroy() {
  }

  @Override
  public String decode(Reader reader) throws DecodeException, IOException {
    return new BufferedReader(reader).readLine();
  }

  
}
