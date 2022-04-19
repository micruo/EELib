package service;

import com.google.gson.Gson;
import com.rtsoft.swingEx.apps.App;
import com.rtsoft.swingEx.layout.DialogLayout;
import com.rtsoft.utils.HttpConn;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;

/**
 *
 * @author micruo
 */
public abstract class BrowserCreator extends App implements Receiver {
  private EndPoint ct;
  private final List<JLabel> labels = new ArrayList<>();
  private final List<JButton> buttons = new ArrayList<>();
  private final JButton skipBtn = new JButton();
  private String gameName;

  public BrowserCreator(String name) {
    super(name, null);
  }
  public static  String getGameName() {
    String id = Preferences.userRoot().get("micruo.it", "");
    String name = "";
    while(true) {
      try {
        if (id.isEmpty()) {
          JPanel panel = new JPanel(new DialogLayout());
          JTextField ctlName = new JTextField();
          panel.add(new JLabel("Nome"));
          panel.add(ctlName);
          JOptionPane.showMessageDialog(null, panel);
          name = ctlName.getText();
          if(name.isEmpty()) {
            throw new IOException("Nome non valido!!!!");
          }
          try(HttpConn conn = new HttpConn(new URL("http://www.micruo.it"), "Micruo/StartGame", null, false)) {
            id = conn.getRequestGet("command=getName&name=" + URLEncoder.encode(name, "UTF8"));
          }
          if(id.startsWith("??"))
            throw new IOException("Nome già esistente");
          Preferences.userRoot().put("micruo.it", id);
        }
        try(HttpConn conn = new HttpConn(new URL("http://www.micruo.it"), "Micruo/StartGame", null, false)) {
          name = conn.getRequestGet("command=getId&id=" + URLEncoder.encode(id, "UTF8"));
        }
        if(id.startsWith("??"))
          throw new IOException("Id non trovato");
        Preferences.userRoot().put("micruo.it", id);
        break;
      } catch (IOException ex) {
        id = "";
        App.getInstance().showError(ex);
      }
    }
    return name;
  }
  public void createGame(ServiceDescription svc, Object session) throws IOException {
    createGame(svc, session, null);
  }
  public void createGame(ServiceDescription svc, Object session, String name) throws IOException {
    initialize(null, null, null);
    if(name == null) {
      name = getGameName();
    }
    content.setLayout(new BorderLayout());
    AbstractAction a = new AbstractAction(">>") {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        NetMsg msg = new NetMsg(-1, "startService", gameName);
        sendMsg(msg);
      }

    };
    skipBtn.setAction(a);
    content.add(skipBtn, BorderLayout.SOUTH);
    gameName = svc.getInstanceName();
    while(true) {
      try {
        ct = svc.getGames(this, session);
        break;
      } catch (IOException ex) {
        if(JOptionPane.showConfirmDialog(content,
                MessageFormat.format("Errore di connessione {0}. Riprova?", ex.getMessage()),
                null, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
          throw ex;
      }
    }
    ct.setDefault((EndPoint st, String txt) -> {
      NetMsg msg = new Gson().fromJson(txt, NetMsg.class);
      Iterator<?> body = msg.getIterator();
      int j = 0;
      switch (msg.getType()) {
      case "initService":
        JPanel panel = new JPanel(new GridLayout(0, 2));
        while(body.hasNext()) {
          Action action = new AbstractAction((String)body.next()) {
            @Override
            public void actionPerformed(ActionEvent e) {
              sendMsg((String)getValue(Action.ACTION_COMMAND_KEY));
            }
          };
          action.putValue(Action.ACTION_COMMAND_KEY, Integer.toString(j));
          buttons.add(new JButton(action));
          labels.add(new JLabel((String)body.next()));
          panel.add(buttons.get(j));
          panel.add(labels.get(j));
          j++;
        }
        content.add(panel, BorderLayout.CENTER);
        start(true);
        break;
      case "updateService":
        while(body.hasNext()) {
          body.next();
          labels.get(j).setText((String)body.next());
          j++;
        }
        break;
      case "startService":
        ct.setDefault(this);
        break;
      }
    });
    ct.start();
    NetMsg msg = new NetMsg(-1, "initService", gameName, name);
    sendMsg(msg);
  }
  public void sendMsg(NetMsg m) {
    ct.sendMsg(m);
  }

  public EndPoint getEndPoint() {
    return ct;
  }
  private void sendMsg(String nRow) {
    buttons.forEach(b -> b.setEnabled(false));
    sendMsg(new NetMsg(-1, "updateService", gameName, nRow));
  }
}
