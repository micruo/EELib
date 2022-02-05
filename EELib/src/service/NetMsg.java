package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Used by SocketThread
 * @author micruo
 */
public class NetMsg  {
  private final String type;
  private final int idx;
  private final ArrayList<Object> body = new ArrayList<>();

  public NetMsg(int idx, String type, Object... values) {
    this.type = type;
    this.idx = idx;
    body.addAll(Arrays.asList(values));
  }
  public void add(Object b) {
    body.add(b);
  }

  public int getPlayer() {
    return idx;
  }
  public Iterator<Object> getIterator() {
    return body.iterator();
  }
  public ArrayList<Object> getBody() {
    return body;
  }
  public String getType() {
    return type;
  }
}
