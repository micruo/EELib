/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import java.util.*;

/**
 *
 * @author micruo
 */
public class Game {
  private final HashMap<String, EndPoint> rcv = new HashMap<>();
  private final ArrayList<String> players = new ArrayList<>();
  private HashSet<String> accepted = new HashSet<>();
  private String game = "standard";
  
  Game() { }
  Game(String game) {
    this.game = game;
  }
  boolean incAccepted(String name) {
    if(accepted == null)
      return false;
    accepted.add(name);
    return accepted.size() >= players.size();
  }
  public void descr(List<List<String>> d) {
    d.add(Arrays.asList("accepted", accepted == null ? "x" : Integer.toString(accepted.size()), "game", game));
    int i = 0;
    List<String> row = new ArrayList<>();
    for(String p : players) {
      row.add(p);
      if(++i % 4 == 0) {
        d.add(row);
        row = new ArrayList<>();
      }
    }
    if(i % 4 != 0)
      d.add(row);
  }

  public String getGame() {
    return game;
  }

  public ArrayList<String> getPlayers() {
    return players;
  }

  void set(String name, EndPoint st) {
    rcv.put(name, st);
    players.add(name);
  }
  int nofPlayers() {
    return players.size();
  }
  void removePlayer(String name) {
    if(accepted == null) {
      // if the game is started yet
      rcv.values().stream().forEach(s -> s.forceClose());
      rcv.clear();
      players.clear();
      accepted = new HashSet<>();
    } else {
      rcv.remove(name);
      players.remove(name);
      accepted.remove(name);
    }
  }
  public void sendTo(NetMsg msg, String to) {
    rcv.get(to).sendMsg(msg);
  }
  public void send(NetMsg m) {
    rcv.values().stream().forEach(s -> s.sendMsg(m));
  }
  public String getPlayer(EndPoint st) {
    return rcv.entrySet().stream().filter(e -> st == e.getValue()).map(e -> e.getKey()).findFirst().orElse(null);
  }
  boolean isStarted() {
    return accepted == null;
  }
  void setDefault(Receiver r) {
    accepted = null;
    rcv.values().stream().forEach(s -> s.setDefault(r));
  }
  boolean canAccept(int maxPlayer) {
    return accepted != null && nofPlayers() < maxPlayer;
  }
}
