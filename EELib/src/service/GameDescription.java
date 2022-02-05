/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author micruo
 */
public abstract class GameDescription {
  private final int minPlayers;
  private final int maxPlayers;
  private final ArrayList<Game> games = new ArrayList<>();
  private final ArrayList<Entry> entries = new ArrayList<>();

  private class Entry {
    private final String name;
    private final EndPoint ep;
    private Game game;

    public Entry(String name, EndPoint ep) {
      this.name = name;
      this.ep = ep;
    }
  }
  public GameDescription(int minPlayer, int maxPlayer, Enum[] gameKinds) {
    this.minPlayers = minPlayer;
    this.maxPlayers = maxPlayer;
    if(gameKinds == null) {
      games.add(new Game());
      games.add(new Game());
    } else {
      for(Enum m : gameKinds) {
        games.add(new Game(m.name()));
        games.add(new Game(m.name()));
      }
    }
  }
  void set(int idx, EndPoint ep) {
    Entry e = findEndPoint(ep);
    Game g = games.get(idx);
    e.game = g;
    g.set(e.name, ep);
  }
  public abstract Receiver getDoc(Game g);
  public abstract void initAll();
  void sendToAll(boolean update) {
    NetMsg msg = new NetMsg(-1, update ? "updateService" : "startService");
    if(update) {
      appendMsg(msg);
      for(Entry s : entries) {
        if(s.game == null || !s.game.isStarted())
          s.ep.sendMsg(msg);
      }
    } else {
      for(Entry s : entries)
        if(s.game != null && s.game.isStarted())
          s.ep.sendMsg(msg);
    }
  }
  private boolean hasName(String name) {
    return entries.stream().anyMatch(s -> s.name.equals(name));
  }
  private Entry findEndPoint(EndPoint st) {
    return entries.stream().filter(s -> s.ep == st).findFirst().orElse(null);
  }
  private Game getGame(Entry e, boolean bCheckMin) {
    return bCheckMin && e.game.nofPlayers() < minPlayers ? null : e.game;
  }
  String entryName(EndPoint st) {
    Entry e = findEndPoint(st);
    return e.name;
  }
  Game getGame(EndPoint st) {
    Entry e = findEndPoint(st);
    if(e.game.isStarted())
      return null;
    return getGame(e, true);
  }
  boolean destroy(EndPoint st) {
    Entry e = findEndPoint(st);
    if(e == null)
      return false;
    entries.remove(e);
    Game g = getGame(e, false);
    if(g != null) {
      g.removePlayer(e.name);
      sendToAll(true);
    }
    return true;
  }
  void appendMsg(NetMsg msg) {
    for(Game g : games) {
      msg.add(g.getGame());
      msg.add(g.getPlayers().stream().collect(Collectors.joining(",")));
    }
  }
  void createMsg(EndPoint st, String name, NetMsg msg, boolean bService) {
    int n = 0;
    String newName = name;
    if(true) {
      while(hasName(newName)) {
        n++;
        newName = name + n;
      }
    }
    entries.add(new Entry(newName, st));
    appendMsg(msg);
  }
  public void descr(List<List<String>> d) {
    d.add(Arrays.asList("minPlayers", Integer.toString(minPlayers), "maxPlayers", Integer.toString(maxPlayers)));
    for(Game g : games)
      g.descr(d);
    for(Entry s : entries) {
      d.add(Arrays.asList(s.name, s.game == null ? " " : "x", " ", " " ));
    }
  }
}
