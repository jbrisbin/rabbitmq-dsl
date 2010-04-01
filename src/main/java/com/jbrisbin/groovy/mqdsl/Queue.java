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

import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: jbrisbin Date: Mar 31, 2010 Time: 11:34:26 AM To change this template use File |
 * Settings | File Templates.
 */
public class Queue {

  private String name;
  private boolean durable = false;
  private boolean autoDelete = true;
  private boolean passive = false;
  private boolean exclusive = false;
  private String routingKey = null;
  private Map<String, Object> parameters = null;

  public Queue() {
  }

  public Queue(String name, boolean durable, boolean autoDelete) {
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

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean exclusive) {
    this.exclusive = exclusive;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }
}
