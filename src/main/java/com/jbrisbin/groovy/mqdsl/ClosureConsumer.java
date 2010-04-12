/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jbrisbin.groovy.mqdsl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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

  public ClosureConsumer(Connection connection) throws IOException {
    channel = connection.createChannel();
  }

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

  public void monitorQueue(String queue) {
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

      int keepListening = 0;
      for (Closure cl : delegates) {
        try {
          // Set logger such that we can identify the output
          cl.setProperty("log",
              LoggerFactory.getLogger("msg-" + delivery.getEnvelope().getDeliveryTag() + "-" + cl.getProperty("name")));
          Object o = cl.call(new Object[]{msg});
          if (o instanceof Boolean) {
            if (((Boolean) o).booleanValue()) {
              keepListening++;
            }
          } else if (null != o) {
            keepListening++;
          }
        } catch (Throwable t) {
          parent.dispatchError(t);
        }
      }
      setActive(keepListening > 0);
    }
    synchronized (parent.getClosureConsumers()) {
      parent.getClosureConsumers().remove(this);
    }
    channel.close();

    return this;
  }

}
