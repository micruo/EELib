package service;

import com.google.gson.Gson;
import com.rtsoft.utils.Files;
import java.io.*;
import java.net.*;

/**
 * A class that instantiate a new Thread to manage inter socket communication
 * @author micruo
 */
public class SocketThread extends EndPoint implements Runnable {

  private final Socket s;
  private PrintWriter bos;
  private BufferedReader bis;
  /**
   * Construct a new SocketThread that communicate on socket s
   * @param s the socket
   * @param defaultReceiver the default Receiver
   */
  public SocketThread(Socket s, Receiver defaultReceiver) {
    super(defaultReceiver);
    this.s = s;
    try {
      bis = new BufferedReader(new InputStreamReader(s.getInputStream()));
      bos = new PrintWriter(s.getOutputStream(), true);
    } catch(IOException ex) {

    }
  }
  @Override
  public void start() {
    new Thread(this).start();
  }
  /**
   * Called when is closed. Default implementation does nothing
   */
  protected void closed() {

  }
  /**
   * force the socket to be closed
   */
  @Override
  public void forceClose() {
    try {
      s.close();
    } catch (IOException ex) {
      Files.log(ex);
    }
  }

  /**
   * called when an error occurs
   * @param ex the Throwable
   */
  protected void error(Throwable ex) {
    Files.log(ex);
  }


  /**
   * Send a message on this socket
   * @param m the message
   */
  @Override
  public void sendMsg(NetMsg m) {
    try {
      if(s.isClosed())
        throw new IOException("Socket is closed!");
      bos.println(new Gson().toJson(m));
    } catch (IOException ex) {
      Files.log(ex);
    }
  }

  @Override
  public void run() {
    try {
      while(true) {
        receive(bis.readLine());
      }
    } catch (Throwable ex) {
      error(ex);
    } finally {
      try {
        if (s != null) {
          s.close();
        }
      } catch (IOException ex) {
      }
      closed();
    }
  }

}
