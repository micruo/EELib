/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import com.rtsoft.dbLib.DbConnection;
import com.rtsoft.storage.StorageFactory;
import com.rtsoft.utils.Couple;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.sql.DataSource;

/**
 *
 * @author michele
 */
public abstract class DbServlet extends HttpServlet {

  protected interface Pager {
    public String value();
    public Class<? extends Page> page();
  }
  private final String sourceName;
  private final Class<? extends Page> def;
  private final Pager[] pager;

  public DbServlet(String sourceName) {
    this(sourceName, null, null);
  }

  public DbServlet(String sourceName, Class<? extends Page> def, Pager[] pager) {
    this.sourceName = sourceName;
    this.def = def;
    this.pager = pager;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    com.rtsoft.utils.Files.setLogger(new com.rtsoft.utils.Files.Log() {
      @Override
      public void log(Throwable ex) {
        getServletContext().log("", ex);
      }

      @Override
      public void log(String prompt, Object... params) {
        getServletContext().log(MessageFormat.format(prompt, params));
      }
    });
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
  protected void processRequest(DbConnection dbConn, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException  {
    
  }

  private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try(DbConnection dbConn = new DbConnection(getConnection())) {
      req.setCharacterEncoding("UTF-8");
      Class<? extends Page> page = def;
      if(pager != null) {
        Optional<Pager> oPage = Arrays.stream(pager).filter(t -> req.getParameter(t.value()) != null).findFirst();
        if(oPage.isPresent())
          page = oPage.get().page();
      }
      if(page != null) {
        try {
          Page p = page.newInstance();
          p.set(this, dbConn, req, resp);
          p.execute();
          String go = p.getPage();
          if(go != null)
            req.getRequestDispatcher(go).forward(req, resp);
        } catch (InstantiationException | IllegalAccessException ex) {
          throw new ServletException(ex);
        }
      } else {
        processRequest(dbConn, req, resp);
      }
    }
  }
  public <T> Stream<T> getSelected(HttpServletRequest req, String cmd, List<T> elements) {
    return IntStream.range(0, elements.size()).filter(i -> req.getParameter(cmd + i) != null).mapToObj(i -> elements.get(i));
  } 
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    process(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    process(req, resp);
  }
  public void send(String to, String cc, String subject, String body, Object... allegati) throws MessagingException, IOException {
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
  public <T> String choose(HttpServletRequest request, Function<T, String> p, List<T> a, Couple<String, Function<T, String>> ... what) {
    // è più efficace così che con gli stream
    for(T t : a) {
      for(Couple<String, Function<T, String>> c : what) {
        if(request.getParameter(c.getFirst() + p.apply(t)) != null) {
          return c.getSecond().apply(t);
        }
      }
    }
    return null;
  }

  public static <T> T create(DbConnection dbConn, RequestToBean rb, Class<T> clazz, Consumer<T> build) throws SQLException {
    T obj = null;
    try {
      String param = rb.getRequest().getParameter(clazz.getSimpleName().toLowerCase());
      int id = param != null && !param.isEmpty() ? Integer.parseInt(param) : 0;
      if(id == 0) {
        obj = clazz.newInstance();
      } else {
        obj = dbConn.getKey(clazz, id).get();
      }
      rb.read(obj);
      if(id == 0) {
        if(build != null)
          build.accept(obj);
        dbConn.insert(obj);
      } else {
        dbConn.update(obj);
      }
    } catch(InstantiationException | IllegalAccessException ex) {
    }
    return obj;
  }
  public static Path externalDir(StorageFactory sf, HttpServlet servlet, Part file, boolean bSetPerm, String other, String... more) throws IOException {
    Path path = externalDir(servlet, other, more);
    try(OutputStream fo = java.nio.file.Files.newOutputStream(path)) {
      if(sf == null)
        StorageFactory.copyFile(file.getInputStream(), fo);
      else
        sf.copyFile(file.getInputStream(), fo, false);
    }
    if(bSetPerm) {
      Set<java.nio.file.attribute.PosixFilePermission> s = java.nio.file.Files.getPosixFilePermissions(path);
      s.add(PosixFilePermission.OTHERS_READ);
      java.nio.file.Files.setPosixFilePermissions(path, s);
    }
    return path;
  }
  public Path externalDir(String other, Part file, boolean bSetPerm, String... more) throws IOException {
    return externalDir(null, this, file, bSetPerm, other, more);
  }
  public static Path externalDir(HttpServlet servlet, String other, String... more) {
    Path path = Paths.get(servlet.getServletContext().getRealPath("/")).resolveSibling(other);
    try {
      if(!Files.exists(path))
        Files.createDirectories(path);
    } catch (IOException ex) {
    }
    for(String m : more)
      path = path.resolve(m);
    return path;
  }
}
