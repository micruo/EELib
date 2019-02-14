/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import com.rtsoft.dbLib.DbConnection;
import com.rtsoft.utils.Couple;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
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
      String s = request.getParameter(cmd.parameter);
      if(s != null) {
        try {
          if(cmd.function.exec(s))
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
  public void send(String to, String cc, String subject, String body, File allegati) throws MessagingException, IOException {
    String from;
    String passwd;
    String smtp;
    String bcc;
    boolean tls;
    Properties prop = new Properties();
    try(InputStream inStream = getServletContext().getResourceAsStream("WEB-INF/prop.txt")) {
      prop.load(inStream);
      from = prop.getProperty("from");
      passwd = prop.getProperty("passwd");
      smtp = prop.getProperty("smtp");
      bcc = prop.getProperty("bcc");
      tls = Boolean.valueOf(prop.getProperty("tls"));
    }
    Properties properties = new Properties();
    properties.setProperty("mail.smtp.submitter", from);
    properties.setProperty("mail.smtp.auth", "true");
    properties.setProperty("mail.smtp.host", smtp);
    if(tls) {
      properties.put("mail.smtp.starttls.enable","true");
      properties.put("mail.smtp.ssl.enable", "true");  // If you need to authenticate
    }
    Session mailSession = Session.getInstance(properties);
    Multipart multipart = new MimeMultipart();
    Message message = new MimeMessage(mailSession);
    message.setHeader("Disposition-Notification-To:", from);

    message.setFrom(new InternetAddress(from));
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
    if(cc != null && !cc.isEmpty())
      message.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));
    if(bcc != null && !bcc.isEmpty())
      message.setRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
    message.setSubject(subject);
    MimeBodyPart bodyPart = new MimeBodyPart();
    bodyPart.setText(body, "UTF-8");
    multipart.addBodyPart( bodyPart );
    if(allegati != null) {
      bodyPart = new MimeBodyPart();
      bodyPart.attachFile( allegati );
      bodyPart.setFileName( allegati.getName() );
      multipart.addBodyPart( bodyPart );
    }
    message.setContent( multipart );
    Transport.send(message, from, passwd);
  }
  public <T> void choose(HttpServletRequest request, Function<T, String> p, List<T> a, Couple<String, Consumer<T>> ... what) {
    // è più efficace così che con gli stream
    for(T t : a) {
      for(Couple<String, Consumer<T>> c : what) {
        if(request.getParameter(c.getFirst() + p.apply(t)) != null) {
          c.getSecond().accept(t);
          return;
        }
      }
    }
  }
 
}
