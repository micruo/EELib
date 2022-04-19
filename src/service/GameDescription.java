/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import com.rtsoft.utils.Files;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author micruo
 */
public class GameDescription {
  private final int minPlayers;
  private final int maxPlayers;
  private final ArrayList<Game> games = new ArrayList<>();
  private final ArrayList<Entry> entries = new ArrayList<>();
  private final Class<? extends Game> gClass;

  private class Entry {
    private final String name;
    private final EndPoint ep;
    private Game game;

    public Entry(String name, EndPoint ep) {
      this.name = name;
      this.ep = ep;
    }
  }
  public GameDescription(int minPlayer, int maxPlayer, Class<? extends Game> gClass, Enum[] gameKinds) {
    this.minPlayers = minPlayer;
    this.maxPlayers = maxPlayer;
    this.gClass = gClass;
    if(gameKinds == null) {
      newGame("standard");
    } else {
      for(Enum m : gameKinds) {
        newGame(m.name());
      }
    }
  }
  final Game newGame(String name) {
    Game g = null;
    try {
      Constructor<? extends Game> c = gClass.getConstructor(String.class);
      g = c.newInstance(name);
      games.add(g);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
      Files.log(ex);
    }
    return g;
  }
  Game set(int idx, EndPoint ep) {
    Entry e = findEndPoint(ep);
    Game g = games.get(idx);
    e.game = g;
    g.set(e.name, ep);
    return g;
  }
  void sendToAll(boolean update, NetMsg msg) {
    if(update) {
      entries.stream().filter((s) -> (s.game == null || !s.game.isStarted())).forEach((s) -> s.ep.sendMsg(msg));
    } else {
      entries.stream().filter((s) -> (s.game != null && s.game.isStarted())).forEach((s) -> s.ep.sendMsg(msg));
    }
    
  }
  void sendToAll(boolean update) {
    NetMsg msg = new NetMsg(-1, update ? "updateService" : "startService");
    if(update)
      appendMsg(msg);
    sendToAll(update, msg);
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
      if(!g.isStarted())
        sendToAll(true);
    }
    return true;
  }
  void appendMsg(NetMsg msg, Game g) {
    msg.add("" + games.indexOf(g));
    msg.add(g.getGame());
    msg.add(g.getPlayers().stream().collect(Collectors.joining(",")));
    msg.add(g.nofPlayers() >= minPlayers);
    msg.add(g.nofPlayers() < maxPlayers);
  }
  void appendMsg(NetMsg msg) {
    for(Game g : games) {
      appendMsg(msg, g);
    }
  }
  int createMsg(EndPoint st, String name, NetMsg msg, boolean bService) {
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
    String nn = newName;
    return IntStream.range(0, games.size()).filter(i -> games.get(i).getPlayers().contains(nn)).findFirst().orElse(-1);
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
