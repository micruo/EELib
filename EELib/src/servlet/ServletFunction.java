/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import java.io.IOException;
import javax.servlet.ServletException;

/**
 *
 * @author michele
 */
@FunctionalInterface
public interface ServletFunction {
  public void exec(String command) throws IOException, ServletException;
}
