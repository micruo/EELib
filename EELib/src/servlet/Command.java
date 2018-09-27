/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

/**
 *
 * @author michele
 */
public class Command {
  final String parameter;
  final ServletFunction function;

  public Command(String parameter, ServletFunction function) {
    this.parameter = parameter;
    this.function = function;
  }
  
}
