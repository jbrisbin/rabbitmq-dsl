package com.jbrisbin.groovy.mqdsl;

import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: jbrisbin Date: Mar 31, 2010 Time: 11:34:21 AM To change this template use File |
 * Settings | File Templates.
 */
public class Exchange {

  private String name = "";
  private String type = "direct";
  private boolean durable = false;
  private boolean autoDelete = true;
  private boolean passive = false;
  private Map<String, Object> parameters = null;

  public Exchange() {
  }

  public Exchange(String name, boolean durable, boolean autoDelete) {
    this.name = name;
    this.durable = durable;
    this.autoDelete = autoDelete;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isDurable() {
    return durable;
  }

  public void setDurable(boolean durable) {
    this.durable = durable;
  }

  public boolean isAutoDelete() {
    return autoDelete;
  }

  public void setAutoDelete(boolean autoDelete) {
    this.autoDelete = autoDelete;
  }

  public boolean isPassive() {
    return passive;
  }

  public void setPassive(boolean passive) {
    this.passive = passive;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }
}
