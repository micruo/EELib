/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import com.rtsoft.dbLib.DbConnection;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author micru
 */
public abstract class Page {
  protected DbServlet parent;
  protected DbConnection dbConn;
  protected HttpServletRequest request;
  protected HttpServletResponse response;
  public void set(DbServlet parent, DbConnection dbConn, HttpServletRequest request, HttpServletResponse response) {
    this.parent = parent;
    this.dbConn = dbConn;
    this.request = request;
    this.response = response;
  }
  protected Page newPage(DbServlet.Pager t) {
    Page p = null;
    try {
      p = t.page().newInstance();
      p.set(parent, dbConn, request, response);
    } catch (InstantiationException | IllegalAccessException ex) {
    }
    return p;
  }
  public String getPage() {
    return (String)request.getAttribute("pageToGo");
  }

  public void setPage(String page) {
    request.setAttribute("pageToGo", page);
  }
  public void exception(IllegalArgumentException ex) {
    
  }
  public String[] commands() {
    return null;
  }
  public String command() {
    return null;
  }
  private void ex(InvocationTargetException ex) throws ServletException {
    if(ex.getCause() instanceof IllegalArgumentException)
      exception((IllegalArgumentException)ex.getCause());
    else
      throw new ServletException(ex.getCause().getMessage());
  }
  public void execute() throws ServletException, IOException {
    String mode = command();
    if(mode != null) {
      mode = request.getParameter(mode);
      if(mode != null && !mode.isEmpty()) {
        try {
          Method m = getClass().getMethod(mode, String.class);
          m.invoke(this, mode);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException ex) {
        } catch(InvocationTargetException ex2) {
          ex(ex2);
        }
      }
    } else {
      String[] commands = commands();
      if(commands == null)
        return;
      for(String cmd : commands) {
        String s = request.getParameter(cmd);
        if(s != null) {
          try {
            Method m = getClass().getMethod(cmd, String.class);
            if((Boolean)m.invoke(this, s))
              break;
          } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException ex) {
          } catch(InvocationTargetException ex2) {
            ex(ex2);
          }
        }
      }
    }
  }
}
