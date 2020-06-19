/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import com.rtsoft.dbLib.DbConnection;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

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
  public Optional<String> getParam(String key) {
    return dbConn.getKey(Cfg.class, key).map(f -> f.getValue());
  }
  public void send(String to, String cc, String subject, String body, Object... allegati) throws MessagingException, IOException {
    String from = getParam("from").get();
    String passwd = getParam("passwd").get();
    String smtp = getParam("smtp").get();
    String bcc = getParam("bcc").orElse(null);
    boolean tls = getParam("tls").map(c -> Boolean.valueOf(c)).orElse(false);
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
    bodyPart.setText(body, "UTF-8", "html");
    multipart.addBodyPart( bodyPart );
    for(Object att : allegati) {
      bodyPart = new MimeBodyPart();
      if(att instanceof File) {
        File f = (File)att;
        bodyPart.attachFile( f );
        bodyPart.setFileName( f.getName() );
      } else if(att instanceof Part) {
        Part p = (Part)att;
        ByteArrayDataSource ds = new ByteArrayDataSource(p.getInputStream(), p.getContentType());
        bodyPart.setDataHandler(new DataHandler(ds));
        bodyPart.setFileName( p.getSubmittedFileName() );
      } else if(att instanceof String[]) {
        String[] s = (String[])att;
        ByteArrayDataSource ds = new ByteArrayDataSource(s[1], "text/plain");
        bodyPart.setDataHandler(new DataHandler(ds));
        bodyPart.setFileName( s[0] );
      }
      multipart.addBodyPart( bodyPart );
    }
    message.setContent( multipart );
    Transport.send(message, from, passwd);
  }
}
