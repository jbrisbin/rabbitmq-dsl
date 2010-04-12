package com.jbrisbin.groovy.mqdsl;

import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jbrisbin
 * Date: Mar 31, 2010
 * Time: 8:34:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class RabbitMQDslTest {

  @Test
  public void testRabbitMQDsl() {

    String host = System.getProperty( "mq.host", "localhost" );
    String port = System.getProperty( "mq.port", "5672" );
    String username = System.getProperty( "mq.user", "guest" );
    String password = System.getProperty( "mq.password", "guest" );
    String vhost = System.getProperty( "mq.virtualhost", "/" );

    RabbitMQDsl.main(
        new String[]{"-h", host, "-p", port, "-v", vhost, "-U", username, "-P", password, "-f", "src/test/groovy/rabbitmqtest.g"} );

  }

}
