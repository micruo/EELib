/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import com.rtsoft.dbLib.Entity;
import com.rtsoft.swingEx.table.Column;

/**
 *
 * @author micheleruotolo
 */
@Entity
public class Cfg {
  @Column(key = Column.Key.key) private String key;
  private String value;

  public String getValue() {
    return value;
  }
  
}
