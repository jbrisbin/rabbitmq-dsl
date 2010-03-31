package com.jbrisbin.groovy.mqdsl;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

/**
 * Created by IntelliJ IDEA. User: jbrisbin Date: Mar 31, 2010 Time: 11:34:35 AM To change this template use File |
 * Settings | File Templates.
 */
public class Message {

  private Envelope envelope;
  private AMQP.BasicProperties properties;
  private byte[] body;

  public Envelope getEnvelope() {
    return envelope;
  }

  public void setEnvelope(Envelope envelope) {
    this.envelope = envelope;
  }

  public AMQP.BasicProperties getProperties() {
    return properties;
  }

  public void setProperties(AMQP.BasicProperties properties) {
    this.properties = properties;
  }

  public byte[] getBody() {
    return body;
  }

  public String getBodyAsString() {
    return new String(body);
  }

  public void setBody(byte[] body) {
    this.body = body;
  }

  @Override
  public String toString() {
    StringBuffer buff = new StringBuffer();
    if (null != envelope) {
      buff.append("Envelope: ").append(envelope.toString()).append(", ");
    } else {
      buff.append("Envelope: <NULL>, ");
    }
    buff.append("Properties: ").append(properties.toString()).append(", body: ");
    if (null != body) {
      buff.append(new String(body));
    } else {
      buff.append("<NULL>");
    }
    return buff.toString();
  }
}
