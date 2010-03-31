package com.jbrisbin.groovy.mqdsl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by IntelliJ IDEA. User: jbrisbin Date: Mar 31, 2010 Time: 1:18:04 PM To change this template use File |
 * Settings | File Templates.
 */
public class ClosureConsumer implements Callable<ClosureConsumer> {

  private Logger log = LoggerFactory.getLogger(getClass());
  private List<Closure> delegates = new ArrayList<Closure>();
  private BlockingQueue<QueueingConsumer.Delivery> incomingQueue = new LinkedBlockingQueue<QueueingConsumer.Delivery>();
  private QueueingConsumer queueingConsumer;
  private Channel channel;
  private String consumerTag = null;
  private boolean ack = true;
  private boolean active = true;
  private RabbitMQBuilder parent;

  public List<Closure> getDelegates() {
    return delegates;
  }

  public void addDelegate(Closure delegate) {
    this.delegates.add(delegate);
  }

  public BlockingQueue<QueueingConsumer.Delivery> getIncomingQueue() {
    return incomingQueue;
  }

  public QueueingConsumer getQueueingConsumer() {
    return queueingConsumer;
  }

  public Channel getChannel() {
    return channel;
  }

  public void monitorQueue(Channel channel, String queue) {
    this.channel = channel;
    this.queueingConsumer = new QueueingConsumer(channel, incomingQueue);
    try {
      if (null != consumerTag) {
        channel.basicConsume(queue, false, consumerTag, queueingConsumer);
      } else {
        channel.basicConsume(queue, queueingConsumer);
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  public String getConsumerTag() {
    return consumerTag;
  }

  public void setConsumerTag(String consumerTag) {
    this.consumerTag = consumerTag;
  }

  public boolean isAck() {
    return ack;
  }

  public synchronized void setAck(boolean ack) {
    this.ack = ack;
  }

  public boolean isActive() {
    return active;
  }

  public synchronized void setActive(boolean active) {
    this.active = active;
  }

  public RabbitMQBuilder getParent() {
    return parent;
  }

  public void setParent(RabbitMQBuilder parent) {
    this.parent = parent;
  }

  public ClosureConsumer call() throws Exception {
    while (active) {
      QueueingConsumer.Delivery delivery = incomingQueue.take();
      if (ack) {
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      }
      Message msg = new Message();
      msg.setEnvelope(delivery.getEnvelope());
      msg.setProperties(delivery.getProperties());
      msg.setBody(delivery.getBody());

      for (Closure cl : delegates) {
        try {
          // Set logger such that we can identify the output
          cl.setProperty("log",
              LoggerFactory.getLogger("msg-" + delivery.getEnvelope().getDeliveryTag() + "-" + cl.getProperty("name")));
          Object o = cl.call(new Object[]{msg});
          if (o instanceof Boolean) {
            setActive((Boolean) o);
          } else if (null == o) {
            setActive(false);
          }
        } catch (Throwable t) {
          parent.dispatchError(t);
        }
      }
    }
    return this;
  }
}
