/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import com.google.gson.Gson;
import com.rtsoft.utils.Files;
import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 *
 * @author micruo
 */
public class ResponderCreator implements Receiver {
  private final HashMap<String, GameDescription> games = new HashMap<>();
  private static ResponderCreator rc;
  private boolean bService = false;
  
  public static ResponderCreator getInstance() {
    if(rc == null)
      rc = new ResponderCreator();
    return rc;
  }
  private ResponderCreator() {
  }
  public boolean isEmpty() {
    return games.isEmpty();
  }
  public final void unregister() {
    games.clear();
  }
  public final void register(String game, GameDescription gd) {
    games.put(game, gd);
  }
  public final void start(String serviceName) throws IOException {
    bService = true;
    start(ServiceResponder.createServer(serviceName));
  }
  public final void start(ServerSocket ss) throws IOException {
    Files.log("In attesa su {0}:{1}", ss.getInetAddress(), ss.getLocalPort());
    for (;;) {
      EndPoint st = new SocketThread(ss.accept(), this) {

        @Override
        protected void closed() {
          removeGame(this);
        }

      };
      st.setDefault(this);
      st.start();
    }
  }
  @Override
  public void receive(EndPoint st, String txt) {
    try {
      NetMsg msg = new Gson().fromJson(txt, NetMsg.class);
      Iterator<?> body = msg.getIterator();
      String gameName = (String)body.next();
      GameDescription gd = games.get(gameName);
      int row;
      Game g;
      String name;
      NetMsg msg2 = new NetMsg(-1, "initService");
      switch (msg.getType()) {
      case "initService":
        name = (String)body.next();
        synchronized(games) {
          row = gd.createMsg(st, name, msg2, bService);
          if(row >= 0) {
            g = gd.set(row, st);
            st.setDefault(g);
            st.sendMsg(new NetMsg(-1, "startService"));
            g.load(name);
            return;
          }
        }
        st.sendMsg(msg2);
        break;
      case "updateService":
        row = Integer.parseInt((String)body.next());
        synchronized(games) {
          gd.set(row, st);
        }
        gd.sendToAll(true);
        break;
      case "newService":
        name = (String)body.next();
        synchronized(games) {
          g = gd.newGame(name);
          gd.appendMsg(msg2, g);
        }
        gd.sendToAll(true, msg2);
        break;
      case "startService":
        synchronized(games) {
          g = gd.getGame(st);
          if(g == null)
            return;
        }
        if(g.incAccepted(gd.entryName(st))) {
          gd.sendToAll(true); // per settare la partita chiusa...
          try {
            g.init();
          } catch(Exception e) {
            Files.log(e);
          }
          g.setDefault(g);
          gd.sendToAll(false); // per settare la partita chiusa...
          g.load(null);
        }
        break;
      }
    } catch(Exception ex) {
      Files.log(ex);
    }
  }
  public void removeGame(EndPoint ep) {
    synchronized(games) {
      for(GameDescription gd : games.values())
        if(gd.destroy(ep))
          break;
    }
  }

  public List<List<String>> descr() {
    ArrayList<List<String>> descr = new ArrayList<>();
    games.forEach((g, v) -> {
      descr.add(Arrays.asList(g, " ", " ", " "));
      v.descr(descr);
    });
    return descr;
  }

 }
