# RabbitMQ Groovy DSL

The Groovy DSL (Domain Specific Language) for RabbitMQ allows you to mix standard
Groovy code with a builder-style syntax for working with a RabbitMQ server. For
example, consider the following:

  mq.exchange(name: "test", durable: false, autoDelete: true) {
    // Named, non-durable queue
    queue name: "test", routingKey: "test.key", {
      consume tag: "test", onmessage: {msg ->
        log.info(msg.bodyAsString)
      }
    }
  }

This example uses the RabbitMQ DSL to:

1. Declares a non-durable, autodelete exchange named "test"
2. Declares a named, non-durable, autodelete queue named "test" and binds that queue
   to the enclosing exchange.
3. Creates a queueing consumer (tagged with "test") that executes the "onmessage"
   closure every time a message is received.

## License:

The RabbitMQ DSL is licensed under the Apache 2 license (included).