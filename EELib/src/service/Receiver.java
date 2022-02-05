/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

/**
 *
 * @author micruo
 */
@FunctionalInterface
public interface Receiver {
  public void receive(EndPoint st, String msg);
}
