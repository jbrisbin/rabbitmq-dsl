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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.rabbitmq.client.Channel;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyObjectSupport;
import groovy.util.BuilderSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.util.StringUtils;

/**
 * Created by IntelliJ IDEA. User: jbrisbin Date: Mar 31, 2010 Time: 10:16:03 AM To change this template use File |
 * Settings | File Templates.
 */
@SuppressWarnings({"unchecked"})
public class RabbitMQBuilder extends BuilderSupport {

	static final String BEFORE_PUBLISH = "beforePublish";
	static final String AFTER_PUBLISH = "afterPublish";
	static final String EXCHANGE = "exchange";
	static final String ROUTING_KEY = "routingKey";
	static final String DURABLE = "durable";
	static final String AUTO_DELETE = "autoDelete";
	static final String EXCLUSIVE = "exclusive";
	static final String ARGUMENTS = "arguments";
	static final String NAME = "name";
	static final String TYPE = "type";
	static final String DIRECT = "direct";
	static final String TOPIC = "topic";
	static final String FANOUT = "fanout";
	static final String HEADERS = "headers";
	static final String ON = "on";
	static final String QUEUE = "queue";
	static final String CONSUME = "consume";
	static final String PUBLISH = "publish";
	static final String ON_MESSAGE = "onmessage";
	static final String ACK = "ack";
	static final String CALL = "call";
	static final String ERROR = "error";

	Logger log = LoggerFactory.getLogger(getClass());
	ConnectionFactory connectionFactory;
	ConcurrentLinkedQueue<SimpleMessageListenerContainer> listenerContainers = new ConcurrentLinkedQueue<SimpleMessageListenerContainer>();
	RabbitTemplate rabbitTemplate;
	RabbitAdmin rabbitAdmin;
	Exchange currentExchange;
	Queue currentQueue;
	String currentRoutingKey = null;
	List<Channel> activeChannels = new LinkedList<Channel>();
	Map<String, List<Closure>> eventHandlers = new LinkedHashMap<String, List<Closure>>();

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		this.rabbitTemplate = new RabbitTemplate(connectionFactory);
		this.rabbitAdmin = new RabbitAdmin(connectionFactory);
	}

	public boolean isActive() {
		for (SimpleMessageListenerContainer c : listenerContainers) {
			if (c.isActive()) {
				return true;
			}
		}
		return false;
	}

	public void cancelAllConsumers() {
		for (SimpleMessageListenerContainer c : listenerContainers) {
			c.shutdown();
		}
	}

	public void call(Closure cl) {
		Channel channel = connectionFactory.createConnection().createChannel(false);
		cl.call(new Object[]{channel});
		activeChannels.add(channel);
	}

	public void close() {
		for (Channel channel : activeChannels) {
			try {
				channel.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		for (SimpleMessageListenerContainer c : listenerContainers) {
			c.shutdown();
		}
	}

	@Override
	protected void nodeCompleted(Object parent, Object node) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("nodeCompleted(Object parent, Object node): %s, %s", parent, node));
		}
		super.nodeCompleted(parent, node);
	}

	@Override
	protected void setParent(Object from, Object to) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setParent(Object o, Object o1): %s, %s", from, to));
		}
		if (from instanceof Exchange) {
			currentExchange = (Exchange) from;
		}
		if (from instanceof Queue) {
			currentQueue = (Queue) from;
		}
	}

	@Override
	protected Object createNode(Object o) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("createNode(Object o): %s", o));
		}
		if (o instanceof Consume || o instanceof Publish) {
			return o;
		}
		return null;
	}

	@Override
	protected Object createNode(Object node, Object arg) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setParent(Object node, Object arg): %s, %s", node, arg));
		}
		if (arg instanceof Consume || arg instanceof Publish) {
			return arg;
		}

		return null;
	}

	@Override
	protected Object createNode(Object o, final Map params) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("createNode(Object o, Map params): %s, %s", o, params));
		}
		String node = o.toString();
		if (ON.equals(node)) {
			// Event handlers
			for (Map.Entry<String, Closure> entry : ((Map<String, Closure>) params).entrySet()) {
				String eventName = entry.getKey();
				List<Closure> handlers;
				if (eventHandlers.containsKey(eventName)) {
					handlers = eventHandlers.get(eventName);
				} else {
					handlers = new ArrayList<Closure>();
					eventHandlers.put(eventName, handlers);
				}
				Closure cl = entry.getValue();
				cl.setProperty(NAME, eventName);
				handlers.add(cl);
			}
			return null;
		} else if (EXCHANGE.equals(node)) {
			boolean durable = params.containsKey(DURABLE) ? (Boolean) params.get(DURABLE) : false;
			boolean autoDelete = params.containsKey(AUTO_DELETE) ? (Boolean) params.get(AUTO_DELETE) : false;
			Map arguments = params.containsKey(ARGUMENTS) ? (Map) params.get(ARGUMENTS) : null;
			currentRoutingKey = null;

			Exchange exchange = null;
			String name = null;
			if (params.containsKey(NAME)) {
				name = params.get(NAME).toString();
			}
			if (params.containsKey(TYPE)) {
				String type = params.containsKey(TYPE) ? params.get(TYPE).toString() : DIRECT;
				if (DIRECT.equals(type)) {
					exchange = new DirectExchange(name, durable, autoDelete, arguments);
				} else if (TOPIC.equals(type)) {
					exchange = new TopicExchange(name, durable, autoDelete, arguments);
				} else if (FANOUT.equals(type)) {
					exchange = new FanoutExchange(name, durable, autoDelete, arguments);
				} else if (HEADERS.equals(type)) {
					exchange = new HeadersExchange(name, durable, autoDelete, arguments);
				}
				currentExchange = exchange;

				try {
					rabbitAdmin.declareExchange(exchange);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					dispatchError(e);
				}
			} else {
				currentExchange = new DirectExchange(name);
			}
			return exchange;
		} else if (QUEUE.equals(node)) {
			boolean durable = params.containsKey(DURABLE) ? (Boolean) params.get(DURABLE) : false;
			boolean autoDelete = params.containsKey(AUTO_DELETE) ? (Boolean) params.get(AUTO_DELETE) : true;
			String routingKey = params.containsKey(ROUTING_KEY) ? params.get(ROUTING_KEY).toString() : null;
			if (null != routingKey) {
				currentRoutingKey = routingKey;
			}
			boolean exclusive = params.containsKey(EXCLUSIVE) ? (Boolean) params.get(EXCLUSIVE) : false;
			Map arguments = params.containsKey(ARGUMENTS) ? (Map) params.get(ARGUMENTS) : null;

			Queue q = null;
			String name;
			if (params.containsKey(NAME)) {
				try {
					if (null == params.get(NAME)) {
						name = null;
					} else {
						name = params.get(NAME).toString();
					}

					if (name == null) {
						q = rabbitAdmin.declareQueue();
					} else {
						q = new Queue(name, durable, exclusive, autoDelete);
						rabbitAdmin.declareQueue(q);
					}
					currentQueue = q;

					if (null != currentExchange) {
						Binding binding = null;
						if (currentExchange instanceof FanoutExchange) {
							binding = new Binding(q, (FanoutExchange) currentExchange);
						} else if (currentExchange instanceof DirectExchange) {
							binding = new Binding(q, (DirectExchange) currentExchange, routingKey);
						} else if (currentExchange instanceof HeadersExchange) {
							binding = new Binding(q, (HeadersExchange) currentExchange, arguments);
						} else if (currentExchange instanceof TopicExchange) {
							binding = new Binding(q, (TopicExchange) currentExchange, routingKey);
						}

						if (null != binding) {
							rabbitAdmin.declareBinding(binding);
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					dispatchError(e);
				}
			}
			return q;
		}

		return null;
	}

	@Override
	protected Object createNode(Object o, Map map, Object o1) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("createNode(Object o, Map map, Object o1): %s, %s, %s", o, map, o1));
		}
		return null;
	}

	@Override
	public Object invokeMethod(String methodName, Object args) {
		if (CONSUME.equals(methodName)) {
			Consume consume = new Consume();
			SimpleMessageListenerContainer listenerContainer = consume.getListenerContainer();
			Object[] params = (Object[]) args;
			for (Object param : params) {
				if (param instanceof Map) {
					Map paramMap = (Map) param;

					if (paramMap.containsKey(ON_MESSAGE)) {
						Object onMessage = paramMap.get(ON_MESSAGE);
						if (onMessage instanceof String) {
							consume.setEventName((String) onMessage);
						} else if (onMessage instanceof Closure) {
							consume.setDelegate((Closure) onMessage);
						}
					}

					if (paramMap.containsKey(ACK)) {
						AcknowledgeMode mode = AcknowledgeMode.valueOf(paramMap.get(ACK).toString().toUpperCase());
						listenerContainer.setAcknowledgeMode(mode);
					} else {
						listenerContainer.setAcknowledgeMode(AcknowledgeMode.AUTO);
					}

				} else if (param instanceof Closure) {
					consume.setDelegate((Closure) param);
				}
			}
			listenerContainer.setQueues(currentQueue);
			listenerContainer.afterPropertiesSet();
			listenerContainer.start();
			listenerContainers.add(listenerContainer);

			return super.invokeMethod(methodName, consume);
		} else if (PUBLISH.equals(methodName)) {
			Publish publish = new Publish();
			publish.invokeMethod(CALL, args);
			return super.invokeMethod(methodName, publish);
		}

		return super.invokeMethod(methodName, args);
	}

	void dispatchError(Throwable t) {
		dispatchEvent(ERROR, new Object[]{t});
	}

	void dispatchEvent(String name, Object[] args) {
		if (log.isDebugEnabled()) {
			log.debug("Dispatching event " + name + " with args: " + arrayToString(args));
		}
		if (eventHandlers.containsKey(name)) {
			for (Closure cl : eventHandlers.get(name)) {
				cl.call(args);
			}
		}
	}

	String arrayToString(Object[] o) {
		StringBuffer buff = new StringBuffer("[");
		for (int i = 0; i < o.length; i++) {
			if (i > 0) {
				buff.append(",");
			}
			buff.append(String.valueOf(o[i]));
		}
		buff.append("]");
		return buff.toString();
	}

	class Consume extends GroovyObjectSupport {

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		String eventName = null;
		Closure delegate = null;

		Consume() {
		}

		public String getEventName() {
			return eventName;
		}

		public void setEventName(String eventName) {
			this.eventName = eventName;
			listenerContainer.setMessageListener(new EventInvokingMessageListener(this));
		}

		public Closure getDelegate() {
			return delegate;
		}

		public void setDelegate(Closure delegate) {
			this.delegate = delegate;
			listenerContainer.setMessageListener(new ClosureInvokingMessageListener(this));
		}

		public SimpleMessageListenerContainer getListenerContainer() {
			return listenerContainer;
		}

		public void shutdown() {
			listenerContainer.shutdown();
			listenerContainers.remove(listenerContainer);
		}
	}

	class ClosureInvokingMessageListener implements MessageListener {

		Consume consume;
		Object lastResult = null;

		ClosureInvokingMessageListener(Consume consume) {
			this.consume = consume;
		}

		public Object getLastResult() {
			return lastResult;
		}

		@Override
		public void onMessage(Message message) {
			if (null != consume.getDelegate()) {
				lastResult = consume.getDelegate().call(message);
				if (null == lastResult || (lastResult instanceof Boolean && !((Boolean) lastResult).booleanValue())) {
					consume.shutdown();
				}
			}
		}
	}

	class EventInvokingMessageListener implements MessageListener {

		Consume consume;

		EventInvokingMessageListener(Consume consume) {
			this.consume = consume;
		}

		@Override
		public void onMessage(Message message) {
			if (null != consume.getEventName()) {
				dispatchEvent(consume.getEventName(), new Object[]{message});
				consume.shutdown();
			}
		}
	}

	class Publish extends GroovyObjectSupport {
		public Object call(Map<String, Object> params, Object bodyObj) throws IOException {
			String exchange = null != currentExchange ? currentExchange.getName() : null;
			if (null != params && params.containsKey(EXCHANGE)) {
				Object oexchange = params.get(EXCHANGE);
				if (null != oexchange && StringUtils.hasText(oexchange.toString())) {
					exchange = oexchange.toString();
				}
			}
			String routingKey = currentRoutingKey;
			if (null != params && params.containsKey(ROUTING_KEY)) {
				Object oroutingKey = params.get(ROUTING_KEY);
				if (null != oroutingKey && StringUtils.hasText(oroutingKey.toString())) {
					routingKey = oroutingKey.toString();
				}
			}
			return call(exchange, routingKey, params, bodyObj);
		}

		public Object call(String exchange, String routingKey, Map<String, Object> headers, Object bodyObj) throws IOException {
			Message msg = createMessage(headers, bodyObj);
			if (null == exchange && null != currentExchange) {
				exchange = currentExchange.getName();
			}
			if (null == routingKey && null != currentRoutingKey) {
				routingKey = currentRoutingKey;
			}

			dispatchEvent(BEFORE_PUBLISH, new Object[]{exchange, routingKey, msg});
			try {
				rabbitTemplate.send(exchange, routingKey, msg);
				dispatchEvent(AFTER_PUBLISH, new Object[]{exchange, routingKey, msg});
			} catch (Exception e) {
				dispatchError(e);
			}


			return this;
		}

		public Object call(Map<String, Object> params, Closure bodyClosure) throws IOException {
			String exchange = params.containsKey(EXCHANGE) ? params.remove(EXCHANGE).toString() : null;
			String routingKey = params.containsKey(ROUTING_KEY) ? params.remove(ROUTING_KEY).toString() : null;
			ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
			Object bodyObj = bodyClosure.call(bytesOut);
			bytesOut.flush();

			byte[] bytes = bytesOut.toByteArray();
			if (bytes.length > 0) {
				return call(exchange, routingKey, params, bytes);
			} else {
				return call(exchange, routingKey, params, bodyObj);
			}
		}

		private MessageProperties createProperties(Map<String, Object> headers) {
			MessageProperties msgProps = new MessageProperties();
			for (Map.Entry<String, Object> entry : headers.entrySet()) {
				String key = entry.getKey();
				String value = null != entry.getValue() ? entry.getValue().toString() : null;
				if ("contentType".equals(key)) {
					msgProps.setContentType(value);
				} else if ("correlationId".equals(key)) {
					msgProps.setCorrelationId(value.getBytes());
				} else if ("replyTo".equals(key)) {
					msgProps.setReplyTo(new Address(value));
				} else if ("contentEncoding".equals(key)) {
					msgProps.setContentEncoding(value);
				} else {
					msgProps.setHeader(key, value);
				}
			}
			return msgProps;
		}

		private Message createMessage(Map<String, Object> params, Object bodyObj) throws IOException {
			MessageProperties msgProps = createProperties(params);
			if (bodyObj instanceof Closure) {
				// If it's a Closure, invoke it to get the value
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Closure cl = (Closure) bodyObj;
				Object returnFromBodyClosure = cl.call(new Object[]{out});
				if (returnFromBodyClosure instanceof Message) {
					return (Message) returnFromBodyClosure;
				} else if (returnFromBodyClosure instanceof String) {
					return new Message(((String) returnFromBodyClosure).getBytes(), msgProps);
				} else {
					out.flush();
					return new Message(out.toByteArray(), msgProps);
				}
			} else if (bodyObj instanceof String || bodyObj instanceof GString) {
				// If it's sort of a String, toString() it
				return new Message(bodyObj.toString().getBytes(), msgProps);
			} else if (bodyObj instanceof byte[]) {
				// If it's raw bytes, don't do anything
				return new Message((byte[]) bodyObj, msgProps);
			} else {
				// Otherwise, write the object out using JDK serialization
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				ObjectOutputStream oout = new ObjectOutputStream(bout);
				oout.writeObject(bodyObj);
				oout.flush();
				oout.close();
				bout.flush();

				return new Message(bout.toByteArray(), msgProps);
			}
		}
	}

}
