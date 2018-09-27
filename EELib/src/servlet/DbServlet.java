/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import com.rtsoft.dbLib.DbConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 *
 * @author michele
 */
public abstract class DbServlet extends HttpServlet {

  private final String sourceName;

  public DbServlet(String sourceName) {
    this.sourceName = sourceName;
  }
  public Connection getConnection() throws ServletException {
    try {
      InitialContext cxt = new InitialContext();
      if ( cxt == null ) {
         throw new ServletException("Uh oh -- no context!");
      }

      DataSource ds = (DataSource) cxt.lookup( "java:/comp/env/jdbc/" + sourceName );

      if ( ds == null ) {
         throw new ServletException("Data source not found!");
      }
      return ds.getConnection();
    } catch(NamingException | SQLException ex) {
      throw new ServletException(ex);
    }
  }
  public void exec(Command[] cmds, HttpServletRequest request) throws IOException, ServletException {
    for(Command cmd : cmds) {
      if(request.getParameter(cmd.parameter) != null) {
        try {
          cmd.function.exec(cmd.parameter);
          break;
        } catch (IOException | ServletException ex) {
          throw ex;
        }
      }
    }
  }
  protected abstract void processRequest(DbConnection dbConn, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try(DbConnection dbConn = new DbConnection(getConnection())) {
      processRequest(dbConn, req, resp);
    } catch(Throwable ex) {
      try (PrintWriter out = resp.getWriter()) {
        ex.printStackTrace(out);
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try(DbConnection dbConn = new DbConnection(getConnection())) {
      processRequest(dbConn, req, resp);
    } catch(Throwable ex) {
      try (PrintWriter out = resp.getWriter()) {
        ex.printStackTrace(out);
      }
    }
  }
  
}
