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
2. Declares a named, non-durable, autodelete queue named "test" and binds that queue
   to the enclosing exchange.
3. Creates a queueing consumer (tagged with "test") that executes the "onmessage"
   closure every time a message is received.

## Usage:

The Groovy DSL is intended to be used to create compact, self-documenting, RabbitMQ
code similar to what ActiveMQ users have with Camel. Nesting queues within exchanges
ensures that an exchange exists and that all queues are properly bound before your
Groovy code is actually run.

If you want to run arbitrary Groovy code and access the RabbitMQ Channel object directly,
declare a closure like so:

<pre><code>mq {channel ->
  // Do channel-specific stuff here...
  channel.exchangeDelete("test.exchange")
}</code></pre>

#### Notes:

* Returning a non-null or "true" result from the closure specified on the "onmessage"
  property causes the consumer to keep listening for messages until the closure returns
  a false or <NULL> value.

## License:

The RabbitMQ DSL is licensed under the Apache 2 license (included).