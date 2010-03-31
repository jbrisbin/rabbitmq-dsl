package com.jbrisbin.groovy.mqdsl;

import groovy.util.GroovyScriptEngine;

/**
 * Created by IntelliJ IDEA. User: jbrisbin Date: Mar 31, 2010 Time: 10:21:41 AM To change this template use File |
 * Settings | File Templates.
 */
public class RabbitMQScript {

  private String href;
  private String source;
  private GroovyScriptEngine groovyEngine;

  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public GroovyScriptEngine getGroovyEngine() {
    return groovyEngine;
  }

  public void setGroovyEngine(GroovyScriptEngine groovyEngine) {
    this.groovyEngine = groovyEngine;
  }
}
