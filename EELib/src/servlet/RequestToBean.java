/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import com.rtsoft.swingEx.table.FieldsInfo;
import com.rtsoft.utils.Utils;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 *
 * @author michele
 */
public class RequestToBean {
  private final static SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");
  private final static DateTimeFormatter dtd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private final static DateTimeFormatter dtft = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
  private final HttpServletRequest request;

  public RequestToBean(HttpServletRequest request) {
    this.request = request;
  }

  public HttpServletRequest getRequest() {
    return request;
  }
  private void get(Class<?> clazz, Object p) {
    FieldsInfo fields = FieldsInfo.getInstance(clazz);
    for(FieldsInfo.FieldInfo fi : fields.getFields()) {
      try {
        fi.getField().setAccessible(true);
        try {
          Method m = clazz.getMethod("get" + Utils.capitalize(fi.getName()), HttpServletRequest.class);
          fi.getField().set(p, m.invoke(p, request));
          continue;
        } catch (NoSuchMethodException | SecurityException | InvocationTargetException ex) {
          // ok
        }
        String s = request.getParameter(fi.getName());
        if(s == null)
          continue;
        switch(fi.getType()) {
        case BOOL:
          fi.getField().setBoolean(p, !s.isEmpty());
          break;
        case INTEGER:
          fi.getField().setInt(p, Integer.parseInt(s));
          break;
        case DOUBLE:
          fi.getField().setDouble(p, Double.parseDouble(s));
          break;
        case SHORT:
          fi.getField().setShort(p, Short.parseShort(s));
          break;
        case BYTE:
          fi.getField().setByte(p, Byte.parseByte(s));
          break;
        case LONG:
          fi.getField().setLong(p, Long.parseLong(s));
          break;
        case DATE:
          fi.getField().set(p, df.parse(s));
          break;
        case LDATETIME:
          fi.getField().set(p, dtft.parse(s, LocalDateTime::from));
          break;
        case LDATE:
          fi.getField().set(p, dtd.parse(s, LocalDate::from));
          break;
        case POINT:
          //fi.getField().set(p, new Point(rs.getInt(fi.getName()+"x"), rs.getInt(fi.getName()+"y")));
          break;
        case RPOINT:
          //fi.getField().set(p, new RealPoint(rs.getDouble(fi.getName()+"x"), rs.getDouble(fi.getName()+"y")));
          break;
        case ENUM:
          fi.getField().set(p, Enum.valueOf(fi.getField().getType().asSubclass(Enum.class), s));
          break;
        default:
          fi.getField().set(p, s);
          break;
        }
      } catch (IllegalArgumentException | IllegalAccessException | ParseException ex) {
      }
    }
  }
  public void read(Object obj) {
    get(obj.getClass(), obj);
  }
}
