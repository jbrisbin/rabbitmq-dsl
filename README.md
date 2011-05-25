## Installing

Download the latest binary package:

http://github.com/jbrisbin/rabbitmq-dsl/downloads

Unzip in your desired location:

    cd /opt
    sudo tar -zxvf ~/rabbitmq-dsl-1.x.tar.gz

Get the list of possible command-line switches:

    cd rabbitmq-dsl-1.x
    bin/mqdsl -?

## Building

Clone the source code:

    git clone git://github.com/jbrisbin/rabbitmq-dsl.git

Build a jar file of the command-line DSL runner:

    cd rabbitmq-dsl
    mvn -Dmaven.test.skip=true package

Copy the dependencies into a directory called "lib":

    mkdir lib
    mvn dependency:copy-dependencies -DoutputDirectory=lib

Then create a directory for your log files (or edit the bash script to put log files elsewhere):

    mkdir log

These directories are not tracked in Git, so you have to keep them updated manually if you're building from source.

To use the DSL runner, execute the bash script located in "./bin":

    bin/mqdsl -?

...will give you a run-down on what options are available.

You really, really, really should look at the test file to get a sense of what the DSL expects.

#### Note on login information:

If you create a file in your home directory called ".rabbitmqrc" and place the following properties inside it, you don't
have to specify them on the command line:

    vi ~/.rabbitmqrc
    ...
    mq.host = rabbitmqserver.mycompany.com
    mq.port = 5672
    mq.user = mquser
    mq.password = mqpass
    mq.virtualhost = /

#### Notes:

* There is a unit test that will run in the normal Maven build stage. Look in the src/test/groovy/rabbitmq-test.groovy
file to see the full text of the DSL test. To run the test, you'll need to specify the property "log.dir" (in the bash
script, defaults to "./log").

## RabbitMQ Groovy DSL

The Groovy DSL (Domain Specific Language) for RabbitMQ allows you to mix standard Groovy code with a builder-style
syntax for working with a RabbitMQ server. For example, consider the following:

    mq.exchange(name: "test", type: "direct") {
    	// Named, non-durable queue
    	queue(name: "test", routingKey: "test.key") {
    		consume(ack: "auto") { msg ->
    			def body = new String(msg.body)
    			println "body: ${body}"
    		}
    	}
    }

#### This example uses the RabbitMQ DSL to:

1. Declare a non-durable, auto-delete exchange named "test"
2. Declare a named, non-durable, auto-delete queue named "test" and binds that queue to the enclosing exchange using
		the routing key "test.key".
3. Creates a Spring AMQP SimpleMessageListenerContainer that executes the body of the closure every time a message is
		received.

## Usage:

The Groovy DSL is intended to be used to create compact, self-documenting code similar to what ActiveMQ users have with
Camel. Nesting queues within exchanges ensures that an exchange exists and that all queues are properly bound before
your Groovy code is actually run.

If you want an anonymous, server-generated queue name, set the "name" property to "null":

    mq.exchange(name: "test") {
      // Anonymous, non-durable queue
    	queue(name: null, routingKey: "test.key") {
        consume { msg ->
          println msg.messageProperties
        }
      }
    }

If you want to consume messages, you can specify an event name on an "onmessage" property when you define "consume" or
whatever you define inside that Closure will be invoked whenever a message is received. Your closure will be passed a
Spring AMQP Message object which has the message properties on the "messageProperties" property and the byte array (the
body of the message) on the "body" property.

Check the rabbitmqtest.groovy file for examples of both styles of creating consumers.

To publish messages, use the "publish" node (assume this node is defined inside the above
"exchange" node, so that our exchange name can be implied):

    publish(routingKey: "test.key") {
    	"this is a test"
    }

There are a couple variations on publishing to give you some flexibility. You can return a string from the Closure and
that will be used as the body of the message. You can also define a parameter that will accept a ByteArrayOutputStream
that your closure can write to:

    publish(routingKey: "test.key") { out ->
    	out.write("this is a test".bytes)
    }

You can specify parameters like contentType, correlationId, replyTo, etc... by specifying them along with routingKey:

    publish(routingKey: "test.key", contentType: "text/plain") { out ->
    	out.write("this is a test".bytes)
    }

Anything it doesn't understand as a common AMQP header will be passed through as a generic application header:

    publish(routingKey: "test.key", myHeader: "my custom header value") { out ->
    	out.write("this is a test".bytes)
    }

You can publish from anywhere, even inside a consume Closure (noticed we're replying here, so setting the exchange
name to "" rather than letting it be picked up from the enclosing exchange Closure we're nested in):

    consume(ack: "auto") { msg ->
    	// Do work to generate a reply...
    	publish(exchange: "", routingKey: msg.messageProperties.replyTo) {
    		"this is the reply"
    	}
    }

If you want to run arbitrary Groovy code and access the RabbitMQ Channel object directly, declare a closure like so:

    mq { channel ->
      // Do channel-specific stuff here...
      channel.exchangeDelete("test.exchange")
    }

## Helper scripts

To facilitate re-using bits of code, you might want to put commonly-used functions inside helper scripts. By default,
the DSL command-line client will look in your user's HOME/.mqdsl.d/ directory for *.groovy files and "sources" them into
your current context. To change this behaviour, define an environment variable named MQDSL_INCLUDE. Each path element can
be a directory in which to search for scripts, or can be the path to an actual script:

    export MQDSL_INCLUDE=$HOME/.mqdsl.d:$HOME/src/groovy/utils.groovy

When the file is evaluated, it doesn't put them in the root context. It uses the name of the file as the variable name.
e.g. create a file in ~/.mqdsl.d/helper.groovy. In that file, put:

    def format(msg, Object... args) {
      println String.format(msg, args)
    }

and in your DSL file, use it like so:

    helper.format("[DEBUG]: %s", "debug data")

When you run your DSL file, in the console you should see:

    [DEBUG]: debug data


#### Notes:

* Returning a non-null or "true" result from the closure specified on the consumer causes it to keep listening for
		messages until the closure returns a false or &lt;NULL&gt; value.

## License:

The RabbitMQ DSL is licensed under the Apache 2 license (included).