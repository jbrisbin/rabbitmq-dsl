## Installing

Download the latest binary package:

http://github.com/jbrisbin/rabbitmq-dsl/downloads

Unzip in your desired location:

<pre><code>cd /opt
sudo tar -zxvf ~/rabbitmq-dsl-1.0.x.tar.gz</code></pre>

Get the list of possible command-line switches:

<pre><code>cd rabbitmq-dsl-1.0.x
bin/mqdsl -?</code></pre>

## Building

Clone the source code:

<pre><code>git clone git://github.com/jbrisbin/rabbitmq-dsl.git</code></pre>

Build a jar file of the command-line DSL runner:

<pre><code>cd rabbitmq-dsl
mvn -Dmaven.test.skip=true package</code></pre>

Copy the dependencies into a directory called "lib":

<pre><code>mkdir lib
mvn dependency:copy-dependencies -DoutputDirectory=lib</code></pre>

Then create a directory for your log files (or edit the bash script to put
log files elsewhere):

<pre><code>mkdir log</code></pre>

These directories are not tracked in Git, so you have to keep them updated
manually (if anyone wants to contribute a patch to do this automagically,
it would be appreciated! :).

To use the DSL runner, execute the bash script located in "./bin":

<pre><code>bin/mqdsl -?</code></pre>

...will give you a run-down on what options are available.

#### Note on login information:

If you create a file in your home directory called ".rabbitmqrc" and place
the following properties inside it, you don't have to specify them on the
command line:

<pre><code>vi ~/.rabbitmqrc
...
mq.host = rabbitmqserver.mycompany.com
mq.port = 5672
mq.user = mquser
mq.password = mqpass
mq.virtualhost = /</code></pre>

#### Notes:

* There is a unit test that will run in the normal Maven build stage. Look in
the src/test/groovy/rabbitmq-test.g file to see the full text of the DSL test.
To run the test, you'll need to specify the property "log.dir" (in the bash
script, defaults to "./log").

## RabbitMQ Groovy DSL

The Groovy DSL (Domain Specific Language) for RabbitMQ allows you to mix standard
Groovy code with a builder-style syntax for working with a RabbitMQ server. For
example, consider the following:

<pre><code>mq.exchange(name: "test", durable: false, autoDelete: true) {
  // Named, non-durable queue
  queue name: "test", routingKey: "test.key", {
    consume tag: "test", onmessage: {msg ->
      log.info(msg.bodyAsString)
    }
  }
}</code></pre>

#### This example uses the RabbitMQ DSL to:

1. Declares a non-durable, autodelete exchange named "test"
2. Declares a named, non-durable, autodelete queue named "test" and binds
    that queue to the enclosing exchange.
3. Creates a queueing consumer (tagged with "test") that executes the
    "onmessage" closure every time a message is received.

## Usage:

The Groovy DSL is intended to be used to create compact, self-documenting,
RabbitMQ code similar to what ActiveMQ users have with Camel. Nesting queues
within exchanges ensures that an exchange exists and that all queues are
properly bound before your Groovy code is actually run.

If you want an anonymous, server-generated queue name, set the "name" property
to "null":

<pre><code>mq.exchange(name: "test", durable: false, autoDelete: true) {
  // Anonymous, non-durable queue
  queue name: null, routingKey: "test.key", {
    consume tag: "test", onmessage: {msg ->
      log.info(msg.bodyAsString)
    }
  }
}</code></pre>

To publish messages, use the "publish" node:

<pre><code> // Publish some messages
  queue routingKey: "test.key", {
    publish body: "this is a test"
  }</code></pre>

If you set the "body" property to a string, the builder will call .getBytes()
on it and set the body of the message to that. If you set the "body" property
to a real byte array (maybe you read in a file or posted a file, or something),
it will blindly use what you give it. If you want to pass a closure, you can
do that too:

<pre><code>  queue routingKey: "test.key", {
    publish myHeaderValue: "customHeader", body: {msg, out ->
      msg.properties.contentType = "text/plain"
      out.write("these are test bytes".bytes)
    }
  }</code></pre>

The "msg" variable passed to your closure is actually a lightweight bean
wrapper that puts an instance of AMQP.BasicProperties ("properties"), an AMQP
Envelope ("envelope"), and the body ("body" and "bodyAsString") onto a single
object, which makes the bean introspection in Groovy happier.

If you want to run arbitrary Groovy code and access the RabbitMQ Channel object
directly, declare a closure like so:

<pre><code>mq {channel ->
  // Do channel-specific stuff here...
  channel.exchangeDelete("test.exchange")
}</code></pre>

#### Notes:

* Returning a non-null or "true" result from the closure specified on the
   "onmessage" property causes the consumer to keep listening for messages until
   the closure returns a false or &lt;NULL&gt; value.

## License:

The RabbitMQ DSL is licensed under the Apache 2 license (included).