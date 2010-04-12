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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Encapsulate functionality to send a basic AMQP message via Groovy code.
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
@SuppressWarnings({"unchecked"})
public class PublishClosure extends Closure {

  private Logger log = LoggerFactory.getLogger( getClass() );
  private Connection connection;

  public PublishClosure( Object owner, Connection connection ) {
    super( owner );
    this.connection = connection;
  }

  @Override
  public Object call( Object[] args ) {
    if ( args.length < 2 ) {
      return null;
    }

    String exchange = args[0].toString();
    String routingKey = args[1].toString();
    Map headers = null;
    byte[] body = null;
    for ( int i = 2; i < args.length; i++ ) {
      if ( args[i] instanceof Map ) {
        headers = (Map) args[i];
      } else if ( args[i] instanceof byte[] ) {
        body = (byte[]) args[i];
      }
    }

    AMQP.BasicProperties properties = new AMQP.BasicProperties();
    if ( null != headers ) {
      if ( headers.containsKey( "contentType" ) ) {
        properties.setContentType( headers.remove( "contentType" ).toString() );
      }
      if ( headers.containsKey( "correlationId" ) ) {
        properties.setCorrelationId( headers.remove( "correlationId" ).toString() );
      }
      if ( headers.containsKey( "replyTo" ) ) {
        properties.setReplyTo( headers.remove( "replyTo" ).toString() );
      }
      if ( headers.containsKey( "contentEncoding" ) ) {
        properties.setContentEncoding( headers.remove( "contentEncoding" ).toString() );
      }
      properties.setHeaders( headers );
    }

    try {
      Channel channel = connection.createChannel();
      channel.basicPublish( exchange, routingKey, properties, body );
      channel.close();
    } catch ( IOException e ) {
      log.error( e.getMessage(), e );
    }


    return this;
  }
}
