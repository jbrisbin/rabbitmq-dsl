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

		RabbitMQDsl.main(new String[]{"-f", "src/test/groovy/rabbitmqtest.groovy"});

	}

}
